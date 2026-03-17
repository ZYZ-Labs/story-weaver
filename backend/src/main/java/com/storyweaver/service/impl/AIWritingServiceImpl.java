package com.storyweaver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.dto.AIWritingRequestDTO;
import com.storyweaver.domain.entity.AIWritingRecord;
import com.storyweaver.domain.entity.Chapter;
import com.storyweaver.domain.vo.AIWritingResponseVO;
import com.storyweaver.repository.AIWritingRecordMapper;
import com.storyweaver.service.AIWritingService;
import com.storyweaver.service.ChapterService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class AIWritingServiceImpl extends ServiceImpl<AIWritingRecordMapper, AIWritingRecord> implements AIWritingService {
    
    @Autowired
    private ChapterService chapterService;
    
    private final Random random = new Random();
    
    @Override
    public AIWritingResponseVO generateContent(AIWritingRequestDTO requestDTO) {
        // Get chapter information
        Chapter chapter = chapterService.getById(requestDTO.getChapterId());
        if (chapter == null) {
            throw new IllegalArgumentException("章节不存在");
        }
        
        // Generate mock AI content based on writing type
        String generatedContent = generateMockAIContent(
            requestDTO.getCurrentContent(),
            requestDTO.getWritingType(),
            requestDTO.getUserInstruction(),
            requestDTO.getMaxTokens()
        );
        
        // Save AI writing record
        AIWritingRecord record = new AIWritingRecord();
        record.setChapterId(requestDTO.getChapterId());
        record.setOriginalContent(requestDTO.getCurrentContent());
        record.setGeneratedContent(generatedContent);
        record.setWritingType(requestDTO.getWritingType() != null ? requestDTO.getWritingType() : "continue");
        record.setUserInstruction(requestDTO.getUserInstruction());
        record.setStatus("draft");
        
        this.save(record);
        
        return convertToVO(record);
    }
    
    @Override
    public List<AIWritingResponseVO> getRecordsByChapterId(Long chapterId) {
        LambdaQueryWrapper<AIWritingRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AIWritingRecord::getChapterId, chapterId)
                   .eq(AIWritingRecord::getDeleted, 0)
                   .orderByDesc(AIWritingRecord::getCreateTime);
        
        List<AIWritingRecord> records = this.list(queryWrapper);
        
        return records.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    @Override
    public AIWritingResponseVO getRecordById(Long id) {
        AIWritingRecord record = this.getById(id);
        if (record == null || record.getDeleted() == 1) {
            return null;
        }
        return convertToVO(record);
    }
    
    @Override
    public AIWritingResponseVO acceptGeneratedContent(Long id) {
        AIWritingRecord record = this.getById(id);
        if (record == null || record.getDeleted() == 1) {
            return null;
        }
        
        // Update chapter content with generated content
        Chapter chapter = chapterService.getById(record.getChapterId());
        if (chapter != null) {
            // For continue writing, append the generated content
            if ("continue".equals(record.getWritingType())) {
                String newContent = chapter.getContent() + "\n\n" + record.getGeneratedContent();
                chapter.setContent(newContent);
            } else {
                // For other types, replace with generated content
                chapter.setContent(record.getGeneratedContent());
            }
            chapterService.updateById(chapter);
        }
        
        // Update record status
        record.setStatus("accepted");
        this.updateById(record);
        
        return convertToVO(record);
    }
    
    @Override
    public void rejectGeneratedContent(Long id) {
        AIWritingRecord record = this.getById(id);
        if (record != null) {
            record.setStatus("rejected");
            this.updateById(record);
        }
    }
    
    private String generateMockAIContent(String currentContent, String writingType, String userInstruction, Integer maxTokens) {
        StringBuilder content = new StringBuilder();
        
        if (writingType == null || "continue".equals(writingType)) {
            content.append("【AI续写内容】\n\n");
            content.append("随着故事的发展，");
            
            String[] continuations = {
                "主角面临了新的挑战。他深吸一口气，握紧了手中的武器，准备迎接即将到来的战斗。",
                "天空突然暗了下来，乌云密布，雷声隆隆。一场暴风雨即将来临，给原本平静的旅程增添了变数。",
                "他们来到了一个陌生的地方，这里的建筑风格奇特，居民们用好奇的目光打量着这些外来者。",
                "一个神秘的声音在耳边响起，指引他们前往未知的目的地。这个声音既熟悉又陌生，让人心生警惕。",
                "回忆如潮水般涌来，过去的片段在脑海中闪现。这些记忆似乎与当前的处境有着某种联系。"
            };
            content.append(continuations[random.nextInt(continuations.length)]);
            
            if (userInstruction != null && !userInstruction.isEmpty()) {
                content.append("\n\n【根据用户指令：").append(userInstruction).append("】");
            }
            
        } else if ("polish".equals(writingType)) {
            content.append("【AI润色版本】\n\n");
            content.append("经过润色后的文本更加流畅优美：\n\n");
            
            // Simple polish - just add some descriptive words
            String polished = currentContent
                .replace("他", "这位勇敢的战士")
                .replace("她", "这位美丽的女子")
                .replace("它", "这个神秘的物体")
                .replace("走", "缓缓前行")
                .replace("说", "轻声说道");
            
            content.append(polished.substring(0, Math.min(polished.length(), maxTokens != null ? maxTokens : 500)));
            
        } else if ("expand".equals(writingType)) {
            content.append("【AI扩写内容】\n\n");
            content.append("在原内容的基础上进行了扩展：\n\n");
            
            String[] expansions = {
                "细节描写：周围的环境变得更加清晰。微风轻拂着树叶，发出沙沙的声响。阳光透过枝叶的缝隙洒下斑驳的光影。",
                "心理描写：内心涌起复杂的情感。既有对未来的期待，也有对未知的恐惧。这种矛盾的心情让他犹豫不决。",
                "对话扩展：\"你真的确定要这样做吗？\"同伴担忧地问道。\"我别无选择，\"他坚定地回答，\"这是唯一的出路。\"",
                "场景渲染：气氛变得紧张起来。每个人都屏住呼吸，等待着下一刻会发生什么。时间仿佛凝固了一般。",
                "背景补充：这个地方有着悠久的历史。传说中，曾经有一位英雄在这里完成了伟大的壮举，留下了不朽的传奇。"
            };
            
            content.append(currentContent).append("\n\n");
            content.append(expansions[random.nextInt(expansions.length)]);
            
        } else if ("rewrite".equals(writingType)) {
            content.append("【AI改写版本】\n\n");
            content.append("以不同的风格进行了改写：\n\n");
            
            String[] styles = {
                "【诗意风格】\n月光如水洒落，身影在夜色中摇曳。心随风云动，梦逐星辰远。",
                "【简洁风格】\n事情发生了。他做出了决定。行动开始了。",
                "【悬疑风格】\n事情并不简单。每个细节都隐藏着线索。真相就在眼前，却又遥不可及。",
                "【浪漫风格】\n那一刻，时间仿佛停止了。他们的目光相遇，心中涌起莫名的情感。",
                "【史诗风格】\n命运之轮开始转动，英雄踏上征途。前方是未知的挑战，背后是坚定的信念。"
            };
            
            content.append(styles[random.nextInt(styles.length)]);
        }
        
        // Ensure content doesn't exceed maxTokens
        String result = content.toString();
        if (maxTokens != null && result.length() > maxTokens) {
            result = result.substring(0, maxTokens);
        }
        
        return result;
    }
    
    private AIWritingResponseVO convertToVO(AIWritingRecord record) {
        AIWritingResponseVO vo = new AIWritingResponseVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}