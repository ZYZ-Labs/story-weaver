package com.storyweaver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
@MapperScan("com.storyweaver.repository")
public class StoryWeaverApplication {
    public static void main(String[] args) {
        SpringApplication.run(StoryWeaverApplication.class, args);
    }
}