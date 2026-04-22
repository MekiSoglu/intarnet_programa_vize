package com.muhasebe.dynamic.validator;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Kullanicinin sectigi mantiksal tipleri PostgreSQL tiplerine cevirir.
 *
 * BUG FIX: Eski projede 'decimal' yoktu ve para hesaplari INTEGER ile yapiliyordu (felaket).
 *          NUMERIC(19,4) muhasebe icin standart - 4 ondalik basamak.
 */
@ApplicationScoped
public class ColumnTypeMapper {

    public String toSqlType(String logicalType) {
        if (logicalType == null) {
            throw new IllegalArgumentException("Kolon tipi belirtilmedi");
        }
        return switch (logicalType.toLowerCase().trim()) {
            case "number", "integer", "int" -> "INTEGER";
            case "bigint", "long"           -> "BIGINT";
            case "decimal", "money", "para" -> "NUMERIC(19,4)";   // YENI - muhasebe icin
            case "double", "float"          -> "DOUBLE PRECISION";
            case "varchar", "string", "text"-> "VARCHAR(255)";
            case "longtext"                 -> "TEXT";
            case "date"                     -> "DATE";
            case "datetime", "timestamp"    -> "TIMESTAMP";
            case "boolean", "bool"          -> "BOOLEAN";
            case "json", "jsonb"            -> "JSONB";
            case "enum"                     -> "VARCHAR(50)";
            default -> throw new IllegalArgumentException(
                    "Desteklenmeyen kolon tipi: " + logicalType +
                    ". Desteklenen tipler: number, decimal, varchar, longtext, date, datetime, boolean, json, enum"
            );
        };
    }
}
