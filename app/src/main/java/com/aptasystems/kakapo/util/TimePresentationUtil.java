package com.aptasystems.kakapo.util;

import android.content.Context;

import com.aptasystems.kakapo.R;
import com.takisoft.colorpicker.ColorPickerDialog;
import com.takisoft.colorpicker.OnColorSelectedListener;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import androidx.core.content.ContextCompat;
import kakapo.util.TimeUtil;

@Singleton
public class TimePresentationUtil {

    private Context _context;

    @Inject
    public TimePresentationUtil(Context context) {
        _context = context;
    }

    public String formatAsTimePast(long timestampInZulu) {
        long nowTimestamp = TimeUtil.timestampInZulu(TimeUtil.timestampInGMT());
        long deltaMs = nowTimestamp - timestampInZulu;
        float deltaSec = deltaMs / 1000f;
        float deltaMin = deltaSec / 60f;
        float deltaHour = deltaMin / 60f;
        float deltaDay = deltaHour / 24f;
        float deltaMon = deltaDay / 30.4f;
        float deltaYear = deltaDay / 365.25f;

        if( deltaYear > 1 )
        {
            return String.format("%1$dy", (int)deltaYear);
        }
        if( deltaMon > 1 ) {
            return String.format("%1$dmon", (int)deltaMon);
        }
        if( deltaDay > 1 ) {
            return String.format("%1$dd",(int) deltaDay);
        }
        if( deltaHour > 1 ) {
            return String.format("%1$dh", (int)deltaHour);
        }
        if( deltaMin > 1 ) {
            return String.format("%1$dmin", (int)deltaMin);
        }
        if( deltaSec > 1 ) {
            return String.format("%1$ds", (int)deltaSec);
        }
        return "now";
    }
}
