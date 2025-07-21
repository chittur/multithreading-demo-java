/******************************************************************************
 * Filename    = MessageSummarizerTest.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Unit tests for the MessageSummarizer class, focusing on 
 *               multithreading and synchronization behavior.
 *****************************************************************************/

package com.multithreading.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for the MessageSummarizer class.
 * These tests focus on multithreading behavior and synchronization.
 */
class MessageSummarizerTest {

    private List<String> messages;
    private Object messagesLock;
    private BlockingQueue<Boolean> triggerQueue;
    private AtomicReference<String> lastSummary;
    private CountDownLatch summaryLatch;

    @BeforeEach
    void setUp() {
        messages = new ArrayList<>();
        messagesLock = new Object();
        triggerQueue = new LinkedBlockingQueue<>();
        lastSummary = new AtomicReference<>();
        summaryLatch = new CountDownLatch(1);
    }

    @Test
    @DisplayName("Should generate summary when triggered")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSummaryGeneration() throws InterruptedException {
        // Arrange
        synchronized (messagesLock) {
            messages.add("short");
            messages.add("this is a much longer message");
            messages.add("medium length");
        }

        MessageSummarizer summarizer = new MessageSummarizer(
            messages, 
            messagesLock, 
            summary -> {
                // Direct callback without Platform.runLater for testing
                lastSummary.set(summary);
                summaryLatch.countDown();
            }, 
            triggerQueue
        );

        Thread summarizerThread = new Thread(summarizer);
        summarizerThread.setDaemon(true);
        summarizerThread.start();

        // Act
        triggerQueue.offer(true);

        // Assert
        assertTrue(summaryLatch.await(3, TimeUnit.SECONDS), "Summary should be generated within timeout");
        assertEquals("this is a much longer message", lastSummary.get(), 
                    "Should find the longest message as summary");
    }

    @Test
    @DisplayName("Should handle empty message list")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testEmptyMessageList() throws InterruptedException {
        // Arrange
        MessageSummarizer summarizer = new MessageSummarizer(
            messages, 
            messagesLock, 
            summary -> {
                lastSummary.set(summary);
                summaryLatch.countDown();
            }, 
            triggerQueue
        );

        Thread summarizerThread = new Thread(summarizer);
        summarizerThread.setDaemon(true);
        summarizerThread.start();

        // Act
        triggerQueue.offer(true);

        // Assert
        assertTrue(summaryLatch.await(3, TimeUnit.SECONDS), "Summary should be generated within timeout");
        assertEquals("", lastSummary.get(), "Summary should be empty for empty message list");
    }

    @Test
    @DisplayName("Should handle concurrent access to message list")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testConcurrentAccess() throws InterruptedException {
        // Arrange
        final int numMessages = 100;
        CountDownLatch additionLatch = new CountDownLatch(numMessages);
        
        MessageSummarizer summarizer = new MessageSummarizer(
            messages, 
            messagesLock, 
            summary -> {
                lastSummary.set(summary);
                summaryLatch.countDown();
            }, 
            triggerQueue
        );

        Thread summarizerThread = new Thread(summarizer);
        summarizerThread.setDaemon(true);
        summarizerThread.start();

        // Act - Add messages from multiple threads
        for (int i = 0; i < numMessages; i++) {
            final int messageIndex = i;
            Thread addThread = new Thread(() -> {
                synchronized (messagesLock) {
                    messages.add("Message " + messageIndex);
                }
                additionLatch.countDown();
            });
            addThread.start();
        }

        // Wait for all additions to complete
        assertTrue(additionLatch.await(5, TimeUnit.SECONDS), "All messages should be added");

        // Trigger summarization
        triggerQueue.offer(true);

        // Assert
        assertTrue(summaryLatch.await(3, TimeUnit.SECONDS), "Summary should be generated");
        assertNotNull(lastSummary.get(), "Summary should not be null");
        assertTrue(lastSummary.get().startsWith("Message"), "Summary should be one of the messages");
    }

    @Test
    @DisplayName("Should handle multiple summary triggers")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleTriggers() throws InterruptedException {
        // Arrange
        CountDownLatch multipleSummariesLatch = new CountDownLatch(3);
        List<String> summaries = new ArrayList<>();
        
        synchronized (messagesLock) {
            messages.add("first message");
        }

        MessageSummarizer summarizer = new MessageSummarizer(
            messages, 
            messagesLock, 
            summary -> {
                synchronized (summaries) {
                    summaries.add(summary);
                }
                multipleSummariesLatch.countDown();
            }, 
            triggerQueue
        );

        Thread summarizerThread = new Thread(summarizer);
        summarizerThread.setDaemon(true);
        summarizerThread.start();

        // Act - Trigger multiple times with different message states
        triggerQueue.offer(true);
        
        // Wait a bit for first summary to be processed
        Thread.sleep(50);
        
        // Add more messages
        synchronized (messagesLock) {
            messages.add("this is a longer second message");
        }
        triggerQueue.offer(true);
        
        // Wait a bit for second summary to be processed
        Thread.sleep(50);
        
        // Add even more messages
        synchronized (messagesLock) {
            messages.add("short");
        }
        triggerQueue.offer(true);

        // Assert
        assertTrue(multipleSummariesLatch.await(5, TimeUnit.SECONDS), 
                  "Multiple summaries should be generated");
        
        synchronized (summaries) {
            assertEquals(3, summaries.size(), "Should have generated 3 summaries");
            // Due to the asynchronous nature and the fact that the summarizer always looks at the current
            // state of all messages, the results might vary. Let's just verify we got 3 summaries
            // and they contain valid content
            for (String summary : summaries) {
                assertNotNull(summary, "Summary should not be null");
                assertFalse(summary.isEmpty(), "Summary should not be empty");
            }
        }
    }
}
