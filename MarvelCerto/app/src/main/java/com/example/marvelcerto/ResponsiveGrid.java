package com.example.marvelcerto;

import android.view.View;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/** Adjusts the number of cards to the available width. */
final class ResponsiveGrid {
    private ResponsiveGrid() {}

    static void configure(RecyclerView list, int minimumCellWidthDp) {
        if (!(list.getLayoutManager() instanceof GridLayoutManager)) return;
        GridLayoutManager grid = (GridLayoutManager) list.getLayoutManager();
        View.OnLayoutChangeListener listener = (view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int width = right - left - list.getPaddingLeft() - list.getPaddingRight();
            if (width <= 0) return;
            int minimum = Math.round(minimumCellWidthDp * list.getResources().getDisplayMetrics().density);
            int spans = Math.max(1, width / minimum);
            if (grid.getSpanCount() != spans) grid.setSpanCount(spans);
        };
        list.addOnLayoutChangeListener(listener);
        list.post(() -> listener.onLayoutChange(list, list.getLeft(), list.getTop(), list.getRight(), list.getBottom(),
                list.getLeft(), list.getTop(), list.getRight(), list.getBottom()));
    }
}
