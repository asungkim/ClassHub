package com.classhub.global.response;

import com.classhub.global.exception.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RsCode {

    // Common
    FORBIDDEN(RsConstant.FORBIDDEN, "접근 권한이 없습니다."),
    SUCCESS(RsConstant.SUCCESS, "요청이 성공했습니다."),
    CREATED(RsConstant.CREATED, "새로운 리소스를 생성했습니다."),
    NOT_FOUND(RsConstant.NOT_FOUND, "요청하신 리소스를 찾을 수 없습니다."),
    BAD_REQUEST(RsConstant.BAD_REQUEST, "잘못된 요청입니다."),
    INTERNAL_SERVER(RsConstant.INTERNAL_SERVER_ERROR, "서버 내부 오류입니다."),
    UNAUTHENTICATED(RsConstant.UNAUTHORIZED, "인증이 실패했습니다."),
    UNAUTHORIZED(RsConstant.FORBIDDEN, "접근 권한이 없습니다."),
    TOO_MANY_REQUESTS(RsConstant.TOO_MANY_REQUESTS, "너무 많은 요청입니다."),
    DUPLICATE_EMAIL(RsConstant.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_INVITATION(RsConstant.BAD_REQUEST, "유효하지 않은 초대입니다."),
    INVITATION_ALREADY_EXISTS(RsConstant.CONFLICT, "이미 처리되지 않은 초대가 존재합니다."),
    MEMBER_INACTIVE(RsConstant.UNAUTHORIZED, "비활성화된 계정입니다."),

    COURSE_NOT_FOUND(RsConstant.NOT_FOUND, "반 정보를 찾을 수 없습니다."),
    COURSE_FORBIDDEN(RsConstant.FORBIDDEN, "해당 반에 대한 권한이 없습니다."),
    ASSISTANT_NOT_FOUND(RsConstant.NOT_FOUND, "담당 조교 정보를 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(RsConstant.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    STUDENT_PROFILE_NOT_FOUND(RsConstant.NOT_FOUND, "학생 프로필을 찾을 수 없습니다."),
    STUDENT_PROFILE_DUPLICATE_PHONE(RsConstant.CONFLICT, "이미 사용 중인 연락처입니다."),
    STUDENT_PROFILE_MEMBER_IN_USE(RsConstant.CONFLICT, "이미 다른 학생 프로필에 연결된 계정입니다."),
    INVALID_STUDENT_PROFILE(RsConstant.BAD_REQUEST, "초대할 수 없는 학생 프로필입니다."),
    PERSONAL_LESSON_NOT_FOUND(RsConstant.NOT_FOUND, "개별 진도 기록을 찾을 수 없습니다."),
    SHARED_LESSON_NOT_FOUND(RsConstant.NOT_FOUND, "공통 진도 기록을 찾을 수 없습니다.");

    private final Integer code;
    private final String message;

    public BusinessException toException() {
        return new BusinessException(this);
    }
}
