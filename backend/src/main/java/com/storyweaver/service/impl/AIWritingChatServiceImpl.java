package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.storyweaver.domain.dto.AIWritingChatMessageRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingChatMessage;
import com.storyweaver.domain.entity.AIWritingSession;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.vo.AIWritingChatMessageVO;
import com.storyweaver.domain.vo.AIWritingChatSessionVO;
import com.storyweaver.repository.AIWritingChatMessageMapper;
import com.storyweaver.repository.AIWritingSessionMapper;
import com.storyweaver.service.AIModelRoutingService;
import com.storyweaver.service.AIProviderService;
import com.storyweaver.service.AIWritingChatService;
import com.storyweaver.service.ChapterService;
import com.storyweaver.service.SystemConfigService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class AIWritingChatServiceImpl implements AIWritingChatService {

    private static final int DEFAULT_MAX_ACTIVE_CHARS = 6000;
    private static final int DEFAULT_KEEP_RECENT_MESSAGES = 4;

    private final AIWritingSessionMapper sessionMapper;
    private final AIWritingChatMessageMapper messageMapper;
    private final ChapterService chapterService;
    private final AIProviderService aiProviderService;
    private final AIModelRoutingService aiModelRoutingService;
    private final SystemConfigService systemConfigService;

    public AIWritingChatServiceImpl(
            AIWritingSessionMapper sessionMapper,
            AIWritingChatMessageMapper messageMapper,
            ChapterService chapterService,
            AIProviderService aiProviderService,
            AIModelRoutingService aiModelRoutingService,
            SystemConfigService systemConfigService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.chapterService = chapterService;
        this.aiProviderService = aiProviderService;
        this.aiModelRoutingService = aiModelRoutingService;
        this.systemConfigService = systemConfigService;
    }

    @Override
    @Transactional
    public AIWritingChatSessionVO getSession(Long userId, Long chapterId) {
        Chapter chapter = requireChapter(chapterId, userId);
        AIWritingSession session = ensureSession(chapter, userId);
        syncActiveWindowChars(session);
        return toSessionVO(session, loadMessages(session.getId()));
    }

    @Override
    @Transactional
    public AIWritingChatSessionVO sendMessage(Long userId, Long chapterId, AIWritingChatMessageRequestDTO requestDTO) {
        Chapter chapter = requireChapter(chapterId, userId);
        String content = normalizeText(requestDTO.getContent());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        AIWritingSession session = ensureSession(chapter, userId);
        appendMessage(session, chapterId, "user", content);
        syncActiveWindowChars(session);

        AIModelRoutingService.ResolvedModelSelection selection = aiModelRoutingService.resolve(
                requestDTO.getSelectedProviderId(),
                requestDTO.getSelectedModel(),
                requestDTO.getEntryPoint()
        );

        compressActiveMessagesIfNeeded(session, chapter, selection.provider(), selection.model());

        String assistantReply = aiProviderService.generateText(
                selection.provider(),
                selection.model(),
                buildChatSystemPrompt(chapter),
                buildChatUserPrompt(chapter, session, loadMessages(session.getId())),
                null,
                900
        );

        appendMessage(session, chapterId, "assistant", normalizeText(assistantReply));
        syncActiveWindowChars(session);
        return toSessionVO(session, loadMessages(session.getId()));
    }

    @Override
    @Transactional
    public AIWritingChatSessionVO setMessagePinnedToBackground(Long userId, Long messageId, boolean pinned) {
        AIWritingChatMessage message = messageMapper.selectById(messageId);
        if (message == null || Integer.valueOf(1).equals(message.getDeleted())) {
            throw new IllegalArgumentException("消息不存在");
        }

        Chapter chapter = requireChapter(message.getChapterId(), userId);
        AIWritingSession session = ensureSession(chapter, userId);

        message.setPinnedToBackground(pinned ? 1 : 0);
        messageMapper.updateById(message);
        return toSessionVO(session, loadMessages(session.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public String buildBackgroundContext(Long userId, Long chapterId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return "";
        }

        AIWritingSession session = findSession(chapterId, userId);
        if (session == null) {
            return "";
        }

        List<AIWritingChatMessage> messages = loadMessages(session.getId());
        StringBuilder builder = new StringBuilder();

        if (StringUtils.hasText(session.getCompressedSummary())) {
            builder.append("[聊天摘要]\n")
                    .append(session.getCompressedSummary().trim())
                    .append("\n\n");
        }

        List<AIWritingChatMessage> backgroundMessages = messages.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getPinnedToBackground()))
                .toList();
        if (!backgroundMessages.isEmpty()) {
            builder.append("[已置顶背景信息]\n");
            for (AIWritingChatMessage message : backgroundMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 220))
                        .append('\n');
            }
            builder.append('\n');
        }

        List<AIWritingChatMessage> recentMessages = recentActiveMessages(messages, 6);
        if (!recentMessages.isEmpty()) {
            builder.append("[最近对话]\n");
            for (AIWritingChatMessage message : recentMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 180))
                        .append('\n');
            }
        }

        return builder.toString().trim();
    }

    private Chapter requireChapter(Long chapterId, Long userId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }
        return chapter;
    }

    private AIWritingSession ensureSession(Chapter chapter, Long userId) {
        AIWritingSession session = findSession(chapter.getId(), userId);
        if (session != null) {
            return session;
        }

        AIWritingSession created = new AIWritingSession();
        created.setProjectId(chapter.getProjectId());
        created.setChapterId(chapter.getId());
        created.setUserId(userId);
        created.setActiveSegmentNo(1);
        created.setActiveWindowChars(0);
        created.setCompressedSummary("");
        try {
            sessionMapper.insert(created);
            return created.getId() != null ? created : findSession(chapter.getId(), userId);
        } catch (DataIntegrityViolationException exception) {
            AIWritingSession existing = findSession(chapter.getId(), userId);
            if (existing != null) {
                return existing;
            }
            throw exception;
        }
    }

    private AIWritingSession findSession(Long chapterId, Long userId) {
        QueryWrapper<AIWritingSession> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("chapter_id", chapterId)
                .eq("user_id", userId)
                .eq("deleted", 0)
                .last("LIMIT 1");
        return sessionMapper.selectOne(queryWrapper);
    }

    private void appendMessage(AIWritingSession session, Long chapterId, String role, String content) {
        AIWritingChatMessage message = new AIWritingChatMessage();
        message.setSessionId(session.getId());
        message.setChapterId(chapterId);
        message.setRole(role);
        message.setContent(StringUtils.hasText(content) ? content.trim() : "");
        message.setSegmentNo(session.getActiveSegmentNo());
        message.setPinnedToBackground(0);
        message.setCompressed(0);
        messageMapper.insert(message);
    }

    private List<AIWritingChatMessage> loadMessages(Long sessionId) {
        QueryWrapper<AIWritingChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", sessionId)
                .eq("deleted", 0)
                .orderByAsc("id");
        return messageMapper.selectList(queryWrapper);
    }

    private void compressActiveMessagesIfNeeded(AIWritingSession session, Chapter chapter, AIProvider provider, String model) {
        int maxActiveChars = getConfiguredInt("ai.chat.max_active_chars", DEFAULT_MAX_ACTIVE_CHARS);
        if ((session.getActiveWindowChars() == null ? 0 : session.getActiveWindowChars()) < maxActiveChars) {
            return;
        }

        List<AIWritingChatMessage> activeMessages = loadMessages(session.getId()).stream()
                .filter(item -> !Integer.valueOf(1).equals(item.getCompressed()))
                .filter(item -> session.getActiveSegmentNo().equals(item.getSegmentNo()))
                .toList();

        int keepRecentMessages = getConfiguredInt("ai.chat.keep_recent_messages", DEFAULT_KEEP_RECENT_MESSAGES);
        if (activeMessages.size() <= keepRecentMessages) {
            return;
        }

        int splitIndex = Math.max(activeMessages.size() - keepRecentMessages, 1);
        List<AIWritingChatMessage> archivedMessages = new ArrayList<>(activeMessages.subList(0, splitIndex));
        List<AIWritingChatMessage> retainedMessages = new ArrayList<>(activeMessages.subList(splitIndex, activeMessages.size()));

        String summary = aiProviderService.generateText(
                provider,
                model,
                """
                你负责压缩较早的创作聊天上下文。
                请输出一段简洁、可复用的中文摘要。
                只保留稳定事实、已确认决策、风格约束和待解决问题。
                """,
                buildCompressionPrompt(chapter, session.getCompressedSummary(), archivedMessages),
                null,
                1200
        );

        session.setCompressedSummary(mergeSummary(session.getCompressedSummary(), summary));
        int nextSegmentNo = (session.getActiveSegmentNo() == null ? 1 : session.getActiveSegmentNo()) + 1;

        for (AIWritingChatMessage message : archivedMessages) {
            message.setCompressed(1);
            messageMapper.updateById(message);
        }
        for (AIWritingChatMessage message : retainedMessages) {
            message.setSegmentNo(nextSegmentNo);
            messageMapper.updateById(message);
        }

        session.setActiveSegmentNo(nextSegmentNo);
        sessionMapper.updateById(session);
        appendMessage(session, chapter.getId(), "system", "系统已将较早对话压缩为可复用的背景信息。");
        syncActiveWindowChars(session);
    }

    private String buildChatSystemPrompt(Chapter chapter) {
        return """
                你是一名协作式中文小说写作助手。
                你的任务是帮助用户讨论世界观、人物选择、场景目标与章节走向。
                请始终使用中文回复，给出直接、务实的建议，不要假装自己已经改写过章节正文。
                当前章节标题：%s
                """.formatted(safe(chapter.getTitle(), "未命名章节"));
    }

    private String buildChatUserPrompt(Chapter chapter, AIWritingSession session, List<AIWritingChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("请用中文回复用户最新一条消息。\n");
        builder.append("[当前章节]\n");
        builder.append("标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        if (chapter.getOrderNum() != null) {
            builder.append("章节顺序：第 ").append(chapter.getOrderNum()).append(" 章\n");
        }
        if (StringUtils.hasText(chapter.getContent())) {
            builder.append("当前正文摘录：").append(limit(chapter.getContent(), 1000)).append("\n\n");
        }

        if (StringUtils.hasText(session.getCompressedSummary())) {
            builder.append("[压缩后的聊天摘要]\n")
                    .append(session.getCompressedSummary().trim())
                    .append("\n\n");
        }

        List<AIWritingChatMessage> pinnedMessages = messages.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getPinnedToBackground()))
                .toList();
        if (!pinnedMessages.isEmpty()) {
            builder.append("[已置顶背景信息]\n");
            for (AIWritingChatMessage message : pinnedMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 220))
                        .append('\n');
            }
            builder.append('\n');
        }

        builder.append("[最近对话]\n");
        for (AIWritingChatMessage message : recentActiveMessages(messages, 8)) {
            builder.append(resolveRoleLabel(message.getRole()))
                    .append(": ")
                    .append(limit(message.getContent(), 320))
                    .append('\n');
        }
        builder.append("\n请直接、有帮助地回应用户最新一条消息。");
        return builder.toString();
    }

    private String buildCompressionPrompt(Chapter chapter, String existingSummary, List<AIWritingChatMessage> archivedMessages) {
        StringBuilder builder = new StringBuilder();
        builder.append("请把这些较早的写作聊天消息压缩成可复用的中文记忆。\n");
        builder.append("保留稳定事实、已确认决策、风格偏好、禁止改动项和待解决问题。\n");
        builder.append("不要编造任何原对话中没有提到的内容。\n\n");
        builder.append("章节：").append(safe(chapter.getTitle(), "未命名章节")).append("\n\n");
        if (StringUtils.hasText(existingSummary)) {
            builder.append("[已有摘要]\n").append(existingSummary.trim()).append("\n\n");
        }
        builder.append("[待压缩消息]\n");
        for (AIWritingChatMessage message : archivedMessages) {
            builder.append(resolveRoleLabel(message.getRole()))
                    .append(": ")
                    .append(message.getContent())
                    .append('\n');
        }
        return builder.toString();
    }

    private List<AIWritingChatMessage> recentActiveMessages(List<AIWritingChatMessage> messages, int limit) {
        List<AIWritingChatMessage> activeMessages = messages.stream()
                .filter(item -> !Integer.valueOf(1).equals(item.getCompressed()))
                .toList();
        long skip = Math.max(0L, activeMessages.size() - (long) limit);
        return activeMessages.stream().skip(skip).toList();
    }

    private void syncActiveWindowChars(AIWritingSession session) {
        QueryWrapper<AIWritingChatMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("session_id", session.getId())
                .eq("deleted", 0)
                .eq("compressed", 0)
                .eq("segment_no", session.getActiveSegmentNo());
        List<AIWritingChatMessage> messages = messageMapper.selectList(queryWrapper);
        int charCount = messages.stream()
                .map(AIWritingChatMessage::getContent)
                .filter(StringUtils::hasText)
                .mapToInt(String::length)
                .sum();
        session.setActiveWindowChars(charCount);
        sessionMapper.updateById(session);
    }

    private AIWritingChatSessionVO toSessionVO(AIWritingSession session, List<AIWritingChatMessage> messages) {
        AIWritingChatSessionVO vo = new AIWritingChatSessionVO();
        vo.setSessionId(session.getId());
        vo.setProjectId(session.getProjectId());
        vo.setChapterId(session.getChapterId());
        vo.setActiveSegmentNo(session.getActiveSegmentNo());
        vo.setActiveWindowChars(session.getActiveWindowChars());
        vo.setMaxWindowChars(getConfiguredInt("ai.chat.max_active_chars", DEFAULT_MAX_ACTIVE_CHARS));
        vo.setCompressedSummary(session.getCompressedSummary());
        vo.setMessages(messages.stream().map(this::toMessageVO).toList());
        return vo;
    }

    private AIWritingChatMessageVO toMessageVO(AIWritingChatMessage message) {
        AIWritingChatMessageVO vo = new AIWritingChatMessageVO();
        vo.setId(message.getId());
        vo.setRole(message.getRole());
        vo.setContent(message.getContent());
        vo.setSegmentNo(message.getSegmentNo());
        vo.setPinnedToBackground(Integer.valueOf(1).equals(message.getPinnedToBackground()));
        vo.setCompressed(Integer.valueOf(1).equals(message.getCompressed()));
        vo.setCreateTime(message.getCreateTime());
        return vo;
    }

    private int getConfiguredInt(String key, int fallback) {
        String value = systemConfigService.getConfigValue(key);
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String mergeSummary(String existingSummary, String latestSummary) {
        String merged = (normalizeText(existingSummary) + "\n\n" + normalizeText(latestSummary)).trim();
        if (!StringUtils.hasText(merged)) {
            return "";
        }
        return merged.length() <= 4000 ? merged : merged.substring(merged.length() - 4000);
    }

    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String safe(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String resolveRoleLabel(String role) {
        return switch (safe(role, "assistant")) {
            case "user" -> "用户";
            case "system" -> "系统";
            default -> "助手";
        };
    }
}
