package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.vo.ProviderDiscoveryVO;

import java.util.List;

public interface AIProviderService extends IService<AIProvider> {
    List<AIProvider> listProviders();
    AIProvider createProvider(AIProvider provider);
    AIProvider updateProvider(Long id, AIProvider provider);
    boolean deleteProvider(Long id);
    boolean testProvider(Long id);
    ProviderDiscoveryVO discoverModels(AIProvider provider);
    String generateText(AIProvider provider, String modelName, String systemPrompt, String userPrompt, Double temperature, Integer maxTokens);
}
