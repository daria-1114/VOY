package com.example.voy.data.converter;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;

public class StringListConverter {
    @TypeConverter
    public static String fromList(List<String> list) {
        return list == null ? null : new Gson().toJson(list);
    }

    @TypeConverter
    public static List<String> toList(String value) {
        if (value == null) return new ArrayList<>();
        return new Gson().fromJson(value,
                new TypeToken<List<String>>(){}.getType());
    }
}