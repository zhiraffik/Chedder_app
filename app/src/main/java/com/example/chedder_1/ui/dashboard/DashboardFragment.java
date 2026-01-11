package com.example.chedder_1.ui.dashboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.chedder_1.R;
import com.example.chedder_1.domain.db.AppDataBase;
import com.example.chedder_1.domain.entity.Schedule;
import com.example.chedder_1.domain.entity.User;
import com.example.chedder_1.domain.entity.WorkHours;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {

    // UI
    private TextView tvMonthTitle;
    private Button btnMonthPrev, btnMonthCurrent, btnMonthNext;

    private TextView tvPosition;
    private TextView tvRatePerHour;
    private TextView tvRatePerShift;
    private TextView tvWorkedShifts;
    private TextView tvEarnedSoFar;
    private TextView tvMonthTotal;

    private Spinner spinnerEmployeeStat;

    private AppDataBase db;
    private SharedPreferences prefs;

    private int currentAdminId = -1;
    private int currentEmployeeId = -1;

    private List<User> staff = new ArrayList<>();

    private static final String PREF_SELECTED_EMPLOYEE_STAT = "selectedEmployeeIdStat";
    private static final String PREF_STAT_YEAR = "statYear";
    private static final String PREF_STAT_MONTH = "statMonth"; // 0..11

    private int statYear;
    private int statMonth;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = AppDataBase.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences("auth", Context.MODE_PRIVATE);

        tvMonthTitle = view.findViewById(R.id.tvMonthTitle);
        btnMonthPrev = view.findViewById(R.id.btnMonthPrev);
        btnMonthCurrent = view.findViewById(R.id.btnMonthCurrent);
        btnMonthNext = view.findViewById(R.id.btnMonthNext);

        tvPosition      = view.findViewById(R.id.tvPosition);
        tvRatePerHour   = view.findViewById(R.id.tvRatePerHour);
        tvRatePerShift  = view.findViewById(R.id.tvRatePerShift);
        tvWorkedShifts  = view.findViewById(R.id.tvWorkedShifts);
        tvEarnedSoFar   = view.findViewById(R.id.tvEarnedSoFar);
        tvMonthTotal    = view.findViewById(R.id.tvMonthTotal);

        spinnerEmployeeStat = view.findViewById(R.id.spinnerEmployeeStats);

        if (!resolveSession()) {
            Toast.makeText(requireContext(), "Нет авторизации", Toast.LENGTH_SHORT).show();
            return view;
        }

        initMonthFromPrefs();
        setupMonthNav();

        if (isAdmin()) {
            setupEmployeeSelectorForStats();
        } else {
            if (spinnerEmployeeStat != null) spinnerEmployeeStat.setVisibility(View.GONE);
            fillStatisticsFor(currentEmployeeId, statYear, statMonth);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getContext() == null) return;

        if (!resolveSession()) return;

        if (isAdmin()) {
            setupEmployeeSelectorForStats();
        } else {
            fillStatisticsFor(currentEmployeeId, statYear, statMonth);
        }
    }

    private boolean isAdmin() {
        return "admin".equals(prefs.getString("role", null));
    }

    private boolean resolveSession() {
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

    // ===== МЕСЯЦ СТАТИСТИКИ =====

    private void initMonthFromPrefs() {
        Calendar now = Calendar.getInstance();
        int defYear = now.get(Calendar.YEAR);
        int defMonth = now.get(Calendar.MONTH);

        statYear = prefs.getInt(PREF_STAT_YEAR, defYear);
        statMonth = prefs.getInt(PREF_STAT_MONTH, defMonth);

        updateMonthTitle();
    }

    private void setupMonthNav() {
        if (btnMonthPrev != null) btnMonthPrev.setOnClickListener(v -> shiftStatMonth(-1));
        if (btnMonthNext != null) btnMonthNext.setOnClickListener(v -> shiftStatMonth(+1));

        if (btnMonthCurrent != null) {
            btnMonthCurrent.setOnClickListener(v -> {
                Calendar now = Calendar.getInstance();
                statYear = now.get(Calendar.YEAR);
                statMonth = now.get(Calendar.MONTH);
                persistStatMonth();
                refreshStats();
            });
        }
    }

    private void shiftStatMonth(int deltaMonths) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, statYear);
        c.set(Calendar.MONTH, statMonth);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.add(Calendar.MONTH, deltaMonths);

        statYear = c.get(Calendar.YEAR);
        statMonth = c.get(Calendar.MONTH);

        persistStatMonth();
        refreshStats();
    }

    private void persistStatMonth() {
        prefs.edit()
                .putInt(PREF_STAT_YEAR, statYear)
                .putInt(PREF_STAT_MONTH, statMonth)
                .apply();
    }

    private void updateMonthTitle() {
        if (tvMonthTitle == null) return;

        String[] months = new DateFormatSymbols(new Locale("ru")).getMonths();
        String m = (statMonth >= 0 && statMonth < months.length) ? months[statMonth] : "";
        if (m == null) m = "";
        if (!m.isEmpty()) m = m.substring(0, 1).toUpperCase() + m.substring(1);

        tvMonthTitle.setText(m + " " + statYear);
    }

    private void refreshStats() {
        updateMonthTitle();
        if (currentEmployeeId != -1) {
            fillStatisticsFor(currentEmployeeId, statYear, statMonth);
        }
    }

    // ===== СПИННЕР СОТРУДНИКА =====

    private void setupEmployeeSelectorForStats() {
        if (spinnerEmployeeStat == null) return;

        spinnerEmployeeStat.setVisibility(View.VISIBLE);
        spinnerEmployeeStat.setOnItemSelectedListener(null);

        staff = db.userDao().getEmployeesForAdmin(currentAdminId);
        if (staff == null || staff.isEmpty()) {
            spinnerEmployeeStat.setAdapter(null);
            tvPosition.setText("Должность: -");
            tvRatePerHour.setText("Ставка в час: -");
            tvRatePerShift.setText("Ставка за смену: -");
            tvWorkedShifts.setText("Отработано дней: -");
            tvEarnedSoFar.setText("Заработано: -");
            tvMonthTotal.setText("Часов за месяц: -");
            return;
        }

        List<String> labels = new ArrayList<>();
        for (User u : staff) {
            String name = (u.lastName == null ? "" : u.lastName + " ")
                    + (u.firstName == null ? "" : u.firstName);
            String pos = (u.position == null ? "" : " (" + u.position + ")");
            labels.add(name.trim() + pos);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                labels
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployeeStat.setAdapter(adapter);

        int savedId = prefs.getInt(PREF_SELECTED_EMPLOYEE_STAT, -1);
        int initialIndex = 0;
        if (savedId != -1) {
            for (int i = 0; i < staff.size(); i++) {
                if (staff.get(i).id == savedId) {
                    initialIndex = i;
                    break;
                }
            }
        }

        spinnerEmployeeStat.setSelection(initialIndex);
        currentEmployeeId = staff.get(initialIndex).id;
        fillStatisticsFor(currentEmployeeId, statYear, statMonth);

        spinnerEmployeeStat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentEmployeeId = staff.get(position).id;
                prefs.edit().putInt(PREF_SELECTED_EMPLOYEE_STAT, currentEmployeeId).apply();
                fillStatisticsFor(currentEmployeeId, statYear, statMonth);
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    // ===== СТАТИСТИКА =====

    private void fillStatisticsFor(int employeeId, int year, int month) {
        if (employeeId <= 0) return;

        User user = db.userDao().getById(employeeId);
        if (user == null) return;

        String position = user.position == null ? "-" : user.position;
        double ratePerHour = user.ratePerHour;
        int hoursPerShift = user.hoursPerShift > 0 ? user.hoursPerShift : 13;

        tvPosition.setText("Должность: " + position);
        tvRatePerHour.setText(String.format(Locale.getDefault(), "Ставка в час: %.0f ₽", ratePerHour));
        tvRatePerShift.setText(String.format(Locale.getDefault(), "Ставка за смену: %.0f ₽", ratePerHour * hoursPerShift));

        Schedule s = db.scheduleDao().getSchedule(currentAdminId, employeeId, year, month);

        Set<Integer> work = new HashSet<>();
        Set<Integer> canteen = new HashSet<>();
        Set<Integer> lunch = new HashSet<>();
        Set<Integer> cleaning = new HashSet<>();
        Set<Integer> vacation = new HashSet<>();

        if (s != null) {
            if (s.workDays != null) work.addAll(s.workDays);
            if (s.canteenDutyDays != null) canteen.addAll(s.canteenDutyDays);
            if (s.lunchPrepDutyDays != null) lunch.addAll(s.lunchPrepDutyDays);
            if (s.cleaningDays != null) cleaning.addAll(s.cleaningDays);
            if (s.vacationDays != null) vacation.addAll(s.vacationDays);
        }

        Set<Integer> paidWorkDays = new HashSet<>();
        paidWorkDays.addAll(work);
        paidWorkDays.addAll(canteen);
        paidWorkDays.addAll(lunch);
        paidWorkDays.removeAll(vacation);

        List<WorkHours> rows = db.workHoursDao().getMonth(currentAdminId, employeeId, year, month);
        Map<Integer, WorkHours> whByDay = new HashMap<>();
        if (rows != null) {
            for (WorkHours wh : rows) whByDay.put(wh.day, wh);
        }

        int hoursMonth = 0;

        for (int d : paidWorkDays) {
            WorkHours wh = whByDay.get(d);
            int baseHours = (wh != null && wh.hours > 0) ? wh.hours : hoursPerShift;
            hoursMonth += baseHours;
        }

        int cleaningHours = 0;
        for (int d : cleaning) {
            if (vacation.contains(d)) continue;
            cleaningHours += 1;
        }

        int totalHoursMonth = hoursMonth + cleaningHours;
        double monthTotal = totalHoursMonth * ratePerHour;

        Set<Integer> workedAnyDays = new HashSet<>(paidWorkDays);
        workedAnyDays.addAll(cleaning);
        workedAnyDays.removeAll(vacation);

        tvWorkedShifts.setText(String.format(Locale.getDefault(), "Отработано дней: %d", workedAnyDays.size()));
        tvEarnedSoFar.setText(String.format(Locale.getDefault(), "Заработано за месяц: %.0f ₽", monthTotal));
        tvMonthTotal.setText(String.format(Locale.getDefault(), "Часов за месяц: %d (уборка %d ч)", totalHoursMonth, cleaningHours));
    }
}
