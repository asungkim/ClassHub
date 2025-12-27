package com.classhub.global.init.data;

import com.classhub.domain.assignment.model.StudentTeacherRequest;
import com.classhub.domain.assignment.repository.StudentTeacherRequestRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test"})
public class StudentTeacherRequestInitData extends BaseInitData {

    private static final int TOTAL_STUDENTS = 100;
    private static final int BOTH_REQUEST_COUNT = 50;
    private static final int TEACHER_ONE_ONLY_COUNT = 25;
    private static final String TEACHER_ONE_EMAIL = "te1@n.com";
    private static final String TEACHER_TWO_EMAIL = "te2@n.com";

    private final StudentTeacherRequestRepository requestRepository;
    private final MemberRepository memberRepository;

    public StudentTeacherRequestInitData(StudentTeacherRequestRepository requestRepository,
                                         MemberRepository memberRepository) {
        super("season2-student-teacher-request-seed", 90);
        this.requestRepository = requestRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        Optional<Member> teacherOne = memberRepository.findByEmail(TEACHER_ONE_EMAIL);
        Optional<Member> teacherTwo = memberRepository.findByEmail(TEACHER_TWO_EMAIL);
        if (teacherOne.isEmpty() || teacherTwo.isEmpty()) {
            log.warn("Skipping student-teacher request seed. teachers missing");
            return;
        }

        for (int i = 1; i <= TOTAL_STUDENTS; i += 1) {
            String email = "st" + i + "@n.com";
            Optional<Member> student = memberRepository.findByEmail(email);
            if (student.isEmpty()) {
                log.warn("Skipping student request seed. Student not found for email={}", email);
                continue;
            }
            if (i <= BOTH_REQUEST_COUNT) {
                upsertRequest(student.get().getId(), teacherOne.get().getId(), force);
                upsertRequest(student.get().getId(), teacherTwo.get().getId(), force);
                continue;
            }
            if (i <= BOTH_REQUEST_COUNT + TEACHER_ONE_ONLY_COUNT) {
                upsertRequest(student.get().getId(), teacherOne.get().getId(), force);
                continue;
            }
            upsertRequest(student.get().getId(), teacherTwo.get().getId(), force);
        }
    }

    private void upsertRequest(UUID studentId, UUID teacherId, boolean force) {
        Optional<StudentTeacherRequest> existing = requestRepository
                .findByStudentMemberIdAndTeacherMemberId(studentId, teacherId);
        if (existing.isPresent()) {
            if (force) {
                requestRepository.delete(existing.get());
            } else {
                return;
            }
        }
        StudentTeacherRequest request = StudentTeacherRequest.builder()
                .studentMemberId(studentId)
                .teacherMemberId(teacherId)
                .message("테스트 요청")
                .build();
        requestRepository.save(request);
    }
}
