package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.ConversationSummaryService;
import com.storyweaver.story.generation.SummarySuggestionPack;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ConversationSummaryServiceImpl implements ConversationSummaryService {

    private static final Map<String, List<String>> DEFAULT_SCOPE_QUESTIONS = createDefaultQuestions();

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final ChapterService chapterService;

    public ConversationSummaryServiceImpl(
            ProjectService projectService,
            CharacterService characterService,
            ChapterService chapterService) {
        this.projectService = projectService;
        this.characterService = characterService;
        this.chapterService = chapterService;
    }

    @Override
    public SummarySuggestionPack suggestProjectBrief(Long userId, Long projectId, String inputText) {
        requireProjectAccess(projectId, userId);

        SummarySuggestionPack pack = buildPack("project", inputText);
        if (StringUtils.hasText(pack.getCanonSummaryText())) {
            pack.putStructuredField("description", pack.getCanonSummaryText());
        }
        String detectedGenre = detectGenre(pack.getCanonSummaryText());
        if (StringUtils.hasText(detectedGenre)) {
            pack.putStructuredField("genre", detectedGenre);
        }
        return pack;
    }

    @Override
    public SummarySuggestionPack suggestCharacterCard(Long userId, Long projectId, Long characterId, String inputText) {
        requireProjectAccess(projectId, userId);
        if (characterId != null) {
            Character character = characterService.getCharacterWithAuth(characterId, userId);
            if (character == null) {
                throw new IllegalArgumentException("人物不存在或无权访问");
            }
        }

        SummarySuggestionPack pack = buildPack("character", inputText);
        if (StringUtils.hasText(pack.getCanonSummaryText())) {
            pack.putStructuredField("description", pack.getCanonSummaryText());
        }
        return pack;
    }

    @Override
    public SummarySuggestionPack suggestChapterBrief(Long userId, Long projectId, Long chapterId, String inputText) {
        requireProjectAccess(projectId, userId);
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null || !Objects.equals(chapter.getProjectId(), projectId)) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }

        SummarySuggestionPack pack = buildPack("chapter", inputText);
        if (StringUtils.hasText(pack.getCanonSummaryText())) {
            pack.putStructuredField("summary", pack.getCanonSummaryText());
        }
        return pack;
    }

    private SummarySuggestionPack buildPack(String scope, String inputText) {
        SummarySuggestionPack pack = SummarySuggestionPack.empty(scope);
        String normalizedText = normalizeText(inputText);
        pack.setRawInputSummary(truncate(normalizedText, 240));
        pack.setCanonSummaryText(normalizedText);
        pack.addConfidenceHint("仅整理已明确表达的信息，未提及内容不会自动补全。");

        if (!StringUtils.hasText(normalizedText) || normalizedText.length() < 60) {
            for (String question : DEFAULT_SCOPE_QUESTIONS.getOrDefault(scope, List.of())) {
                pack.addMissingQuestion(question);
            }
        }
        return pack;
    }

    private void requireProjectAccess(Long projectId, Long userId) {
        if (projectId == null || !projectService.hasProjectAccess(projectId, userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }
    }

    private String detectGenre(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        if (containsAny(text, "玄幻", "修仙", "仙侠", "宗门")) {
            return "玄幻";
        }
        if (containsAny(text, "科幻", "星际", "机甲", "赛博")) {
            return "科幻";
        }
        if (containsAny(text, "悬疑", "推理", "谜案", "侦探")) {
            return "悬疑";
        }
        if (containsAny(text, "武侠", "江湖", "门派")) {
            return "武侠";
        }
        if (containsAny(text, "历史", "朝堂", "王朝")) {
            return "历史";
        }
        if (containsAny(text, "都市", "职场", "校园", "现实")) {
            return "都市";
        }
        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim().replace("\r\n", "\n");
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }

    private static Map<String, List<String>> createDefaultQuestions() {
        Map<String, List<String>> questions = new LinkedHashMap<>();
        questions.put("project", List.of(
                "主角是谁？",
                "这个故事最核心的冲突是什么？",
                "故事最终大概要走到哪里？"
        ));
        questions.put("character", List.of(
                "这个人是谁？",
                "他现在最想得到什么？",
                "他和主角是什么关系？"
        ));
        questions.put("chapter", List.of(
                "这一章谁视角？",
                "这一章必须发生什么？",
                "这一章写到哪里停？"
        ));
        return questions;
    }
}
