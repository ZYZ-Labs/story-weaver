package com.storyweaver.story.generation.orchestration.impl;

import com.storyweaver.story.generation.orchestration.ReviewerSessionService;
import com.storyweaver.story.generation.orchestration.StorySessionContextPacket;
import com.storyweaver.story.generation.orchestration.WriterSessionResult;
import com.storyweaver.storyunit.session.ReviewDecision;
import com.storyweaver.storyunit.session.ReviewIssue;
import com.storyweaver.storyunit.session.ReviewResult;
import com.storyweaver.storyunit.session.ReviewSeverity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleBasedReviewerSessionService implements ReviewerSessionService {

    @Override
    public ReviewDecision review(StorySessionContextPacket contextPacket, WriterSessionResult writerSessionResult) {
        List<ReviewIssue> issues = new ArrayList<>();
        String draftText = writerSessionResult.draftText();

        if (!draftText.contains("【目标】")) {
            issues.add(new ReviewIssue("missing_goal", "writer 草稿缺少目标段。", ReviewSeverity.ERROR, true));
        }
        if (!draftText.contains("【收束点】")) {
            issues.add(new ReviewIssue("missing_stop_condition", "writer 草稿缺少收束点。", ReviewSeverity.ERROR, true));
        }
        if (contextPacket.readerKnownState().knownFacts().isEmpty()) {
            issues.add(new ReviewIssue("reader_known_empty", "当前章节读者已知事实仍为空，后续真实写作时要避免默认读者已知。", ReviewSeverity.WARNING, false));
        }

        ReviewResult result = issues.stream().anyMatch(issue -> issue.severity() == ReviewSeverity.ERROR || issue.severity() == ReviewSeverity.BLOCKER)
                ? ReviewResult.REVISE
                : ReviewResult.PASS;
        String summary = result == ReviewResult.PASS
                ? "规则审校通过。"
                : "规则审校发现需要修正的问题。";

        return new ReviewDecision(
                writerSessionResult.sceneId(),
                result,
                summary,
                List.copyOf(issues),
                result == ReviewResult.REVISE,
                result == ReviewResult.REVISE ? "补齐目标与收束点后重新生成。": ""
        );
    }
}
