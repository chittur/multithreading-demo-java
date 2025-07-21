/******************************************************************************
 * Filename    = MultithreadingIntegrationTest.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Integration tests that demonstrate the complete multithreading
 *               workflow including UDP communication, message processing, and
 *               summarization working together.
 *****************************************************************************/

package com.multithreading.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for the complete multithreading demo.
 * These tests verify that all components work together correctly
 * in a multithreaded environment.
 */
class MultithreadingIntegrationTest {

    private List<String> messages;
    private Object messagesLock;
    private BlockingQueue<Boolean> triggerQueue;
    private AtomicReference<String> lastSummary;
    private AtomicInteger messageCount;
    private CountDownLatch summaryLatch;

    @BeforeEach
    void setUp() {
        messages = new ArrayList<>();
        messagesLock = new Object();
        triggerQueue = new LinkedBlockingQueue<>();
        lastSummary = new AtomicReference<>();
        messageCount = new AtomicInteger(0);
        summaryLatch = new CountDownLatch(1);
    }

    @Test
    @DisplayName("Complete workflow: UDP listener receives messages and triggers summarizer")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testCompleteMultithreadingWorkflow() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        
        // Set up message tracking
        List<String> receivedMessages = new ArrayList<>();
        CountDownLatch fiveMessagesLatch = new CountDownLatch(5);

        // Create and start UDP listener
        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {
                synchronized (receivedMessages) {
                    receivedMessages.add(message);
                }
                fiveMessagesLatch.countDown();
            },
            count -> messageCount.set(count),
            error -> fail("UDP Listener error: " + error),
            triggerQueue
        );

        // Create and start message summarizer
        MessageSummarizer summarizer = new MessageSummarizer(
            messages,
            messagesLock,
            summary -> {
                lastSummary.set(summary);
                summaryLatch.countDown();
            },
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        Thread summarizerThread = new Thread(summarizer);
        
        listenerThread.setDaemon(true);
        summarizerThread.setDaemon(true);
        
        listenerThread.start();
        summarizerThread.start();

        // Give threads time to start
        Thread.sleep(200);

        // Act - Send 5 messages to trigger summarization
        String[] testMessages = {
            "Short",
            "This is a much longer message that should be selected as summary",
            "Medium length message",
            "Brief",
            "Another message"
        };

        for (String message : testMessages) {
            UdpSender.sendMessage(testPort, message);
            Thread.sleep(50); // Small delay between messages
        }

        // Assert
        assertTrue(fiveMessagesLatch.await(10, TimeUnit.SECONDS), 
                  "All 5 messages should be received by UDP listener");
        
        assertTrue(summaryLatch.await(10, TimeUnit.SECONDS), 
                  "Summary should be generated after 5 messages");

        // Verify all messages were received
        synchronized (receivedMessages) {
            assertEquals(5, receivedMessages.size(), "Should receive all 5 messages");
            for (String originalMessage : testMessages) {
                assertTrue(receivedMessages.contains(originalMessage), 
                          "Should contain message: " + originalMessage);
            }
        }

        // Verify message count
        assertEquals(5, messageCount.get(), "Message count should be 5");

        // Verify summary (should be the longest message)
        assertEquals("This is a much longer message that should be selected as summary", 
                    lastSummary.get(), "Summary should be the longest message");

        // Verify shared message list
        synchronized (messagesLock) {
            assertEquals(5, messages.size(), "Shared messages list should contain 5 messages");
        }

        // Clean up
        listener.stop();
        summarizer.stop();
    }

    @Test
    @DisplayName("Stress test: Multiple senders with concurrent processing")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testConcurrentStressTest() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        int numSenders = 5;
        int messagesPerSender = 10;
        int totalMessages = numSenders * messagesPerSender;
        
        CountDownLatch allMessagesLatch = new CountDownLatch(totalMessages);
        List<String> allReceivedMessages = new ArrayList<>();

        // Create and start UDP listener
        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {
                synchronized (allReceivedMessages) {
                    allReceivedMessages.add(message);
                }
                allMessagesLatch.countDown();
            },
            count -> messageCount.set(count),
            error -> fail("UDP Listener error: " + error),
            triggerQueue
        );

        // Create and start message summarizer
        CountDownLatch multipleSummariesLatch = new CountDownLatch(totalMessages / 5);
        List<String> summaries = new ArrayList<>();
        
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

        Thread listenerThread = new Thread(listener);
        Thread summarizerThread = new Thread(summarizer);
        
        listenerThread.setDaemon(true);
        summarizerThread.setDaemon(true);
        
        listenerThread.start();
        summarizerThread.start();

        // Give threads time to start
        Thread.sleep(200);

        // Act - Create multiple sender threads
        List<Thread> senderThreads = new ArrayList<>();
        for (int senderId = 0; senderId < numSenders; senderId++) {
            final int finalSenderId = senderId;
            Thread senderThread = new Thread(() -> {
                for (int msgId = 0; msgId < messagesPerSender; msgId++) {
                    String message = String.format("Sender_%d_Message_%d_with_varying_lengths_to_test_summary_algorithm", 
                                                  finalSenderId, msgId);
                    UdpSender.sendMessage(testPort, message);
                    
                    try {
                        Thread.sleep(10); // Small delay to avoid overwhelming
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            senderThreads.add(senderThread);
            senderThread.start();
        }

        // Wait for all sender threads to complete
        for (Thread senderThread : senderThreads) {
            senderThread.join(5000); // Wait up to 5 seconds per thread
        }

        // Assert
        assertTrue(allMessagesLatch.await(15, TimeUnit.SECONDS), 
                  "All messages should be received");

        // Verify message count
        assertEquals(totalMessages, messageCount.get(), 
                    "Should receive all sent messages");

        // Verify all messages were received
        synchronized (allReceivedMessages) {
            assertEquals(totalMessages, allReceivedMessages.size(), 
                        "Should receive all messages");
        }

        // Verify shared message list
        synchronized (messagesLock) {
            assertEquals(totalMessages, messages.size(), 
                        "Shared messages list should contain all messages");
        }

        // Verify summaries were generated (every 5 messages)
        assertTrue(multipleSummariesLatch.await(10, TimeUnit.SECONDS), 
                  "Summaries should be generated for message batches");

        synchronized (summaries) {
            int expectedSummaries = totalMessages / 5;
            assertEquals(expectedSummaries, summaries.size(), 
                        "Should generate correct number of summaries");
        }

        // Clean up
        listener.stop();
        summarizer.stop();
    }

    @Test
    @DisplayName("Thread safety: Verify no race conditions in concurrent access")
    @Timeout(value = 20, unit = TimeUnit.SECONDS)
    void testThreadSafety() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        int numMessages = 100;
        CountDownLatch allProcessedLatch = new CountDownLatch(numMessages);

        // Use thread-safe counters to detect race conditions
        AtomicInteger receivedCount = new AtomicInteger(0);
        AtomicInteger processedCount = new AtomicInteger(0);

        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {
                receivedCount.incrementAndGet();
                allProcessedLatch.countDown();
            },
            count -> processedCount.set(count),
            error -> fail("Error: " + error),
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        Thread.sleep(200); // Give listener time to start

        // Act - Send messages rapidly from multiple threads
        List<Thread> senderThreads = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Thread senderThread = new Thread(() -> {
                for (int j = 0; j < 10; j++) {
                    UdpSender.sendMessage(testPort, "Race condition test message");
                    try {
                        Thread.sleep(1); // Minimal delay
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            senderThreads.add(senderThread);
            senderThread.start();
        }

        // Wait for completion
        for (Thread senderThread : senderThreads) {
            senderThread.join();
        }

        assertTrue(allProcessedLatch.await(15, TimeUnit.SECONDS), 
                  "All messages should be processed");

        // Assert - Verify consistency (no race conditions)
        assertEquals(numMessages, receivedCount.get(), 
                    "Received count should match sent count");
        assertEquals(numMessages, processedCount.get(), 
                    "Processed count should match sent count");

        synchronized (messagesLock) {
            assertEquals(numMessages, messages.size(), 
                        "Messages list size should match sent count");
        }

        // Clean up
        listener.stop();
    }

    /**
     * Helper method to find an available port for testing.
     */
    private int findAvailablePort() throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
