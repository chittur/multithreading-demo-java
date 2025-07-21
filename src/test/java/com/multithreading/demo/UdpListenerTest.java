/******************************************************************************
 * Filename    = UdpListenerTest.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Unit tests for the UdpListener class, focusing on 
 *               multithreading and UDP communication behavior.
 *****************************************************************************/

package com.multithreading.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for the UdpListener class.
 * These tests focus on multithreading behavior and UDP communication.
 */
class UdpListenerTest {

    private List<String> messages;
    private Object messagesLock;
    private BlockingQueue<Boolean> triggerQueue;
    private AtomicReference<String> lastReceivedMessage;
    private AtomicInteger lastMessageCount;
    private AtomicReference<String> lastError;
    private CountDownLatch messageLatch;
    private CountDownLatch countLatch;
    private CountDownLatch errorLatch;

    @BeforeEach
    void setUp() {
        messages = new ArrayList<>();
        messagesLock = new Object();
        triggerQueue = new LinkedBlockingQueue<>();
        lastReceivedMessage = new AtomicReference<>();
        lastMessageCount = new AtomicInteger(0);
        lastError = new AtomicReference<>();
        messageLatch = new CountDownLatch(1);
        countLatch = new CountDownLatch(1);
        errorLatch = new CountDownLatch(1);
    }

    @Test
    @DisplayName("Should receive and process UDP messages")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testReceiveMessage() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        String testMessage = "Hello UDP Listener!";

        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {
                lastReceivedMessage.set(message);
                messageLatch.countDown();
            },
            count -> {
                lastMessageCount.set(count);
                countLatch.countDown();
            },
            error -> {
                lastError.set(error);
                errorLatch.countDown();
            },
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Give the listener time to start
        Thread.sleep(100);

        // Act - Send a UDP message
        sendUdpMessage(testPort, testMessage);

        // Assert
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Message should be received");
        assertTrue(countLatch.await(5, TimeUnit.SECONDS), "Count should be updated");
        
        assertEquals(testMessage, lastReceivedMessage.get(), "Should receive the correct message");
        assertEquals(1, lastMessageCount.get(), "Message count should be 1");
        
        // Verify message was added to the shared list
        synchronized (messagesLock) {
            assertEquals(1, messages.size(), "Messages list should contain 1 message");
            assertEquals(testMessage, messages.get(0), "Messages list should contain the received message");
        }

        // Clean up
        listener.stop();
    }

    @Test
    @DisplayName("Should handle multiple concurrent messages")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testMultipleMessages() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        int numMessages = 10;
        CountDownLatch multipleMessagesLatch = new CountDownLatch(numMessages);
        List<String> receivedMessages = new ArrayList<>();

        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {
                synchronized (receivedMessages) {
                    receivedMessages.add(message);
                }
                multipleMessagesLatch.countDown();
            },
            count -> {
                lastMessageCount.set(count);
            },
            error -> {
                lastError.set(error);
                errorLatch.countDown();
            },
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Give the listener time to start
        Thread.sleep(100);

        // Act - Send multiple messages concurrently
        for (int i = 0; i < numMessages; i++) {
            final int messageIndex = i;
            Thread senderThread = new Thread(() -> {
                try {
                    sendUdpMessage(testPort, "Message " + messageIndex);
                } catch (Exception e) {
                    fail("Failed to send message: " + e.getMessage());
                }
            });
            senderThread.start();
        }

        // Assert
        assertTrue(multipleMessagesLatch.await(10, TimeUnit.SECONDS), 
                  "All messages should be received");
        
        synchronized (receivedMessages) {
            assertEquals(numMessages, receivedMessages.size(), 
                        "Should receive all sent messages");
        }
        
        synchronized (messagesLock) {
            assertEquals(numMessages, messages.size(), 
                        "Messages list should contain all messages");
        }

        assertEquals(numMessages, lastMessageCount.get(), 
                    "Final count should equal number of messages sent");

        // Clean up
        listener.stop();
    }

    @Test
    @DisplayName("Should trigger summarizer after every 5 messages")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testSummarizerTrigger() throws Exception {
        // Arrange
        int testPort = findAvailablePort();
        CountDownLatch fiveMessagesLatch = new CountDownLatch(5);
        CountDownLatch triggerLatch = new CountDownLatch(1);

        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> fiveMessagesLatch.countDown(),
            count -> {},
            error -> {
                lastError.set(error);
                errorLatch.countDown();
            },
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Monitor the trigger queue
        Thread triggerMonitor = new Thread(() -> {
            try {
                triggerQueue.take(); // Wait for trigger
                triggerLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        triggerMonitor.setDaemon(true);
        triggerMonitor.start();

        // Give the listener time to start
        Thread.sleep(100);

        // Act - Send exactly 5 messages
        for (int i = 0; i < 5; i++) {
            sendUdpMessage(testPort, "Trigger test message " + i);
        }

        // Assert
        assertTrue(fiveMessagesLatch.await(10, TimeUnit.SECONDS), 
                  "All 5 messages should be received");
        assertTrue(triggerLatch.await(5, TimeUnit.SECONDS), 
                  "Summarizer should be triggered after 5 messages");

        // Clean up
        listener.stop();
    }

    @Test
    @DisplayName("Should handle listener stop gracefully")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testStopListener() throws Exception {
        // Arrange
        int testPort = findAvailablePort();

        UdpListener listener = new UdpListener(
            testPort,
            messages,
            messagesLock,
            message -> {},
            count -> {},
            error -> {},
            triggerQueue
        );

        Thread listenerThread = new Thread(listener);
        listenerThread.setDaemon(true);
        listenerThread.start();

        // Give the listener time to start
        Thread.sleep(100);
        assertTrue(listenerThread.isAlive(), "Listener thread should be running");

        // Act
        listener.stop();

        // Assert
        // Thread should terminate within reasonable time
        boolean terminated = false;
        for (int i = 0; i < 50; i++) { // Wait up to 5 seconds
            if (!listenerThread.isAlive()) {
                terminated = true;
                break;
            }
            Thread.sleep(100);
        }
        
        assertTrue(terminated, "Listener thread should terminate after stop()");
    }

    /**
     * Helper method to find an available port for testing.
     */
    private int findAvailablePort() throws Exception {
        try (DatagramSocket socket = new DatagramSocket(0)) {
            return socket.getLocalPort();
        }
    }

    /**
     * Helper method to send a UDP message to the specified port.
     */
    private void sendUdpMessage(int port, String message) throws Exception {
        try (DatagramSocket socket = new DatagramSocket()) {
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            InetAddress address = InetAddress.getByName("127.0.0.1");
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            socket.send(packet);
        }
    }
}
