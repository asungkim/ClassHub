package com.classhub.domain.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void saveMember() {
        Member member = Member.builder()
                .email("teacher@classhub.com")
                .password("encoded")
                .name("Teacher Kim")
                .role(MemberRole.TEACHER)
                .build();

        Member saved = memberRepository.save(member);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("teacher@classhub.com");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getRole()).isEqualTo(MemberRole.TEACHER);
    }

    @Test
    void uniqueEmailConstraint() {
        Member teacher = Member.builder()
                .email("duplicate@classhub.com")
                .password("encoded")
                .name("Teacher One")
                .role(MemberRole.TEACHER)
                .build();
        memberRepository.saveAndFlush(teacher);

        Member duplicate = Member.builder()
                .email("duplicate@classhub.com")
                .password("encoded")
                .name("Teacher Two")
                .role(MemberRole.TEACHER)
                .build();

        assertThatThrownBy(() -> memberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void assignTeacherForAssistant() {
        UUID teacherId = UUID.randomUUID();
        Member assistant = Member.builder()
                .email("assistant@classhub.com")
                .password("encoded")
                .name("Assistant Lee")
                .role(MemberRole.ASSISTANT)
                .teacherId(teacherId)
                .build();

        Member saved = memberRepository.save(assistant);

        assertThat(saved.getTeacherId()).isEqualTo(teacherId);
    }
}
