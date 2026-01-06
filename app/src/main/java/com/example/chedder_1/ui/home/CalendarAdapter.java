package com.example.chedder_1.ui.home;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.chedder_1.R;

import java.util.List;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {

    private final Context context;
    private final List<Integer> days;
    private final Set<Integer> workDays;
    private final Set<Integer> vacationDays;
    private final Set<Integer> cleaningDays;

    public CalendarAdapter(Context context,
                           List<Integer> days,
                           Set<Integer> workDays,
                           Set<Integer> vacationDays,
                           Set<Integer> cleaningDays) {
        this.context = context;
        this.days = days;
        this.workDays = workDays;
        this.vacationDays = vacationDays;
        this.cleaningDays = cleaningDays;
    }

    @Override
    public int getCount() {
        return days.size();
    }

    @Override
    public Integer getItem(int position) {
        return days.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false);
        }

        TextView dayText = view.findViewById(R.id.dayText);
        int day = days.get(position);

        if (day == 0) {
            dayText.setText("");
            view.setBackgroundColor(Color.TRANSPARENT);
            return view;
        }

        dayText.setText(String.valueOf(day));

        // приоритет: отпуск > уборка > обычная смена > выходной
        if (vacationDays != null && vacationDays.contains(day)) {
            view.setBackgroundColor(Color.parseColor("#77B9FF")); // голубой – отпуск
        } else if (cleaningDays != null && cleaningDays.contains(day)) {
            view.setBackgroundColor(Color.parseColor("#FF4B4B")); // красный – уборка
        } else if (workDays != null && workDays.contains(day)) {
            view.setBackgroundColor(Color.parseColor("#F4C430")); // жёлтый – смена
        } else {
            view.setBackgroundColor(Color.TRANSPARENT);
        }

        return view;
    }
}
