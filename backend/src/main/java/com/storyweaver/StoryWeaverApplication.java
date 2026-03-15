package com.storyweaver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class StoryWeaverApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoryWeaverApplication.class, args);
    }
}