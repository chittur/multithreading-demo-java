/******************************************************************************
 * Filename    = MessageSummarizer.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Message summarizer thread that generates summaries from 
 *               collected messages.
 *****************************************************************************/

package com.multithreading.demo;

import javafx.application.Platform;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * Message summarizer thread that waits for triggers and generates summaries
 * from the collection of messages. This class demonstrates thread synchronization
 * using CountDownLatch and thread-safe access to shared resources.
 */
public class MessageSummarizer implements Runnable {

    private final List<String> messages;
    private final Object messagesLock;
    private final Consumer<String> onSummaryGenerated;
    private final BlockingQueue<Boolean> triggerSummarize;
    private volatile boolean running = true;

    /**
     * Creates a new message summarizer.
     * 
     * @param messages Shared list of messages (must be thread-safe access)
     * @param messagesLock Lock object for synchronizing access to messages
     * @param onSummaryGenerated Callback when a summary is generated
     * @param triggerSummarize Queue to wait for summarization triggers
     */
    public MessageSummarizer(final List<String> messages, final Object messagesLock,
                            final Consumer<String> onSummaryGenerated, final BlockingQueue<Boolean> triggerSummarize) {
        this.messages = messages;
        this.messagesLock = messagesLock;
        this.onSummaryGenerated = onSummaryGenerated;
        this.triggerSummarize = triggerSummarize;
    }

    /**
     * Main thread execution method.
     * Waits for triggers and generates message summaries.
     */
    @Override
    public void run() {
        System.out.println("Summarizer Thread Id = " + Thread.currentThread().getId());

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // Wait for the trigger to generate summary
                triggerSummarize.take();
                
                if (!running) {
                    break;
                }

                // Create a copy of messages to avoid holding the lock for too long
                List<String> messagesCopy = new ArrayList<>();
                synchronized (messagesLock) {
                    messagesCopy.addAll(messages);
                }

                // Generate summary (find the longest message as a simple algorithm)
                String summary = generateSummary(messagesCopy);

                // Update UI - use Platform.runLater only if JavaFX is available
                try {
                    Platform.runLater(() -> onSummaryGenerated.accept(summary));
                } catch (IllegalStateException e) {
                    // JavaFX not initialized (likely in tests), call directly
                    onSummaryGenerated.accept(summary);
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * Generates a summary from the given list of messages.
     * This is a simple implementation that finds the longest message.
     * In a real-world scenario, this could use machine learning algorithms.
     * 
     * @param messageList List of messages to summarize
     * @return The generated summary
     */
    private String generateSummary(final List<String> messageList) {
        if (messageList.isEmpty()) {
            return "";
        }

        // Simple algorithm: find the longest message
        String longest = "";
        for (String message : messageList) {
            if (message.length() > longest.length()) {
                longest = message;
            }
        }

        return longest;
    }

    /**
     * Stops the message summarizer thread.
     */
    public void stop() {
        running = false;
        // Add a signal to unblock the waiting thread
        triggerSummarize.offer(false);
    }
}
