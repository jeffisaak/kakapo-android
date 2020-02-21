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

@Singleton
public class ColourUtil {

    private List<Integer> _colours;

    @Inject
    public ColourUtil(Context context) {

        // Set up the colour array in roughly ROYGBIV order. Greys at the end.
        _colours = new ArrayList<>();
        _colours.add(ContextCompat.getColor(context, R.color.pink_500));
        _colours.add(ContextCompat.getColor(context, R.color.red_500));
        _colours.add(ContextCompat.getColor(context, R.color.deep_orange_500));
        _colours.add(ContextCompat.getColor(context, R.color.orange_600));
        _colours.add(ContextCompat.getColor(context, R.color.amber_700));
        _colours.add(ContextCompat.getColor(context, R.color.yellow_600));
        _colours.add(ContextCompat.getColor(context, R.color.lime_600));
        _colours.add(ContextCompat.getColor(context, R.color.light_green_500));
        _colours.add(ContextCompat.getColor(context, R.color.green_500));
        _colours.add(ContextCompat.getColor(context, R.color.teal_500));
        _colours.add(ContextCompat.getColor(context, R.color.cyan_500));
        _colours.add(ContextCompat.getColor(context, R.color.light_blue_500));
        _colours.add(ContextCompat.getColor(context, R.color.blue_500));
        _colours.add(ContextCompat.getColor(context, R.color.indigo_500));
        _colours.add(ContextCompat.getColor(context, R.color.purple_500));
        _colours.add(ContextCompat.getColor(context, R.color.deep_purple_500));
        _colours.add(ContextCompat.getColor(context, R.color.blue_grey_500));
        _colours.add(ContextCompat.getColor(context, R.color.grey_500));
        _colours.add(ContextCompat.getColor(context, R.color.brown_500));
    }

    public List<Integer> getColours() {
        return _colours;
    }

    public int[] getColourArray() {
        int[] result = new int[_colours.size()];
        for (int ii = 0; ii < _colours.size(); ii++) {
            result[ii] = _colours.get(ii);
        }
        return result;
    }

    public int randomColour() {
        SecureRandom random = new SecureRandom();
        return _colours.get(random.nextInt(_colours.size()));
    }

    public ColorPickerDialog showColourPickerDialog(final Context context, final Class<?> eventSource, final int selectedColour, final OnColorSelectedListener onColorSelectedListener) {
        ColorPickerDialog.Params params = new ColorPickerDialog.Params.Builder(context)
                .setSelectedColor(selectedColour)
                .setColors(getColourArray())
                .setSize(getColours().size())
                .build();

        // Haven't found good documentation of this library, but took usage from the Signal github
        // repository. Other setters include:
        //.setColorContentDescriptions()
        //.setSortColors()
        //.setColumns()

        ColorPickerDialog colourPickerDialog =
                new ColorPickerDialog(context, onColorSelectedListener, params);
        colourPickerDialog.show();
        return colourPickerDialog;
    }
}
