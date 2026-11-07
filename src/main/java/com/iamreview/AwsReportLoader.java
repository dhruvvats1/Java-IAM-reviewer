package com.iamreview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/** Imports an exported AWS IAM credential-report CSV without AWS credentials or SDK. */
public final class AwsReportLoader {
    private AwsReportLoader() {}

    public static List<Account> load(Path report, Path employeeRoster, Path adminRoster, LocalDate auditDate) throws IOException {
        Map<String, Boolean> employeeStatus = employeeRoster == null ? Map.of() : roster(employeeRoster, "employed");
        Map<String, Boolean> adminStatus = adminRoster == null ? Map.of() : roster(adminRoster, "admin_rights");
        List<List<String>> rows = Csv.read(report);
        if (rows.isEmpty()) throw new IllegalArgumentException("AWS credential report is empty");
        Map<String, Integer> columns = columnMap(rows.get(0));
        for (String h : List.of("user", "password_enabled", "password_last_used", "mfa_active", "access_key_1_active", "access_key_1_last_rotated", "access_key_2_active", "access_key_2_last_rotated"))
            if (!columns.containsKey(h)) throw new IllegalArgumentException("AWS credential report missing column: " + h);
        List<Account> accounts = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            if (row.size() != rows.get(0).size()) throw new IllegalArgumentException("Invalid column count at AWS report row " + (r+1));
            String user = field(row, columns, "user");
            if (user.equals("<root_account>") || user.equals("root_account") || user.equals("<root>")) continue; // user-scoped analysis only
            if (employeeRoster != null && !employeeStatus.containsKey(user)) throw new IllegalArgumentException("Employee roster is missing IAM user: " + user);
            if (adminRoster != null && !adminStatus.containsKey(user)) throw new IllegalArgumentException("Admin roster is missing IAM user: " + user);
            boolean console = truth(field(row, columns, "password_enabled"));
            boolean mfa = truth(field(row, columns, "mfa_active"));
            boolean key1 = truth(field(row, columns, "access_key_1_active"));
            boolean key2 = truth(field(row, columns, "access_key_2_active"));
            LocalDate mostRecent = console ? timestamp(field(row, columns, "password_last_used")) : null;
            for (int k = 1; k <= 2; k++) {
                if ((k == 1 && key1) || (k == 2 && key2)) {
                    LocalDate keyUsed = timestamp(field(row, columns, "access_key_" + k + "_last_used_date"));
                    if (keyUsed != null && (mostRecent == null || keyUsed.isAfter(mostRecent))) mostRecent = keyUsed;
                }
            }
            Integer oldestKeyAge = null;
            for (int k = 1; k <= 2; k++) {
                if ((k == 1 && key1) || (k == 2 && key2)) {
                    LocalDate rotated = timestamp(field(row, columns, "access_key_" + k + "_last_rotated"));
                    if (rotated != null) {
                        int age = (int) java.time.temporal.ChronoUnit.DAYS.between(rotated, auditDate);
                        if (age < 0) throw new IllegalArgumentException("Future key rotation date for: " + user);
                        if (oldestKeyAge == null || age > oldestKeyAge) oldestKeyAge = age;
                    }
                }
            }
            accounts.add(new Account(user, mfa, mostRecent, oldestKeyAge, adminRoster != null && adminStatus.get(user), employeeRoster == null || employeeStatus.get(user),
                console || key1 || key2, console, key1 || key2));
        }
        return accounts;
    }

    private static Map<String, Boolean> roster(Path input, String boolColumn) throws IOException {
        List<List<String>> rows = Csv.read(input);
        if (rows.isEmpty()) throw new IllegalArgumentException("Empty roster: " + input);
        Map<String,Integer> cols = columnMap(rows.get(0));
        if (!cols.containsKey("username") || !cols.containsKey(boolColumn))
            throw new IllegalArgumentException("Roster needs username," + boolColumn + ": " + input);
        Map<String,Boolean> map = new HashMap<>();
        for (int i=1;i<rows.size();i++) {
            List<String> row=rows.get(i);
            if (row.size()!=rows.get(0).size()) throw new IllegalArgumentException("Invalid roster row " + (i+1));
            String user=field(row,cols,"username");
            String value=field(row,cols,boolColumn);
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
                throw new IllegalArgumentException("Invalid " + boolColumn + " for " + user);
            if (map.putIfAbsent(user, Boolean.parseBoolean(value)) != null)
                throw new IllegalArgumentException("Duplicate roster username: " + user);
        }
        return map;
    }
    private static Map<String,Integer> columnMap(List<String> row) {
        Map<String,Integer> cols = new HashMap<>();
        for(int i=0;i<row.size();i++) cols.put(row.get(i).trim().toLowerCase(Locale.ROOT),i);
        return cols;
    }
    private static String field(List<String> row,Map<String,Integer> cols,String name) {
        Integer index=cols.get(name);
        return index==null?"":row.get(index).trim();
    }
    private static boolean truth(String s) { return s.equalsIgnoreCase("true"); }
    private static LocalDate timestamp(String value) {
        if (value.isBlank() || value.equalsIgnoreCase("N/A") || value.equalsIgnoreCase("no_information") || value.equalsIgnoreCase("not_supported")) return null;
        if (value.length() < 10) throw new IllegalArgumentException("Invalid AWS timestamp: " + value);
        try { return LocalDate.parse(value.substring(0,10)); }
        catch (Exception e) { throw new IllegalArgumentException("Invalid AWS timestamp: " + value); }
    }
}
