package com.storyweaver.service.impl;

import com.storyweaver.domain.entity.Plot;
import com.storyweaver.service.PlotCrudService;
import com.storyweaver.service.PlotService;
import com.storyweaver.service.ProjectService;
import org.springframework.stereotype.Service;

@Service
public class PlotCrudServiceImpl implements PlotCrudService {

    private final PlotService plotService;
    private final ProjectService projectService;

    public PlotCrudServiceImpl(PlotService plotService, ProjectService projectService) {
        this.plotService = plotService;
        this.projectService = projectService;
    }

    @Override
    public Plot getPlot(Long id, Long userId) {
        Plot plot = plotService.getPlotById(id);
        if (plot == null || !projectService.hasProjectAccess(plot.getProjectId(), userId)) {
            return null;
        }
        return plot;
    }

    @Override
    public Plot createPlot(Long projectId, Long userId, Plot plot) {
        if (!projectService.hasProjectAccess(projectId, userId)) {
            return null;
        }
        plot.setId(null);
        plot.setProjectId(projectId);
        return plotService.createPlot(plot, userId) ? plot : null;
    }

    @Override
    public boolean updatePlot(Long id, Long userId, Plot plot) {
        Plot existing = getPlot(id, userId);
        if (existing == null) {
            return false;
        }
        existing.setChapterId(plot.getChapterId());
        existing.setTitle(plot.getTitle());
        existing.setDescription(plot.getDescription());
        existing.setContent(plot.getContent());
        existing.setPlotType(plot.getPlotType());
        existing.setSequence(plot.getSequence());
        existing.setCharacters(plot.getCharacters());
        existing.setLocations(plot.getLocations());
        existing.setTimeline(plot.getTimeline());
        existing.setConflicts(plot.getConflicts());
        existing.setResolutions(plot.getResolutions());
        existing.setTags(plot.getTags());
        existing.setStatus(plot.getStatus());
        return plotService.updatePlot(existing, userId);
    }

    @Override
    public boolean deletePlot(Long id, Long userId) {
        Plot existing = getPlot(id, userId);
        if (existing == null) {
            return false;
        }
        return plotService.deletePlot(id);
    }
}
