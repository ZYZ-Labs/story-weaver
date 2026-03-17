package com.storyweaver;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntegrationTest extends BaseTest {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Test
    void contextLoads() {
        assertNotNull(applicationContext);
    }
    
    @Test
    void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            applicationContext.getBean("dataSource");
        });
    }
    
    @Test
    void testJwtUtilBean() {
        assertDoesNotThrow(() -> {
            applicationContext.getBean("jwtUtil");
        });
    }
    
    @Test
    void testPasswordEncoderBean() {
        assertDoesNotThrow(() -> {
            applicationContext.getBean("passwordEncoder");
        });
    }
    
    @Test
    void testMyBatisPlusConfiguration() {
        assertDoesNotThrow(() -> {
            applicationContext.getBean("mybatisPlusInterceptor");
        });
    }
}