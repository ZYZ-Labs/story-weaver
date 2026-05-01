package com.storyweaver.domain.vo;

import java.util.List;

public record AIWritingRollbackResponseVO(
        Long chapterId,
        String mode,
        List<Long> rolledBackRecordIds,
        List<String> rolledBackSceneIds,
        List<String> remainingAcceptedSceneIds,
        String currentUnlockedSceneId,
        Integer restoredContentLength,
        String message) {

    public AIWritingRollbackResponseVO {
        mode = mode == null ? "" : mode.trim();
        rolledBackRecordIds = rolledBackRecordIds == null ? List.of() : List.copyOf(rolledBackRecordIds);
        rolledBackSceneIds = rolledBackSceneIds == null ? List.of() : List.copyOf(rolledBackSceneIds);
        remainingAcceptedSceneIds = remainingAcceptedSceneIds == null ? List.of() : List.copyOf(remainingAcceptedSceneIds);
        currentUnlockedSceneId = currentUnlockedSceneId == null ? "" : currentUnlockedSceneId.trim();
        restoredContentLength = restoredContentLength == null ? 0 : Math.max(restoredContentLength, 0);
        message = message == null ? "" : message.trim();
    }
}
