package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.OutlineRequestDTO;
import com.storyweaver.domain.entity.Outline;

import java.util.List;

public interface OutlineService extends IService<Outline> {

    List<Outline> getProjectOutlines(Long projectId, Long userId);

    Outline getOutlineWithAuth(Long outlineId, Long userId);

    Outline createOutline(Long projectId, Long userId, OutlineRequestDTO requestDTO);

    boolean updateOutline(Long projectId, Long outlineId, Long userId, OutlineRequestDTO requestDTO);

    boolean deleteOutline(Long outlineId, Long userId);
}
