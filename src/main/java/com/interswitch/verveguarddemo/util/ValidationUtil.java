package com.interswitch.verveguarddemo.util;

import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.ConflictException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ValidationUtil {

    public static void checkConflicts(List<Map<String, Object>> results) {
        if (results.stream().anyMatch(row -> Boolean.TRUE.equals(row.get("invalid_role")))) {
            throw new BadRequestException("Invalid role for merchant");
        }

        List<String> conflicts = new ArrayList<>();
        for (Map<String, Object> row : results) {
            if (Boolean.TRUE.equals(row.get("email_exists"))) conflicts.add("Email already in use");
            if (Boolean.TRUE.equals(row.get("phone_exists"))) conflicts.add("Phone already in use");
        }
        if (!conflicts.isEmpty()) throw new ConflictException(String.join(", ", conflicts));
    }
}