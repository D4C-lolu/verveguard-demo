package com.interswitch.verveguarddemo.models.response;

public record MerchantInfo(
        String firstname,
        String lastname,
        String othername,
        String email,
        String phone
) {
}
