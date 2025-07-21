/******************************************************************************
 * Filename    = UdpListener.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = UDP listener thread for receiving messages on a specified port.
 *****************************************************************************/

package com.multithreading.demo;

import javafx.application.Platform;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

/**
 * UDP listener thread that listens for incoming messages on a specified port.
 * This class demonstrates background threading and thread-safe communication
 * with the UI thread using JavaFX Platform.runLater().
 */
public class UdpListener implements Runnable {

    private final int listenPort;
    private final List<String> messages;
    private final Object messagesLock;
    private final Consumer<String> onMessageReceived;
    private final Consumer<Integer> onMessageCountUpdated;
    private final Consumer<String> onError;
    private final BlockingQueue<Boolean> triggerSummarize;
    private volatile boolean running = true;
    private DatagramSocket socket;

    /**
     * Creates a new UDP listener.
     * 
     * @param listenPort The port to listen on
     * @param messages Shared list of messages (must be thread-safe access)
     * @param messagesLock Lock object for synchronizing access to messages
     * @param onMessageReceived Callback when a message is received
     * @param onMessageCountUpdated Callback when message count is updated
     * @param onError Callback when an error occurs
     * @param triggerSummarize Queue to signal summarizer thread
     */
    public UdpListener(final int listenPort, final List<String> messages, final Object messagesLock,
                      final Consumer<String> onMessageReceived, final Consumer<Integer> onMessageCountUpdated,
                      final Consumer<String> onError, final BlockingQueue<Boolean> triggerSummarize) {
        this.listenPort = listenPort;
        this.messages = messages;
        this.messagesLock = messagesLock;
        this.onMessageReceived = onMessageReceived;
        this.onMessageCountUpdated = onMessageCountUpdated;
        this.onError = onError;
        this.triggerSummarize = triggerSummarize;
    }

    /**
     * Main thread execution method.
     * Listens for UDP messages and processes them.
     */
    @Override
    public void run() {
        System.out.println("Listener Thread Id = " + Thread.currentThread().getId());

        try {
            socket = new DatagramSocket(listenPort);
            byte[] buffer = new byte[1024];

            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    // Create packet to receive data
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    
                    // Wait for and receive a message
                    socket.receive(packet);
                    
                    // Convert received data to string
                    String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    
                    int totalMessageCount;
                    
                    // Add message to shared collection (thread-safe)
                    synchronized (messagesLock) {
                        messages.add(message);
                        totalMessageCount = messages.size();
                    }
                    
                    // Update UI - use Platform.runLater only if JavaFX is available
                    try {
                        Platform.runLater(() -> {
                            onMessageReceived.accept(message);
                            onMessageCountUpdated.accept(totalMessageCount);
                        });
                    } catch (IllegalStateException e) {
                        // JavaFX not initialized (likely in tests), call directly
                        onMessageReceived.accept(message);
                        onMessageCountUpdated.accept(totalMessageCount);
                    }
                    
                    // Trigger summarizer if needed (every 5 messages)
                    if (totalMessageCount % 5 == 0) {
                        triggerSummarize.offer(true);
                    }
                    
                } catch (IOException e) {
                    if (running) {
                        try {
                            Platform.runLater(() -> onError.accept("Error receiving message: " + e.getMessage()));
                        } catch (IllegalStateException ex) {
                            // JavaFX not initialized (likely in tests), call directly
                            onError.accept("Error receiving message: " + e.getMessage());
                        }
                    }
                    break;
                }
            }
        } catch (SocketException e) {
            try {
                Platform.runLater(() -> onError.accept("Could not setup UDP listener: " + e.getMessage()));
            } catch (IllegalStateException ex) {
                // JavaFX not initialized (likely in tests), call directly
                onError.accept("Could not setup UDP listener: " + e.getMessage());
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    /**
     * Stops the UDP listener thread.
     */
    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}
