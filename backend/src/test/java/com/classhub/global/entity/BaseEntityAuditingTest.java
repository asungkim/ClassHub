package com.classhub.global.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class BaseEntityAuditingTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void auditingColumnsAreFilledAutomatically() {
        Member saved = memberRepository.save(
                Member.builder()
                        .email("audit@classhub.com")
                        .password("encoded")
                        .name("Audit User")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
