package com.classhub.global.response;

import com.classhub.global.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RsCode {

    // ===== Common / Infra =====
    SUCCESS(RsConstant.SUCCESS, "요청이 성공했습니다."),
    CREATED(RsConstant.CREATED, "새로운 리소스를 생성했습니다."),
    BAD_REQUEST(RsConstant.BAD_REQUEST, "잘못된 요청입니다."),
    NOT_FOUND(RsConstant.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER(RsConstant.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    FORBIDDEN(RsConstant.FORBIDDEN, "접근 권한이 없습니다."),
    TOO_MANY_REQUESTS(RsConstant.TOO_MANY_REQUESTS, "너무 많은 요청입니다."),

    // ===== Auth / Member =====
    UNAUTHENTICATED(RsConstant.UNAUTHORIZED, "인증이 실패했습니다."),
    UNAUTHORIZED(RsConstant.FORBIDDEN, "접근 권한이 없습니다."),
    MEMBER_NOT_FOUND(RsConstant.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(RsConstant.CONFLICT, "이미 사용 중인 이메일입니다."),
    CONCURRENT_UPDATE(RsConstant.CONFLICT, "동시 요청으로 작업을 처리할 수 없습니다."),
    MEMBER_INACTIVE(RsConstant.UNAUTHORIZED, "비활성화된 계정입니다."),

    // ===== Invitation =====
    INVALID_INVITATION(RsConstant.BAD_REQUEST, "유효하지 않은 초대입니다."),
    INVITATION_ALREADY_EXISTS(RsConstant.CONFLICT, "이미 처리되지 않은 초대가 존재합니다."),
    INVITATION_EXPIRED(RsConstant.BAD_REQUEST, "만료된 초대입니다."),

    // ===== Course / Assignment =====
    COURSE_NOT_FOUND(RsConstant.NOT_FOUND, "반 정보를 찾을 수 없습니다."),
    COURSE_FORBIDDEN(RsConstant.FORBIDDEN, "해당 반에 대한 권한이 없습니다."),
    COURSE_ENDED(RsConstant.CONFLICT, "종료된 반입니다."),
    COURSE_SCHEDULE_OVERLAP(RsConstant.CONFLICT, "반 수업 시간이 겹칩니다."),
    ASSISTANT_NOT_FOUND(RsConstant.NOT_FOUND, "담당 조교 정보를 찾을 수 없습니다."),
    ASSISTANT_ALREADY_ASSIGNED(RsConstant.CONFLICT, "이미 연결된 조교입니다."),
    COMPANY_NOT_FOUND(RsConstant.NOT_FOUND, "학원 정보를 찾을 수 없습니다."),
    BRANCH_ALREADY_ASSIGNED(RsConstant.CONFLICT, "이미 연결된 지점입니다."),
    TEACHER_BRANCH_ASSIGNMENT_NOT_FOUND(RsConstant.NOT_FOUND, "지점 연결 정보를 찾을 수 없습니다."),
    BRANCH_NOT_FOUND(RsConstant.NOT_FOUND, "지점 정보를 찾을 수 없습니다."),

    // ===== Student Profile =====
    STUDENT_PROFILE_NOT_FOUND(RsConstant.NOT_FOUND, "학생 프로필을 찾을 수 없습니다."),
    STUDENT_PROFILE_DUPLICATE_PHONE(RsConstant.CONFLICT, "이미 사용 중인 연락처입니다."),
    STUDENT_PROFILE_MEMBER_IN_USE(RsConstant.CONFLICT, "이미 다른 학생 프로필에 연결된 계정입니다."),
    INVALID_STUDENT_PROFILE(RsConstant.BAD_REQUEST, "초대할 수 없는 학생 프로필입니다."),

    // ===== Lesson / Clinic =====
    PERSONAL_LESSON_NOT_FOUND(RsConstant.NOT_FOUND, "개별 진도 기록을 찾을 수 없습니다."),
    SHARED_LESSON_NOT_FOUND(RsConstant.NOT_FOUND, "공통 진도 기록을 찾을 수 없습니다."),
    CLINIC_SLOT_NOT_FOUND(RsConstant.NOT_FOUND, "클리닉 슬롯을 찾을 수 없습니다."),
    CLINIC_SLOT_CONFLICT(RsConstant.CONFLICT, "다른 클리닉 슬롯과 시간이 겹칩니다."),
    CLINIC_SLOT_TIME_INVALID(RsConstant.BAD_REQUEST, "클리닉 슬롯 시간이 올바르지 않습니다."),
    CLINIC_SLOT_TIME_OVERLAP(RsConstant.CONFLICT, "클리닉 슬롯 시간이 겹칩니다."),
    CLINIC_SLOT_CAPACITY_CONFLICT(RsConstant.CONFLICT, "클리닉 슬롯 정원이 현재 배정 인원보다 작습니다."),
    CLINIC_SLOT_CAPACITY_EXCEEDED(RsConstant.CONFLICT, "클리닉 슬롯 정원이 초과되었습니다."),
    CLINIC_SLOT_DUPLICATED(RsConstant.CONFLICT, "이미 선택된 클리닉 슬롯입니다."),
    CLINIC_SESSION_NOT_FOUND(RsConstant.NOT_FOUND, "클리닉 세션을 찾을 수 없습니다."),
    CLINIC_SESSION_ALREADY_EXISTS(RsConstant.CONFLICT, "이미 생성된 클리닉 세션입니다."),
    CLINIC_SESSION_TIME_INVALID(RsConstant.BAD_REQUEST, "클리닉 세션 시간이 올바르지 않습니다."),
    CLINIC_SESSION_CANCEL_FORBIDDEN(RsConstant.CONFLICT, "클리닉 세션을 취소할 수 없습니다."),
    CLINIC_SESSION_CANCELED(RsConstant.CONFLICT, "취소된 클리닉 세션입니다."),
    CLINIC_SESSION_FULL(RsConstant.CONFLICT, "클리닉 세션 정원이 초과되었습니다."),
    CLINIC_ATTENDANCE_NOT_FOUND(RsConstant.NOT_FOUND, "클리닉 출석 정보를 찾을 수 없습니다."),
    CLINIC_ATTENDANCE_DUPLICATED(RsConstant.CONFLICT, "이미 등록된 클리닉 출석입니다."),
    CLINIC_ATTENDANCE_TIME_OVERLAP(RsConstant.CONFLICT, "클리닉 시간이 겹칩니다."),
    CLINIC_ATTENDANCE_LOCKED(RsConstant.CONFLICT, "클리닉 출석을 변경할 수 없습니다."),
    CLINIC_ATTENDANCE_MOVE_FORBIDDEN(RsConstant.CONFLICT, "클리닉 이동이 불가능합니다."),
    CLINIC_ATTENDANCE_CANCEL_FORBIDDEN(RsConstant.CONFLICT, "기본 클리닉 출석은 취소할 수 없습니다."),
    CLINIC_RECORD_NOT_FOUND(RsConstant.NOT_FOUND, "클리닉 기록을 찾을 수 없습니다."),
    CLINIC_RECORD_ALREADY_EXISTS(RsConstant.CONFLICT, "클리닉 기록이 이미 존재합니다."),

    // ===== Enrollment =====
    STUDENT_ENROLLMENT_REQUEST_NOT_FOUND(RsConstant.NOT_FOUND, "수업 신청을 찾을 수 없습니다."),
    STUDENT_ENROLLMENT_REQUEST_CONFLICT(RsConstant.CONFLICT, "이미 처리 중인 신청이 있습니다."),
    STUDENT_ENROLLMENT_ALREADY_EXISTS(RsConstant.CONFLICT, "이미 수강 중인 반입니다."),
    INVALID_ENROLLMENT_REQUEST_STATE(RsConstant.BAD_REQUEST, "현재 상태에서는 처리할 수 없습니다."),
    STUDENT_COURSE_ASSIGNMENT_ALREADY_EXISTS(RsConstant.CONFLICT, "이미 배치된 학생입니다."),
    STUDENT_COURSE_ASSIGNMENT_NOT_FOUND(RsConstant.NOT_FOUND, "배치 정보를 찾을 수 없습니다."),

    // ===== Teacher-Student Request =====
    TEACHER_STUDENT_REQUEST_NOT_FOUND(RsConstant.NOT_FOUND, "선생님 요청을 찾을 수 없습니다."),
    TEACHER_STUDENT_REQUEST_CONFLICT(RsConstant.CONFLICT, "이미 처리 중인 요청이 있습니다."),
    TEACHER_STUDENT_ALREADY_ASSIGNED(RsConstant.CONFLICT, "이미 연결된 선생님입니다."),
    INVALID_TEACHER_STUDENT_REQUEST_STATE(RsConstant.BAD_REQUEST, "현재 상태에서는 처리할 수 없습니다."),

    // ===== Feedback =====
    FEEDBACK_NOT_FOUND(RsConstant.NOT_FOUND, "피드백을 찾을 수 없습니다."),
    FEEDBACK_ALREADY_RESOLVED(RsConstant.CONFLICT, "이미 해결된 피드백입니다."),

    STUDENT_COURSE_RECORD_NOT_FOUND(RsConstant.NOT_FOUND, "학생 수업 기록을 찾을 수 없습니다.");

    private final Integer code;
    private final String message;

    public BusinessException toException() {
        return new BusinessException(this);
    }
}
