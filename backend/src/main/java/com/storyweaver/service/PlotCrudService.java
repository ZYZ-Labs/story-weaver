package com.storyweaver.service;

import com.storyweaver.domain.entity.Plot;

public interface PlotCrudService {
    Plot getPlot(Long id, Long userId);
    Plot createPlot(Long projectId, Long userId, Plot plot);
    boolean updatePlot(Long id, Long userId, Plot plot);
    boolean deletePlot(Long id, Long userId);
}
