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
            return String.format(_context.getString(R.string.time_presentation_years), (int)deltaYear);
        }
        if( deltaMon > 1 ) {
            return String.format(_context.getString(R.string.time_presentation_months), (int)deltaMon);
        }
        if( deltaDay > 1 ) {
            return String.format(_context.getString(R.string.time_presentation_days),(int) deltaDay);
        }
        if( deltaHour > 1 ) {
            return String.format(_context.getString(R.string.time_presentation_hours), (int)deltaHour);
        }
        if( deltaMin > 1 ) {
            return String.format(_context.getString(R.string.time_presentation_minutes), (int)deltaMin);
        }
        if( deltaSec > 1 ) {
            return String.format(_context.getString(R.string.time_presentation_seconds), (int)deltaSec);
        }
        return _context.getString(R.string.time_presentation_now);
    }
}
