package com.aptasystems.kakapo.view;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class GenericDividerDecorator extends RecyclerView.ItemDecoration {
    private Drawable _dividerDrawable;

    public GenericDividerDecorator(Drawable dividerDrawable) {
        _dividerDrawable = dividerDrawable;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas, RecyclerView parent, @NonNull RecyclerView.State state) {
        int dividerLeft = parent.getPaddingLeft();
        int dividerRight = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int dividerTop = child.getBottom() + params.bottomMargin;
            int dividerBottom = dividerTop + _dividerDrawable.getIntrinsicHeight();

            _dividerDrawable.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
            _dividerDrawable.draw(canvas);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.set(0, 0, 0, _dividerDrawable.getIntrinsicHeight());
    }
}