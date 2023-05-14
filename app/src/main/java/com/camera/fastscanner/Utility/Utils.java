package com.camera.fastscanner.Utility;

import com.camera.fastscanner.callbacks.Availability;

public class Utils {


    public static boolean availabilityToBoolean(@Availability int availability) {
        return availability == Availability.ON;
    }
}
