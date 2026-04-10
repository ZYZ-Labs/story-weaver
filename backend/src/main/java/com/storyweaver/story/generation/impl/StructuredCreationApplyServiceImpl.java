package com.storyweaver.story.generation.impl;

import com.storyweaver.domain.dto.CharacterRequestDTO;
import com.storyweaver.domain.dto.StructuredCreationApplyRequestDTO;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.CharacterService;
import com.storyweaver.service.PlotCrudService;
import com.storyweaver.service.ProjectService;
import com.storyweaver.story.generation.StructuredCreationApplyResult;
import com.storyweaver.story.generation.StructuredCreationApplyService;
import com.storyweaver.story.generation.StructuredCreationSuggestion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class StructuredCreationApplyServiceImpl implements StructuredCreationApplyService {

    private final ProjectService projectService;
    private final CharacterService characterService;
    private final CausalityService causalityService;
    private final PlotCrudService plotCrudService;

    public StructuredCreationApplyServiceImpl(
            ProjectService projectService,
            CharacterService characterService,
            CausalityService causalityService,
            PlotCrudService plotCrudService) {
        this.projectService = projectService;
        this.characterService = characterService;
        this.causalityService = causalityService;
        this.plotCrudService = plotCrudService;
    }

    @Override
    @Transactional
    public StructuredCreationApplyResult apply(Long userId, StructuredCreationApplyRequestDTO requestDTO) {
        if (requestDTO == null || requestDTO.getProjectId() == null) {
            throw new IllegalArgumentException("projectId 不能为空");
        }
        if (!projectService.hasProjectAccess(requestDTO.getProjectId(), userId)) {
            throw new IllegalArgumentException("项目不存在或无权访问");
        }
        StructuredCreationSuggestion suggestion = requestDTO.getSuggestion();
        if (suggestion == null || !StringUtils.hasText(suggestion.getEntityType())) {
            throw new IllegalArgumentException("待确认新增对象不能为空");
        }

        return switch (suggestion.getEntityType().trim()) {
            case "character" -> applyCharacter(userId, requestDTO.getProjectId(), suggestion);
            case "causality" -> applyCausality(userId, requestDTO.getProjectId(), suggestion);
            case "plot" -> applyPlot(userId, requestDTO.getProjectId(), suggestion);
            default -> throw new IllegalArgumentException("不支持的新增对象类型: " + suggestion.getEntityType());
        };
    }

    private StructuredCreationApplyResult applyCharacter(
            Long userId,
            Long projectId,
            StructuredCreationSuggestion suggestion) {
        Map<String, Object> fields = normalizeFields(suggestion);
        CharacterRequestDTO requestDTO = new CharacterRequestDTO();
        requestDTO.setName(firstNonBlank(asString(fields.get("name")), "待确认人物"));
        requestDTO.setDescription(asString(fields.get("description")));
        requestDTO.setIdentity(asString(fields.get("identity")));
        requestDTO.setCoreGoal(asString(fields.get("coreGoal")));
        requestDTO.setGrowthArc(asString(fields.get("growthArc")));
        requestDTO.setActiveStage(asString(fields.get("activeStage")));
        requestDTO.setProjectRole(firstNonBlank(asString(fields.get("projectRole")), "配角"));
        requestDTO.setRoleType(firstNonBlank(asString(fields.get("roleType")), requestDTO.getProjectRole()));
        requestDTO.setFirstAppearanceChapterId(firstNonNull(asLong(fields.get("firstAppearanceChapterId")), suggestion.getSourceChapterId()));

        com.storyweaver.domain.entity.Character created = characterService.createCharacter(projectId, userId, requestDTO);
        if (created == null) {
            throw new IllegalStateException("新增人物创建失败");
        }

        StructuredCreationApplyResult result = new StructuredCreationApplyResult();
        result.setEntityType("character");
        result.setCreatedId(created.getId());
        result.setCreated(created);
        return result;
    }

    private StructuredCreationApplyResult applyCausality(
            Long userId,
            Long projectId,
            StructuredCreationSuggestion suggestion) {
        Map<String, Object> fields = normalizeFields(suggestion);
        Causality causality = new Causality();
        causality.setName(firstNonBlank(asString(fields.get("name")), "待确认因果"));
        causality.setDescription(asString(fields.get("description")));
        causality.setRelationship(firstNonBlank(asString(fields.get("relationship")), "causes"));
        causality.setCausalType(firstNonBlank(asString(fields.get("causalType")), "trigger"));
        causality.setTriggerMode(firstNonBlank(asString(fields.get("triggerMode")), "conditional"));
        causality.setConditions(asString(fields.get("conditions")));
        causality.setCauseEntityType(asString(fields.get("causeEntityType")));
        causality.setCauseEntityId(asString(fields.get("causeEntityId")));
        causality.setEffectEntityType(asString(fields.get("effectEntityType")));
        causality.setEffectEntityId(asString(fields.get("effectEntityId")));

        Causality created = causalityService.createCausality(projectId, userId, causality);
        if (created == null) {
            throw new IllegalStateException("新增因果创建失败");
        }

        StructuredCreationApplyResult result = new StructuredCreationApplyResult();
        result.setEntityType("causality");
        result.setCreatedId(created.getId());
        result.setCreated(created);
        return result;
    }

    private StructuredCreationApplyResult applyPlot(
            Long userId,
            Long projectId,
            StructuredCreationSuggestion suggestion) {
        Map<String, Object> fields = normalizeFields(suggestion);
        Plot plot = new Plot();
        plot.setChapterId(firstNonNull(asLong(fields.get("chapterId")), suggestion.getSourceChapterId()));
        plot.setTitle(firstNonBlank(asString(fields.get("title")), firstNonBlank(asString(fields.get("name")), "待确认剧情节点")));
        plot.setDescription(asString(fields.get("description")));
        plot.setContent(asString(fields.get("content")));
        plot.setPlotType(firstNonNull(asInteger(fields.get("plotType")), 1));
        plot.setStoryBeatType(asString(fields.get("storyBeatType")));
        plot.setStoryFunction(asString(fields.get("storyFunction")));

        Plot created = plotCrudService.createPlot(projectId, userId, plot);
        if (created == null) {
            throw new IllegalStateException("新增剧情节点创建失败");
        }

        StructuredCreationApplyResult result = new StructuredCreationApplyResult();
        result.setEntityType("plot");
        result.setCreatedId(created.getId());
        result.setCreated(created);
        return result;
    }

    private Map<String, Object> normalizeFields(StructuredCreationSuggestion suggestion) {
        return suggestion.getCandidateFields() == null
                ? Map.of()
                : new LinkedHashMap<>(suggestion.getCandidateFields());
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = asString(value);
        return StringUtils.hasText(text) ? Long.parseLong(text) : null;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = asString(value);
        return StringUtils.hasText(text) ? Integer.parseInt(text) : null;
    }

    private String firstNonBlank(String first, String second) {
        if (StringUtils.hasText(first)) {
            return first.trim();
        }
        return StringUtils.hasText(second) ? second.trim() : null;
    }

    private Long firstNonNull(Long first, Long second) {
        return first != null ? first : second;
    }

    private Integer firstNonNull(Integer first, Integer second) {
        return first != null ? first : second;
    }
}
