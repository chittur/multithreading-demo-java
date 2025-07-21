/******************************************************************************
 * Filename    = UdpSender.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Utility class for sending UDP messages.
 *****************************************************************************/

package com.multithreading.demo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending UDP messages.
 * This class provides static methods for sending messages via UDP protocol.
 */
public final class UdpSender {

    private static final String LOCAL_HOST = "127.0.0.1";

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private UdpSender() {
        // Utility class - no instantiation
    }

    /**
     * Sends a message to the specified port on the local device via UDP.
     * 
     * @param port The port to send the message to
     * @param message The message to be sent
     * @return true if the message was sent successfully, false otherwise
     */
    public static boolean sendMessage(final int port, final String message) {
        // Validate input parameters
        if (port < 1 || port > 65535) {
            System.err.println("Error sending UDP message: Invalid port number: " + port);
            return false;
        }
        
        if (message == null) {
            System.err.println("Error sending UDP message: Message cannot be null");
            return false;
        }
        
        try (DatagramSocket socket = new DatagramSocket()) {
            // Convert message to bytes
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);
            
            // Create the datagram packet
            InetAddress address = InetAddress.getByName(LOCAL_HOST);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
            
            // Send the packet
            socket.send(packet);
            
            return true;
        } catch (IOException e) {
            System.err.println("Error sending UDP message: " + e.getMessage());
            return false;
        }
    }
}
