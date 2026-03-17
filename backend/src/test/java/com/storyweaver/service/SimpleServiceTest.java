package com.storyweaver.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SimpleServiceTest extends com.storyweaver.BaseTest {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Test
    void testPasswordEncoderBean() {
        assertNotNull(passwordEncoder);
        
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword);
        assertTrue(encodedPassword.length() > 0);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
    }
    
    @Test
    void testContextLoads() {
        assertTrue(true, "Context should load successfully");
    }
    
    @Test
    void testBasicAssertions() {
        assertEquals(4, 2 + 2);
        assertNotEquals(5, 2 + 2);
        assertTrue(10 > 5);
        assertFalse(5 > 10);
        assertNull(null);
        assertNotNull("not null");
    }
}