package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.storyunit.session.SceneContinuityState;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneContinuitySupportTest {

    @Test
    void shouldBuildAcceptedContinuityStateFromAcceptedContent() {
        SceneContinuityState continuityState = SceneContinuitySupport.buildAcceptedContinuityState(
                "scene-1",
                """
                老陈的短信只有一句话：新纪元明天中午十二点正式开服。
                林沉舟盯着那条消息看了很久，最后回了一个“好”字。
                他把接入器放到桌上，决定明天准时上线。
                """,
                "林沉舟答应明天准时上线。",
                "他把接入器放到桌上，决定明天准时上线。",
                List.of("主角重新登录《旧日王座》的决定已经成立"),
                "scene-2",
                "主角登录游戏，进入游戏大厅。",
                "林沉舟做出决定并停住。",
                SceneContinuityState.empty()
        );

        assertTrue(continuityState.carryForwardFacts().stream().anyMatch(item -> item.contains("明天中午十二点正式开服")));
        assertTrue(continuityState.timeAnchors().stream().anyMatch(item -> item.contains("明天")));
        assertTrue(continuityState.counterpartNames().contains("老陈"));
        assertTrue(continuityState.expectedNames().contains("林沉舟"));
        assertFalse(continuityState.counterpartNames().contains("游戏大厅"));
    }

    @Test
    void shouldNotTreatNextSceneTargetNamesAsCurrentSceneCounterparts() {
        SceneContinuityState continuityState = SceneContinuitySupport.buildAcceptedContinuityState(
                "scene-2",
                """
                老陈发来的消息还停在屏幕上，林沉舟戴上接入器，决定先登录游戏。
                """,
                "林沉舟决定登录新纪元。",
                "他准备进入游戏，不再继续停留在现实房间。",
                List.of("主角开始执行回归决定"),
                "scene-3",
                "林沉舟进入大厅后与陆川会面。",
                "林沉舟进入游戏大厅前停住。",
                SceneContinuityState.empty()
        );

        assertFalse(continuityState.counterpartNames().contains("陆川"));
        assertFalse(continuityState.expectedNames().contains("陆川"));
    }

    @Test
    void shouldPreferLatestSceneCounterpartAndTimeAnchors() {
        SceneContinuityState previousState = new SceneContinuityState(
                "scene-2",
                "林沉舟完成登录前准备。",
                "他决定去见老陈。",
                List.of("老陈约他见面。"),
                List.of("明天中午十二点正式开服。"),
                List.of("林沉舟", "老陈"),
                List.of("老陈"),
                true,
                "scene-3",
                "林沉舟登录游戏后先进入大厅。",
                "进入游戏大厅前停住。"
        );

        SceneContinuityState continuityState = SceneContinuitySupport.buildAcceptedContinuityState(
                "scene-3",
                """
                第二天中午，林沉舟登录新纪元，在大厅边缘看见陆川正朝他走来。
                """,
                "林沉舟登录新纪元，并在大厅见到陆川。",
                "他停在大厅入口，准备听陆川开口。",
                List.of("陆川正式出场"),
                "scene-4",
                "林沉舟与陆川交换情报。",
                "在大厅入口与陆川对上视线后停住。",
                previousState
        );

        assertTrue(continuityState.counterpartNames().contains("陆川"));
        assertFalse(continuityState.counterpartNames().contains("老陈"));
        assertTrue(continuityState.timeAnchors().stream().anyMatch(item -> item.contains("第二天中午")));
        assertFalse(continuityState.timeAnchors().stream().anyMatch(item -> item.contains("明天中午十二点正式开服")));
    }

    @Test
    void shouldAllowCurrentSceneGoalToIntroduceNewCounterpart() {
        SceneContinuityState continuityState = new SceneContinuityState(
                "scene-2",
                "林沉舟完成登录前准备。",
                "他决定去见老陈。",
                List.of("老陈约他见面。"),
                List.of("明天中午十二点正式开服。"),
                List.of("林沉舟", "老陈"),
                List.of("老陈"),
                true,
                "scene-4",
                "林沉舟与陆川交换情报。",
                "在大厅入口与陆川对上视线后停住。"
        );

        List<String> issues = SceneContinuitySupport.detectContinuityIssues(
                continuityState,
                """
                林沉舟刚踏进大厅，就看见陆川站在光门旁等他。两人隔着人群对上视线，谁都没有先开口。
                """,
                "林沉舟在大厅入口见到陆川，并停在正式交谈前。",
                "在大厅入口与陆川对上视线后停住。",
                "scene-4",
                "林沉舟与陆川交换情报。"
        );

        assertFalse(issues.stream().anyMatch(item -> item.contains("会话对象") || item.contains("称呼")));
    }

    @Test
    void shouldDetectContinuityDriftWhenTimeAndCounterpartAreOverwritten() {
        SceneContinuityState continuityState = new SceneContinuityState(
                "scene-1",
                "林沉舟答应明天准时上线。",
                "他把接入器放到桌上，决定明天准时上线。",
                List.of(
                        "新纪元明天中午十二点正式开服。",
                        "老陈让他明天中午上线。"
                ),
                List.of("新纪元明天中午十二点正式开服。"),
                List.of("林沉舟", "老陈"),
                List.of("老陈"),
                true,
                "scene-2",
                "主角登录游戏，进入游戏大厅。",
                "主角做出决定后停住。"
        );

        List<String> issues = SceneContinuitySupport.detectContinuityIssues(
                continuityState,
                """
                《新纪元》已经正式开服。林沉舟刚上线，就看见老猫在游戏大厅另一头朝他挥手。
                他穿过人群，准备和老猫并肩进入大厅深处。
                """,
                "主角收到旧战队召回并决定重新登录。",
                "主角做出决定后停住。",
                "scene-2",
                "主角登录游戏，进入游戏大厅。"
        );

        assertTrue(issues.stream().anyMatch(item -> item.contains("会话对象") || item.contains("称呼")));
        assertTrue(issues.stream().anyMatch(item -> item.contains("提前展开下一镜头")));
    }
}
