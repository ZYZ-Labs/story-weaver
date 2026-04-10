package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.dto.ChapterRequestDTO;
import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.dto.ProjectRequestDTO;
import com.storyweaver.domain.dto.StructuredSummaryApplyRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Character;
import com.storyweaver.domain.entity.KnowledgeDocument;
import com.storyweaver.domain.entity.Project;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.KnowledgeDocumentService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.GenerationReadinessService;
import com.storyweaver.story.generation.GenerationReadinessVO;
import com.storyweaver.story.generation.StructuredSummaryApplyResult;
import com.storyweaver.story.generation.StructuredSummaryApplyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class StructuredSummaryApplyServiceImpl implements StructuredSummaryApplyService {

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final ChapterService chapterService;
    private final KnowledgeDocumentService knowledgeDocumentService;
    private final GenerationReadinessService generationReadinessService;

    public StructuredSummaryApplyServiceImpl(
            ProjectService projectService,
            CharacterService characterService,
            ChapterService chapterService,
            KnowledgeDocumentService knowledgeDocumentService,
            GenerationReadinessService generationReadinessService) {
        this.projectService = projectService;
        this.characterService = characterService;
        this.chapterService = chapterService;
        this.knowledgeDocumentService = knowledgeDocumentService;
        this.generationReadinessService = generationReadinessService;
    }

    @Override
    @Transactional
    public StructuredSummaryApplyResult apply(Long userId, StructuredSummaryApplyRequestDTO requestDTO) {
        if (requestDTO == null) {
            throw new IllegalArgumentException("请求不能为空");
        }

        String scope = normalizeScope(requestDTO.getScope());
        Map<String, Object> structuredFields = requestDTO.getStructuredFields() == null
                ? Map.of()
                : new LinkedHashMap<>(requestDTO.getStructuredFields());

        return switch (scope) {
            case "project" -> applyProject(userId, requestDTO, structuredFields);
            case "character" -> applyCharacter(userId, requestDTO, structuredFields);
            case "chapter" -> applyChapter(userId, requestDTO, structuredFields);
            default -> throw new IllegalArgumentException("不支持的摘要作用域: " + requestDTO.getScope());
        };
    }

    private StructuredSummaryApplyResult applyProject(
            Long userId,
            StructuredSummaryApplyRequestDTO requestDTO,
            Map<String, Object> structuredFields) {
        Long projectId = firstNonNull(requestDTO.getProjectId(), requestDTO.getTargetId());
        Project current = requireProject(projectId, userId);

        ProjectRequestDTO update = new ProjectRequestDTO();
        update.setName(current.getName());
        update.setDescription(current.getDescription());
        update.setGenre(current.getGenre());
        update.setTags(current.getTags());
        update.setWorldSettingIds(copyLongList(current.getWorldSettingIds()));

        if (structuredFields.containsKey("name")) {
            update.setName(asString(structuredFields.get("name")));
        }
        if (structuredFields.containsKey("description")) {
            update.setDescription(asString(structuredFields.get("description")));
        }
        if (structuredFields.containsKey("genre")) {
            update.setGenre(asString(structuredFields.get("genre")));
        }
        if (structuredFields.containsKey("tags")) {
            update.setTags(asString(structuredFields.get("tags")));
        }
        if (structuredFields.containsKey("worldSettingIds")) {
            update.setWorldSettingIds(asLongList(structuredFields.get("worldSettingIds")));
        }

        boolean updated = projectService.updateProject(projectId, userId, update);
        if (!updated) {
            throw new IllegalStateException("项目摘要写回失败");
        }

        Project refreshed = requireProject(projectId, userId);
        Long canonDocumentId = upsertCanonDocument(
                projectId,
                "canon_project",
                String.valueOf(projectId),
                firstNonBlank("项目摘要：" + refreshed.getName(), "项目摘要"),
                firstNonBlank(requestDTO.getCanonSummaryText(), refreshed.getDescription())
        );

        StructuredSummaryApplyResult result = new StructuredSummaryApplyResult();
        result.setScope("project");
        result.setTargetId(projectId);
        result.setTarget(refreshed);
        result.setCanonDocumentId(canonDocumentId);
        return result;
    }

    private StructuredSummaryApplyResult applyCharacter(
            Long userId,
            StructuredSummaryApplyRequestDTO requestDTO,
            Map<String, Object> structuredFields) {
        Long projectId = requestDTO.getProjectId();
        Long characterId = requestDTO.getTargetId();
        Character current = requireProjectCharacter(projectId, characterId, userId);

        CharacterRequestDTO update = new CharacterRequestDTO();
        update.setName(current.getName());
        update.setDescription(current.getDescription());
        update.setIdentity(current.getIdentity());
        update.setCoreGoal(current.getCoreGoal());
        update.setGrowthArc(current.getGrowthArc());
        update.setFirstAppearanceChapterId(current.getFirstAppearanceChapterId());
        update.setActiveStage(current.getActiveStage());
        update.setIsRetired(Integer.valueOf(1).equals(current.getIsRetired()));
        update.setAttributes(current.getAttributes());
        update.setAdvancedProfileJson(current.getAdvancedProfileJson());
        update.setProjectRole(firstNonBlank(current.getProjectRole(), current.getRoleType()));
        update.setRoleType(firstNonBlank(current.getRoleType(), current.getProjectRole()));

        if (structuredFields.containsKey("name")) {
            update.setName(asString(structuredFields.get("name")));
        }
        if (structuredFields.containsKey("description")) {
            update.setDescription(asString(structuredFields.get("description")));
        }
        if (structuredFields.containsKey("identity")) {
            update.setIdentity(asString(structuredFields.get("identity")));
        }
        if (structuredFields.containsKey("coreGoal")) {
            update.setCoreGoal(asString(structuredFields.get("coreGoal")));
        }
        if (structuredFields.containsKey("growthArc")) {
            update.setGrowthArc(asString(structuredFields.get("growthArc")));
        }
        if (structuredFields.containsKey("firstAppearanceChapterId")) {
            update.setFirstAppearanceChapterId(asLong(structuredFields.get("firstAppearanceChapterId")));
        }
        if (structuredFields.containsKey("activeStage")) {
            update.setActiveStage(asString(structuredFields.get("activeStage")));
        }
        if (structuredFields.containsKey("isRetired")) {
            update.setIsRetired(asBoolean(structuredFields.get("isRetired")));
        }
        if (structuredFields.containsKey("attributes")) {
            update.setAttributes(asString(structuredFields.get("attributes")));
        }
        if (structuredFields.containsKey("advancedProfileJson")) {
            update.setAdvancedProfileJson(asString(structuredFields.get("advancedProfileJson")));
        }
        if (structuredFields.containsKey("projectRole")) {
            String projectRole = asString(structuredFields.get("projectRole"));
            update.setProjectRole(projectRole);
            update.setRoleType(projectRole);
        }
        if (structuredFields.containsKey("roleType")) {
            String roleType = asString(structuredFields.get("roleType"));
            update.setRoleType(roleType);
            update.setProjectRole(roleType);
        }

        boolean updated = characterService.updateCharacter(projectId, characterId, userId, update);
        if (!updated) {
            throw new IllegalStateException("人物摘要写回失败");
        }

        Character refreshed = requireProjectCharacter(projectId, characterId, userId);
        Long canonDocumentId = upsertCanonDocument(
                projectId,
                "canon_character",
                String.valueOf(characterId),
                firstNonBlank("人物卡：" + refreshed.getName(), "人物卡"),
                firstNonBlank(requestDTO.getCanonSummaryText(), refreshed.getDescription())
        );

        StructuredSummaryApplyResult result = new StructuredSummaryApplyResult();
        result.setScope("character");
        result.setTargetId(characterId);
        result.setTarget(refreshed);
        result.setCanonDocumentId(canonDocumentId);
        return result;
    }

    private StructuredSummaryApplyResult applyChapter(
            Long userId,
            StructuredSummaryApplyRequestDTO requestDTO,
            Map<String, Object> structuredFields) {
        Long projectId = requestDTO.getProjectId();
        Long chapterId = requestDTO.getTargetId();
        Chapter current = requireChapter(projectId, chapterId, userId);

        ChapterRequestDTO update = new ChapterRequestDTO();
        update.setTitle(current.getTitle());
        update.setSummary(current.getSummary());
        update.setContent(current.getContent());
        update.setOrderNum(current.getOrderNum());
        update.setStatus(current.getStatus());
        update.setChapterStatus(current.getChapterStatus());
        update.setOutlineId(current.getOutlineId());
        update.setStoryBeatIds(copyLongList(current.getStoryBeatIds()));
        update.setPrevChapterId(current.getPrevChapterId());
        update.setNextChapterId(current.getNextChapterId());
        update.setMainPovCharacterId(current.getMainPovCharacterId());
        update.setRequiredCharacterIds(copyLongList(current.getRequiredCharacterIds()));

        if (structuredFields.containsKey("title")) {
            update.setTitle(asString(structuredFields.get("title")));
        }
        if (structuredFields.containsKey("summary")) {
            update.setSummary(asString(structuredFields.get("summary")));
        }
        if (structuredFields.containsKey("content")) {
            update.setContent(asString(structuredFields.get("content")));
        }
        if (structuredFields.containsKey("orderNum")) {
            update.setOrderNum(asInteger(structuredFields.get("orderNum")));
        }
        if (structuredFields.containsKey("status")) {
            update.setStatus(asInteger(structuredFields.get("status")));
        }
        if (structuredFields.containsKey("chapterStatus")) {
            update.setChapterStatus(asString(structuredFields.get("chapterStatus")));
        }
        if (structuredFields.containsKey("outlineId")) {
            update.setOutlineId(asLong(structuredFields.get("outlineId")));
        }
        if (structuredFields.containsKey("storyBeatIds")) {
            update.setStoryBeatIds(asLongList(structuredFields.get("storyBeatIds")));
        }
        if (structuredFields.containsKey("prevChapterId")) {
            update.setPrevChapterId(asLong(structuredFields.get("prevChapterId")));
        }
        if (structuredFields.containsKey("nextChapterId")) {
            update.setNextChapterId(asLong(structuredFields.get("nextChapterId")));
        }
        if (structuredFields.containsKey("mainPovCharacterId")) {
            update.setMainPovCharacterId(asLong(structuredFields.get("mainPovCharacterId")));
        }
        if (structuredFields.containsKey("requiredCharacterIds")) {
            update.setRequiredCharacterIds(asLongList(structuredFields.get("requiredCharacterIds")));
        }

        boolean updated = chapterService.updateChapter(projectId, chapterId, userId, update);
        if (!updated) {
            throw new IllegalStateException("章节摘要写回失败");
        }

        Chapter refreshed = requireChapter(projectId, chapterId, userId);
        GenerationReadinessVO readiness = generationReadinessService.evaluate(userId, chapterId);
        Long canonDocumentId = upsertCanonDocument(
                projectId,
                "canon_chapter",
                String.valueOf(chapterId),
                firstNonBlank("章节摘要：" + refreshed.getTitle(), "章节摘要"),
                firstNonBlank(requestDTO.getCanonSummaryText(), refreshed.getSummary())
        );

        StructuredSummaryApplyResult result = new StructuredSummaryApplyResult();
        result.setScope("chapter");
        result.setTargetId(chapterId);
        result.setTarget(refreshed);
        result.setCanonDocumentId(canonDocumentId);
        result.setGenerationReadiness(readiness);
        return result;
    }

    private Project requireProject(Long projectId, Long userId) {
        if (projectId == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        return projectService.getUserProjects(userId).stream()
                .filter(item -> Objects.equals(item.getId(), projectId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("项目不存在或无权访问"));
    }

    private Character requireProjectCharacter(Long projectId, Long characterId, Long userId) {
        if (projectId == null || characterId == null) {
            throw new IllegalArgumentException("projectId 和 targetId 不能为空");
        }
        return characterService.getProjectCharacters(projectId, userId).stream()
                .filter(item -> Objects.equals(item.getId(), characterId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("人物不存在、未关联当前项目或无权访问"));
    }

    private Chapter requireChapter(Long projectId, Long chapterId, Long userId) {
        if (projectId == null || chapterId == null) {
            throw new IllegalArgumentException("projectId 和 targetId 不能为空");
        }
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null || !Objects.equals(chapter.getProjectId(), projectId)) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }
        return chapter;
    }

    private Long upsertCanonDocument(
            Long projectId,
            String sourceType,
            String sourceRefId,
            String title,
            String canonSummaryText) {
        String resolvedContent = trimToNull(canonSummaryText);
        if (!StringUtils.hasText(resolvedContent)) {
            return null;
        }

        List<KnowledgeDocument> existingDocuments = knowledgeDocumentService.list();
        KnowledgeDocument existing = existingDocuments.stream()
                .filter(item -> item != null
                        && !Integer.valueOf(1).equals(item.getDeleted())
                        && Objects.equals(item.getProjectId(), projectId)
                        && Objects.equals(item.getSourceType(), sourceType)
                        && Objects.equals(item.getSourceRefId(), sourceRefId))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            KnowledgeDocument created = new KnowledgeDocument();
            created.setProjectId(projectId);
            created.setSourceType(sourceType);
            created.setSourceRefId(sourceRefId);
            created.setTitle(title);
            created.setSummary(truncate(resolvedContent, 220));
            created.setContentText(resolvedContent);
            created.setStatus("ready");
            knowledgeDocumentService.save(created);
            return created.getId();
        }

        existing.setTitle(title);
        existing.setSummary(truncate(resolvedContent, 220));
        existing.setContentText(resolvedContent);
        existing.setStatus("ready");
        knowledgeDocumentService.updateById(existing);
        return existing.getId();
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            throw new IllegalArgumentException("scope 不能为空");
        }
        return scope.trim().toLowerCase();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Iterable<?> iterable) {
            List<String> parts = new ArrayList<>();
            for (Object item : iterable) {
                if (item != null) {
                    parts.add(String.valueOf(item).trim());
                }
            }
            return parts.isEmpty() ? null : String.join(", ", parts);
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Long.parseLong(text);
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private Boolean asBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return switch (text.trim().toLowerCase()) {
            case "1", "true", "yes", "y" -> true;
            case "0", "false", "no", "n" -> false;
            default -> Boolean.parseBoolean(text);
        };
    }

    private List<Long> asLongList(Object value) {
        if (value == null) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                Long parsed = asLong(item);
                if (parsed != null) {
                    result.add(parsed);
                }
            }
            return result;
        }
        String text = asString(value);
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        for (String token : text.split(",")) {
            String trimmed = token.trim();
            if (!trimmed.isEmpty()) {
                result.add(Long.parseLong(trimmed));
            }
        }
        return result;
    }

    private List<Long> copyLongList(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return new ArrayList<>(values);
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return trimToNull(second);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text) || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength).trim();
    }
}
