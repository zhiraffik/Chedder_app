package com.example.chedder_1.domain.db;

import androidx.room.TypeConverter;

import java.util.ArrayList;
import java.util.List;

public class Converters {

    @TypeConverter
    public static String fromList(List<Integer> list) {
        if (list == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i : list) {
            sb.append(i).append(",");
        }
        return sb.toString();
    }

    @TypeConverter
    public static List<Integer> toList(String data) {
        List<Integer> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String s : data.split(",")) {
            if (!s.isEmpty()) {
                list.add(Integer.parseInt(s));
            }
        }
        return list;
    }
}
