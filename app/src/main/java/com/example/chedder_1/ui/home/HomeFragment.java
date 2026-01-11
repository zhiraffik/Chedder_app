package com.example.chedder_1.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.Schedule;
import com.example.chedder_1.domain.entity.User;
import com.example.chedder_1.domain.entity.WorkHours;
import com.example.chedder_1.ui.AddEmployeeActivity;
import com.google.android.material.slider.RangeSlider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.widget.LinearLayout;

public class HomeFragment extends Fragment {

    private GridView calendarGrid;
    private TextView title;
    private View btnAddEmployee;
    private View btnPrev, btnNext;
    private Spinner spinnerEmployee;

    private Calendar calendar;

    //  3 месяца назад, 2 месяца вперед от текущего
    private Calendar minCalendar; // current -3
    private Calendar maxCalendar; // current +2

    private AppDataBase db;
    private SharedPreferences prefs;

    private int currentAdminId = -1;
    private int currentEmployeeId = -1;
    private Schedule currentSchedule;

    private final Set<Integer> workDays = new HashSet<>();
    private final Set<Integer> vacationDays = new HashSet<>();
    private final Set<Integer> cleaningDays = new HashSet<>();
    private final Set<Integer> canteenDutyDays = new HashSet<>();
    private final Set<Integer> lunchPrepDutyDays = new HashSet<>();

    // day -> WorkHours (кастомный диапазон смены)
    private final Map<Integer, WorkHours> workHoursByDay = new HashMap<>();

    private List<User> staff = new ArrayList<>();

    private static final String PREF_SELECTED_EMPLOYEE = "selectedEmployeeId";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);
        db = AppDataBase.getInstance(requireContext());

        calendarGrid = view.findViewById(R.id.calendarGrid);
        title = view.findViewById(R.id.textView);
        btnPrev = view.findViewById(R.id.btn_prev);
        btnNext = view.findViewById(R.id.btn_next);
        spinnerEmployee = view.findViewById(R.id.spinnerEmployee);
        btnAddEmployee = view.findViewById(R.id.btnAddEmployee);

        // ====== текущий месяц ======
        calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // ====== границы просмотра ======
        minCalendar = (Calendar) calendar.clone();
        minCalendar.add(Calendar.MONTH, -3); // 3 месяца назад
        minCalendar.set(Calendar.DAY_OF_MONTH, 1);

        maxCalendar = (Calendar) calendar.clone();
        maxCalendar.add(Calendar.MONTH, 2);  // 2 месяца вперед
        maxCalendar.set(Calendar.DAY_OF_MONTH, 1);

        if (!resolveBaseSession()) {
            Toast.makeText(requireContext(), "Нет авторизации", Toast.LENGTH_SHORT).show();
            setupAdminUi(false);
            updateCalendar(false);
            return view;
        }

        setupAdminUi(isAdmin());
        setupEmployeeSelectorIfAdmin();

        updateCalendar(isAdmin());

        btnPrev.setOnClickListener(v -> {
            Calendar candidate = (Calendar) calendar.clone();
            candidate.add(Calendar.MONTH, -1);
            candidate.set(Calendar.DAY_OF_MONTH, 1);

            if (candidate.before(minCalendar)) {
                Toast.makeText(requireContext(), "Можно смотреть только 3 месяца назад", Toast.LENGTH_SHORT).show();
                updateNavButtons();
                return;
            }
            calendar = candidate;
            updateCalendar(isAdmin());
        });

        btnNext.setOnClickListener(v -> {
            Calendar candidate = (Calendar) calendar.clone();
            candidate.add(Calendar.MONTH, 1);
            candidate.set(Calendar.DAY_OF_MONTH, 1);

            if (candidate.after(maxCalendar)) {
                Toast.makeText(requireContext(), "Можно смотреть только 2 месяца вперёд", Toast.LENGTH_SHORT).show();
                updateNavButtons();
                return;
            }
            calendar = candidate;
            updateCalendar(isAdmin());
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;

        if (isAdmin()) {
            setupEmployeeSelectorIfAdmin();
            updateCalendar(true);
        } else {
            updateCalendar(false);
        }
    }

    private boolean isAdmin() {
        return prefs != null && "admin".equals(prefs.getString("role", null));
    }

    private void setupAdminUi(boolean adminMode) {
        if (adminMode) {
            if (btnAddEmployee != null) {
                btnAddEmployee.setVisibility(View.VISIBLE);
                btnAddEmployee.setOnClickListener(v ->
                        startActivity(new Intent(requireContext(), AddEmployeeActivity.class))
                );
            }
            if (spinnerEmployee != null) spinnerEmployee.setVisibility(View.VISIBLE);
        } else {
            if (btnAddEmployee != null) btnAddEmployee.setVisibility(View.GONE);
            if (spinnerEmployee != null) spinnerEmployee.setVisibility(View.GONE);
        }
    }

    private boolean resolveBaseSession() {
        int userId = prefs.getInt("userId", -1);
        int adminId = prefs.getInt("adminId", -1);
        String role = prefs.getString("role", null);

        if (userId == -1 || role == null) return false;

        if ("admin".equals(role)) {
            currentAdminId = userId;
            return true;
        }

        if ("employee".equals(role)) {
            currentEmployeeId = userId;
            currentAdminId = adminId;
            return currentAdminId != -1;
        }

        return false;
    }

    private void setupEmployeeSelectorIfAdmin() {
        if (!isAdmin()) return;

        spinnerEmployee.setOnItemSelectedListener(null);

        staff = db.userDao().getEmployeesForAdmin(currentAdminId);
        if (staff == null || staff.isEmpty()) {
            Toast.makeText(requireContext(), "Добавьте сотрудников, чтобы составлять график", Toast.LENGTH_SHORT).show();
            currentEmployeeId = -1;
            spinnerEmployee.setAdapter(null);
            updateNavButtons();
            return;
        }

        List<String> labels = new ArrayList<>();
        for (User u : staff) {
            String name = (u.lastName == null ? "" : u.lastName + " ") + (u.firstName == null ? "" : u.firstName);
            String pos = (u.position == null ? "" : " (" + u.position + ")");
            labels.add(name.trim() + pos);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployee.setAdapter(adapter);

        int savedEmployeeId = prefs.getInt(PREF_SELECTED_EMPLOYEE, -1);
        int initialIndex = 0;
        if (savedEmployeeId != -1) {
            for (int i = 0; i < staff.size(); i++) {
                if (staff.get(i).id == savedEmployeeId) {
                    initialIndex = i;
                    break;
                }
            }
        }

        spinnerEmployee.setSelection(initialIndex);
        currentEmployeeId = staff.get(initialIndex).id;

        spinnerEmployee.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentEmployeeId = staff.get(position).id;
                prefs.edit().putInt(PREF_SELECTED_EMPLOYEE, currentEmployeeId).apply();
                updateCalendar(true);
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private void updateCalendar(boolean editable) {
        SimpleDateFormat sdf = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
        String text = sdf.format(calendar.getTime());
        text = text.substring(0, 1).toUpperCase() + text.substring(1);
        title.setText(text);

        List<Integer> cells = buildMonthCells(calendar);

        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);

        if (isAdmin() && currentEmployeeId == -1) {
            clearSets();
            workHoursByDay.clear();

            CalendarAdapter ca = new CalendarAdapter(
                    getContext(),
                    cells,
                    workDays,
                    vacationDays,
                    cleaningDays,
                    canteenDutyDays,
                    lunchPrepDutyDays,
                    workHoursByDay,
                    false,
                    null,
                    null
            );
            calendarGrid.setAdapter(ca);
            updateNavButtons();
            return;
        }

        loadScheduleAndWorkHours(year, month);

        CalendarAdapter adapter = new CalendarAdapter(
                getContext(),
                cells,
                workDays,
                vacationDays,
                cleaningDays,
                canteenDutyDays,
                lunchPrepDutyDays,
                workHoursByDay,
                editable,
                day -> onPrimaryToggle(day, year, month),
                day -> onDayActions(day, year, month)
        );

        calendarGrid.setAdapter(adapter);
        updateNavButtons();
    }

    private void updateNavButtons() {
        if (btnPrev == null || btnNext == null) return;

        Calendar cur = (Calendar) calendar.clone();
        cur.set(Calendar.DAY_OF_MONTH, 1);

        boolean canPrev = !cur.equals(minCalendar) && cur.after(minCalendar);
        boolean canNext = !cur.equals(maxCalendar) && cur.before(maxCalendar);

        btnPrev.setEnabled(canPrev);
        btnNext.setEnabled(canNext);

        btnPrev.setAlpha(canPrev ? 1f : 0.45f);
        btnNext.setAlpha(canNext ? 1f : 0.45f);
    }

    private List<Integer> buildMonthCells(Calendar calendar) {
        List<Integer> cells = new ArrayList<>();

        Calendar temp = (Calendar) calendar.clone();
        temp.set(Calendar.DAY_OF_MONTH, 1);

        int dayOfWeek = temp.get(Calendar.DAY_OF_WEEK);
        int shift = (dayOfWeek + 5) % 7; // ПН = 0

        for (int i = 0; i < shift; i++) cells.add(0);

        int daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH);
        for (int d = 1; d <= daysInMonth; d++) cells.add(d);

        return cells;
    }

    private void clearSets() {
        workDays.clear();
        vacationDays.clear();
        cleaningDays.clear();
        canteenDutyDays.clear();
        lunchPrepDutyDays.clear();
    }

    private void loadScheduleAndWorkHours(int year, int month) {
        clearSets();
        workHoursByDay.clear();

        currentSchedule = db.scheduleDao().getSchedule(currentAdminId, currentEmployeeId, year, month);

        if (currentSchedule == null) {
            currentSchedule = new Schedule();
            currentSchedule.adminId = currentAdminId;
            currentSchedule.userId = currentEmployeeId;
            currentSchedule.year = year;
            currentSchedule.month = month;
            db.scheduleDao().upsert(currentSchedule);
        }

        if (currentSchedule.workDays != null) workDays.addAll(currentSchedule.workDays);
        if (currentSchedule.vacationDays != null) vacationDays.addAll(currentSchedule.vacationDays);
        if (currentSchedule.cleaningDays != null) cleaningDays.addAll(currentSchedule.cleaningDays);
        if (currentSchedule.canteenDutyDays != null) canteenDutyDays.addAll(currentSchedule.canteenDutyDays);
        if (currentSchedule.lunchPrepDutyDays != null) lunchPrepDutyDays.addAll(currentSchedule.lunchPrepDutyDays);

        List<WorkHours> list = db.workHoursDao().getMonth(currentAdminId, currentEmployeeId, year, month);
        if (list != null) {
            for (WorkHours wh : list) {
                workHoursByDay.put(wh.day, wh);
            }
        }
    }

    private void onPrimaryToggle(int day, int year, int month) {
        if (!isAdmin() || day <= 0) return;

        boolean isVacation = vacationDays.contains(day);
        boolean isWork = workDays.contains(day);
        boolean isCanteen = canteenDutyDays.contains(day);
        boolean isLunch = lunchPrepDutyDays.contains(day);

        workDays.remove(day);
        canteenDutyDays.remove(day);
        lunchPrepDutyDays.remove(day);
        vacationDays.remove(day);

        if (!isWork && !isCanteen && !isLunch && !isVacation) {
            workDays.add(day);
        } else if (isWork) {
            canteenDutyDays.add(day);
        } else if (isCanteen) {
            lunchPrepDutyDays.add(day);
        } else if (isLunch) {
            vacationDays.add(day);
            cleaningDays.remove(day);
            resetShiftRange(day, year, month);
        } else if (isVacation) {
            cleaningDays.remove(day);
            resetShiftRange(day, year, month);
        }

        boolean isWorkLikeNow = workDays.contains(day) || canteenDutyDays.contains(day) || lunchPrepDutyDays.contains(day);
        if (!isWorkLikeNow) {
            resetShiftRange(day, year, month);
        }

        saveSchedule(year, month);
        ((CalendarAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
    }

    private void onDayActions(int day, int year, int month) {
        if (!isAdmin() || day <= 0) return;

        String[] items = {"Переключить уборку", "Задать время смены (10–23)"};

        new AlertDialog.Builder(requireContext())
                .setTitle("День " + day)
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        toggleCleaning(day, year, month);
                    } else if (which == 1) {
                        showShiftRangeDialog(day, year, month);
                    }
                })
                .show();
    }

    private void toggleCleaning(int day, int year, int month) {
        if (vacationDays.contains(day)) {
            Toast.makeText(requireContext(), "Нельзя поставить уборку в отпуск", Toast.LENGTH_SHORT).show();
            return;
        }

        if (cleaningDays.contains(day)) cleaningDays.remove(day);
        else cleaningDays.add(day);

        saveSchedule(year, month);
        ((CalendarAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
    }

    private void showShiftRangeDialog(int day, int year, int month) {
        boolean isWorkLike = workDays.contains(day) || canteenDutyDays.contains(day) || lunchPrepDutyDays.contains(day);

        if (!isWorkLike) {
            Toast.makeText(requireContext(), "Время смены задаётся только для смены/дежурств", Toast.LENGTH_SHORT).show();
            return;
        }
        if (vacationDays.contains(day)) {
            Toast.makeText(requireContext(), "В отпуск смену не ставим", Toast.LENGTH_SHORT).show();
            return;
        }

        WorkHours existing = workHoursByDay.get(day);

        int defStart = 10;
        int defEnd = 23;

        if (existing != null && existing.hours > 0) {
            defStart = existing.startHour;
            defEnd = Math.min(23, existing.startHour + existing.hours);
        }

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView label = new TextView(requireContext());
        label.setTextSize(16);

        RangeSlider slider = new RangeSlider(requireContext());
        slider.setValueFrom(10f);
        slider.setValueTo(23f);
        slider.setStepSize(1f);
        slider.setValues((float) defStart, (float) defEnd);

        Runnable updateLabel = () -> {
            List<Float> vals = slider.getValues();
            int start = Math.round(vals.get(0));
            int end = Math.round(vals.get(1));
            if (end <= start) end = start + 1;

            String extra = cleaningDays.contains(day) ? " (+ уборка 09–10)" : "";
            label.setText(String.format(Locale.getDefault(), "Смена: %02d:00 – %02d:00%s", start, end, extra));
        };
        updateLabel.run();

        slider.addOnChangeListener((s, value, fromUser) -> updateLabel.run());

        root.addView(label);
        root.addView(slider);

        new AlertDialog.Builder(requireContext())
                .setTitle("День " + day)
                .setView(root)
                .setPositiveButton("Сохранить", (dlg, w) -> {
                    List<Float> vals = slider.getValues();
                    int start = Math.round(vals.get(0));
                    int end = Math.round(vals.get(1));

                    if (end <= start) {
                        Toast.makeText(requireContext(), "Конец должен быть позже начала", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int hours = end - start;

                    if (start == 10 && end == 23) {
                        if (existing != null) {
                            db.workHoursDao().deleteDay(currentAdminId, currentEmployeeId, year, month, day);
                            workHoursByDay.remove(day);
                        }
                        Toast.makeText(requireContext(),
                                cleaningDays.contains(day) ? "Смена: 10–23 (+ уборка)" : "Смена: 10–23",
                                Toast.LENGTH_SHORT).show();
                        ((CalendarAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
                        return;
                    }

                    WorkHours wh = (existing != null) ? existing : new WorkHours();
                    wh.adminId = currentAdminId;
                    wh.userId = currentEmployeeId;
                    wh.year = year;
                    wh.month = month;
                    wh.day = day;

                    wh.startHour = start;
                    wh.startMinute = 0;
                    wh.hours = hours;

                    db.workHoursDao().upsert(wh);
                    workHoursByDay.put(day, wh);

                    Toast.makeText(requireContext(),
                            String.format(Locale.getDefault(), "Смена: %02d–%02d", start, end),
                            Toast.LENGTH_SHORT).show();

                    ((CalendarAdapter) calendarGrid.getAdapter()).notifyDataSetChanged();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void resetShiftRange(int day, int year, int month) {
        if (currentEmployeeId == -1) return;
        db.workHoursDao().deleteDay(currentAdminId, currentEmployeeId, year, month, day);
        workHoursByDay.remove(day);
    }

    private void saveSchedule(int year, int month) {
        if (currentSchedule == null) return;

        currentSchedule.adminId = currentAdminId;
        currentSchedule.userId = currentEmployeeId;
        currentSchedule.year = year;
        currentSchedule.month = month;

        currentSchedule.workDays = new ArrayList<>(workDays);
        currentSchedule.vacationDays = new ArrayList<>(vacationDays);
        currentSchedule.cleaningDays = new ArrayList<>(cleaningDays);
        currentSchedule.canteenDutyDays = new ArrayList<>(canteenDutyDays);
        currentSchedule.lunchPrepDutyDays = new ArrayList<>(lunchPrepDutyDays);

        db.scheduleDao().upsert(currentSchedule);
    }
}
