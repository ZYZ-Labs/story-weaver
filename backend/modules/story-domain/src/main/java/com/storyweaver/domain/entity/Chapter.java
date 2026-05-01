package com.storyweaver.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("chapter")
public class Chapter {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long projectId;

    private String title;

    private String summary;

    private String content;

    private Integer orderNum;

    private Integer status;

    private String chapterStatus;

    private Integer wordCount;

    private Long outlineId;

    private Long prevChapterId;

    private Long nextChapterId;

    private Long mainPovCharacterId;

    @TableField(exist = false)
    private List<Long> requiredCharacterIds;

    @TableField(exist = false)
    private List<String> requiredCharacterNames;

    @TableField(exist = false)
    private String outlineTitle;

    @TableField(exist = false)
    private List<Long> storyBeatIds;

    @TableField(exist = false)
    private List<String> storyBeatTitles;

    @TableField(exist = false)
    private String mainPovCharacterName;

    @TableField(exist = false)
    private Integer readingTimeMinutes;

    @TableField(exist = false)
    private String narrativeRuntimeMode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    // Manual getters and setters for IDE compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getOrderNum() { return orderNum; }
    public void setOrderNum(Integer orderNum) { this.orderNum = orderNum; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getChapterStatus() { return chapterStatus; }
    public void setChapterStatus(String chapterStatus) { this.chapterStatus = chapterStatus; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public Long getOutlineId() { return outlineId; }
    public void setOutlineId(Long outlineId) { this.outlineId = outlineId; }

    public Long getPrevChapterId() { return prevChapterId; }
    public void setPrevChapterId(Long prevChapterId) { this.prevChapterId = prevChapterId; }

    public Long getNextChapterId() { return nextChapterId; }
    public void setNextChapterId(Long nextChapterId) { this.nextChapterId = nextChapterId; }

    public Long getMainPovCharacterId() { return mainPovCharacterId; }
    public void setMainPovCharacterId(Long mainPovCharacterId) { this.mainPovCharacterId = mainPovCharacterId; }

    public List<Long> getRequiredCharacterIds() { return requiredCharacterIds; }
    public void setRequiredCharacterIds(List<Long> requiredCharacterIds) { this.requiredCharacterIds = requiredCharacterIds; }

    public List<String> getRequiredCharacterNames() { return requiredCharacterNames; }
    public void setRequiredCharacterNames(List<String> requiredCharacterNames) { this.requiredCharacterNames = requiredCharacterNames; }

    public String getOutlineTitle() { return outlineTitle; }
    public void setOutlineTitle(String outlineTitle) { this.outlineTitle = outlineTitle; }

    public List<Long> getStoryBeatIds() { return storyBeatIds; }
    public void setStoryBeatIds(List<Long> storyBeatIds) { this.storyBeatIds = storyBeatIds; }

    public List<String> getStoryBeatTitles() { return storyBeatTitles; }
    public void setStoryBeatTitles(List<String> storyBeatTitles) { this.storyBeatTitles = storyBeatTitles; }

    public String getMainPovCharacterName() { return mainPovCharacterName; }
    public void setMainPovCharacterName(String mainPovCharacterName) { this.mainPovCharacterName = mainPovCharacterName; }

    public Integer getReadingTimeMinutes() { return readingTimeMinutes; }
    public void setReadingTimeMinutes(Integer readingTimeMinutes) { this.readingTimeMinutes = readingTimeMinutes; }

    public String getNarrativeRuntimeMode() { return narrativeRuntimeMode; }
    public void setNarrativeRuntimeMode(String narrativeRuntimeMode) { this.narrativeRuntimeMode = narrativeRuntimeMode; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
