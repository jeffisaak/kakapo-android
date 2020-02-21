package com.aptasystems.kakapo.view;

import android.util.Pair;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class FloatingMenu {

    public static class Builder {

        private FloatingActionButton _addButton;
        private List<Pair<FloatingActionButton, FloatingActionLabel>> _extraButtons;
        private float _perItemTranslation;

        public Builder() {
            _extraButtons = new ArrayList<>();
        }

        public Builder withAddButton(FloatingActionButton floatingActionButton) {
            _addButton = floatingActionButton;
            return this;
        }

        public Builder withExtraButton(FloatingActionButton floatingActionButton,
                                       FloatingActionLabel floatingActionLabel) {
            _extraButtons.add(new Pair<>(floatingActionButton, floatingActionLabel));
            return this;
        }

        public Builder perItemTranslation(float perItemTranslation) {
            _perItemTranslation = perItemTranslation;
            return this;
        }

        public FloatingMenu build() {
            FloatingMenu floatingMenu = new FloatingMenu();
            floatingMenu._addButton = _addButton;
            floatingMenu._extraButtons = new ArrayList<>();
            floatingMenu._extraButtons.addAll(_extraButtons);
            floatingMenu._perItemTranslation = _perItemTranslation;
            System.out.println("Add button 0: " + _addButton);
            System.out.println("Add button 1: " + floatingMenu._addButton);
            return floatingMenu;
        }
    }

    private FloatingActionButton _addButton;
    private List<Pair<FloatingActionButton, FloatingActionLabel>> _extraButtons;
    private float _perItemTranslation;

    public void open(boolean animate) {

        int animationDuration = animate ? 200 : 0;
System.out.println("Add button: " + _addButton);
        _addButton.animate().rotationBy(135f).setDuration(animationDuration).setInterpolator(new OvershootInterpolator());

        for (int ii = 0; ii < _extraButtons.size(); ii++) {
            FloatingActionButton extraButton = _extraButtons.get(ii).first;
            FloatingActionLabel extraLabel = _extraButtons.get(ii).second;
            float translation = _perItemTranslation * (float) (ii + 1) * -1f;
            extraButton.animate()
                    .translationY(translation)
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setInterpolator(new OvershootInterpolator());
            extraLabel.animate()
                    .translationY(translation)
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setInterpolator(new OvershootInterpolator());
        }
    }

    public void close(boolean animate) {

        int animationDuration = animate ? 100 : 0;

        _addButton.animate().rotationBy(-135f).setDuration(animationDuration).setInterpolator(new OvershootInterpolator());

        for (int ii = 0; ii < _extraButtons.size(); ii++) {
            FloatingActionButton extraButton = _extraButtons.get(ii).first;
            FloatingActionLabel extraLabel = _extraButtons.get(ii).second;
            extraButton.animate()
                    .translationY(0)
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setInterpolator(new LinearInterpolator());
            extraLabel.animate()
                    .translationY(0)
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setInterpolator(new LinearInterpolator());
        }
    }
}
