package com.storyweaver.storyunit.context;

import java.util.Optional;

public interface CharacterRuntimeStateQueryService {

    Optional<CharacterRuntimeStateView> getCharacterRuntimeState(Long projectId, Long characterId);
}
