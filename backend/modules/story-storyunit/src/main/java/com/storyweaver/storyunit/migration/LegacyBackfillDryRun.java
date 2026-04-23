package com.storyweaver.storyunit.migration;

import java.util.List;

public record LegacyBackfillDryRun(
        LegacyChapterBackfillAnalysis analysis,
        boolean canRunBackfill,
        List<LegacyBackfillActionPlan> actions,
        List<String> riskNotes) {

    public LegacyBackfillDryRun {
        actions = actions == null ? List.of() : List.copyOf(actions);
        riskNotes = riskNotes == null ? List.of() : List.copyOf(riskNotes);
    }
}
