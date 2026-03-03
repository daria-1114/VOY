package com.example.voy.data.converter;

import androidx.room.TypeConverter;

import com.example.voy.enums.TripItemType;

public class TripItemTypeConverter {
    @TypeConverter
    public static String fromType(TripItemType type){
        return type == null ? null :type.name();
    }
    @TypeConverter
    public static TripItemType toType(String value) {
        if (value == null) return TripItemType.NOTE;

        try {
            return TripItemType.valueOf(value);
        } catch (IllegalArgumentException e) {
            // compatibility with old DB values
            if ("MEDIA".equals(value)) return TripItemType.PHOTO;  // or choose a default
            return TripItemType.NOTE;
        }
    }
}
