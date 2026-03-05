package com.controlmedicamentos.myapplication.utils;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * LinearLayoutManager que mide todo el contenido cuando el RecyclerView está dentro de un ScrollView.
 * Con height wrap_content, el RecyclerView por defecto puede solo medir los primeros ítems;
 * este manager fuerza la medición de todos para que se muestre la lista completa al hacer scroll en el ScrollView.
 */
public class WrapContentLinearLayoutManager extends LinearLayoutManager {

    public WrapContentLinearLayoutManager(Context context) {
        super(context);
    }

    @Override
    public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
                          int widthSpec, int heightSpec) {
        int itemCount = state.getItemCount();
        if (itemCount == 0 || state.isPreLayout()) {
            super.onMeasure(recycler, state, widthSpec, heightSpec);
            return;
        }
        int width = View.MeasureSpec.getSize(widthSpec);
        int totalHeight = 0;
        for (int i = 0; i < itemCount; i++) {
            View view = recycler.getViewForPosition(i);
            if (view != null) {
                measureChild(view, 0, 0);
                totalHeight += getDecoratedMeasuredHeight(view);
                recycler.recycleView(view);
            }
        }
        setMeasuredDimension(width, totalHeight);
    }
}
