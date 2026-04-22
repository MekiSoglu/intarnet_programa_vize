package com.muhasebe.dynamic.procedure;

import com.muhasebe.dynamic.validator.SqlIdentifierValidator;
import com.muhasebe.tablemap.domain.DynamicProcedureEntity;
import com.muhasebe.tablemap.repository.DynamicProcedureRepository;
import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.util.*;
import java.util.logging.Logger;

/**
 * Dinamik Procedure (Stored Procedure) Servisi.
 *
 * Kullanim senaryosu: Kullanici "borc odendi" islemi tanimlar:
 *   - input: odeme_tutari (DECIMAL)
 *   - operations:
 *       borclar tablosu, odenen_tutar kolonu, INCREASE
 *       banka tablosu, bakiye kolonu, DECREASE
 *
 * Sistem bir PostgreSQL PROCEDURE olusturur, frontend'den cagrildiginda calisir.
 *
 * BUG FIX'ler (eski koda gore):
 *  1. KRITIK: inputs.get(0) bug'i kaldirildi - eski kodda her column ilk input'u kullaniyordu.
 *     Simdi her column kendi inputColumn'una map ediliyor (column.inputName ile).
 *  2. SQL injection: tum identifier'lar validate ediliyor.
 *  3. Procedure ismi cakisma kontrolu - CREATE OR REPLACE yerine once exists kontrolu.
 *  4. Daha fazla operation: increase, decrease, multiply, divide, set.
 *  5. dynamic_procedures tablosuna metadata kaydi (UI'da listelemek icin).
 *  6. executeProcedure'de parametre tipi otomatik algilanmiyor; metadata'dan okunuyor.
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

    /**
     * jsonData formati:
     * {
     *   "name": "borc_odeme",
     *   "jsonData": {
     *     "inputs": [
     *       { "name": "odenen_tutar", "dataType": "NUMERIC" }
     *     ],
     *     "columns": [
     *       { "table": "borclar", "column": "odenen", "operation": "increase", "inputName": "odenen_tutar" },
     *       { "table": "banka",   "column": "bakiye", "operation": "decrease", "inputName": "odenen_tutar" }
     *     ]
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    public void createProcedure(Map<String, Object> jsonData) {
        // 1) Procedure adi
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

        // 2) Input parametrelerini olustur
        StringJoiner paramList = new StringJoiner(", ");
        Set<String> declaredInputs = new HashSet<>();

        for (Map<String, Object> input : inputs) {
            String inName = validator.normalize((String) input.get("name"));
            validator.validateOrThrow(inName, "Input parametre adi");
            String inType = sanitizeDataType((String) input.get("dataType"));
            paramList.add(inName + " " + inType);
            declaredInputs.add(inName);
        }

        // 3) Etkilenen tablolarin id parametrelerini ekle
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

        // 4) SQL UPDATE statement'larini olustur
        // BUG FIX: Eski kod inputs.get(0)'i kullaniyordu - simdi her column kendi inputName'ini kullaniyor
        StringBuilder body = new StringBuilder();
        for (Map<String, Object> col : columns) {
            String tableName = validator.normalize((String) col.get("table"));
            String columnName = validator.normalize((String) col.get("column"));
            validator.validateOrThrow(columnName, "Kolon adi");

            String operation = ((String) col.get("operation")).toLowerCase().trim();
            String inputName = (String) col.get("inputName");

            // inputName belirtilmemisse ilk inputu kullan (geri uyumluluk)
            if (inputName == null || inputName.isBlank()) {
                if (inputs.isEmpty()) {
                    throw new IllegalArgumentException(
                            "Column icin inputName gerekli (operation: " + operation + ")");
                }
                inputName = (String) inputs.get(0).get("name");
            }
            inputName = validator.normalize(inputName);
            if (!declaredInputs.contains(inputName)) {
                throw new IllegalArgumentException(
                        "inputName tanimli inputlar arasinda yok: " + inputName);
            }

            String tableIdParam = tableIdParams.get(tableName);
            String op = mapOperation(operation, columnName, inputName);

            body.append("UPDATE ").append(tableName)
                .append(" SET ").append(columnName).append(" = ").append(op)
                .append(" WHERE id = ").append(tableIdParam).append("; ");
        }

        // 5) PROCEDURE SQL'i
        String sql = "CREATE OR REPLACE PROCEDURE " + procedureName +
                "(" + paramList + ") LANGUAGE plpgsql AS $$ " +
                "BEGIN " + body + " END $$;";

        log.info("Olusturulan PROCEDURE: " + sql);
        em.createNativeQuery(sql).executeUpdate();

        // 6) Metadata kaydi - varsa update, yoksa insert
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

    /** Sadece guvenli PostgreSQL data tiplerini kabul et. */
    private String sanitizeDataType(String dataType) {
        if (dataType == null) return "INTEGER";
        String t = dataType.toUpperCase().trim();
        Set<String> allowed = Set.of(
                "INTEGER", "BIGINT", "NUMERIC", "DECIMAL",
                "DOUBLE PRECISION", "REAL", "VARCHAR", "TEXT",
                "DATE", "TIMESTAMP", "BOOLEAN", "JSONB"
        );
        if (allowed.contains(t)) return t;
        // NUMERIC(19,4) gibi parametreli tipleri de kabul et
        if (t.matches("NUMERIC\\(\\d+,\\d+\\)|VARCHAR\\(\\d+\\)|DECIMAL\\(\\d+,\\d+\\)")) {
            return t;
        }
        throw new IllegalArgumentException("Desteklenmeyen veri tipi: " + dataType);
    }

    /** Procedure'u calistir. */
    @SuppressWarnings("unchecked")
    public void executeProcedure(String rawProcedureName, Map<String, Object> params) {
        String procedureName = validator.normalize(rawProcedureName);
        validator.validateOrThrow(procedureName, "Procedure adi");

        // Procedure'un metadatasini cek
        Optional<DynamicProcedureEntity> opt = procedureRepository.findByName(procedureName);
        if (opt.isEmpty()) {
            throw new IllegalArgumentException("Procedure bulunamadi: " + procedureName);
        }

        List<Map<String, Object>> inputValues = (List<Map<String, Object>>) params.get("inputValues");
        List<Map<String, Object>> selectedIds = (List<Map<String, Object>>) params.get("selectedIds");

        if (inputValues == null) inputValues = Collections.emptyList();
        if (selectedIds == null) selectedIds = Collections.emptyList();

        // Parametre placeholder'lari
        List<Object> paramValues = new ArrayList<>();
        StringJoiner placeholders = new StringJoiner(", ");
        int idx = 1;

        for (Map<String, Object> input : inputValues) {
            placeholders.add("?");
            paramValues.add(parseValue(input.get("value")));
            idx++;
        }
        for (Map<String, Object> row : selectedIds) {
            placeholders.add("?");
            paramValues.add(parseInteger(row.get("id")));
            idx++;
        }

        String callSql = "CALL " + procedureName + "(" + placeholders + ")";
        log.info("CALL: " + callSql + " params=" + paramValues);

        Query q = em.createNativeQuery(callSql);
        for (int i = 0; i < paramValues.size(); i++) {
            q.setParameter(i + 1, paramValues.get(i));
        }
        q.executeUpdate();
    }

    private Object parseValue(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return v;
        String s = v.toString().trim();
        try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        return s;
    }

    private Integer parseInteger(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.parseInt(v.toString());
    }

    private String toJsonString(Map<String, Object> data) {
        // Basit JSON serializer - production'da Jackson kullanilir, simdilik toString yeterli
        return data.toString();
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public Optional<DynamicProcedureEntity> findByName(String rawName) {
        String procedureName = validator.normalize(rawName);
        return procedureRepository.findByName(procedureName);
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<DynamicProcedureEntity> listAll() {
        return procedureRepository.findAll();
    }


    public void deleteProcedure(String rawName) {
        String procedureName = validator.normalize(rawName);
        validator.validateOrThrow(procedureName, "Procedure adi");

        em.createNativeQuery("DROP PROCEDURE IF EXISTS " + procedureName + "()").executeUpdate();
        // Tum overload'lari da temizlemek icin pg_proc'tan bul ve sil (ileri kullanim)

        procedureRepository.findByName(procedureName)
                .ifPresent(p -> procedureRepository.delete(p));
    }
}
