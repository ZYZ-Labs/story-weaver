package com.storyweaver.storyunit.migration;

public enum CompatibilityMode {
    NEW_PRIMARY,
    DUAL_READ,
    DUAL_WRITE,
    LEGACY_PRIMARY,
    LEGACY_FALLBACK,
    DISABLED
}
