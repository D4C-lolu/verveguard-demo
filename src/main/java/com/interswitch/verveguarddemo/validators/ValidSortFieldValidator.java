package com.interswitch.verveguarddemo.validators;

import com.interswitch.verveguarddemo.annotation.ValidSortField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ValidSortFieldValidator implements ConstraintValidator<ValidSortField, String> {

    private Set<String> allowedFields;

    @Override
    public void initialize(ValidSortField annotation) {
        allowedFields = new HashSet<>();
        Class<?> clazz = annotation.target();

        while (clazz != null && clazz != Object.class) {
            Arrays.stream(clazz.getDeclaredFields())
                    .map(Field::getName)
                    .forEach(allowedFields::add);
            clazz = clazz.getSuperclass();
        }
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        if (allowedFields.contains(value)) return true;

        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
                String.format("'%s' is not a valid sort field.", value)
        ).addConstraintViolation();

        return false;
    }
}
