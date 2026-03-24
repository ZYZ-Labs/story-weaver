package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.entity.Causality;

import java.util.List;

public interface CausalityService extends IService<Causality> {
    List<Causality> getProjectCausalities(Long projectId, Long userId);
    Causality getCausality(Long id, Long userId);
    Causality createCausality(Long projectId, Long userId, Causality causality);
    boolean updateCausality(Long id, Long userId, Causality causality);
    boolean deleteCausality(Long id, Long userId);
}
