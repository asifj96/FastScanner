package com.camera.fastscanner.callbacks;

import androidx.annotation.IntDef;

@IntDef({Availability.ON, Availability.OFF, Availability.UNAVAILABLE})
public @interface Availability {
    int ON = 1;
    int OFF = 0;
    int UNAVAILABLE = -1;
}
