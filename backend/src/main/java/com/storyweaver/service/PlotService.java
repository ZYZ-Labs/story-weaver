package com.storyweaver.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.storyweaver.domain.entity.Plot;
import com.storyweaver.repository.PlotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlotService extends ServiceImpl<PlotMapper, Plot> {
    
    public List<Plot> getProjectPlots(Long projectId) {
        return baseMapper.findByProjectId(projectId);
    }
    
    public List<Plot> getChapterPlots(Long chapterId) {
        return baseMapper.findByChapterId(chapterId);
    }
    
    public List<Plot> getPlotsByType(Long projectId, Integer plotType) {
        return baseMapper.findByPlotType(projectId, plotType);
    }
    
    public List<Plot> searchPlots(Long projectId, String keyword) {
        return baseMapper.searchByKeyword(projectId, keyword);
    }
    
    public List<Plot> getPlotsByCharacter(Long projectId, String characterName) {
        return baseMapper.findByCharacter(projectId, characterName);
    }
    
    @Transactional
    public boolean createPlot(Plot plot, Long userId) {
        normalizePlotFields(plot);
        if (plot.getSequence() == null) {
            Integer maxSequence = baseMapper.getMaxSequenceByChapterId(plot.getChapterId());
            plot.setSequence(maxSequence != null ? maxSequence + 1 : 1);
        }
        
        plot.setCreateTime(LocalDateTime.now());
        plot.setUpdateTime(LocalDateTime.now());
        plot.setCreateBy(userId);
        plot.setUpdateBy(userId);
        plot.setDeleted(0);
        plot.setStatus(1);
        
        return save(plot);
    }
    
    @Transactional
    public boolean updatePlot(Plot plot, Long userId) {
        normalizePlotFields(plot);
        plot.setUpdateTime(LocalDateTime.now());
        plot.setUpdateBy(userId);
        
        return updateById(plot);
    }
    
    @Transactional
    public boolean deletePlot(Long plotId) {
        if (getPlotById(plotId) == null) {
            return false;
        }

        // Use MyBatis-Plus logical delete so the deleted flag is persisted consistently.
        return removeById(plotId);
    }
    
    public Plot getPlotById(Long plotId) {
        LambdaQueryWrapper<Plot> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Plot::getId, plotId)
                   .eq(Plot::getDeleted, 0);
        return getOne(queryWrapper);
    }
    
    @Transactional
    public boolean reorderPlots(Long chapterId, List<Long> plotIds) {
        for (int i = 0; i < plotIds.size(); i++) {
            Plot plot = getById(plotIds.get(i));
            if (plot != null && plot.getChapterId().equals(chapterId)) {
                plot.setSequence(i + 1);
                updateById(plot);
            }
        }
        return true;
    }
    
    public String generatePlotSummary(Long projectId) {
        List<Plot> plots = getProjectPlots(projectId);
        StringBuilder summary = new StringBuilder();
        
        summary.append("Plot Summary for Project ID: ").append(projectId).append("\n\n");
        summary.append("Total Plots: ").append(plots.size()).append("\n\n");
        
        int mainPlots = 0;
        int subPlots = 0;
        int characterPlots = 0;
        
        for (Plot plot : plots) {
            if (plot.getPlotType() == 1) mainPlots++;
            else if (plot.getPlotType() == 2) subPlots++;
            else if (plot.getPlotType() == 3) characterPlots++;
            
            summary.append(plot.getSequence()).append(". ").append(plot.getTitle())
                   .append(" (").append(getPlotTypeName(plot.getPlotType())).append(")\n");
        }
        
        summary.append("\nStatistics:\n");
        summary.append("- Main Plots: ").append(mainPlots).append("\n");
        summary.append("- Sub Plots: ").append(subPlots).append("\n");
        summary.append("- Character Plots: ").append(characterPlots).append("\n");
        
        return summary.toString();
    }
    
    private String getPlotTypeName(Integer plotType) {
        switch (plotType) {
            case 1: return "Main Plot";
            case 2: return "Sub Plot";
            case 3: return "Character Plot";
            case 4: return "Background Plot";
            default: return "Unknown";
        }
    }

    private void normalizePlotFields(Plot plot) {
        if (plot == null) {
            return;
        }

        if (!StringUtils.hasText(plot.getStoryBeatType())) {
            plot.setStoryBeatType(resolveStoryBeatType(plot.getPlotType()));
        }
        if (!StringUtils.hasText(plot.getStoryFunction())) {
            plot.setStoryFunction(resolveStoryFunction(plot.getPlotType()));
        }
        if (!StringUtils.hasText(plot.getEventResult()) && StringUtils.hasText(plot.getResolutions())) {
            plot.setEventResult(plot.getResolutions().trim());
        }
        if (!StringUtils.hasText(plot.getResolutions()) && StringUtils.hasText(plot.getEventResult())) {
            plot.setResolutions(plot.getEventResult().trim());
        }
        if (plot.getPlotType() == null) {
            plot.setPlotType(resolveLegacyPlotType(plot.getStoryBeatType()));
        }
        if (plot.getOutlinePriority() == null) {
            plot.setOutlinePriority(plot.getSequence());
        }
    }

    private String resolveStoryBeatType(Integer plotType) {
        if (plotType == null) {
            return "main";
        }
        return switch (plotType) {
            case 2 -> "side";
            case 3 -> "climax";
            case 4 -> "foreshadow";
            case 5 -> "reveal";
            default -> "main";
        };
    }

    private String resolveStoryFunction(Integer plotType) {
        if (plotType == null) {
            return "advance_mainline";
        }
        return switch (plotType) {
            case 2 -> "character_growth";
            case 3 -> "conflict_upgrade";
            case 4 -> "foreshadow";
            case 5 -> "payoff";
            default -> "advance_mainline";
        };
    }

    private Integer resolveLegacyPlotType(String storyBeatType) {
        if (!StringUtils.hasText(storyBeatType)) {
            return 1;
        }
        return switch (storyBeatType.trim()) {
            case "side" -> 2;
            case "climax" -> 3;
            case "foreshadow" -> 4;
            case "reveal" -> 5;
            default -> 1;
        };
    }
}
