package com.storyweaver.config;

import com.storyweaver.storyunit.assembler.StoryUnitAssembler;
import com.storyweaver.storyunit.registry.DefaultStoryUnitRegistry;
import com.storyweaver.storyunit.registry.StoryUnitRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class StoryUnitProjectionConfig {

    @Bean
    public StoryUnitRegistry storyUnitRegistry(List<StoryUnitAssembler<?>> assemblers) {
        return new DefaultStoryUnitRegistry(assemblers);
    }
}
