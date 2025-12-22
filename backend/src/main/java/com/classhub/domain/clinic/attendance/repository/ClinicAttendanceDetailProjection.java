package com.classhub.domain.clinic.attendance.repository;

import com.classhub.domain.member.model.StudentGrade;
import java.time.LocalDate;
import java.util.UUID;

public interface ClinicAttendanceDetailProjection {
    UUID getAttendanceId();

    UUID getRecordId();

    UUID getStudentCourseRecordId();

    UUID getStudentMemberId();

    String getStudentName();

    String getPhoneNumber();

    String getSchoolName();

    StudentGrade getGrade();

    String getParentPhoneNumber();

    LocalDate getBirthDate();
}
