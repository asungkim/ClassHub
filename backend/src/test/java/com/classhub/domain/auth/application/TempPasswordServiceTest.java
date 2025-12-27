package com.classhub.domain.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.classhub.domain.auth.dto.request.TempPasswordRequest;
import com.classhub.domain.auth.dto.response.TempPasswordResponse;
import com.classhub.domain.auth.support.TempPasswordGenerator;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.RsCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class TempPasswordServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private TempPasswordGenerator tempPasswordGenerator;

    @InjectMocks
    private TempPasswordService tempPasswordService;

    private Member member;
    private UUID memberId;

    @BeforeEach
    void setUp() {
        memberId = UUID.randomUUID();
        member = Member.builder()
                .email("user@classhub.dev")
                .password("encoded")
                .name("User")
                .phoneNumber("010-1234-5678")
                .role(MemberRole.TEACHER)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
    }

    @Test
    void issueTempPassword_shouldReturnTempPassword_whenEmailAndPhoneMatch() {
        TempPasswordRequest request = new TempPasswordRequest("User@classhub.dev", "01012345678");
        given(memberRepository.findByEmailAndPhoneNumber("user@classhub.dev", "010-1234-5678"))
                .willReturn(Optional.of(member));
        given(tempPasswordGenerator.generate()).willReturn("Classmate1234!");
        given(passwordEncoder.encode("Classmate1234!")).willReturn("encoded-temp");

        TempPasswordResponse response = tempPasswordService.issueTempPassword(request);

        assertThat(response.tempPassword()).isEqualTo("Classmate1234!");
        assertThat(member.getPassword()).isEqualTo("encoded-temp");
    }

    @Test
    void issueTempPassword_shouldThrowNotFound_whenEmailOrPhoneMismatch() {
        TempPasswordRequest request = new TempPasswordRequest("user@classhub.dev", "01012345678");
        given(memberRepository.findByEmailAndPhoneNumber("user@classhub.dev", "010-1234-5678"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> tempPasswordService.issueTempPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("rsCode", RsCode.MEMBER_NOT_FOUND);
    }
}
