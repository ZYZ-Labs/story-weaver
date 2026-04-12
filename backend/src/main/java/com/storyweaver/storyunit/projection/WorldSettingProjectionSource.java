package com.storyweaver.storyunit.projection;

import com.storyweaver.domain.entity.OutlineWorldSettingLink;
import com.storyweaver.domain.entity.ProjectWorldSettingLink;
import com.storyweaver.domain.entity.WorldSetting;

import java.util.List;
import java.util.Objects;

public record WorldSettingProjectionSource(
        WorldSetting worldSetting,
        List<ProjectWorldSettingLink> projectLinks,
        List<OutlineWorldSettingLink> outlineLinks) {

    public WorldSettingProjectionSource {
        worldSetting = Objects.requireNonNull(worldSetting, "worldSetting must not be null");
        projectLinks = projectLinks == null ? List.of() : List.copyOf(projectLinks);
        outlineLinks = outlineLinks == null ? List.of() : List.copyOf(outlineLinks);
    }
}
