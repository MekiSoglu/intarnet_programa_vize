package com.muhasebe.dynamic.validator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL Identifier Validator.
 */
@ApplicationScoped
public class SqlIdentifierValidator {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,62}$");

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "select", "insert", "update", "delete", "drop", "create", "alter", "table",
            "from", "where", "join", "inner", "outer", "left", "right", "on", "as",
            "and", "or", "not", "null", "true", "false", "user", "group", "order", "by",
            "having", "limit", "offset", "into", "values", "set", "primary", "key",
            "foreign", "references", "constraint", "index", "view", "procedure", "function",
            "trigger", "schema", "database", "grant", "revoke", "commit", "rollback",
            "transaction", "begin", "end", "case", "when", "then", "else", "if",
            "exists", "between", "like", "in", "is", "all", "any", "some", "union",
            "intersect", "except", "with", "using", "natural", "cross", "lateral",
            "current_date", "current_time", "current_timestamp", "current_user",
            "session_user", "default", "asymmetric", "symmetric", "placing", "returning"
    );

    private static final Set<String> SYSTEM_TABLES = Set.of(
            "table_map", "dynamic_tables", "view_map", "dynamic_procedures",
            "pg_catalog", "information_schema"
    );

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    public boolean isValid(String identifier) {
        if (identifier == null || identifier.isBlank()) return false;
        if (!IDENTIFIER_PATTERN.matcher(identifier).matches()) return false;
        if (RESERVED_KEYWORDS.contains(identifier.toLowerCase())) return false;
        return true;
    }

    public String validateOrThrow(String identifier, String fieldName) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException(fieldName + " bos olamaz");
        }
        String trimmed = identifier.trim();
        if (!IDENTIFIER_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(
                    fieldName + " gecersiz: '" + identifier + "'. " +
                            "Sadece harf, rakam ve _ kullanin. Harf veya _ ile baslamalidir.");
        }
        if (RESERVED_KEYWORDS.contains(trimmed.toLowerCase())) {
            throw new IllegalArgumentException(
                    fieldName + " bir SQL anahtar kelimesi olamaz: '" + identifier + "'");
        }
        return trimmed.toLowerCase();
    }

    public void rejectSystemTable(String tableName) {
        if (tableName == null) return;
        if (SYSTEM_TABLES.contains(tableName.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Sistem tablosu uzerinde islem yapilamaz: " + tableName);
        }
    }

    public boolean tableExists(String tableName) {
        if (!isValid(tableName)) return false;
        Object result = em.createNativeQuery(
                                  "SELECT EXISTS (SELECT 1 FROM information_schema.tables " +
                                          "WHERE table_schema = 'public' AND table_name = ?1)")
                          .setParameter(1, tableName)
                          .getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    public boolean columnExists(String tableName, String columnName) {
        if (!isValid(tableName) || !isValid(columnName)) return false;
        Object result = em.createNativeQuery(
                                  "SELECT EXISTS (SELECT 1 FROM information_schema.columns " +
                                          "WHERE table_schema = 'public' AND table_name = ?1 AND column_name = ?2)")
                          .setParameter(1, tableName)
                          .setParameter(2, columnName)
                          .getSingleResult();
        return Boolean.TRUE.equals(result);
    }

    public String normalize(String input) {
        if (input == null) return null;
        String normalized = input.trim().toLowerCase()
                                 .replace("ı", "i").replace("İ", "i")
                                 .replace("ğ", "g").replace("Ğ", "g")
                                 .replace("ü", "u").replace("Ü", "u")
                                 .replace("ş", "s").replace("Ş", "s")
                                 .replace("ö", "o").replace("Ö", "o")
                                 .replace("ç", "c").replace("Ç", "c");
        normalized = normalized.replaceAll("[^a-zA-Z0-9_]", "_");
        while (normalized.endsWith("_")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.isEmpty() && Character.isDigit(normalized.charAt(0))) {
            normalized = "t_" + normalized;
        }
        return normalized;
    }
}