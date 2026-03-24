package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.AIProvider;

import java.util.List;

public interface AIProviderService extends IService<AIProvider> {
    List<AIProvider> listProviders();
    AIProvider createProvider(AIProvider provider);
    AIProvider updateProvider(Long id, AIProvider provider);
    boolean deleteProvider(Long id);
    boolean testProvider(Long id);
}
