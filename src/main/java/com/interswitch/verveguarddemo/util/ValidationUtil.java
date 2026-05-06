package com.interswitch.verveguarddemo.util;

import com.interswitch.verveguarddemo.exceptions.ConflictException;

import java.util.List;
import java.util.Map;

public class ValidationUtil {
    public static void checkConflicts(List<Map<String, Object>> results, String email, String phone) {
        for (Map<String, Object> res : results) {
            if (Boolean.TRUE.equals(res.get("email")) && Boolean.TRUE.equals(res.get("phone"))) throw new ConflictException("Email or phone already exists");
            if (Boolean.TRUE.equals(res.get("emailExists"))) throw new ConflictException("Email already in use");
            if (Boolean.TRUE.equals(res.get("phoneExists"))) throw new ConflictException("Phone already in use");
        }
    }
}