package com.classhub.domain.member.support;

import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchoolNameFormatter {

    public static String format(String schoolName) {
        if (schoolName == null) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        String trimmed = schoolName.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return trimmed.replaceAll("\\s+", " ");
    }
}
