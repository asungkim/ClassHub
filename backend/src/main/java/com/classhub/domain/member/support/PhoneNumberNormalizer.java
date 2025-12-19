package com.classhub.domain.member.support;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PhoneNumberNormalizer {

    public static String normalize(String phoneNumber) {
        if (phoneNumber == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }

        String digitsOnly = phoneNumber.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 11) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }

        if (digitsOnly.length() == 10) {
            return format(digitsOnly, 3, 6);
        }
        return format(digitsOnly, 3, 7);
    }

    private static String format(String digits, int middleStart, int middleEnd) {
        return digits.substring(0, 3)
                + "-"
                + digits.substring(3, middleEnd)
                + "-"
                + digits.substring(middleEnd);
    }
}
