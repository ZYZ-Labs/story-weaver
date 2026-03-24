package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.SystemConfig;
import com.storyweaver.repository.SystemConfigMapper;
import com.storyweaver.service.SystemConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements SystemConfigService {

    private static final Map<String, SystemConfig> DEFAULT_CONFIGS = buildDefaultConfigs();

    @Override
    public List<SystemConfig> listMergedConfigs() {
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByAsc("id");
        List<SystemConfig> existingConfigs = list(queryWrapper);

        Map<String, SystemConfig> configMap = new LinkedHashMap<>();
        for (SystemConfig config : existingConfigs) {
            configMap.put(config.getConfigKey(), config);
        }

        List<SystemConfig> merged = new ArrayList<>();
        for (Map.Entry<String, SystemConfig> entry : DEFAULT_CONFIGS.entrySet()) {
            SystemConfig existing = configMap.remove(entry.getKey());
            if (existing == null) {
                merged.add(copyConfig(entry.getValue()));
                continue;
            }
            if (!StringUtils.hasText(existing.getDescription())) {
                existing.setDescription(entry.getValue().getDescription());
            }
            if (!StringUtils.hasText(existing.getConfigValue())) {
                existing.setConfigValue(entry.getValue().getConfigValue());
            }
            merged.add(existing);
        }

        merged.addAll(configMap.values());
        return merged;
    }

    @Override
    @Transactional
    public List<SystemConfig> saveConfigs(List<SystemConfig> configs) {
        if (configs == null || configs.isEmpty()) {
            return listMergedConfigs();
        }

        for (SystemConfig incoming : configs) {
            if (incoming == null || !StringUtils.hasText(incoming.getConfigKey())) {
                continue;
            }

            QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("config_key", incoming.getConfigKey());
            SystemConfig existing = getOne(queryWrapper, false);

            if (existing == null) {
                SystemConfig created = new SystemConfig();
                created.setConfigKey(incoming.getConfigKey());
                created.setConfigValue(incoming.getConfigValue());
                created.setDescription(resolveDescription(incoming));
                save(created);
                continue;
            }

            existing.setConfigValue(incoming.getConfigValue());
            existing.setDescription(resolveDescription(incoming));
            updateById(existing);
        }

        return listMergedConfigs();
    }

    @Override
    public String getConfigValue(String configKey) {
        QueryWrapper<SystemConfig> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("config_key", configKey);
        SystemConfig existing = getOne(queryWrapper, false);
        if (existing != null && StringUtils.hasText(existing.getConfigValue())) {
            return existing.getConfigValue();
        }

        SystemConfig defaultConfig = DEFAULT_CONFIGS.get(configKey);
        return defaultConfig == null ? null : defaultConfig.getConfigValue();
    }

    private String resolveDescription(SystemConfig incoming) {
        if (StringUtils.hasText(incoming.getDescription())) {
            return incoming.getDescription();
        }
        SystemConfig defaultConfig = DEFAULT_CONFIGS.get(incoming.getConfigKey());
        return defaultConfig == null ? null : defaultConfig.getDescription();
    }

    private static Map<String, SystemConfig> buildDefaultConfigs() {
        Map<String, SystemConfig> defaults = new LinkedHashMap<>();
        defaults.put("site_name", createDefault("site_name", "Story Weaver", "站点名称"));
        defaults.put("site_description", createDefault("site_description", "AI小说创作平台", "站点描述"));
        defaults.put("default_ai_model", createDefault("default_ai_model", "gpt-4.1", "默认AI模型"));
        defaults.put("default_ai_provider_id", createDefault("default_ai_provider_id", "1", "默认AI Provider"));
        defaults.put("default_embedding_provider_id", createDefault("default_embedding_provider_id", "1", "默认Embedding Provider"));
        defaults.put("max_chapter_length", createDefault("max_chapter_length", "5000", "章节最大字数"));
        defaults.put("auto_save_interval", createDefault("auto_save_interval", "300", "自动保存间隔（秒）"));
        defaults.put("rag_enabled", createDefault("rag_enabled", "true", "是否启用RAG"));
        defaults.put("registration_enabled", createDefault("registration_enabled", "true", "是否允许注册"));
        defaults.put("default_theme", createDefault("default_theme", "light", "默认主题"));
        defaults.put("prompt.continue", createDefault("prompt.continue", "继续当前章节的叙事节奏，保持人物口吻和世界设定一致，优先推进当前冲突。", "续写 Prompt 模板"));
        defaults.put("prompt.expand", createDefault("prompt.expand", "在不偏离原意的前提下补足细节、动作、环境与情绪描写，让场景更饱满。", "扩写 Prompt 模板"));
        defaults.put("prompt.rewrite", createDefault("prompt.rewrite", "保留关键信息与剧情目标，重写表达方式，提升清晰度、节奏和戏剧性。", "改写 Prompt 模板"));
        defaults.put("prompt.polish", createDefault("prompt.polish", "在不改变剧情事实的前提下润色语言，让句子更自然、流畅且有画面感。", "润色 Prompt 模板"));
        defaults.put("prompt.plot", createDefault("prompt.plot", "围绕当前章节和角色目标，输出可执行的剧情推进建议，明确冲突、转折和结果。", "剧情 Prompt 模板"));
        defaults.put("prompt.causality", createDefault("prompt.causality", "分析事件之间的因果链路，明确原因、结果、触发条件和影响强度。", "因果 Prompt 模板"));
        defaults.put("prompt.rag_query", createDefault("prompt.rag_query", "根据当前项目上下文组织检索关键词，优先召回与角色、章节、剧情和因果强相关的知识片段。", "RAG 检索 Prompt 模板"));
        defaults.put("prompt.knowledge_extract", createDefault("prompt.knowledge_extract", "从确认后的正文中抽取人物、设定、事件、因果和可复用事实，整理成适合入库的知识摘要。", "知识抽取 Prompt 模板"));
        return defaults;
    }

    private static SystemConfig createDefault(String key, String value, String description) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setDescription(description);
        return config;
    }

    private static SystemConfig copyConfig(SystemConfig source) {
        SystemConfig target = new SystemConfig();
        target.setConfigKey(source.getConfigKey());
        target.setConfigValue(source.getConfigValue());
        target.setDescription(source.getDescription());
        return target;
    }
}
