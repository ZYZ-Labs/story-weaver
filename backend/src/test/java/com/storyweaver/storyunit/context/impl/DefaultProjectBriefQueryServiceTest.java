package com.storyweaver.storyunit.context.impl;

import com.storyweaver.domain.entity.Project;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;
import com.storyweaver.repository.ProjectMapper;
import com.storyweaver.repository.ProjectWorldSettingMapper;
import com.storyweaver.repository.WorldSettingMapper;
import com.storyweaver.storyunit.context.ProjectBriefView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultProjectBriefQueryServiceTest {

    @Test
    void shouldBuildProjectBriefFromProjectAndLinkedWorldSettings() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectWorldSettingMapper projectWorldSettingMapper = mock(ProjectWorldSettingMapper.class);
        WorldSettingMapper worldSettingMapper = mock(WorldSettingMapper.class);

        Project project = new Project();
        project.setId(28L);
        project.setName("旧日王座");
        project.setDescription("一个关于退役选手重返旧日王座的故事");
        project.setGenre("电竞悬疑");
        project.setTags("回归, 旧战队");
        project.setDeleted(0);

        ProjectWorldSettingLink link = new ProjectWorldSettingLink();
        link.setProjectId(28L);
        link.setWorldSettingId(101L);

        WorldSetting worldSetting = new WorldSetting();
        worldSetting.setId(101L);
        worldSetting.setName("旧日王座职业圈");
        worldSetting.setDeleted(0);

        when(projectMapper.selectById(28L)).thenReturn(project);
        when(projectWorldSettingMapper.selectList(any())).thenReturn(List.of(link));
        when(worldSettingMapper.selectBatchIds(any())).thenReturn(List.of(worldSetting));

        DefaultProjectBriefQueryService service = new DefaultProjectBriefQueryService(
                projectMapper,
                projectWorldSettingMapper,
                worldSettingMapper
        );

        Optional<ProjectBriefView> result = service.getProjectBrief(28L);

        assertTrue(result.isPresent());
        assertEquals("旧日王座", result.get().projectTitle());
        assertEquals("一个关于退役选手重返旧日王座的故事", result.get().logline());
        assertTrue(result.get().summary().contains("题材：电竞悬疑"));
        assertTrue(result.get().summary().contains("标签：回归, 旧战队"));
        assertTrue(result.get().summary().contains("关联世界观：旧日王座职业圈"));
    }
}
