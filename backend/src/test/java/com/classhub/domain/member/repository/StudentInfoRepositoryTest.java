package com.classhub.domain.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.model.StudentGrade;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.global.config.JpaConfig;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class StudentInfoRepositoryTest {

    @Autowired
    private StudentInfoRepository studentInfoRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void findByMemberId_shouldReturnStudentInfo() {
        Member member = memberRepository.save(
                Member.builder()
                        .email("student@classhub.com")
                        .password("encoded")
                        .name("Student Kim")
                        .phoneNumber("01011112222")
                        .role(MemberRole.STUDENT)
                        .build()
        );

        StudentInfo info = StudentInfo.create(
                member,
                "서울중학교",
                StudentGrade.MIDDLE_2,
                LocalDate.of(2010, 3, 15),
                "010-9999-8888"
        );
        studentInfoRepository.save(info);

        assertThat(studentInfoRepository.findByMemberId(member.getId()))
                .isPresent()
                .get()
                .extracting(StudentInfo::getSchoolName)
                .isEqualTo("서울중학교");
    }
}
