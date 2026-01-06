package com.example.chedder_1.ui.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.R;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DashboardFragment extends Fragment {

    // элементы экрана
    private TextView tvPosition;
    private TextView tvRatePerHour;
    private TextView tvRatePerShift;
    private TextView tvWorkedShifts;
    private TextView tvEarnedSoFar;
    private TextView tvMonthTotal;

    // ====== ПРИМЕРНЫЕ ЗНАЧЕНИЯ (потом можно заменить переменными / из БД) ======
    private static final String POSITION = "Повар сушист";
    private static final double RATE_PER_HOUR = 250.0;   // ставка в час, ₽
    private static final int HOURS_PER_SHIFT = 13;       // длительность смены, часов
    // если нужна доплата за генеральную уборку — можно использовать
    private static final double CLEANING_BONUS = 250.0;    // пример: 500.0 за каждую уборку

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        // находим View
        tvPosition      = view.findViewById(R.id.tvPosition);
        tvRatePerHour   = view.findViewById(R.id.tvRatePerHour);
        tvRatePerShift  = view.findViewById(R.id.tvRatePerShift);
        tvWorkedShifts  = view.findViewById(R.id.tvWorkedShifts);
        tvEarnedSoFar   = view.findViewById(R.id.tvEarnedSoFar);
        tvMonthTotal    = view.findViewById(R.id.tvMonthTotal);

        // заполняем данными
        fillStatistics();

        return view;
    }

    private void fillStatistics() {
        // ====== 1. Базовые данные по ставке ======
        double ratePerShift = RATE_PER_HOUR * HOURS_PER_SHIFT;

        tvPosition.setText("Должность: " + POSITION);
        tvRatePerHour.setText(
                String.format(Locale.getDefault(), "Ставка в час: %.0f ₽", RATE_PER_HOUR)
        );
        tvRatePerShift.setText(
                String.format(Locale.getDefault(), "Ставка за смену: %.0f ₽", ratePerShift)
        );

        // ====== 2. Берём текущий месяц / год ======
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        int month = now.get(Calendar.MONTH);            // 0 = январь
        int today = now.get(Calendar.DAY_OF_MONTH);

        Calendar temp = Calendar.getInstance();
        temp.set(Calendar.YEAR, year);
        temp.set(Calendar.MONTH, month);
        temp.set(Calendar.DAY_OF_MONTH, 1);

        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);

        // ====== 3. Строим тот же график 2/2, что в HomeFragment ======
        Set<Integer> workDays = new HashSet<>();
        Set<Integer> vacationDays = new HashSet<>();
        Set<Integer> cleaningDays = new HashSet<>();

        // --- рабочие / выходные ---
        for (int d = 1; d <= daysInMonth; d++) {

            // отпуск в декабре с 1 по 7 (точно как в HomeFragment)
            if (month == Calendar.DECEMBER && d >= 1 && d <= 7) {
                vacationDays.add(d);
                continue;   // эти дни не считаем как рабочие
            }

            // 2/2: (0,1) — работа, (2,3) — выходной
            int pattern = (d - 1) % 4;
            if (pattern == 0 || pattern == 1) {
                workDays.add(d);
            }
        }

        // --- генеральная уборка: понедельник раз в 2 недели ---
        Calendar tmp = Calendar.getInstance();
        tmp.set(Calendar.YEAR, year);
        tmp.set(Calendar.MONTH, month);

        for (int d = 1; d <= daysInMonth; d++) {
            tmp.set(Calendar.DAY_OF_MONTH, d);

            if (tmp.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                int weekOfYear = tmp.get(Calendar.WEEK_OF_YEAR);
                if (weekOfYear % 2 == 0) {
                    cleaningDays.add(d);
                }
            }
        }

        // ====== 4. Считаем, сколько смен уже прошло и сколько всего ======
        int workedShiftsSoFar = 0;
        for (int d : workDays) {
            if (d <= today) {
                workedShiftsSoFar++;
            }
        }

        int totalWorkShiftsInMonth = workDays.size();

        // уже прошедшие уборки (если за них есть доплата)
        int cleaningDoneSoFar = 0;
        for (int d : cleaningDays) {
            if (d <= today) {
                cleaningDoneSoFar++;
            }
        }

        int totalCleaningInMonth = cleaningDays.size();

        // ====== 5. Деньги, привязанные к графику ======
        double earnedBase = workedShiftsSoFar * ratePerShift;
        double earnedCleaning = cleaningDoneSoFar * CLEANING_BONUS;
        double earnedTotal = earnedBase + earnedCleaning;

        double monthBase = totalWorkShiftsInMonth * ratePerShift;
        double monthCleaning = totalCleaningInMonth * CLEANING_BONUS;
        double monthTotal = monthBase + monthCleaning;

        // ====== 6. Выводим на экран ======
        tvWorkedShifts.setText(
                String.format(
                        Locale.getDefault(),
                        "Отработано смен: %d из %d",
                        workedShiftsSoFar,
                        totalWorkShiftsInMonth
                )
        );

        tvEarnedSoFar.setText(
                String.format(
                        Locale.getDefault(),
                        "Заработано: ≈ %.0f ₽",
                        earnedTotal
                )
        );

        tvMonthTotal.setText(
                String.format(
                        Locale.getDefault(),
                        "Всего за месяц: ≈ %.0f ₽",
                        monthTotal
                )
        );
    }
}
