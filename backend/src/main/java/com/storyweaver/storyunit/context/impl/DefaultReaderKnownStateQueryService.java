package com.storyweaver.storyunit.context.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.repository.ChapterMapper;
import com.storyweaver.storyunit.context.ChapterAnchorBundleQueryService;
import com.storyweaver.storyunit.context.ChapterAnchorBundleView;
import com.storyweaver.storyunit.context.ReaderKnownStateQueryService;
import com.storyweaver.storyunit.context.ReaderKnownStateView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultReaderKnownStateQueryService implements ReaderKnownStateQueryService {

    private final ChapterMapper chapterMapper;
    private final ChapterAnchorBundleQueryService chapterAnchorBundleQueryService;

    public DefaultReaderKnownStateQueryService(
            ChapterMapper chapterMapper,
            ChapterAnchorBundleQueryService chapterAnchorBundleQueryService) {
        this.chapterMapper = chapterMapper;
        this.chapterAnchorBundleQueryService = chapterAnchorBundleQueryService;
    }

    @Override
    public Optional<ReaderKnownStateView> getReaderKnownState(Long projectId, Long chapterId) {
        if (projectId == null || chapterId == null) {
            return Optional.empty();
        }
        Chapter current = chapterMapper.selectById(chapterId);
        if (current == null || Integer.valueOf(1).equals(current.getDeleted()) || !projectId.equals(current.getProjectId())) {
            return Optional.empty();
        }

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .eq(Chapter::getDeleted, 0));

        List<String> rawKnownFacts = new ArrayList<>();
        chapters.stream()
                .filter(chapter -> chapter != null && chapter.getId() != null && !chapter.getId().equals(chapterId))
                .filter(chapter -> isEarlierChapter(chapter, current))
                .sorted(Comparator
                        .comparing((Chapter item) -> item.getOrderNum() == null ? Integer.MAX_VALUE : item.getOrderNum())
                        .thenComparing(Chapter::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(chapter -> {
                    String fact = buildChapterFact(chapter);
                    if (StringUtils.hasText(fact)) {
                        rawKnownFacts.add(fact);
                    }
                });
        List<String> knownFacts = rawKnownFacts.size() > 5
                ? rawKnownFacts.subList(Math.max(0, rawKnownFacts.size() - 5), rawKnownFacts.size())
                : List.copyOf(rawKnownFacts);

        List<String> unrevealedFacts = chapterAnchorBundleQueryService.getChapterAnchorBundle(projectId, chapterId)
                .map(bundle -> buildUnrevealedFacts(current, bundle))
                .orElseGet(List::of);

        return Optional.of(new ReaderKnownStateView(projectId, chapterId, knownFacts, unrevealedFacts));
    }

    private boolean isEarlierChapter(Chapter candidate, Chapter current) {
        int candidateOrder = candidate.getOrderNum() == null ? Integer.MAX_VALUE : candidate.getOrderNum();
        int currentOrder = current.getOrderNum() == null ? Integer.MAX_VALUE : current.getOrderNum();
        if (candidateOrder != currentOrder) {
            return candidateOrder < currentOrder;
        }
        if (candidate.getCreateTime() != null && current.getCreateTime() != null) {
            return candidate.getCreateTime().isBefore(current.getCreateTime());
        }
        return candidate.getId() < current.getId();
    }

    private String buildChapterFact(Chapter chapter) {
        String title = ContextViewSupport.trimToEmpty(chapter.getTitle());
        String summary = ContextViewSupport.firstNonBlank(chapter.getSummary(), ContextViewSupport.truncate(chapter.getContent(), 80));
        if (!StringUtils.hasText(summary) && !StringUtils.hasText(title)) {
            return "";
        }
        if (!StringUtils.hasText(title)) {
            return summary;
        }
        if (!StringUtils.hasText(summary)) {
            return "已发生章节：" + title;
        }
        return title + "：" + summary;
    }

    private List<String> buildUnrevealedFacts(Chapter current, ChapterAnchorBundleView bundle) {
        if (StringUtils.hasText(current.getContent()) && current.getWordCount() != null && current.getWordCount() > 0) {
            return List.of();
        }
        List<String> unrevealed = new ArrayList<>();
        if (StringUtils.hasText(bundle.chapterSummary())) {
            unrevealed.add("本章待揭晓：" + bundle.chapterSummary());
        }
        if (!bundle.storyBeats().isEmpty()) {
            unrevealed.add("待推进剧情：" + String.join("、", bundle.storyBeats()));
        }
        if (StringUtils.hasText(bundle.mainPovCharacterName())) {
            unrevealed.add("当前视角人物：" + bundle.mainPovCharacterName());
        }
        return ContextViewSupport.sanitizeDistinct(unrevealed);
    }
}
