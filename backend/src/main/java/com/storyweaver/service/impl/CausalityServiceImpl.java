package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Causality;
import com.storyweaver.repository.CausalityMapper;
import com.storyweaver.service.CausalityService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CausalityServiceImpl extends ServiceImpl<CausalityMapper, Causality> implements CausalityService {

    private final ProjectService projectService;

    public CausalityServiceImpl(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public List<Causality> getProjectCausalities(Long projectId, Long userId) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return List.of();
        }
        return baseMapper.findByProjectId(projectId);
    }

    @Override
    public Causality getCausality(Long id, Long userId) {
        Causality causality = getById(id);
        if (causality == null || !projectService.hasProjectAccess(causality.getProjectId(), userId)) {
            return null;
        }
        return causality;
    }

    @Override
    @Transactional
    public Causality createCausality(Long projectId, Long userId, Causality causality) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }
        normalizeCausality(causality);
        causality.setId(null);
        causality.setProjectId(projectId);
        causality.setCreateBy(userId);
        causality.setUpdateBy(userId);
        causality.setCreateTime(LocalDateTime.now());
        causality.setUpdateTime(LocalDateTime.now());
        if (causality.getStatus() == null) {
            causality.setStatus(1);
        }
        if (causality.getStrength() == null) {
            causality.setStrength(50);
        }
        save(causality);
        return causality;
    }

    @Override
    @Transactional
    public boolean updateCausality(Long id, Long userId, Causality causality) {
        Causality existing = getCausality(id, userId);
        if (existing == null) {
            return false;
        }
        normalizeCausality(causality);
        existing.setName(causality.getName());
        existing.setDescription(causality.getDescription());
        existing.setCauseType(causality.getCauseType());
        existing.setEffectType(causality.getEffectType());
        existing.setCauseEntityId(causality.getCauseEntityId());
        existing.setEffectEntityId(causality.getEffectEntityId());
        existing.setCauseEntityType(causality.getCauseEntityType());
        existing.setEffectEntityType(causality.getEffectEntityType());
        existing.setRelationship(causality.getRelationship());
        existing.setCausalType(causality.getCausalType());
        existing.setTriggerMode(causality.getTriggerMode());
        existing.setPayoffStatus(causality.getPayoffStatus());
        existing.setUpstreamCauseIdsJson(causality.getUpstreamCauseIdsJson());
        existing.setDownstreamEffectIdsJson(causality.getDownstreamEffectIdsJson());
        existing.setStrength(causality.getStrength());
        existing.setConditions(causality.getConditions());
        existing.setTags(causality.getTags());
        existing.setStatus(causality.getStatus());
        existing.setUpdateBy(userId);
        existing.setUpdateTime(LocalDateTime.now());
        return updateById(existing);
    }

    @Override
    @Transactional
    public boolean deleteCausality(Long id, Long userId) {
        Causality existing = getCausality(id, userId);
        if (existing == null) {
            return false;
        }
        return removeById(id);
    }

    private void normalizeCausality(Causality causality) {
        if (causality == null) {
            return;
        }
        causality.setCauseEntityType(normalizeEntityType(causality.getCauseEntityType()));
        causality.setEffectEntityType(normalizeEntityType(causality.getEffectEntityType()));
        causality.setCauseEntityId(normalizeEntityId(causality.getCauseEntityId()));
        causality.setEffectEntityId(normalizeEntityId(causality.getEffectEntityId()));

        if (!StringUtils.hasText(causality.getCausalType())) {
            causality.setCausalType(resolveCausalType(causality.getRelationship()));
        }
        if (!StringUtils.hasText(causality.getRelationship())) {
            causality.setRelationship(resolveLegacyRelationship(causality.getCausalType()));
        }
        if (!StringUtils.hasText(causality.getTriggerMode())) {
            causality.setTriggerMode(StringUtils.hasText(causality.getConditions()) ? "conditional" : "instant");
        }
        if (!StringUtils.hasText(causality.getPayoffStatus())) {
            causality.setPayoffStatus("pending");
        }
    }

    private String normalizeEntityType(String entityType) {
        if (!StringUtils.hasText(entityType)) {
            return entityType;
        }
        return switch (entityType.trim()) {
            case "plot" -> "story_beat";
            case "knowledge", "writing", "manual" -> "state";
            default -> entityType.trim();
        };
    }

    private String normalizeEntityId(String entityId) {
        if (!StringUtils.hasText(entityId)) {
            return entityId;
        }
        String value = entityId.trim();
        if (value.startsWith("chapter-") || value.startsWith("plot-") || value.startsWith("character-")) {
            return value.substring(value.indexOf('-') + 1);
        }
        if (value.startsWith("plot:")) {
            return value.substring(value.indexOf(':') + 1);
        }
        return value;
    }

    private String resolveCausalType(String relationship) {
        if (!StringUtils.hasText(relationship)) {
            return "trigger";
        }
        return switch (relationship.trim()) {
            case "lead_to", "escalates" -> "lead_to";
            case "block", "blocks" -> "block";
            case "reverse", "motivates" -> "reverse";
            case "foreshadow", "reveals" -> "foreshadow";
            case "payoff", "resolves" -> "payoff";
            case "escalate" -> "escalate";
            default -> "trigger";
        };
    }

    private String resolveLegacyRelationship(String causalType) {
        if (!StringUtils.hasText(causalType)) {
            return "causes";
        }
        return switch (causalType.trim()) {
            case "lead_to" -> "escalates";
            case "block" -> "blocks";
            case "reverse" -> "motivates";
            case "foreshadow" -> "reveals";
            case "payoff" -> "resolves";
            default -> "causes";
        };
    }
}
