package com.classhub.domain.auth.application;

import com.classhub.domain.auth.dto.request.TempPasswordRequest;
import com.classhub.domain.auth.dto.response.TempPasswordResponse;
import com.classhub.domain.auth.support.TempPasswordGenerator;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TempPasswordService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TempPasswordGenerator tempPasswordGenerator;

    @Transactional
    public TempPasswordResponse issueTempPassword(TempPasswordRequest request) {
        String normalizedEmail = request.normalizedEmail();
        String normalizedPhone = request.normalizedPhoneNumber();
        Member member = memberRepository.findByEmailAndPhoneNumber(normalizedEmail, normalizedPhone)
                .orElseThrow(() -> new BusinessException(RsCode.MEMBER_NOT_FOUND));

        if (member.isDeleted()) {
            throw new BusinessException(RsCode.MEMBER_INACTIVE);
        }

        String tempPassword = tempPasswordGenerator.generate();
        member.changePassword(passwordEncoder.encode(tempPassword));
        return new TempPasswordResponse(tempPassword);
    }
}
