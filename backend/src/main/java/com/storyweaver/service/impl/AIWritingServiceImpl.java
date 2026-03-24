package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.KnowledgeDocumentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AIWritingServiceImpl extends ServiceImpl<AIWritingRecordMapper, AIWritingRecord> implements AIWritingService {

    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final AIProviderService aiProviderService;
    private final Random random = new Random();

    public AIWritingServiceImpl(
            ChapterService chapterService,
            KnowledgeDocumentService knowledgeDocumentService,
            AIProviderService aiProviderService) {
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.aiProviderService = aiProviderService;
    }

    @Override
    public AIWritingResponseVO generateContent(AIWritingRequestDTO requestDTO) {
        Chapter chapter = chapterService.getById(requestDTO.getChapterId());
        if (chapter == null) {
            throw new IllegalArgumentException("chapter not found");
        }

        AIProvider selectedProvider = resolveProvider(requestDTO.getSelectedProviderId());
        Long selectedProviderId = selectedProvider != null ? selectedProvider.getId() : requestDTO.getSelectedProviderId();
        String selectedModel = StringUtils.hasText(requestDTO.getSelectedModel())
                ? requestDTO.getSelectedModel()
                : selectedProvider != null ? selectedProvider.getModelName() : null;
        String promptSnapshot = StringUtils.hasText(requestDTO.getPromptSnapshot())
                ? requestDTO.getPromptSnapshot()
                : requestDTO.getUserInstruction();

        String generatedContent = generateMockAIContent(
                requestDTO.getCurrentContent(),
                requestDTO.getWritingType(),
                requestDTO.getUserInstruction(),
                requestDTO.getMaxTokens());

        AIWritingRecord record = new AIWritingRecord();
        record.setChapterId(requestDTO.getChapterId());
        record.setOriginalContent(requestDTO.getCurrentContent());
        record.setGeneratedContent(generatedContent);
        record.setWritingType(StringUtils.hasText(requestDTO.getWritingType()) ? requestDTO.getWritingType() : "continue");
        record.setUserInstruction(requestDTO.getUserInstruction());
        record.setSelectedProviderId(selectedProviderId);
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
            if (provider != null && !Integer.valueOf(1).equals(provider.getDeleted())) {
                return provider;
            }
        }

        return aiProviderService.listProviders().stream()
                .filter(item -> Integer.valueOf(1).equals(item.getIsDefault()))
                .findFirst()
                .orElse(null);
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

    private String generateMockAIContent(String currentContent, String writingType, String userInstruction, Integer maxTokens) {
        StringBuilder content = new StringBuilder();

        if (writingType == null || "continue".equals(writingType)) {
            content.append("[AI continuation]\n\n");
            content.append("As the story moves forward, ");

            String[] continuations = {
                    "the protagonist senses a new threat and steadies himself before stepping into the unknown.",
                    "the quiet atmosphere breaks as a hidden clue pushes the conflict toward a sharper turning point.",
                    "an unexpected ally arrives and changes the direction of the entire chapter.",
                    "memories from the past resurface, reframing the choice that now stands in front of everyone.",
                    "the scene grows tense, and even a small action begins to carry irreversible consequences."
            };
            content.append(continuations[random.nextInt(continuations.length)]);

            if (StringUtils.hasText(userInstruction)) {
                content.append("\n\nAdditional instruction: ").append(userInstruction);
            }
        } else if ("polish".equals(writingType)) {
            content.append("[AI polish]\n\n");
            content.append(currentContent);
            content.append("\n\nThe prose becomes tighter, smoother, and more vivid while preserving the plot facts.");
        } else if ("expand".equals(writingType)) {
            content.append("[AI expansion]\n\n");
            content.append(currentContent);
            content.append("\n\nThe scene is expanded with richer sensory detail, emotional beats, and movement.");
        } else if ("rewrite".equals(writingType)) {
            content.append("[AI rewrite]\n\n");
            content.append("The passage is rewritten with clearer rhythm, sharper dramatic focus, and preserved story intent.");
        } else {
            content.append("[AI draft]\n\n");
            content.append(currentContent);
        }

        String result = content.toString();
        if (maxTokens != null && result.length() > maxTokens) {
            result = result.substring(0, maxTokens);
        }
        return result;
    }

    private AIWritingResponseVO convertToVO(AIWritingRecord record) {
        AIWritingResponseVO vo = new AIWritingResponseVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
