package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.storyweaver.domain.dto.AIWritingBackgroundNoteRequestDTO;
import com.storyweaver.domain.dto.AIWritingChatMessageRequestDTO;
import com.storyweaver.domain.entity.AIProvider;
import com.storyweaver.domain.entity.AIWritingChatMessage;
import com.storyweaver.domain.entity.AIWritingSession;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.vo.AIWritingChatParticipationVO;
import com.storyweaver.domain.vo.AIWritingChatMessageVO;
import com.storyweaver.domain.vo.AIWritingChatSessionVO;
import com.storyweaver.domain.vo.AIWritingChatStreamEventVO;
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
import java.util.function.Consumer;

@Service
public class AIWritingChatServiceImpl implements AIWritingChatService {

    private static final int DEFAULT_MAX_ACTIVE_CHARS = 6000;
    private static final int DEFAULT_KEEP_RECENT_MESSAGES = 4;
    private static final int CHAT_MAX_TOKENS = 1600;

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
        PreparedChatGenerationContext context = prepareChatGeneration(userId, chapterId, requestDTO);

        String assistantReply = aiProviderService.generateText(
                context.provider(),
                context.model(),
                buildChatSystemPrompt(context.chapter()),
                buildChatUserPrompt(context.chapter(), context.session(), loadMessages(context.session().getId())),
                null,
                CHAT_MAX_TOKENS
        );

        return persistAssistantReply(context, assistantReply);
    }

    @Override
    @Transactional
    public void streamMessage(
            Long userId,
            Long chapterId,
            AIWritingChatMessageRequestDTO requestDTO,
            Consumer<AIWritingChatStreamEventVO> eventConsumer) {
        PreparedChatGenerationContext context = prepareChatGeneration(userId, chapterId, requestDTO);
        if (eventConsumer != null) {
            eventConsumer.accept(AIWritingChatStreamEventVO.meta(context.provider().getId(), context.model()));
        }

        StringBuilder builder = new StringBuilder();
        aiProviderService.streamText(
                context.provider(),
                context.model(),
                buildChatSystemPrompt(context.chapter()),
                buildChatUserPrompt(context.chapter(), context.session(), loadMessages(context.session().getId())),
                null,
                CHAT_MAX_TOKENS,
                delta -> {
                    if (!StringUtils.hasText(delta)) {
                        return;
                    }
                    builder.append(delta);
                    if (eventConsumer != null) {
                        eventConsumer.accept(AIWritingChatStreamEventVO.chunk(delta));
                    }
                }
        );

        AIWritingChatSessionVO session = persistAssistantReply(context, builder.toString());
        if (eventConsumer != null) {
            eventConsumer.accept(AIWritingChatStreamEventVO.complete(session));
        }
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
    @Transactional
    public AIWritingChatSessionVO addBackgroundNote(Long userId, Long chapterId, AIWritingBackgroundNoteRequestDTO requestDTO) {
        Chapter chapter = requireChapter(chapterId, userId);
        String content = normalizeText(requestDTO.getContent());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("背景信息内容不能为空");
        }

        AIWritingSession session = ensureSession(chapter, userId);
        appendMessage(session, chapterId, "user", content, true);
        syncActiveWindowChars(session);
        return toSessionVO(session, loadMessages(session.getId()));
    }

    @Override
    @Transactional
    public AIWritingChatSessionVO updateBackgroundNote(Long userId, Long messageId, AIWritingBackgroundNoteRequestDTO requestDTO) {
        AIWritingChatMessage message = messageMapper.selectById(messageId);
        if (message == null || Integer.valueOf(1).equals(message.getDeleted())) {
            throw new IllegalArgumentException("背景信息不存在");
        }
        if (Integer.valueOf(1).equals(message.getCompressed())) {
            throw new IllegalArgumentException("已压缩的背景信息暂不支持修改");
        }

        Chapter chapter = requireChapter(message.getChapterId(), userId);
        AIWritingSession session = ensureSession(chapter, userId);
        String content = normalizeText(requestDTO.getContent());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("背景信息内容不能为空");
        }

        message.setContent(content);
        message.setPinnedToBackground(1);
        messageMapper.updateById(message);
        syncActiveWindowChars(session);
        return toSessionVO(session, loadMessages(session.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasBackgroundContext(Long userId, Long chapterId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return false;
        }

        AIWritingSession session = findSession(chapterId, userId);
        if (session == null) {
            return false;
        }

        List<AIWritingChatMessage> messages = loadMessages(session.getId());
        return hasReusableBackgroundContext(session, messages);
    }

    @Override
    @Transactional(readOnly = true)
    public AIWritingChatParticipationVO buildParticipationContext(Long userId, Long chapterId, AIProvider provider, String model) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            return AIWritingChatParticipationVO.empty();
        }

        AIWritingSession session = findSession(chapterId, userId);
        if (session == null) {
            return AIWritingChatParticipationVO.empty();
        }

        List<AIWritingChatMessage> messages = loadMessages(session.getId());
        if (!hasReusableBackgroundContext(session, messages)) {
            return AIWritingChatParticipationVO.empty();
        }

        String response = aiProviderService.generateText(
                provider,
                model,
                """
                你是一名中文小说写作背景整理助手。
                你的任务是把背景聊天整理成可参与本章生成的结构化上下文，而不是复述聊天记录。
                只能提炼已出现的稳定设定、人物约束、剧情推进方向、写作偏好和不可违背事项，不要补充聊天里没有的新信息。
                请严格按以下格式输出；如果某一栏没有内容，请写“- 无”：
                【世界观补充】
                - ...
                【人物约束】
                - ...
                【剧情推进】
                - ...
                【写作偏好】
                - ...
                【硬性约束】
                - ...
                """,
                buildParticipationPrompt(chapter, session, messages),
                null,
                900
        );
        return parseParticipationContext(response);
    }

    private Chapter requireChapter(Long chapterId, Long userId) {
        Chapter chapter = chapterService.getChapterWithAuth(chapterId, userId);
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在或无权访问");
        }
        return chapter;
    }

    private PreparedChatGenerationContext prepareChatGeneration(
            Long userId,
            Long chapterId,
            AIWritingChatMessageRequestDTO requestDTO) {
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

        return new PreparedChatGenerationContext(chapter, session, selection.provider(), selection.model());
    }

    private AIWritingChatSessionVO persistAssistantReply(PreparedChatGenerationContext context, String assistantReply) {
        String normalizedReply = normalizeText(assistantReply);
        if (!StringUtils.hasText(normalizedReply)) {
            throw new IllegalStateException("模型没有返回可用的聊天回复，请稍后重试");
        }

        appendMessage(context.session(), context.chapter().getId(), "assistant", normalizedReply);
        syncActiveWindowChars(context.session());
        return toSessionVO(context.session(), loadMessages(context.session().getId()));
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
        appendMessage(session, chapterId, role, content, false);
    }

    private void appendMessage(
            AIWritingSession session,
            Long chapterId,
            String role,
            String content,
            boolean pinnedToBackground) {
        AIWritingChatMessage message = new AIWritingChatMessage();
        message.setSessionId(session.getId());
        message.setChapterId(chapterId);
        message.setRole(role);
        message.setContent(StringUtils.hasText(content) ? content.trim() : "");
        message.setSegmentNo(session.getActiveSegmentNo());
        message.setPinnedToBackground(pinnedToBackground ? 1 : 0);
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
        builder.append("【当前章节】\n");
        builder.append("标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        if (chapter.getOrderNum() != null) {
            builder.append("章节顺序：第 ").append(chapter.getOrderNum()).append(" 章\n");
        }
        if (StringUtils.hasText(chapter.getContent())) {
            builder.append("当前正文摘录：").append(limit(chapter.getContent(), 1000)).append("\n\n");
        }

        if (StringUtils.hasText(session.getCompressedSummary())) {
            builder.append("【压缩后的聊天摘要】\n")
                    .append(session.getCompressedSummary().trim())
                    .append("\n\n");
        }

        List<AIWritingChatMessage> pinnedMessages = messages.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getPinnedToBackground()))
                .toList();
        if (!pinnedMessages.isEmpty()) {
            builder.append("【已置顶背景信息】\n");
            for (AIWritingChatMessage message : pinnedMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 220))
                        .append('\n');
            }
            builder.append('\n');
        }

        builder.append("【最近对话】\n");
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
            builder.append("【已有摘要】\n").append(existingSummary.trim()).append("\n\n");
        }
        builder.append("【待压缩消息】\n");
        for (AIWritingChatMessage message : archivedMessages) {
            builder.append(resolveRoleLabel(message.getRole()))
                    .append(": ")
                    .append(message.getContent())
                    .append('\n');
        }
        return builder.toString();
    }

    private String buildParticipationPrompt(Chapter chapter, AIWritingSession session, List<AIWritingChatMessage> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("请把以下背景聊天整理为“参与本章写作的上下文”。\n");
        builder.append("重点提炼世界观补充、人物行为边界、章节推进方向、文风偏好和硬性限制。\n");
        builder.append("不要保留问答口吻，不要直接照抄整段聊天。\n\n");
        builder.append("【当前章节】\n");
        builder.append("标题：").append(safe(chapter.getTitle(), "未命名章节")).append('\n');
        if (chapter.getOrderNum() != null) {
            builder.append("顺序：第 ").append(chapter.getOrderNum()).append(" 章\n");
        }
        builder.append('\n');

        if (StringUtils.hasText(session.getCompressedSummary())) {
            builder.append("【历史摘要】\n")
                    .append(session.getCompressedSummary().trim())
                    .append("\n\n");
        }

        List<AIWritingChatMessage> pinnedMessages = messages.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getPinnedToBackground()))
                .filter(item -> !"system".equals(item.getRole()))
                .toList();
        if (!pinnedMessages.isEmpty()) {
            builder.append("【固定背景】\n");
            for (AIWritingChatMessage message : pinnedMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 260))
                        .append('\n');
            }
            builder.append('\n');
        }

        List<AIWritingChatMessage> recentMessages = recentReusableMessages(messages, 8);
        if (!recentMessages.isEmpty()) {
            builder.append("【最近对话】\n");
            for (AIWritingChatMessage message : recentMessages) {
                builder.append("- ")
                        .append(resolveRoleLabel(message.getRole()))
                        .append(": ")
                        .append(limit(message.getContent(), 220))
                        .append('\n');
            }
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

    private List<AIWritingChatMessage> recentReusableMessages(List<AIWritingChatMessage> messages, int limit) {
        return recentActiveMessages(messages, limit).stream()
                .filter(item -> !"system".equals(item.getRole()))
                .toList();
    }

    private boolean hasReusableBackgroundContext(AIWritingSession session, List<AIWritingChatMessage> messages) {
        if (StringUtils.hasText(session.getCompressedSummary())) {
            return true;
        }
        return messages.stream().anyMatch(message ->
                !"system".equals(message.getRole())
                        && StringUtils.hasText(message.getContent())
                        && (Integer.valueOf(1).equals(message.getPinnedToBackground())
                        || !Integer.valueOf(1).equals(message.getCompressed()))
        );
    }

    private AIWritingChatParticipationVO parseParticipationContext(String response) {
        AIWritingChatParticipationVO participation = AIWritingChatParticipationVO.empty();
        String currentSection = null;
        List<String> worldFacts = new ArrayList<>();
        List<String> characterConstraints = new ArrayList<>();
        List<String> plotGuidance = new ArrayList<>();
        List<String> writingPreferences = new ArrayList<>();
        List<String> hardConstraints = new ArrayList<>();

        for (String rawLine : normalizeText(response).split("\\r?\\n")) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) {
                continue;
            }

            String section = resolveParticipationSection(line);
            if (section != null) {
                currentSection = section;
                addParticipationItem(resolveParticipationTarget(section, worldFacts, characterConstraints, plotGuidance, writingPreferences, hardConstraints), extractInlineSectionValue(line));
                continue;
            }

            if (currentSection == null) {
                continue;
            }

            addParticipationItem(
                    resolveParticipationTarget(currentSection, worldFacts, characterConstraints, plotGuidance, writingPreferences, hardConstraints),
                    line
            );
        }

        participation.setWorldFacts(limitItems(worldFacts, 4, 160));
        participation.setCharacterConstraints(limitItems(characterConstraints, 4, 160));
        participation.setPlotGuidance(limitItems(plotGuidance, 4, 160));
        participation.setWritingPreferences(limitItems(writingPreferences, 4, 160));
        participation.setHardConstraints(limitItems(hardConstraints, 4, 160));
        return participation;
    }

    private List<String> resolveParticipationTarget(
            String section,
            List<String> worldFacts,
            List<String> characterConstraints,
            List<String> plotGuidance,
            List<String> writingPreferences,
            List<String> hardConstraints) {
        return switch (section) {
            case "world" -> worldFacts;
            case "character" -> characterConstraints;
            case "plot" -> plotGuidance;
            case "style" -> writingPreferences;
            case "constraint" -> hardConstraints;
            default -> new ArrayList<>();
        };
    }

    private String resolveParticipationSection(String line) {
        String normalized = line
                .replace("[", "")
                .replace("]", "")
                .replace("【", "")
                .replace("】", "")
                .trim();
        if (normalized.contains("世界观")) {
            return "world";
        }
        if (normalized.contains("人物约束") || normalized.contains("人物限制") || normalized.startsWith("人物")) {
            return "character";
        }
        if (normalized.contains("剧情推进") || normalized.contains("章节推进") || normalized.startsWith("剧情")) {
            return "plot";
        }
        if (normalized.contains("写作偏好") || normalized.contains("风格偏好") || normalized.contains("文风")) {
            return "style";
        }
        if (normalized.contains("硬性约束") || normalized.contains("不可违背") || normalized.contains("禁忌")) {
            return "constraint";
        }
        return null;
    }

    private String extractInlineSectionValue(String line) {
        int separatorIndex = line.indexOf('：');
        if (separatorIndex < 0) {
            separatorIndex = line.indexOf(':');
        }
        if (separatorIndex < 0 || separatorIndex == line.length() - 1) {
            return "";
        }
        return line.substring(separatorIndex + 1).trim();
    }

    private void addParticipationItem(List<String> target, String rawItem) {
        String item = normalizeParticipationItem(rawItem);
        if (!StringUtils.hasText(item)) {
            return;
        }
        target.add(item);
    }

    private String normalizeParticipationItem(String rawItem) {
        if (!StringUtils.hasText(rawItem)) {
            return "";
        }
        String normalized = rawItem
                .replaceFirst("^[-*•]+\\s*", "")
                .replaceFirst("^\\d+[.、]\\s*", "")
                .trim();
        if (!StringUtils.hasText(normalized)
                || "无".equals(normalized)
                || "暂无".equals(normalized)
                || "none".equalsIgnoreCase(normalized)) {
            return "";
        }
        return normalized;
    }

    private List<String> limitItems(List<String> items, int maxItems, int maxLength) {
        return items.stream()
                .filter(StringUtils::hasText)
                .map(item -> limit(item, maxLength))
                .distinct()
                .limit(maxItems)
                .toList();
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

    private record PreparedChatGenerationContext(
            Chapter chapter,
            AIWritingSession session,
            AIProvider provider,
            String model) {
    }
}
