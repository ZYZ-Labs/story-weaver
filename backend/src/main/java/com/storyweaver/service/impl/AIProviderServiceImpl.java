package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.repository.AIProviderMapper;
import com.storyweaver.service.AIProviderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AIProviderServiceImpl extends ServiceImpl<AIProviderMapper, AIProvider> implements AIProviderService {

    @Override
    public List<AIProvider> listProviders() {
        QueryWrapper<AIProvider> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0).orderByDesc("is_default").orderByDesc("update_time");
        return list(queryWrapper);
    }

    @Override
    @Transactional
    public AIProvider createProvider(AIProvider provider) {
        if (provider.getEnabled() == null) {
            provider.setEnabled(1);
        }
        if (provider.getIsDefault() == null) {
            provider.setIsDefault(0);
        }
        save(provider);
        if (Integer.valueOf(1).equals(provider.getIsDefault())) {
            clearOtherDefaults(provider.getId());
        }
        return getById(provider.getId());
    }

    @Override
    @Transactional
    public AIProvider updateProvider(Long id, AIProvider provider) {
        AIProvider existing = getById(id);
        if (existing == null || Integer.valueOf(1).equals(existing.getDeleted())) {
            return null;
        }

        existing.setName(provider.getName());
        existing.setProviderType(provider.getProviderType());
        existing.setBaseUrl(provider.getBaseUrl());
        existing.setApiKey(provider.getApiKey());
        existing.setModelName(provider.getModelName());
        existing.setEmbeddingModel(provider.getEmbeddingModel());
        existing.setTemperature(provider.getTemperature());
        existing.setTopP(provider.getTopP());
        existing.setMaxTokens(provider.getMaxTokens());
        existing.setTimeoutSeconds(provider.getTimeoutSeconds());
        existing.setEnabled(provider.getEnabled());
        existing.setIsDefault(provider.getIsDefault());
        existing.setRemark(provider.getRemark());

        updateById(existing);
        if (Integer.valueOf(1).equals(existing.getIsDefault())) {
            clearOtherDefaults(existing.getId());
        }
        return getById(existing.getId());
    }

    @Override
    @Transactional
    public boolean deleteProvider(Long id) {
        return removeById(id);
    }

    @Override
    public boolean testProvider(Long id) {
        AIProvider provider = getById(id);
        return provider != null
                && !Integer.valueOf(1).equals(provider.getDeleted())
                && StringUtils.hasText(provider.getBaseUrl())
                && StringUtils.hasText(provider.getModelName());
    }

    private void clearOtherDefaults(Long currentId) {
        for (AIProvider item : listProviders()) {
            if (!item.getId().equals(currentId) && Integer.valueOf(1).equals(item.getIsDefault())) {
                item.setIsDefault(0);
                updateById(item);
            }
        }
    }
}
