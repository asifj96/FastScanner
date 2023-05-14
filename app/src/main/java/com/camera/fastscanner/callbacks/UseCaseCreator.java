package com.camera.fastscanner.callbacks;

import androidx.camera.core.UseCase;

@FunctionalInterface
public interface UseCaseCreator {
    UseCase[] create();
}

