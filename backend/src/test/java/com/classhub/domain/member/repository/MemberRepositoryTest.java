package com.classhub.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.global.config.JpaConfig;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findByEmail_shouldReturnMember_whenExists() {
        Member member = memberRepository.save(
                Member.builder()
                        .email("teacher@classhub.com")
                        .password("encoded")
                        .name("Teacher Kim")
                        .phoneNumber("01012345678")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        Optional<Member> result = memberRepository.findByEmail("teacher@classhub.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(member.getId());
        assertThat(result.get().getCreatedAt()).isNotNull();
        assertThat(result.get().getUpdatedAt()).isNotNull();
        assertThat(result.get().getDeletedAt()).isNull();
    }

    @Test
    void findByEmail_shouldReturnEmpty_whenMemberDoesNotExist() {
        Optional<Member> result = memberRepository.findByEmail("unknown@classhub.com");

        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_shouldValidateExistence_viaPresence() {
        memberRepository.save(
                Member.builder()
                        .email("assistant@classhub.com")
                        .password("encoded")
                        .name("Assistant Lee")
                        .phoneNumber("01099998888")
                        .role(MemberRole.ASSISTANT)
                        .build()
        );

        assertThat(memberRepository.findByEmail("assistant@classhub.com")).isPresent();
        assertThat(memberRepository.findByEmail("new@classhub.com")).isEmpty();
    }

    @Test
    void softDelete_shouldPersistDeletedAt() {
        Member member = memberRepository.save(
                Member.builder()
                        .email("student@classhub.com")
                        .password("encoded")
                        .name("Student Choi")
                        .phoneNumber("01022223333")
                        .role(MemberRole.STUDENT)
                        .build()
        );

        member.deactivate();
        memberRepository.save(member);

        assertThat(memberRepository.findByEmail("student@classhub.com"))
                .isPresent()
                .get()
                .extracting(Member::getDeletedAt)
                .isNotNull();
    }

    @Test
    void existsByEmail_shouldReflectCurrentState() {
        memberRepository.save(
                Member.builder()
                        .email("duplicate@classhub.com")
                        .password("encoded")
                        .name("Duplicate")
                        .phoneNumber("01000000000")
                        .role(MemberRole.TEACHER)
                        .build()
        );

        assertThat(memberRepository.existsByEmail("duplicate@classhub.com")).isTrue();
        assertThat(memberRepository.existsByEmail("new@classhub.com")).isFalse();
    }

    @Test
    void findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc_shouldReturnSortedLimitedResults() {
        for (int i = 0; i < 7; i++) {
            memberRepository.save(
                    Member.builder()
                            .email("assistant" + i + "@classhub.com")
                            .password("encoded")
                            .name("Assistant " + i)
                            .phoneNumber("0101000" + i)
                            .role(MemberRole.ASSISTANT)
                            .build()
            );
        }

        var results = memberRepository
                .findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc(
                        MemberRole.ASSISTANT,
                        "assistant"
                );

        assertThat(results).hasSize(5);
        assertThat(results)
                .isSortedAccordingTo((a, b) -> a.getEmail().compareToIgnoreCase(b.getEmail()));
    }

    @Test
    void findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc_shouldExcludeDeletedMembers() {
        Member active = memberRepository.save(
                Member.builder()
                        .email("active@classhub.com")
                        .password("encoded")
                        .name("Active Assistant")
                        .phoneNumber("01099998888")
                        .role(MemberRole.ASSISTANT)
                        .build()
        );
        Member deleted = memberRepository.save(
                Member.builder()
                        .email("deleted@classhub.com")
                        .password("encoded")
                        .name("Deleted Assistant")
                        .phoneNumber("01077776666")
                        .role(MemberRole.ASSISTANT)
                        .build()
        );
        deleted.deactivate();
        memberRepository.save(deleted);

        var results = memberRepository
                .findTop5ByRoleAndDeletedAtIsNullAndEmailContainingIgnoreCaseOrderByEmailAsc(
                        MemberRole.ASSISTANT,
                        "classhub"
                );

        assertThat(results)
                .extracting(Member::getEmail)
                .containsExactly(active.getEmail());
    }
}
