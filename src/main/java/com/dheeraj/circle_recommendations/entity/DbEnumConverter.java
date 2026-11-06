package com.dheeraj.circle_recommendations.entity;

import jakarta.persistence.AttributeConverter;

/**
 * Maps a {@link DbEnum} to its database string and back.
 *
 * <p>Used instead of {@code @Enumerated}: ORDINAL would store positions (reordering the enum
 * would silently change meaning), and STRING would require the constant names to match the
 * lowercase database values exactly.
 */
public abstract class DbEnumConverter<E extends Enum<E> & DbEnum> implements AttributeConverter<E, String> {

    private final Class<E> type;

    protected DbEnumConverter(Class<E> type) {
        this.type = type;
    }

    @Override
    public String convertToDatabaseColumn(E value) {
        return value == null ? null : value.dbValue();
    }

    @Override
    public E convertToEntityAttribute(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        for (E constant : type.getEnumConstants()) {
            if (constant.dbValue().equals(dbValue)) {
                return constant;
            }
        }
        throw new IllegalArgumentException("Unknown " + type.getSimpleName() + " value: " + dbValue);
    }
}
