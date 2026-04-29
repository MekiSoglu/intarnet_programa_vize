package com.muhasebe.dynamic.procedure;

import com.muhasebe.dynamic.validator.SqlIdentifierValidator;
import com.muhasebe.tablemap.domain.DynamicProcedureEntity;
import com.muhasebe.tablemap.repository.DynamicProcedureRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;
import java.util.logging.Logger;

/**
 * Dinamik Procedure (Stored Procedure) Servisi.
 */
@Stateless
public class DynamicProcedureService {

    private static final Logger log = Logger.getLogger(DynamicProcedureService.class.getName());

    @PersistenceContext(unitName = "muhasebePU")
    private EntityManager em;

    @Inject
    private SqlIdentifierValidator validator;

    @Inject
    private DynamicProcedureRepository procedureRepository;

    //  CREATE PROCEDURE

    @SuppressWarnings("unchecked")
    public void createProcedure(Map<String, Object> jsonData) {
        String rawName = (String) jsonData.get("name");
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Procedure adi bos olamaz");
        }
        String procedureName = validator.normalize(rawName);
        validator.validateOrThrow(procedureName, "Procedure adi");

        Map<String, Object> procData = (Map<String, Object>) jsonData.get("jsonData");
        if (procData == null) {
            throw new IllegalArgumentException("jsonData alani bos olamaz");
        }

        List<Map<String, Object>> inputs = (List<Map<String, Object>>) procData.get("inputs");
        List<Map<String, Object>> columns = (List<Map<String, Object>>) procData.get("columns");

        if (inputs == null) inputs = new ArrayList<>();
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("En az bir column operasyonu tanimlamalisiniz");
        }

        StringJoiner paramList = new StringJoiner(", ");
        Set<String> declaredInputs = new HashSet<>();

        for (Map<String, Object> input : inputs) {
            String inName = validator.normalize((String) input.get("name"));
            validator.validateOrThrow(inName, "Input parametre adi");
            String inType = sanitizeDataType((String) input.get("dataType"));
            paramList.add(inName + " " + inType);
            declaredInputs.add(inName);
        }

        Set<String> affectedTables = new LinkedHashSet<>();
        for (Map<String, Object> col : columns) {
            String tn = validator.normalize((String) col.get("table"));
            validator.validateOrThrow(tn, "Tablo adi");
            affectedTables.add(tn);
        }

        Map<String, String> tableIdParams = new HashMap<>();
        for (String t : affectedTables) {
            String idParam = t + "_id";
            paramList.add(idParam + " INTEGER");
            tableIdParams.put(t, idParam);
        }

        StringBuilder body = new StringBuilder();
        for (Map<String, Object> col : columns) {
            String tableName = validator.normalize((String) col.get("table"));
            String columnName = validator.normalize((String) col.get("column"));
            validator.validateOrThrow(columnName, "Kolon adi");

            String operation = ((String) col.get("operation")).toLowerCase().trim();
            String inputName = (String) col.get("inputName");

            if (inputName == null || inputName.isBlank()) {
                inputName = (String) inputs.get(0).get("name");
            }
            inputName = validator.normalize(inputName);

            String tableIdParam = tableIdParams.get(tableName);
            String op = mapOperation(operation, columnName, inputName);

            body.append("UPDATE ").append(tableName)
                .append(" SET ").append(columnName).append(" = ").append(op)
                .append(" WHERE id = ").append(tableIdParam).append("; ");
        }

        String sql = "CREATE OR REPLACE PROCEDURE " + procedureName +
                "(" + paramList + ") LANGUAGE plpgsql AS $$ " +
                "BEGIN " + body + " END $$;";

        log.info("Olusturulan PROCEDURE: " + sql);
        em.createNativeQuery(sql).executeUpdate();

        Optional<DynamicProcedureEntity> existing = procedureRepository.findByName(procedureName);
        DynamicProcedureEntity entity = existing.orElse(new DynamicProcedureEntity());
        entity.setProcedureName(procedureName);
        entity.setDefinitionJson(toJsonString(jsonData));
        if (existing.isPresent()) {
            procedureRepository.update(entity);
        } else {
            procedureRepository.save(entity);
        }
    }

    private String mapOperation(String op, String column, String input) {
        return switch (op) {
            case "increase", "add", "+"      -> column + " + " + input;
            case "decrease", "subtract", "-" -> column + " - " + input;
            case "multiply", "*"             -> column + " * " + input;
            case "divide", "/"               -> column + " / " + input;
            case "set", "="                  -> input;
            default -> throw new IllegalArgumentException("Desteklenmeyen operasyon: " + op);
        };
    }

    private String sanitizeDataType(String dataType) {
        if (dataType == null) return "INTEGER";
        String t = dataType.toUpperCase().trim();
        Set<String> allowed = Set.of(
                "INTEGER", "BIGINT", "NUMERIC", "DECIMAL",
                "DOUBLE PRECISION", "REAL", "VARCHAR", "TEXT",
                "DATE", "TIMESTAMP", "BOOLEAN", "JSONB"
        );
        if (allowed.contains(t)) return t;
        if (t.matches("NUMERIC\\(\\d+,\\d+\\)|VARCHAR\\(\\d+\\)|DECIMAL\\(\\d+,\\d+\\)")) {
            return t;
        }
        return "INTEGER";
    }

    //  EXECUTE PROCEDURE

    @SuppressWarnings("unchecked")
    public void executeProcedure(String rawProcedureName, Map<String, Object> params) {
        String procedureName = validator.normalize(rawProcedureName);
        validator.validateOrThrow(procedureName, "Procedure adi");

        Optional<DynamicProcedureEntity> opt = procedureRepository.findByName(procedureName);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Procedure bulunamadi: " + procedureName);
        }

        List<Map<String, Object>> inputValues = (List<Map<String, Object>>) params.get("inputValues");
        List<Map<String, Object>> selectedIds = (List<Map<String, Object>>) params.get("selectedIds");

        if (inputValues == null) inputValues = Collections.emptyList();
        if (selectedIds == null) selectedIds = Collections.emptyList();

        // PostgreSQL'den temizlenmiş tip listesini al sql injection riski
        List<String> argTypes = fetchProcedureArgTypes(procedureName);

        List<Object> rawValues = new ArrayList<>();
        for (Map<String, Object> input : inputValues) rawValues.add(input.get("value"));
        for (Map<String, Object> row : selectedIds)   rawValues.add(row.get("id"));

        if (!argTypes.isEmpty() && argTypes.size() != rawValues.size()) {
            throw new IllegalArgumentException(
                    "Procedure " + argTypes.size() + " parametre bekliyor, " + rawValues.size() + " verildi");
        }

        List<Object> paramValues = new ArrayList<>();
        for (int i = 0; i < rawValues.size(); i++) {
            String type = (i < argTypes.size()) ? argTypes.get(i) : null;
            paramValues.add(convertToType(rawValues.get(i), type));
        }

        // Placeholder'lara ::cast ekleyerek tip güvenliğini sağla , db den gelen procedur hangi tipleri istediğini kontorol et
        StringJoiner placeholders = new StringJoiner(", ");
        for (int i = 0; i < paramValues.size(); i++) {
            String castType = (i < argTypes.size()) ? sqlCastFor(argTypes.get(i)) : "";
            placeholders.add("?" + castType);
        }

        // bilinmeyen tipleri önler , procedur tip konusunda çok katı
        String callSql = "CALL " + procedureName + "(" + placeholders + ")";
        log.info("CALL EXECUTING: " + callSql + " WITH PARAMS: " + paramValues);

        Query q = em.createNativeQuery(callSql);
        for (int i = 0; i < paramValues.size(); i++) {
            q.setParameter(i + 1, paramValues.get(i));
        }
        q.executeUpdate();
    }

    private String sqlCastFor(String pgType) {
        if (pgType == null) return "";
        String t = pgType.toLowerCase().trim();
        //  contains ile daha esnek yakalama
        if (t.contains("integer") || t.contains("int4"))     return "::integer";
        if (t.contains("bigint") || t.contains("int8"))       return "::bigint";
        if (t.contains("numeric"))                            return "::numeric";
        if (t.contains("decimal"))                            return "::numeric";
        if (t.contains("real") || t.contains("float4"))       return "::real";
        if (t.contains("double") || t.contains("float8"))     return "::double precision";
        if (t.contains("boolean") || t.contains("bool"))      return "::boolean";
        if (t.contains("date"))                                 return "::date";
        if (t.contains("timestamp"))                            return "::timestamp";
        if (t.contains("character varying") || t.contains("varchar")) return "::varchar";
        if (t.contains("text"))                                 return "::text";
        return "";
    }


    //metadaya sorgu at procedur bilgilerini almak için, procedur un ihtiyacı olduğu paremetreler dinamik olarak alınır .
    //frontente dinamik olarak hazır şekilde kayılı işlemin gelmesi
    private List<String> fetchProcedureArgTypes(String procedureName) {
        List<String> types = new ArrayList<>();
        try {
            Object result = em.createNativeQuery(
                                      "SELECT pg_catalog.pg_get_function_arguments(p.oid) " +
                                              "FROM pg_catalog.pg_proc p " +
                                              "JOIN pg_catalog.pg_namespace n ON n.oid = p.pronamespace " +
                                              "WHERE n.nspname = 'public' AND p.proname = ?1 LIMIT 1")
                              .setParameter(1, procedureName)
                              .getSingleResult();

            if (result == null) return types;
            String argsStr = result.toString().trim();
            if (argsStr.isEmpty()) return types;

            log.info("Raw arguments from DB: " + argsStr);

            for (String part : argsStr.split(",")) {
                String trimmed = part.trim();
                if (trimmed.isEmpty()) continue;

                //  "param_adi TIP" formatından sadece TIP kısmını ayıkla
                int firstSpace = trimmed.indexOf(' ');
                if (firstSpace > 0) {
                    types.add(trimmed.substring(firstSpace + 1).trim().toLowerCase());
                } else {
                    types.add(trimmed.toLowerCase());
                }
            }
            log.info("Parsed types: " + types);
        } catch (Exception e) {
            log.warning("Could not fetch procedure arg types: " + e.getMessage());
        }
        return types;
    }

    private Object convertToType(Object value, String pgType) {
        if (value == null) return null;
        String s = value.toString().trim();
        if (s.isEmpty()) return null;
        if (pgType == null) return s;

        try {
            if (pgType.contains("integer") || pgType.contains("int4")) return Integer.parseInt(s);
            if (pgType.contains("bigint") || pgType.contains("int8")) return Long.parseLong(s);
            if (pgType.contains("numeric") || pgType.contains("decimal") ||
                    pgType.contains("real") || pgType.contains("double")) return new java.math.BigDecimal(s);
            if (pgType.contains("boolean") || pgType.contains("bool")) return Boolean.parseBoolean(s);
            return s;
        } catch (Exception e) {
            return s;
        }
    }

    //metadatan gelen verileri yorumla


    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Map<String, List<String>> getProcedureParameters(String rawProcedureName) {
        String procedureName = validator.normalize(rawProcedureName);
        Map<String, List<String>> result = new LinkedHashMap<>();
        result.put("inputs", new ArrayList<>());
        result.put("tables", new ArrayList<>());

        Optional<DynamicProcedureEntity> opt = procedureRepository.findByName(procedureName);
        if (opt.isEmpty()) return result;

        String defStr = opt.get().getDefinitionJson();
        if (defStr == null || defStr.isBlank()) return result;

        String trimmed = defStr.trim();
        if (trimmed.startsWith("{\"")) {
            parseJsonDefinition(trimmed, result);
        } else {
            parseMapToStringDefinition(trimmed, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void parseJsonDefinition(String json, Map<String, List<String>> result) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Map<String, Object> root = jsonb.fromJson(json, Map.class);
            Map<String, Object> jsonData = (Map<String, Object>) root.get("jsonData");
            if (jsonData == null) return;
            List<Map<String, Object>> inputs = (List<Map<String, Object>>) jsonData.get("inputs");
            if (inputs != null) {
                for (Map<String, Object> in : inputs) result.get("inputs").add(in.get("name").toString());
            }
            List<Map<String, Object>> columns = (List<Map<String, Object>>) jsonData.get("columns");
            if (columns != null) {
                Set<String> uniqueTables = new LinkedHashSet<>();
                for (Map<String, Object> col : columns) uniqueTables.add(col.get("table").toString());
                result.get("tables").addAll(uniqueTables);
            }
        } catch (Exception e) { log.warning("JSON parse error: " + e.getMessage()); }
    }

    private void parseMapToStringDefinition(String s, Map<String, List<String>> result) {
        try {
            String inputsBlock = extractBracketContent(s, "inputs=[");
            if (inputsBlock != null) {
                for (String itemMap : splitTopLevelMaps(inputsBlock)) {
                    String name = extractKeyValue(itemMap, "name");
                    if (name != null) result.get("inputs").add(name);
                }
            }
            String columnsBlock = extractBracketContent(s, "columns=[");
            if (columnsBlock != null) {
                Set<String> uniqueTables = new LinkedHashSet<>();
                for (String itemMap : splitTopLevelMaps(columnsBlock)) {
                    String table = extractKeyValue(itemMap, "table");
                    if (table != null) uniqueTables.add(table);
                }
                result.get("tables").addAll(uniqueTables);
            }
        } catch (Exception e) { log.warning("Map parse error: " + e.getMessage()); }
    }

    private String extractBracketContent(String s, String prefix) {
        int start = s.indexOf(prefix);
        if (start < 0) return null;
        start += prefix.length();
        int depth = 1; int i = start;
        while (i < s.length() && depth > 0) {
            char c = s.charAt(i);
            if (c == '[') depth++; else if (c == ']') depth--;
            if (depth == 0) return s.substring(start, i);
            i++;
        }
        return null;
    }

    private List<String> splitTopLevelMaps(String content) {
        List<String> result = new ArrayList<>();
        int depth = 0; int start = -1;
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') { if (depth == 0) start = i; depth++; }
            else if (c == '}') { depth--; if (depth == 0 && start >= 0) { result.add(content.substring(start, i + 1)); start = -1; } }
        }
        return result;
    }

    private String extractKeyValue(String itemMap, String key) {
        String needle = key + "=";
        int idx = itemMap.indexOf(needle);
        if (idx < 0) return null;
        int valueStart = idx + needle.length();
        int valueEnd = valueStart;
        while (valueEnd < itemMap.length()) {
            char c = itemMap.charAt(valueEnd);
            if (c == ',' || c == '}' || c == ']') break;
            valueEnd++;
        }
        return itemMap.substring(valueStart, valueEnd).trim();
    }

    private String toJsonString(Map<String, Object> data) {
        try (Jsonb jsonb = JsonbBuilder.create()) { return jsonb.toJson(data); }
        catch (Exception e) { return data.toString(); }
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DynamicProcedureEntity> listAll() { return procedureRepository.findAll(); }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<DynamicProcedureEntity> findByName(String rawName) {
        return procedureRepository.findByName(validator.normalize(rawName));
    }

    public void deleteProcedure(String rawName) {
        String procedureName = validator.normalize(rawName);
        em.createNativeQuery("DROP PROCEDURE IF EXISTS " + procedureName).executeUpdate();
        procedureRepository.findByName(procedureName).ifPresent(p -> procedureRepository.delete(p));
    }
}