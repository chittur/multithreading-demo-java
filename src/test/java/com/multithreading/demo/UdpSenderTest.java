/******************************************************************************
 * Filename    = UdpSenderTest.java
 *
 * Author      = Ramaswamy Krishnan-Chittur
 *
 * Product     = MultiThreadingDemo
 * 
 * Project     = MultiThreadingDemo
 *
 * Description = Unit tests for the UdpSender class.
 *****************************************************************************/

package com.multithreading.demo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the UdpSender class.
 * These tests demonstrate basic testing concepts for the multithreading demo.
 */
class UdpSenderTest {

    @Test
    @DisplayName("Should handle invalid port gracefully")
    void testSendMessageWithInvalidPort() {
        // Test with an invalid port (negative number)
        boolean result = UdpSender.sendMessage(-1, "test message");
        
        // Should return false for invalid port
        assertFalse(result, "Sending to invalid port should return false");
    }

    @Test
    @DisplayName("Should handle empty message")
    void testSendEmptyMessage() {
        // Test with an empty message
        // Using a port that's likely to fail (0 is reserved)
        boolean result = UdpSender.sendMessage(0, "");
        
        // Should handle empty message gracefully
        assertFalse(result, "Sending empty message to reserved port should return false");
    }

    @Test
    @DisplayName("Should handle null message gracefully")
    void testSendNullMessage() {
        // Test with a null message
        assertDoesNotThrow(() -> {
            UdpSender.sendMessage(12345, null);
        }, "Should not throw exception for null message");
    }
}
