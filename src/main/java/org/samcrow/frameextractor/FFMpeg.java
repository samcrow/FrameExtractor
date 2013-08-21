package org.samcrow.frameextractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;

/**
 * Provides access to a stored FFMpeg binary.
 * 
 * This class is not reentrant.
 * @author Sam Crow
 */
public class FFMpeg {
    
    private static final String os = System.getProperty("os.name");
    
    private static final String arch = System.getProperty("sun.arch.data.model");
    
    private static File tempExecutable;
    
    private static final String mac32Path = "resources/ffmpeg-mac32";
    private static final String mac64Path = "resources/ffmpeg-mac64";
    
    private static final String win32Path = "resources/ffmpeg-win32.exe";
    private static final String win64Path = "resources/ffmpeg-win64.exe";
    
    
    private static final String linux32Path = "resources/ffmpeg-linux32";
    private static final String linux64Path = "resources/ffmpeg-linux64";
    
    /**
     * @return an input stream that can be used to read the executable from
     * inside the JAR file and save it to another file
     */
    public static InputStream getInternalStream() {
        
        if(os.contains("MacOS") || os.contains("Mac OS")) {
            if(arch.contains("64")) {
                return FFMpeg.class.getResourceAsStream(mac64Path);
            }
            else {
                return FFMpeg.class.getResourceAsStream(mac32Path);
            }
        }
        else if(os.contains("Windows")) {
            if(arch.contains("64")) {
                return FFMpeg.class.getResourceAsStream(win64Path);
            }
            else {
                return FFMpeg.class.getResourceAsStream(win32Path);
            }
        }
        else {
            //Linux or other
            if(arch.contains("64")) {
                return FFMpeg.class.getResourceAsStream(linux64Path);
            }
            else {
                return FFMpeg.class.getResourceAsStream(linux32Path);
            }
        }
    }
    
    public static String getPath() throws IOException {
        
        if(tempExecutable == null || !tempExecutable.exists()) {
            //Copy the file from the JAR into a temporary directory
            InputStream stream = getInternalStream();
            tempExecutable = File.createTempFile("ffmpeg", null);
            tempExecutable.deleteOnExit();
            
            //Copy the file
            FileUtils.copyInputStreamToFile(stream, tempExecutable);
            
            if( ! tempExecutable.setExecutable(true)) {
                throw new IOException("Could not make the temporary FFMpeg file executable");
            }
            
            
        }
        return tempExecutable.getAbsolutePath();
    }
    
    private FFMpeg() {}
}
