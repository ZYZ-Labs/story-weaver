package com.storyweaver.storyunit.patch;

import java.util.Objects;

public record PatchOperation(
        PatchOperationType op,
        String path,
        Object value) {

    public PatchOperation {
        op = Objects.requireNonNull(op, "op must not be null");
        path = Objects.requireNonNull(path, "path must not be null").trim();
    }
}
