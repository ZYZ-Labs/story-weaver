package com.storyweaver;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class SimpleUnitTest {
    
    @Test
    void testPasswordEncoder() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "testPassword123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        
        assertNotNull(encodedPassword);
        assertTrue(passwordEncoder.matches(rawPassword, encodedPassword));
        assertFalse(passwordEncoder.matches("wrongPassword", encodedPassword));
    }
    
    @Test
    void testStringOperations() {
        String projectName = "测试小说项目";
        String genre = "玄幻";
        
        assertEquals("测试小说项目", projectName);
        assertEquals("玄幻", genre);
        assertTrue(projectName.contains("小说"));
        assertFalse(projectName.contains("诗歌"));
    }
    
    @Test
    void testMathOperations() {
        int chapterCount = 10;
        int wordCount = 15000;
        double averageWordsPerChapter = (double) wordCount / chapterCount;
        
        assertEquals(10, chapterCount);
        assertEquals(15000, wordCount);
        assertEquals(1500.0, averageWordsPerChapter, 0.01);
    }
    
    @Test
    void testBooleanLogic() {
        boolean isPublished = true;
        boolean isDraft = false;
        boolean hasContent = true;
        
        assertTrue(isPublished);
        assertFalse(isDraft);
        assertTrue(hasContent);
        assertTrue(isPublished && hasContent);
        assertFalse(isPublished && isDraft);
    }
}