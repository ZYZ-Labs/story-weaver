package com.storyweaver.ai.director.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.storyweaver.domain.dto.AIDirectorDecisionRequestDTO;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.entity.Outline;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DirectorDecisionPackAssembler {

    private final ObjectMapper objectMapper;

    public DirectorDecisionPackAssembler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AssembledDecisionPack assemble(
            Chapter chapter,
            Outline outline,
            AIDirectorDecisionRequestDTO requestDTO,
            String entryPoint,
            String stage,
            String writingMode,
            List<DirectorModuleSelection> selectedModules,
            boolean hasBackgroundContext) {
        DecisionDefaults defaults = createDefaults(chapter, outline, stage, writingMode, hasBackgroundContext);
        return assembleResolvedDecision(
                chapter,
                entryPoint,
                stage,
                writingMode,
                defaults.targetWordCount(),
                selectedModules,
                defaults.requiredFacts(),
                defaults.prohibitedMoves(),
                defaults.writerHints(),
                buildDecisionSummary(stage, writingMode, selectedModules, hasBackgroundContext),
                List.of(Map.of(
                        "mode", "heuristic",
                        "message", "当前版本使用后端规则生成 decision pack。"
                ))
        );
    }

    public DecisionDefaults createDefaults(
            Chapter chapter,
            Outline outline,
            String stage,
            String writingMode,
            boolean hasBackgroundContext) {
        return new DecisionDefaults(
                resolveTargetWordCount(writingMode, stage),
                buildRequiredFacts(chapter, outline, hasBackgroundContext),
                buildProhibitedMoves(chapter, outline),
                buildWriterHints(stage, writingMode, hasBackgroundContext)
        );
    }

    public AssembledDecisionPack assembleResolvedDecision(
            Chapter chapter,
            String entryPoint,
            String stage,
            String writingMode,
            Integer targetWordCount,
            List<DirectorModuleSelection> selectedModules,
            List<String> requiredFacts,
            List<String> prohibitedMoves,
            List<String> writerHints,
            String decisionSummary,
            Object toolTrace) {
        List<Map<String, Object>> selectedModulePayload = selectedModules.stream()
                .map(item -> {
                    Map<String, Object> module = new LinkedHashMap<>();
                    module.put("module", item.moduleName());
                    module.put("weight", item.weight());
                    module.put("required", item.required());
                    module.put("topK", item.topK());
                    module.put("fields", item.fields());
                    return module;
                })
                .toList();

        Map<String, Double> moduleWeights = selectedModules.stream()
                .collect(Collectors.toMap(
                        DirectorModuleSelection::moduleName,
                        DirectorModuleSelection::weight,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<String, Object> decisionPack = new LinkedHashMap<>();
        decisionPack.put("version", "v1");
        decisionPack.put("chapterId", chapter.getId());
        decisionPack.put("projectId", chapter.getProjectId());
        decisionPack.put("entryPoint", entryPoint);
        decisionPack.put("stage", stage);
        decisionPack.put("writingMode", writingMode);
        decisionPack.put("targetWordCount", targetWordCount);
        decisionPack.put("decisionSummary", decisionSummary);
        decisionPack.put("selectedModules", selectedModulePayload);
        decisionPack.put("requiredFacts", requiredFacts);
        decisionPack.put("prohibitedMoves", prohibitedMoves);
        decisionPack.put("writerHints", writerHints);

        return new AssembledDecisionPack(
                targetWordCount,
                decisionSummary,
                writeJson(selectedModulePayload),
                writeJson(moduleWeights),
                writeJson(requiredFacts),
                writeJson(prohibitedMoves),
                writeJson(decisionPack),
                writeJson(toolTrace)
        );
    }

    private Integer resolveTargetWordCount(String writingMode, String stage) {
        if ("polish".equals(writingMode)) {
            return 900;
        }
        if ("rewrite".equals(writingMode)) {
            return 1800;
        }
        if ("opening".equals(stage)) {
            return 1200;
        }
        if ("turning".equals(stage) || "convergence".equals(stage)) {
            return 1500;
        }
        return 1300;
    }

    private List<String> buildRequiredFacts(Chapter chapter, Outline outline, boolean hasBackgroundContext) {
        List<String> facts = new ArrayList<>();
        if (chapter.getRequiredCharacterNames() != null && !chapter.getRequiredCharacterNames().isEmpty()) {
            facts.add("本章必出人物：" + String.join("、", chapter.getRequiredCharacterNames()));
        }
        if (outline != null && StringUtils.hasText(outline.getStageGoal())) {
            facts.add("章节目标：" + outline.getStageGoal().trim());
        }
        if (outline != null && StringUtils.hasText(outline.getKeyConflict())) {
            facts.add("核心冲突：" + outline.getKeyConflict().trim());
        }
        if (hasBackgroundContext) {
            facts.add("已存在背景聊天约束，本轮写作需优先遵循稳定设定与硬约束。");
        }
        return new ArrayList<>(new LinkedHashSet<>(facts));
    }

    private List<String> buildProhibitedMoves(Chapter chapter, Outline outline) {
        List<String> moves = new ArrayList<>();
        if (chapter.getRequiredCharacterNames() != null && !chapter.getRequiredCharacterNames().isEmpty()) {
            moves.add("不要遗漏本章必出人物。");
        }
        if (outline != null && StringUtils.hasText(outline.getTurningPoints())) {
            moves.add("不要跳过当前大纲中已经明确的关键转折。");
        }
        if (outline != null && StringUtils.hasText(outline.getExpectedEnding())) {
            moves.add("不要在未完成本章目标前过早结束场景。");
        }
        moves.add("不要引入与既有设定直接冲突的新事实。");
        return new ArrayList<>(new LinkedHashSet<>(moves));
    }

    private List<String> buildWriterHints(String stage, String writingMode, boolean hasBackgroundContext) {
        List<String> hints = new ArrayList<>();
        if ("opening".equals(stage)) {
            hints.add("优先交代场景、人物状态和冲突起点。");
        }
        if ("turning".equals(stage)) {
            hints.add("优先推进关键转折，不要只堆环境描写。");
        }
        if ("convergence".equals(stage)) {
            hints.add("本轮应开始收束冲突并准备后续钩子。");
        }
        if ("polish".equals(writingMode)) {
            hints.add("保持剧情事实不变，重点优化语言节奏和可读性。");
        }
        if (hasBackgroundContext) {
            hints.add("背景聊天里的稳定偏好应优先高于临时发挥。");
        }
        if (hints.isEmpty()) {
            hints.add("优先服务当前章节目标和已确认约束。");
        }
        return hints;
    }

    public String buildDecisionSummary(
            String stage,
            String writingMode,
            List<DirectorModuleSelection> selectedModules,
            boolean hasBackgroundContext) {
        String moduleSummary = selectedModules.stream()
                .map(DirectorModuleSelection::moduleName)
                .collect(Collectors.joining("、"));
        if (hasBackgroundContext) {
            return "当前判定为 " + stage + " 阶段，执行 " + writingMode + " 模式，并优先结合 " + moduleSummary + " 与背景聊天约束。";
        }
        return "当前判定为 " + stage + " 阶段，执行 " + writingMode + " 模式，并优先结合 " + moduleSummary + "。";
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    public record DirectorModuleSelection(
            String moduleName,
            double weight,
            boolean required,
            int topK,
            List<String> fields) {
    }

    public record AssembledDecisionPack(
            Integer targetWordCount,
            String decisionSummary,
            String selectedModulesJson,
            String moduleWeightsJson,
            String requiredFactsJson,
            String prohibitedMovesJson,
            String decisionPackJson,
            String toolTraceJson) {
    }

    public record DecisionDefaults(
            Integer targetWordCount,
            List<String> requiredFacts,
            List<String> prohibitedMoves,
            List<String> writerHints) {
    }
}
