package com.iamreview;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class AccountLoader {
    private static final List<String> HEADERS = List.of("username", "mfa_enabled", "last_login", "access_key_age_days",
        "admin_rights", "employed", "account_active", "console_access", "access_key_active");
    private AccountLoader() {}

    public static List<Account> load(Path input) throws IOException {
        List<List<String>> rows = Csv.read(input);
        if (rows.isEmpty()) throw new IllegalArgumentException("CSV is empty");
        Map<String,Integer> cols = new HashMap<>();
        for (int i = 0; i < rows.get(0).size(); i++) {
            String name = rows.get(0).get(i).trim().toLowerCase(Locale.ROOT);
            if (cols.put(name, i) != null) throw new IllegalArgumentException("Duplicate column: " + name);
        }
        for (String h : HEADERS) if (!cols.containsKey(h)) throw new IllegalArgumentException("Missing required column: " + h);
        List<Account> accounts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            int line = r + 1;
            if (row.size() != rows.get(0).size()) throw new IllegalArgumentException("CSV row " + line + " has " + row.size() + " columns; expected " + rows.get(0).size());
            try {
                String username = get(row, cols, "username");
                if (username.isEmpty()) throw new IllegalArgumentException("username is blank");
                if (!seen.add(username.toLowerCase(Locale.ROOT))) throw new IllegalArgumentException("duplicate username: " + username);
                accounts.add(new Account(username,
                    bool(get(row, cols, "mfa_enabled"), "mfa_enabled"),
                    date(get(row, cols, "last_login")),
                    age(get(row, cols, "access_key_age_days")),
                    bool(get(row, cols, "admin_rights"), "admin_rights"),
                    bool(get(row, cols, "employed"), "employed"),
                    bool(get(row, cols, "account_active"), "account_active"),
                    bool(get(row, cols, "console_access"), "console_access"),
                    bool(get(row, cols, "access_key_active"), "access_key_active")));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("CSV row " + line + ": " + e.getMessage(), e);
            }
        }
        return accounts;
    }

    private static String get(List<String> row, Map<String,Integer> cols, String key) { return row.get(cols.get(key)).trim(); }
    private static boolean bool(String value, String field) {
        if (value.equalsIgnoreCase("true")) return true;
        if (value.equalsIgnoreCase("false")) return false;
        throw new IllegalArgumentException(field + " must be true or false (was '" + value + "')");
    }
    private static LocalDate date(String value) {
        if (value.isEmpty() || value.equalsIgnoreCase("NEVER") || value.equalsIgnoreCase("N/A")) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw new IllegalArgumentException("last_login must be YYYY-MM-DD, NEVER or blank"); }
    }
    private static Integer age(String value) {
        if (value.isEmpty() || value.equalsIgnoreCase("N/A")) return null;
        try {
            int age = Integer.parseInt(value);
            if (age < 0) throw new IllegalArgumentException("access_key_age_days cannot be negative");
            return age;
        } catch (NumberFormatException e) { throw new IllegalArgumentException("access_key_age_days must be a nonnegative integer or N/A"); }
    }
}
