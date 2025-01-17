package com.quranapp.android.adapters.utility;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.peacedesign.android.utils.Dimen;
import com.peacedesign.android.utils.ViewUtils;
import com.quranapp.android.components.utility.SpinnerItem;

import java.util.List;

public class TopicFilterSpinnerAdapter extends SpinnerAdapter2<SpinnerItem> {
    public TopicFilterSpinnerAdapter(@NonNull Context context, int itemLytRes, int textViewId, @NonNull List<SpinnerItem> objects) {
        super(context, itemLytRes, textViewId, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = super.getDropDownView(position, convertView, parent);
        ViewUtils.setPaddingVertical(view, Dimen.dp2px(parent.getContext(),10));
        ViewUtils.setPaddingHorizontal(view, Dimen.dp2px(parent.getContext(),15));
        return view;
    }
}
