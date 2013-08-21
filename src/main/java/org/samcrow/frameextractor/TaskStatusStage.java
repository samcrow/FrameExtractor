package org.samcrow.frameextractor;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import jfxtras.labs.dialogs.MonologFX;

/**
 *
 * @author samcrow
 */
public class TaskStatusStage extends Stage {

    private final Button cancelButton = new Button("Cancel");

    private final ProgressBar progressBar = new ProgressBar();

    private final Label statusLabel = new Label();

    public TaskStatusStage(final Task<?> task) {

        //Begin layout
        {
            VBox outerBox = new VBox();
        
        
            HBox topBox = new HBox();
            topBox.setAlignment(Pos.CENTER);
            final Insets MARGIN = new Insets(10);
            
            progressBar.setPrefWidth(1000);

            cancelButton.setCancelButton(true);
            topBox.getChildren().addAll(progressBar, cancelButton);
            HBox.setMargin(progressBar, MARGIN);
            HBox.setMargin(cancelButton, MARGIN);

            outerBox.getChildren().add(topBox);

            outerBox.getChildren().add(statusLabel);
            VBox.setMargin(statusLabel, MARGIN);
            
            Scene scene = new Scene(outerBox, outerBox.getPrefWidth(), outerBox.getPrefHeight());
            setScene(scene);
            setTitle("Task progress");
        }
        //End layout


        progressBar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());
        
        //Start task when this stage is shown
        setOnShown(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent t) {
                new Thread(task).start();
            }
        });
        
        cancelButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent t) {
                task.cancel();
            }
        });

        task.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                MonologFX dialog = new MonologFX(MonologFX.Type.ERROR);
                dialog.setModal(true);
                Throwable exception = task.getException();
                dialog.setMessage("Task failed: " + exception.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(exception));
                dialog.setTitle("Task failed");
                dialog.setModal(true);
                dialog.initOwner(TaskStatusStage.this);
                dialog.showDialog();
                close();
            }
        });

        task.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                MonologFX dialog = new MonologFX(MonologFX.Type.ACCEPT);
                dialog.setTitle("Complete");
                dialog.setModal(true);
                dialog.initOwner(TaskStatusStage.this);
                dialog.setMessage("Task finished");
                dialog.showDialog();
                close();
            }
        });
        
        task.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                close();
            }
        });
    }

}
