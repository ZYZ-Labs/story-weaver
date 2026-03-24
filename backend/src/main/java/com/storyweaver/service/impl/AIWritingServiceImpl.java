package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class AIWritingServiceImpl extends ServiceImpl<AIWritingRecordMapper, AIWritingRecord> implements AIWritingService {

    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AIProviderService aiProviderService;
    private final ProjectService projectService;
    private final SystemConfigService systemConfigService;

    public AIWritingServiceImpl(
            ChapterService chapterService,
            KnowledgeDocumentService knowledgeDocumentService,
            AIProviderService aiProviderService,
            ProjectService projectService,
            SystemConfigService systemConfigService) {
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.aiProviderService = aiProviderService;
        this.projectService = projectService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public AIWritingResponseVO generateContent(AIWritingRequestDTO requestDTO) {
        Chapter chapter = chapterService.getById(requestDTO.getChapterId());
        if (chapter == null || Integer.valueOf(1).equals(chapter.getDeleted())) {
            throw new IllegalArgumentException("章节不存在");
        }

        String currentContent = normalizeText(requestDTO.getCurrentContent());
        String writingType = normalizeWritingType(requestDTO.getWritingType(), currentContent);
        AIProvider provider = resolveProvider(requestDTO.getSelectedProviderId());
        String selectedModel = resolveModelName(provider, requestDTO.getSelectedModel());
        String promptSnapshot = resolvePromptSnapshot(requestDTO.getPromptSnapshot(), writingType);
        Project project = chapter.getProjectId() == null ? null : projectService.getById(chapter.getProjectId());

        String generatedContent = aiProviderService.generateText(
                provider,
                selectedModel,
                buildSystemPrompt(promptSnapshot),
                buildUserPrompt(project, chapter, currentContent, writingType, normalizeText(requestDTO.getUserInstruction())),
                null,
                requestDTO.getMaxTokens()
        );

        if (!StringUtils.hasText(generatedContent)) {
            throw new IllegalStateException("模型没有返回可用的正文内容，请稍后重试");
        }

        AIWritingRecord record = new AIWritingRecord();
        record.setChapterId(requestDTO.getChapterId());
        record.setOriginalContent(currentContent);
        record.setGeneratedContent(generatedContent.trim());
        record.setWritingType(writingType);
        record.setUserInstruction(normalizeText(requestDTO.getUserInstruction()));
        record.setSelectedProviderId(provider.getId());
        record.setSelectedModel(selectedModel);
        record.setPromptSnapshot(promptSnapshot);
        record.setStatus("draft");

        save(record);
        return convertToVO(record);
    }

    @Override
    public List<AIWritingResponseVO> getRecordsByChapterId(Long chapterId) {
        LambdaQueryWrapper<AIWritingRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIWritingRecord::getChapterId, chapterId)
                .eq(AIWritingRecord::getDeleted, 0)
                .orderByDesc(AIWritingRecord::getCreateTime);

        return list(queryWrapper).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AIWritingResponseVO> getRecordsByProjectId(Long projectId) {
        return baseMapper.findByProjectId(projectId).stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    @Override
    public AIWritingResponseVO getRecordById(Long id) {
        AIWritingRecord record = getById(id);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            return null;
        }
        return convertToVO(record);
    }

    @Override
    public AIWritingResponseVO acceptGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record == null || Integer.valueOf(1).equals(record.getDeleted())) {
            return null;
        }

        Chapter chapter = chapterService.getById(record.getChapterId());
        if (chapter != null) {
            if ("continue".equals(record.getWritingType())) {
                String currentContent = chapter.getContent() == null ? "" : chapter.getContent();
                String newContent = currentContent.isBlank()
                        ? record.getGeneratedContent()
                        : currentContent + "\n\n" + record.getGeneratedContent();
                chapter.setContent(newContent);
            } else {
                chapter.setContent(record.getGeneratedContent());
            }
            chapter.setWordCount(chapter.getContent() == null ? 0 : chapter.getContent().length());
            chapterService.updateById(chapter);
            syncKnowledgeDocument(chapter, record);
        }

        record.setStatus("accepted");
        updateById(record);
        return convertToVO(record);
    }

    @Override
    public void rejectGeneratedContent(Long id) {
        AIWritingRecord record = getById(id);
        if (record != null && !Integer.valueOf(1).equals(record.getDeleted())) {
            record.setStatus("rejected");
            updateById(record);
        }
    }

    private AIProvider resolveProvider(Long selectedProviderId) {
        if (selectedProviderId != null) {
            AIProvider provider = aiProviderService.getById(selectedProviderId);
            if (provider != null
                    && !Integer.valueOf(1).equals(provider.getDeleted())
                    && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        Long configuredProviderId = parseLong(systemConfigService.getConfigValue("default_ai_provider_id"));
        if (configuredProviderId != null) {
            AIProvider provider = aiProviderService.getById(configuredProviderId);
            if (provider != null
                    && !Integer.valueOf(1).equals(provider.getDeleted())
                    && Integer.valueOf(1).equals(provider.getEnabled())) {
                return provider;
            }
        }

        return aiProviderService.listProviders().stream()
                .filter(item -> Integer.valueOf(1).equals(item.getEnabled()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("当前没有可用的默认模型服务，请先在模型服务页启用一个 Provider"));
    }

    private String resolveModelName(AIProvider provider, String selectedModel) {
        if (StringUtils.hasText(selectedModel)) {
            return selectedModel.trim();
        }

        if (provider != null && StringUtils.hasText(provider.getModelName())) {
            return provider.getModelName().trim();
        }

        String configuredModel = systemConfigService.getConfigValue("default_ai_model");
        if (StringUtils.hasText(configuredModel)) {
            return configuredModel.trim();
        }

        throw new IllegalStateException("当前默认模型尚未配置，请先在系统设置里指定默认模型");
    }

    private String resolvePromptSnapshot(String requestPromptSnapshot, String writingType) {
        if (StringUtils.hasText(requestPromptSnapshot)) {
            return requestPromptSnapshot.trim();
        }

        String configuredPrompt = systemConfigService.getConfigValue(resolvePromptKey(writingType));
        if (StringUtils.hasText(configuredPrompt)) {
            return configuredPrompt.trim();
        }

        return getDefaultPromptTemplate(writingType);
    }

    private String buildSystemPrompt(String promptSnapshot) {
        return """
                你是一名中文长篇小说写作助手。
                请只输出可直接使用的小说正文，不要解释，不要标题，不要 Markdown 标记，不要列提纲。
                %s
                """.formatted(promptSnapshot);
    }

    private String buildUserPrompt(
            Project project,
            Chapter chapter,
            String currentContent,
            String writingType,
            String userInstruction) {
        StringBuilder builder = new StringBuilder();
        builder.append("请帮助我处理当前章节正文。\n");
        if (project != null) {
            builder.append("项目名称：").append(safe(project.getName(), "未命名项目")).append('\n');
            if (StringUtils.hasText(project.getGenre())) {
                builder.append("项目题材：").append(project.getGenre().trim()).append('\n');
            }
            if (StringUtils.hasText(project.getDescription())) {
                builder.append("项目简介：").append(project.getDescription().trim()).append('\n');
            }
        }

        builder.append("章节标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        if (chapter.getOrderNum() != null) {
            builder.append("章节顺序：第 ").append(chapter.getOrderNum()).append(" 章\n");
        }

        switch (writingType) {
            case "draft" -> builder.append("当前正文为空，需要先拟生成一段可继续扩写的章节初稿。\n");
            case "continue" -> builder.append("请基于已有正文自然续写后续内容，不要重复已经写过的句子。\n");
            case "expand" -> builder.append("请在保留现有情节事实的前提下，输出扩写后的完整章节版本。\n");
            case "rewrite" -> builder.append("请保留核心剧情意图，输出重写后的完整章节版本。\n");
            case "polish" -> builder.append("请保留剧情事实，输出润色后的完整章节版本。\n");
            default -> builder.append("请输出可直接放入章节中的正文内容。\n");
        }

        if (StringUtils.hasText(currentContent)) {
            builder.append("\n当前正文：\n").append(currentContent.trim()).append('\n');
        } else {
            builder.append("\n当前正文：\n（暂无正文内容）\n");
        }

        if (StringUtils.hasText(userInstruction)) {
            builder.append("\n补充要求：\n").append(userInstruction.trim()).append('\n');
        }

        return builder.toString();
    }

    private void syncKnowledgeDocument(Chapter chapter, AIWritingRecord record) {
        if (chapter.getProjectId() == null) {
            return;
        }

        QueryWrapper<KnowledgeDocument> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", chapter.getProjectId())
                .eq("source_type", "chapter")
                .eq("source_ref_id", String.valueOf(chapter.getId()))
                .eq("deleted", 0);

        KnowledgeDocument document = knowledgeDocumentService.getOne(queryWrapper, false);
        if (document == null) {
            document = new KnowledgeDocument();
            document.setProjectId(chapter.getProjectId());
            document.setSourceType("chapter");
            document.setSourceRefId(String.valueOf(chapter.getId()));
        }

        document.setTitle(chapter.getTitle());
        document.setContentText(chapter.getContent());
        document.setSummary(buildSummary(record.getGeneratedContent()));
        document.setStatus("indexed");

        if (document.getId() == null) {
            knowledgeDocumentService.save(document);
        } else {
            knowledgeDocumentService.updateById(document);
        }
    }

    private String buildSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "Accepted AI writing synchronized to RAG.";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private String normalizeWritingType(String writingType, String currentContent) {
        String normalized = StringUtils.hasText(writingType)
                ? writingType.trim().toLowerCase(Locale.ROOT)
                : "";

        if (!StringUtils.hasText(currentContent)) {
            if (!StringUtils.hasText(normalized)
                    || "continue".equals(normalized)
                    || "expand".equals(normalized)) {
                return "draft";
            }
        }

        return switch (normalized) {
            case "draft", "continue", "expand", "rewrite", "polish" -> normalized;
            default -> StringUtils.hasText(currentContent) ? "continue" : "draft";
        };
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String resolvePromptKey(String writingType) {
        return switch (writingType) {
            case "draft" -> "prompt.draft";
            case "continue" -> "prompt.continue";
            case "expand" -> "prompt.expand";
            case "rewrite" -> "prompt.rewrite";
            case "polish" -> "prompt.polish";
            default -> "prompt.continue";
        };
    }

    private String getDefaultPromptTemplate(String writingType) {
        return switch (writingType) {
            case "draft" -> "先根据章节标题和补充要求拟生成一段可继续扩写的小说正文初稿，优先搭好场景、人物状态和冲突起点。";
            case "continue" -> "延续当前章节的叙事节奏，保持人物口吻与设定一致，优先推进当前冲突。";
            case "expand" -> "在不偏离原意的前提下补足细节、动作、环境与情绪描写，让场景更饱满。";
            case "rewrite" -> "保留关键信息与剧情目标，重写表达方式，提升节奏、清晰度和戏剧性。";
            case "polish" -> "在不改变剧情事实的前提下润色语言，让句子更自然、流畅、有画面感。";
            default -> "输出可直接用于章节正文的中文小说内容。";
        };
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private AIWritingResponseVO convertToVO(AIWritingRecord record) {
        AIWritingResponseVO vo = new AIWritingResponseVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
