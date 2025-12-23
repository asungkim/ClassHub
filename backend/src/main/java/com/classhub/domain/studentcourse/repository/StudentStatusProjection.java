package com.classhub.domain.studentcourse.repository;

import java.util.UUID;

public interface StudentStatusProjection {

    UUID getStudentMemberId();

    boolean getActive();
}
