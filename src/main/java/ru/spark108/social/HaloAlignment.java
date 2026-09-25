package ru.spark108.social;

import java.util.Locale;

public enum HaloAlignment {
    CONTOUR, CENTER, LEFT, RIGHT;

    static HaloAlignment parse(String value) {
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CONTOUR;
        }
    }
}
