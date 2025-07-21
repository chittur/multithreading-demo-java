/******************************************************************************
 * Filename    = MainController.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Main controller class for the JavaFX application that 
 *               coordinates the UI and background threads.
 *****************************************************************************/

package com.multithreading.demo;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Main controller class for the JavaFX multithreading demo application.
 * 
 * # Design
 * The application has a simple JavaFX-based GUI, and supports sending and receiving messages through UDP.
 * - The application runs a JavaFX based GUI.
 * - A worker thread is spawned to listen for messages on a UDP port.
 * - Another worker thread is spawned to summarize messages after a batch of messages has been transacted.
 * - A task is used to send messages to a UDP port.
 * - Synchronization primitives are used to make thread-safe access of shared resources.
 */
public class MainController implements Initializable {

    @FXML
    private TextField receivePortTextField;

    @FXML
    private TextField receiveMessageTextField;

    @FXML
    private TextField sendPortTextField;

    @FXML
    private TextField sendMessageTextField;

    @FXML
    private Button sendMessageButton;

    @FXML
    private TextField totalMessageCountTextField;

    @FXML
    private TextField summaryTextField;

    private List<String> messages;
    private final Object messagesLock = new Object();
    private int listenPort;
    private Thread listenerThread;
    private Thread summarizerThread;
    private UdpListener udpListener;
    private MessageSummarizer messageSummarizer;
    private BlockingQueue<Boolean> triggerSummarize;

    private static final int TRIGGER_SUMMARIZER_MESSAGE_COUNT = 5;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * 
     * @param location The location used to resolve relative paths for the root object
     * @param resources The resources used to localize the root object
     */
    @Override
    public void initialize(final URL location, final ResourceBundle resources) {
        System.out.println("Main Thread Id = " + Thread.currentThread().getId());

        // Initialize the list of messages
        messages = new ArrayList<>();

        // Create the blocking queue for triggering summarization
        triggerSummarize = new LinkedBlockingQueue<>();

        // Pick a random port to listen for messages and display it
        listenPort = new Random().nextInt(55000) + 10000;
        receivePortTextField.setText(String.valueOf(listenPort));

        // Create and start the UDP listener thread
        udpListener = new UdpListener(
                listenPort,
                messages,
                messagesLock,
                this::onMessageReceived,
                this::onMessageCountUpdated,
                this::onError,
                triggerSummarize
        );
        listenerThread = new Thread(udpListener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Create and start the message summarizer thread
        messageSummarizer = new MessageSummarizer(
                messages,
                messagesLock,
                this::onSummaryGenerated,
                triggerSummarize
        );
        summarizerThread = new Thread(messageSummarizer);
        summarizerThread.setDaemon(true);
        summarizerThread.start();
    }

    /**
     * Handles the send message button click event.
     * 
     * @param event The action event
     */
    @FXML
    private void sendMessageButtonClick(final ActionEvent event) {
        sendMessageButton.setDisable(true);

        try {
            // Get the message and sending port details
            int port = Integer.parseInt(sendPortTextField.getText());
            String message = sendMessageTextField.getText();

            // Use CompletableFuture to send the message asynchronously
            CompletableFuture<Boolean> sendTask = CompletableFuture.supplyAsync(() -> 
                UdpSender.sendMessage(port, message)
            );

            sendTask.thenAccept(result -> {
                Platform.runLater(() -> {
                    System.out.println("Result of sending message '" + message + "' to " + port + " was " + result);

                    int totalMessageCount;

                    // Add the message to the message collection (thread-safe)
                    synchronized (messagesLock) {
                        messages.add(message);
                        totalMessageCount = messages.size();
                    }

                    // Update the UI with the message count
                    totalMessageCountTextField.setText(String.valueOf(totalMessageCount));

                    // Trigger the summarizer if required
                    triggerSummarizerIfRequired(totalMessageCount);

                    sendMessageButton.setDisable(false);
                });
            }).exceptionally(throwable -> {
                Platform.runLater(() -> {
                    showError("Error sending message: " + throwable.getMessage());
                    sendMessageButton.setDisable(false);
                });
                return null;
            });

        } catch (NumberFormatException e) {
            showError("Invalid port number: " + sendPortTextField.getText());
            sendMessageButton.setDisable(false);
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            sendMessageButton.setDisable(false);
        }
    }

    /**
     * Callback method called when a message is received.
     * 
     * @param message The received message
     */
    private void onMessageReceived(final String message) {
        receiveMessageTextField.setText(message);
    }

    /**
     * Callback method called when the message count is updated.
     * 
     * @param count The new message count
     */
    private void onMessageCountUpdated(final Integer count) {
        totalMessageCountTextField.setText(String.valueOf(count));
    }

    /**
     * Callback method called when a summary is generated.
     * 
     * @param summary The generated summary
     */
    private void onSummaryGenerated(final String summary) {
        summaryTextField.setText(summary);
    }

    /**
     * Callback method called when an error occurs.
     * 
     * @param errorMessage The error message
     */
    private void onError(final String errorMessage) {
        showError(errorMessage);
        receivePortTextField.setText("");
        receiveMessageTextField.setText("Error: Could not setup listener.");
    }

    /**
     * Shows an error dialog to the user.
     * 
     * @param message The error message to display
     */
    private void showError(final String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Triggers the summarizer if required.
     * We do not want to summarize on every message. Only after a batch of certain number of messages.
     * 
     * @param totalMessageCount Total number of messages sent and received
     */
    private void triggerSummarizerIfRequired(final int totalMessageCount) {
        if ((totalMessageCount % TRIGGER_SUMMARIZER_MESSAGE_COUNT) == 0) {
            triggerSummarize.offer(true);
        }
    }

    /**
     * Cleanup method called when the application is shutting down.
     */
    public void cleanup() {
        if (udpListener != null) {
            udpListener.stop();
        }
        if (messageSummarizer != null) {
            messageSummarizer.stop();
        }
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
        if (summarizerThread != null) {
            summarizerThread.interrupt();
        }
    }
}
