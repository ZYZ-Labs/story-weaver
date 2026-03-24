package com.storyweaver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.storyweaver.domain.dto.WorldSettingDTO;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.domain.vo.WorldSettingVO;

import java.util.List;

public interface WorldSettingService extends IService<WorldSetting> {

    List<WorldSettingVO> getWorldSettingsByProjectId(Long projectId);

    List<WorldSettingVO> listLibraryWorldSettings(Long userId);

    WorldSettingVO getWorldSettingById(Long id, Long userId);

    WorldSettingVO createWorldSetting(WorldSettingDTO worldSettingDTO, Long userId);

    WorldSettingVO updateWorldSetting(Long id, WorldSettingDTO worldSettingDTO, Long userId);

    void deleteWorldSetting(Long id, Long userId);

    boolean attachWorldSettingToProject(Long worldSettingId, Long projectId, Long userId);

    boolean detachWorldSettingFromProject(Long worldSettingId, Long projectId, Long userId);

    void syncProjectAssociations(Long projectId, Long userId, List<Long> worldSettingIds);

    boolean hasAccess(Long worldSettingId, Long userId);
}
