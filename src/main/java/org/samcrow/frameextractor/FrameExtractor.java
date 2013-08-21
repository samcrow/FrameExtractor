package org.samcrow.frameextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.concurrent.Task;

/**
 * Extracts frames from a video file
 * <p/>
 * @author Sam Crow
 */
public class FrameExtractor extends Task<Void> {

    private final String videoPath;

    private final String outputDirectory;

    /**
     * The frame rate to extract frames at, or -1 if the file's native frame
     * rate
     * should be used
     */
    private double frameRate;

    /**
     * Constructor
     * <p/>
     * @param videoPath Absolute path to the video file to read
     * @param outputDirectory Directory, without a trailing slash, to put still
     * frames in
     * @param frameRate The rate at which to extract frames
     */
    public FrameExtractor(String videoPath, String outputDirectory, double frameRate) {
        this.videoPath = videoPath;
        this.outputDirectory = outputDirectory;
        this.frameRate = frameRate;
    }

    public FrameExtractor(String videoPath, String outputDirectory) {
        this(videoPath, outputDirectory, -1);
    }

    @Override
    protected Void call() throws Exception {

        //Denote indeterminate progress with -1 as the first param
        updateProgress(-1, 1);
        updateTitle("Starting process");

        //Check that the input and ouptut locations exist and are read/write-able
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            throw new FileNotFoundException("Input video file " + videoPath + " does not exist");
        }
        if (!videoFile.canRead()) {
            throw new IOException("Input video file " + videoPath + " is not readable");
        }
        File outDir = new File(outputDirectory);
        if (!outDir.isDirectory()) {

            //Try to create the directory
            if (!outDir.mkdirs()) {

                throw new IOException("Output directory " + outputDirectory + " is not a directory or does not exist, and could not be created");
            }
        }
        if (!outDir.canWrite()) {
            throw new IOException("Output directory " + outputDirectory + " is not writable");
        }

        updateMessage("Getting video information");
        VideoInfo info = getVideoInfo();
        if (frameRate == -1) {
            frameRate = info.frameRate;
        }

        //Save the frame rate to a file
        File frameRateFile = new File(outDir, "frame_rate.txt");
        frameRateFile.createNewFile();
        try (FileWriter writer = new FileWriter(frameRateFile)) {
            writer.append(String.valueOf(frameRate));
            writer.append(System.getProperty("line.separator"));
        }
        
        //Scale the resolution to create an output resolution that fits the video's displayed aspect ratio
        //with a stored aspect ratio of 1:1
        final double inputAspectRatio = info.resolution.horizontal / (double) info.resolution.vertical;
        final double desiredAspectRatio = info.aspectRatio.toDecimal();
        
        final VideoInfo.Resolution newResolution = new VideoInfo.Resolution();
        newResolution.vertical = info.resolution.vertical;
        newResolution.horizontal = (int) Math.round( info.resolution.horizontal * (desiredAspectRatio / inputAspectRatio) );
        

        //Assemble arguments
        ProcessBuilder builder = new ProcessBuilder(FFMpeg.getPath(),
                "-i", videoPath, "-r", String.valueOf(frameRate), "-s", newResolution.toString() , "-f", "image2", outputDirectory + "/" + videoFile.getName() + "_%07d.jpg");
        builder.redirectErrorStream(true);
        Process process = builder.start();

        updateMessage("Extracting video length");

        try (BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));) {

            //A pattern that stores the frame number, frame rate, and time in named capture groups 'frame', 'fps', and 'time'
            final Pattern frameLinePattern = Pattern.compile("frame=\\s*(?<frame>\\d+)\\s*fps=\\s*(?<fps>[.\\d]+)\\s*q=\\s*[.\\d]+\\s*size=\\s*[a-zA-Z/]+\\s*time=\\s*(?<time>[:|.|\\d]+)");
            //A pattern for finding the total duration of the input file, with the time in capture group 1
            final Pattern totalTimePattern = Pattern.compile("Duration:\\s*(\\d{2}:\\d{2}:\\d{2}.\\d{2})");

            //Duration of the video file, in milliseconds
            long duration = 0;

            while (!isCancelled()) {

                String line = output.readLine();
                if (line == null) {
                    break;
                }


                if (duration == 0) {
                    Matcher matcher = totalTimePattern.matcher(line);
                    if (matcher.find()) {
                        String timeString = matcher.group(1);
                        duration = parseInterval(timeString);
                        //Mark duration as the maximum
                        updateProgress(0, duration);
                        updateMessage("Extracting frames");
                    }
                }

                Matcher lineMatcher = frameLinePattern.matcher(line);
                if (lineMatcher.find()) {
                    int fps = Math.round(Float.valueOf(lineMatcher.group("fps")));
                    updateMessage("Extracting frames at " + fps + " frames/second");
                    long currentTime = parseInterval(lineMatcher.group("time"));
                    updateProgress(currentTime, duration);
                }

                System.out.println("Read line: " + line);
            }
        }

        if (isCancelled()) {
            process.destroy();
            updateProgress(-1, 1);
            updateMessage("Cancelled");
        }
        else {
            process.waitFor();
        }

        //Note completion
        updateProgress(1, 1);
        updateMessage("Finished");

        return null;
    }

    /**
     * Holds information on a video file
     */
    private static class VideoInfo {

        public double frameRate;

        /**
         * The display aspect ratio of the video, for example "16:9"
         */
        public AspectRatio aspectRatio = new AspectRatio();
        
        public static class AspectRatio {
            public int numerator;
            public int denominator;
            
            /**
             * Returns a decimal form of this aspect ratio
             * @return 
             */
            public double toDecimal() {
                return numerator / (double) denominator;
            }
        }
        /**
         * The resolution
         */
        public Resolution resolution = new Resolution();
        
        public static class Resolution {
            public int horizontal;
            public int vertical;
            
            @Override
            public String toString() {
                return horizontal + "x" + vertical;
            }
        }

    }

    private VideoInfo getVideoInfo() throws Exception {
        String ffMpegPath = FFMpeg.getPath();
        System.out.println("FFMpeg path " + ffMpegPath);

        ProcessBuilder builder = new ProcessBuilder(ffMpegPath, "-i", videoPath);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        VideoInfo info = new VideoInfo();

        //Read the standard output
        try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {


            //Search for 1-3 digits, followed by a space and then "fps"
            //The number is in capture group 1
            final Pattern frameRatePattern = Pattern.compile("([0-9]{1,3})\\sfps");
            //Search for the aspect ratio and resolution
            //The displayed aspect ratio is in capture groups "aspectn" and "aspectd"
            //The resolution is in capture groups "resx" and "resy"
            final Pattern aspectRatioPattern
                    = Pattern.compile("(?<resx>\\d+)x(?<resy>\\d+) \\[SAR \\d{1,2}:\\d{1,2} DAR (?<aspectn>\\d{1,2}):(?<aspectd>\\d{1,2})\\]");

            while (true) {
                String line = outReader.readLine();

                if (line == null) {
                    //End of stream
                    break;
                }
                //Parse the line
                {
                    Matcher matcher = frameRatePattern.matcher(line);
                    boolean fpsFound = matcher.find();
                    if (fpsFound) {
                        //Extract the FPS number (capture group 1)
                        String fpsString = matcher.group(1);
                        info.frameRate = Double.valueOf(fpsString);
                    }
                }
                {
                    Matcher aspectMatcher = aspectRatioPattern.matcher(line);
                    boolean aspectFound = aspectMatcher.find();
                    if (aspectFound) {
                        info.aspectRatio.numerator = Integer.valueOf(aspectMatcher.group("aspectn"));
                        info.aspectRatio.denominator = Integer.valueOf(aspectMatcher.group("aspectd"));
                        
                        info.resolution.horizontal = Integer.valueOf(aspectMatcher.group("resx"));
                        info.resolution.vertical = Integer.valueOf(aspectMatcher.group("resy"));
                    }
                }
            }
        }
        
        if(info.frameRate == 0 || info.aspectRatio == null) {
            throw new Exception("FFMpeg did not provide the video frame rate and aspect ratio");
        }

        return info;
    }

    /**
     * Parses a time interval of up to 99 hours, 59 minutes, 59 seconds, and 990
     * milliseconds
     * <p/>
     * @param interval an interval in HH:mm:ss:SS format
     * @return The length of the interval in milliseconds
     * @throws ParseException
     */
    private static long parseInterval(String interval) throws ParseException {
        long time = 0;
        final Pattern pattern = Pattern.compile("(?<hours>\\d{2}):(?<minutes>\\d{2}):(?<seconds>\\d{2}).(?<centiseconds>\\d{2})");

        final Matcher matcher = pattern.matcher(interval);
        if (!matcher.find()) {
            throw new ParseException("Interval " + interval + "is not in the required format", 0);
        }
        int hours = Integer.valueOf(matcher.group("hours"));
        int minutes = Integer.valueOf(matcher.group("minutes"));
        int seconds = Integer.valueOf(matcher.group("seconds"));
        int centiseconds = Integer.valueOf(matcher.group("centiseconds"));

        time += 60 * 60 * 1000 * hours;
        time += 60 * 1000 * minutes;
        time += 1000 * seconds;
        time += 10 * centiseconds;

        return time;
    }

}
