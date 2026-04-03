package com.storyweaver.item.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.storyweaver.item.domain.entity.CharacterInventoryItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CharacterInventoryItemMapper extends BaseMapper<CharacterInventoryItem> {
}
