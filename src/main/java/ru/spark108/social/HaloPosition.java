package ru.spark108.social;

/** Six attachment points for text beside a player's outline. */
public enum HaloPosition {
    HEAD_LEFT, HEAD_RIGHT,
    BODY_LEFT, BODY_RIGHT,
    LEGS_LEFT, LEGS_RIGHT;

    public boolean isLeft() {
        return this == HEAD_LEFT || this == BODY_LEFT || this == LEGS_LEFT;
    }

    public String configKey() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
