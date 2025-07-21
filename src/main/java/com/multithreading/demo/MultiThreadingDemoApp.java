/******************************************************************************
 * Filename    = MultiThreadingDemoApp.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Main JavaFX application class for demonstrating multithreading 
 *               and synchronization concepts.
 *****************************************************************************/

package com.multithreading.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main JavaFX application class for the Multithreading Demo.
 * This application demonstrates multithreading and synchronization concepts
 * using JavaFX GUI and UDP communication.
 */
public class MultiThreadingDemoApp extends Application {

    /**
     * Starts the JavaFX application.
     * 
     * @param primaryStage The primary stage for this application
     * @throws Exception if there's an error loading the FXML
     */
    @Override
    public void start(final Stage primaryStage) throws Exception {
        // Load the FXML file
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/main-window.fxml"));
        Parent root = loader.load();

        // Configure the primary stage
        primaryStage.setTitle("Multithreading Demo");
        primaryStage.setScene(new Scene(root, 800, 450));
        primaryStage.setResizable(false);
        
        // Show the stage
        primaryStage.show();
    }

    /**
     * Stops the JavaFX application and ensures proper cleanup.
     * 
     * @throws Exception if there's an error during cleanup
     */
    @Override
    public void stop() throws Exception {
        super.stop();
        
        // Ensure all background threads are properly terminated
        // The controller will handle the cleanup of its threads
        System.exit(0);
    }

    /**
     * Main method to launch the JavaFX application.
     * 
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        launch(args);
    }
}
