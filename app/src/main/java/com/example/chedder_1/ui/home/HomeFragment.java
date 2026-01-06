package com.example.chedder_1.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HomeFragment extends Fragment {

    private GridView calendarGrid;
    private TextView title;
    private Button btnPrev, btnNext;
    private Calendar calendar;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        calendarGrid = view.findViewById(R.id.calendarGrid);
        title = view.findViewById(R.id.textView);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);

        calendar = Calendar.getInstance();   // текущий месяц

        updateCalendar();

        btnPrev.setOnClickListener(v -> {
            calendar = Calendar.getInstance(); // вернуться к текущему
            updateCalendar();
        });

        btnNext.setOnClickListener(v -> {
            calendar.add(Calendar.MONTH, 1);   // следующий месяц
            updateCalendar();
        });

        return view;
    }

    private void updateCalendar() {
        // Заголовок "Ноябрь 2025"
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
        String text = sdf.format(calendar.getTime());
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        title.setText(text);

        List<Integer> cells = new ArrayList<>();

        Calendar temp = (Calendar) calendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        int shift = (dayOfWeek + 5) % 7; // начало недели с ПН

        // пустые клетки до 1-го числа
        for (int i = 0; i < shift; i++) cells.add(0);

        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= daysInMonth; d++) {
            cells.add(d);
        }

        // ---------- ГРАФИК 2/2 ----------
        Set<Integer> workDays = new HashSet<>();
        Set<Integer> vacationDays = new HashSet<>();
        Set<Integer> cleaningDays = new HashSet<>();

        int month = calendar.get(Calendar.MONTH);           // 0 = январь
        int year = calendar.get(Calendar.YEAR);

        // 2/2: два рабочих, два выходных
        for (int d = 1; d <= daysInMonth; d++) {
            // отпуск в декабре (пример: 1–7 числа)
            if (month == Calendar.DECEMBER && d >= 1 && d <= 7) {
                vacationDays.add(d);
                continue; // в отпуске не считаем рабочие
            }

            int pattern = (d - 1) % 4; // 0,1 - работа; 2,3 - выходной
            if (pattern == 0 || pattern == 1) {
                workDays.add(d);
            }
        }

        // ---------- ГЕНЕРАЛЬНАЯ УБОРКА ----------
        // по понедельникам раз в две недели
        Calendar tmp = Calendar.getInstance();
        tmp.set(Calendar.YEAR, year);
        tmp.set(Calendar.MONTH, month);

        for (int d = 1; d <= daysInMonth; d++) {
            tmp.set(Calendar.DAY_OF_MONTH, d);

            if (tmp.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                int weekOfYear = tmp.get(Calendar.WEEK_OF_YEAR);
                // каждую вторую неделю (можно поменять чётность)
                if (weekOfYear % 2 == 0) {
                    cleaningDays.add(d);
                }
            }
        }

        CalendarAdapter adapter = new CalendarAdapter(
                getContext(),
                cells,
                workDays,
                vacationDays,
                cleaningDays
        );

        calendarGrid.setAdapter(adapter);
    }
}
