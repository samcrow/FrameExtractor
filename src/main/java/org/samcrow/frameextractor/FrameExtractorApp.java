package org.samcrow.frameextractor;

import java.io.File;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * Frame extractor
 * @author Sam Crow
 */
public class FrameExtractorApp extends Application {
    
    private File inputFile;
    
    private File outputDir;
    
    @Override
    public void start(final Stage primaryStage) {
        final Insets PADDING = new Insets(10);
        
        final GridPane grid = new GridPane();
        
        {
            final Label inputLabel = new Label("Input file:");
            grid.add(inputLabel, 0, 0);
            GridPane.setMargin(inputLabel, PADDING);

            final Label inputPathLabel = new Label("No file selected");
            grid.add(inputPathLabel, 1, 0);
            GridPane.setMargin(inputPathLabel, PADDING);

            final Button selectInButton = new Button("Select...");
            selectInButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    FileChooser chooser = new FileChooser();
                    chooser.setTitle("Choose a video file to open");
                    inputFile = chooser.showOpenDialog(primaryStage);
                    if(inputFile != null) {
                        inputPathLabel.setText(inputFile.getAbsolutePath());
                    }
                    else {
                        inputPathLabel.setText("No file selected");
                    }
                }
            });
            grid.add(selectInButton, 2, 0);
            GridPane.setMargin(selectInButton, PADDING);
        }
        {
            final Label outputLabel = new Label("Output directory:");
            grid.add(outputLabel, 0, 1);
            GridPane.setMargin(outputLabel, PADDING);
            
            final Label outputPathLabel = new Label("No directory selected");
            grid.add(outputPathLabel, 1, 1);
            GridPane.setMargin(outputPathLabel, PADDING);
            
            final Button selectOutButton = new Button("Select...");
            selectOutButton.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent t) {
                    DirectoryChooser chooser = new DirectoryChooser();
                    chooser.setTitle("Choose a directory to save frames");
                    outputDir = chooser.showDialog(primaryStage);
                    if(outputDir != null) {
                        outputPathLabel.setText(outputDir.getAbsolutePath());
                    }
                    else {
                        outputPathLabel.setText("No file selected");
                    }
                }
            });
            grid.add(selectOutButton, 2, 1);
            GridPane.setMargin(selectOutButton, PADDING);
        }
        
        final CheckBox force29Box = new CheckBox("Force frame rate to 29.97 fps");
        grid.add(force29Box, 0, 2, 2, 1);
        GridPane.setMargin(force29Box, PADDING);
        
        final Button startButton = new Button("Start");
        startButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                Task<Void> task;
                boolean force29 = force29Box.isSelected();
                if(force29) {
                    task = new FrameExtractor(inputFile.getAbsolutePath(), outputDir.getAbsolutePath(), 29.97);
                }
                else {
                    task = new FrameExtractor(inputFile.getAbsolutePath(), outputDir.getAbsolutePath());
                }
                
                final TaskStatusStage stage = new TaskStatusStage(task);
                stage.initOwner(primaryStage);
                stage.initModality(Modality.WINDOW_MODAL);
                stage.show();
            }
        });
        startButton.setAlignment(Pos.CENTER_RIGHT);
        grid.add(startButton, 2, 2);
        GridPane.setMargin(startButton, PADDING);
        
        Scene scene = new Scene(grid, 800, grid.getPrefHeight());
        
        
        primaryStage.setTitle("Frame Extractor");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX
     * application. main() serves only as fallback in case the
     * application can not be launched through deployment artifacts,
     * e.g., in IDEs with limited FX support. NetBeans ignores main().
     * <p/>
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

    
}
