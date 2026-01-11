package com.example.chedder_1.ui.home;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.entity.WorkHours;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CalendarAdapter extends BaseAdapter {

    public interface OnDayClickListener {
        void onDayClick(int day);
    }

    public interface OnDayLongClickListener {
        void onDayLongClick(int day);
    }

    private final Context context;
    private final List<Integer> days;

    private final Set<Integer> workDays;
    private final Set<Integer> vacationDays;
    private final Set<Integer> cleaningDays;
    private final Set<Integer> canteenDutyDays;
    private final Set<Integer> lunchPrepDutyDays;

    // day -> WorkHours (для кастомного диапазона)
    private final Map<Integer, WorkHours> workHoursByDay;

    private final boolean editable;
    private final OnDayClickListener clickListener;
    private final OnDayLongClickListener longClickListener;

    // Цвета
    private static final int COLOR_WORK     = Color.parseColor("#F4C430"); // жёлтый
    private static final int COLOR_VACATION = Color.parseColor("#77B9FF"); // голубой
    private static final int COLOR_CLEANING = Color.parseColor("#FF4B4B"); // красный
    private static final int COLOR_CANTEEN  = Color.parseColor("#FF66B2"); // розовый
    private static final int COLOR_LUNCH    = Color.parseColor("#FFA500"); // оранжевый

    public CalendarAdapter(Context context,
                           List<Integer> days,
                           Set<Integer> workDays,
                           Set<Integer> vacationDays,
                           Set<Integer> cleaningDays,
                           Set<Integer> canteenDutyDays,
                           Set<Integer> lunchPrepDutyDays,
                           Map<Integer, WorkHours> workHoursByDay,
                           boolean editable,
                           OnDayClickListener clickListener,
                           OnDayLongClickListener longClickListener) {
        this.context = context;
        this.days = days;
        this.workDays = workDays;
        this.vacationDays = vacationDays;
        this.cleaningDays = cleaningDays;
        this.canteenDutyDays = canteenDutyDays;
        this.lunchPrepDutyDays = lunchPrepDutyDays;
        this.workHoursByDay = workHoursByDay;
        this.editable = editable;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override public int getCount() { return days.size(); }
    @Override public Integer getItem(int position) { return days.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.item_day, parent, false);
        }

        TextView dayText = view.findViewById(R.id.dayText);
        TextView timeText = view.findViewById(R.id.timeText);

        int day = days.get(position);

        view.setOnClickListener(null);
        view.setOnLongClickListener(null);

        if (day == 0) {
            dayText.setText("");
            if (timeText != null) {
                timeText.setText("");
                timeText.setVisibility(View.GONE);
            }
            view.setBackgroundColor(Color.TRANSPARENT);
            return view;
        }

        dayText.setText(String.valueOf(day));

        boolean isVacation = vacationDays != null && vacationDays.contains(day);
        boolean isCleaning = cleaningDays != null && cleaningDays.contains(day);

        boolean isWork = workDays != null && workDays.contains(day);
        boolean isCanteen = canteenDutyDays != null && canteenDutyDays.contains(day);
        boolean isLunch = lunchPrepDutyDays != null && lunchPrepDutyDays.contains(day);

        boolean isWorkLike = isWork || isCanteen || isLunch;

        // ===== ФОН =====
        if (isVacation) {
            view.setBackgroundColor(COLOR_VACATION);
        } else {
            if (isCanteen) {
                if (isCleaning) {
                    view.setBackground(new DiagonalSplitDrawable(COLOR_WORK, COLOR_CLEANING));
                } else {
                    view.setBackground(new DiagonalSplitDrawable(COLOR_WORK, COLOR_CANTEEN));
                }
            } else if (isLunch) {
                if (isCleaning) {
                    view.setBackground(new DiagonalSplitDrawable(COLOR_WORK, COLOR_CLEANING));
                } else {
                    view.setBackground(new DiagonalSplitDrawable(COLOR_WORK, COLOR_LUNCH));
                }
            } else if (isWork) {
                if (isCleaning) {
                    view.setBackground(new DiagonalSplitDrawable(COLOR_WORK, COLOR_CLEANING));
                } else {
                    view.setBackgroundColor(COLOR_WORK);
                }
            } else if (isCleaning) {
                view.setBackgroundColor(COLOR_CLEANING);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }
        }

        // ===== ТЕКСТ ВРЕМЕНИ =====
        if (timeText != null) {
            if (isVacation) {
                timeText.setText("");
                timeText.setVisibility(View.GONE);
            } else {
                WorkHours wh = (workHoursByDay == null) ? null : workHoursByDay.get(day);

                // дефолт смены 10-23
                int shiftStart = 10;
                int shiftEnd = 23;

                if (wh != null && wh.hours > 0) {
                    shiftStart = wh.startHour;
                    shiftEnd = Math.min(23, wh.startHour + wh.hours);
                }

                if (isWorkLike) {
                    // если уборка + смена => показываем 09–END (приходит к 9, уходит в конец смены)
                    int showStart = isCleaning ? 9 : shiftStart;
                    int showEnd = shiftEnd;
                    timeText.setText(String.format(Locale.getDefault(), "%02d–%02d", showStart, showEnd));
                    timeText.setVisibility(View.VISIBLE);
                } else if (isCleaning) {
                    // только уборка => 09–10
                    timeText.setText("09–10");
                    timeText.setVisibility(View.VISIBLE);
                } else {
                    timeText.setText("");
                    timeText.setVisibility(View.GONE);
                }
            }
        }

        // ===== КЛИКИ =====
        if (editable) {
            view.setOnClickListener(v -> {
                if (clickListener != null) clickListener.onDayClick(day);
            });

            view.setOnLongClickListener(v -> {
                if (longClickListener != null) longClickListener.onDayLongClick(day);
                return true;
            });
        }

        return view;
    }

    /** Drawable: квадрат 50/50 по диагонали. Верх-лево = colorA, низ-право = colorB. */
    private static class DiagonalSplitDrawable extends Drawable {
        private final Paint paintA = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint paintB = new Paint(Paint.ANTI_ALIAS_FLAG);

        DiagonalSplitDrawable(int colorA, int colorB) {
            paintA.setStyle(Paint.Style.FILL);
            paintB.setStyle(Paint.Style.FILL);
            paintA.setColor(colorA);
            paintB.setColor(colorB);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect r = getBounds();
            int left = r.left;
            int top = r.top;
            int right = r.right;
            int bottom = r.bottom;

            Path pathA = new Path();
            pathA.moveTo(left, top);
            pathA.lineTo(right, top);
            pathA.lineTo(left, bottom);
            pathA.close();

            Path pathB = new Path();
            pathB.moveTo(right, bottom);
            pathB.lineTo(right, top);
            pathB.lineTo(left, bottom);
            pathB.close();

            canvas.drawPath(pathA, paintA);
            canvas.drawPath(pathB, paintB);
        }

        @Override public void setAlpha(int alpha) {
            paintA.setAlpha(alpha);
            paintB.setAlpha(alpha);
            invalidateSelf();
        }

        @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            paintA.setColorFilter(colorFilter);
            paintB.setColorFilter(colorFilter);
            invalidateSelf();
        }

        @Override public int getOpacity() {
            return PixelFormat.OPAQUE;
        }
    }
}
