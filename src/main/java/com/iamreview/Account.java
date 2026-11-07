package com.iamreview;

import java.time.LocalDate;

public record Account(
    String username,
    boolean mfaEnabled,
    LocalDate lastLogin,
    Integer accessKeyAgeDays,
    boolean adminRights,
    boolean employed,
    boolean accountActive,
    boolean consoleAccess,
    boolean accessKeyActive
) {}
