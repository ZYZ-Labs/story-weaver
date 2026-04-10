package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.story.generation.StructuredCreationSuggestion;
import com.storyweaver.story.generation.StructuredCreationSuggestionService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class StructuredCreationSuggestionServiceImpl implements StructuredCreationSuggestionService {

    private static final Pattern NAME_ACTION_PATTERN = Pattern.compile(
            "([\\u4e00-\\u9fa5]{2,4})(?:说|道|问|答|想|看|笑|喊|低声|抬头|皱眉|点头|转身|看向|望向|走向|走到|盯着|开口|沉声)"
    );

    private static final Set<String> NAME_STOPWORDS = Set.of(
            "自己", "时候", "地方", "声音", "目光", "身体", "空气", "周围", "这里", "那里",
            "现在", "今天", "终于", "只是", "已经", "如果", "于是", "然后", "没有", "不能",
            "不是", "仿佛", "因为", "所以", "眼前", "心里", "门口", "身后", "面前", "此刻"
    );

    private static final List<String> CAUSALITY_MARKERS = List.of("因为", "导致", "使得", "于是", "从而", "埋下", "引出", "触发");

    private final CharacterService characterService;
    private final ChapterService chapterService;

    public StructuredCreationSuggestionServiceImpl(
            CharacterService characterService,
            ChapterService chapterService) {
        this.characterService = characterService;
        this.chapterService = chapterService;
    }

    @Override
    public List<StructuredCreationSuggestion> suggestFromText(Long userId, Long projectId, Long chapterId, String text) {
        if (projectId == null || !StringUtils.hasText(text)) {
            return List.of();
        }

        Set<String> canonNames = resolveCanonNames(characterService.getProjectCharacters(projectId, userId));
        Chapter chapter = chapterId == null ? null : chapterService.getChapterWithAuth(chapterId, userId);

        List<StructuredCreationSuggestion> suggestions = new ArrayList<>();
        suggestions.addAll(extractCharacterSuggestions(text, chapter, canonNames));
        StructuredCreationSuggestion causalitySuggestion = extractCausalitySuggestion(text, chapter);
        if (causalitySuggestion != null) {
            suggestions.add(causalitySuggestion);
        }
        return suggestions;
    }

    private List<StructuredCreationSuggestion> extractCharacterSuggestions(
            String text,
            Chapter chapter,
            Set<String> canonNames) {
        Map<String, Integer> counter = new LinkedHashMap<>();
        Matcher matcher = NAME_ACTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (!StringUtils.hasText(candidate)) {
                continue;
            }

            String normalized = candidate.trim();
            if (normalized.length() < 2 || normalized.length() > 4) {
                continue;
            }
            if (NAME_STOPWORDS.contains(normalized)) {
                continue;
            }
            if (canonNames.contains(normalized)) {
                continue;
            }
            counter.merge(normalized, 1, Integer::sum);
        }

        List<StructuredCreationSuggestion> suggestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counter.entrySet()) {
            if (entry.getValue() < 2) {
                continue;
            }
            StructuredCreationSuggestion suggestion = new StructuredCreationSuggestion();
            suggestion.setEntityType("character");
            suggestion.setSummary("正文中出现了未建档人物候选：" + entry.getKey());
            suggestion.setSourceExcerpt(findExcerpt(text, entry.getKey()));
            suggestion.setSourceChapterId(chapter == null ? null : chapter.getId());
            suggestion.putCandidateField("name", entry.getKey());
            suggestion.putCandidateField(
                    "description",
                    "由正文自动识别出的待确认人物，建议作者补全身份、目标和关系后再正式使用。"
            );
            suggestion.putCandidateField("projectRole", "配角");
            suggestion.putCandidateField("roleType", "配角");
            if (chapter != null) {
                suggestion.putCandidateField("firstAppearanceChapterId", chapter.getId());
                suggestion.putCandidateField("activeStage", firstNonBlank(chapter.getChapterStatus(), "draft"));
            }
            suggestions.add(suggestion);
            if (suggestions.size() >= 2) {
                break;
            }
        }
        return suggestions;
    }

    private StructuredCreationSuggestion extractCausalitySuggestion(String text, Chapter chapter) {
        for (String sentence : splitSentences(text)) {
            String normalized = normalizeSentence(sentence);
            if (!StringUtils.hasText(normalized) || normalized.length() < 18) {
                continue;
            }
            String marker = detectCausalityMarker(normalized);
            if (!StringUtils.hasText(marker)) {
                continue;
            }

            StructuredCreationSuggestion suggestion = new StructuredCreationSuggestion();
            suggestion.setEntityType("causality");
            suggestion.setSummary("正文中出现了可结构化的因果推进候选。");
            suggestion.setSourceExcerpt(normalized);
            suggestion.setSourceChapterId(chapter == null ? null : chapter.getId());
            suggestion.putCandidateField(
                    "name",
                    chapter == null
                            ? "新增因果候选"
                            : "第 " + (chapter.getOrderNum() == null ? "?" : chapter.getOrderNum()) + " 章因果候选"
            );
            suggestion.putCandidateField("description", normalized);
            suggestion.putCandidateField("relationship", resolveRelationship(marker));
            suggestion.putCandidateField("causalType", resolveCausalType(marker));
            suggestion.putCandidateField("triggerMode", "conditional");
            suggestion.putCandidateField("conditions", "来源章节正文：" + normalized);
            return suggestion;
        }
        return null;
    }

    private String detectCausalityMarker(String sentence) {
        for (String marker : CAUSALITY_MARKERS) {
            if (sentence.contains(marker)) {
                return marker;
            }
        }
        return null;
    }

    private String resolveRelationship(String marker) {
        if (!StringUtils.hasText(marker)) {
            return "causes";
        }
        return switch (marker) {
            case "埋下", "引出" -> "reveals";
            case "导致", "使得", "从而" -> "escalates";
            default -> "causes";
        };
    }

    private String resolveCausalType(String marker) {
        if (!StringUtils.hasText(marker)) {
            return "trigger";
        }
        return switch (marker) {
            case "埋下", "引出" -> "foreshadow";
            case "导致", "使得", "从而" -> "lead_to";
            default -> "trigger";
        };
    }

    private List<String> splitSentences(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        for (String sentence : text.replace("\r\n", "\n").split("[。！？\\n]+")) {
            String normalized = normalizeSentence(sentence);
            if (StringUtils.hasText(normalized)) {
                sentences.add(normalized);
            }
        }
        return sentences;
    }

    private String findExcerpt(String text, String candidate) {
        for (String sentence : splitSentences(text)) {
            if (sentence.contains(candidate)) {
                return truncate(sentence, 80);
            }
        }
        return truncate(text, 80);
    }

    private Set<String> resolveCanonNames(List<Character> characters) {
        Set<String> names = new LinkedHashSet<>();
        if (characters == null) {
            return names;
        }
        for (Character character : characters) {
            if (character != null && StringUtils.hasText(character.getName())) {
                names.add(character.getName().trim());
            }
        }
        return names;
    }

    private String normalizeSentence(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim().toLowerCase(Locale.ROOT) : null;
    }

    private String truncate(String text, int maxLength) {
        String normalized = normalizeSentence(text);
        if (!StringUtils.hasText(normalized) || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).trim() + "...";
    }
}
