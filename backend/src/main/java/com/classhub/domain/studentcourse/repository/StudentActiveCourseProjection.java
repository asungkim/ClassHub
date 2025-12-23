package com.classhub.domain.studentcourse.repository;

import java.util.UUID;

public interface StudentActiveCourseProjection {

    UUID getStudentMemberId();

    UUID getCourseId();

    String getCourseName();
}
