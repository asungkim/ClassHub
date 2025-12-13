package com.classhub.domain.studentprofile.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_profile", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_student_profile_course_phone",
                columnNames = {"course_id", "phone_number"}
        )
}, indexes = {
        @Index(name = "idx_student_profile_teacher", columnList = "teacher_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StudentProfile extends BaseEntity {

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "teacher_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID teacherId;

    @Column(name = "assistant_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID assistantId;

    @Column(name = "member_id", columnDefinition = "BINARY(16)")
    private UUID memberId;

    @Column(name = "default_clinic_slot_id", columnDefinition = "BINARY(16)")
    private UUID defaultClinicSlotId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "phone_number", nullable = false, length = 40)
    private String phoneNumber;

    @Column(nullable = false, length = 20)
    private String parentPhone;

    @Column(nullable = false, length = 60)
    private String schoolName;

    @Column(nullable = false, length = 20)
    private String grade;

    @Column(nullable = false)
    private Integer age;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    public void updateBasicInfo(
            String name,
            String parentPhone,
            String schoolName,
            String grade,
            Integer age
    ) {
        if (name != null) {
            this.name = name;
        }
        if (parentPhone != null) {
            this.parentPhone = parentPhone;
        }
        if (schoolName != null) {
            this.schoolName = schoolName;
        }
        if (grade != null) {
            this.grade = grade;
        }
        if (age != null) {
            this.age = age;
        }
    }

    public void assignAssistant(UUID assistantId) {
        if (assistantId != null) {
            this.assistantId = assistantId;
        }
    }

    public void moveToCourse(UUID courseId) {
        if (courseId != null) {
            this.courseId = courseId;
        }
    }

    public void changePhoneNumber(String phoneNumber) {
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    public void assignMember(UUID memberId) {
        this.memberId = memberId;
    }

    public void assignDefaultClinicSlot(UUID clinicSlotId) {
        this.defaultClinicSlotId = clinicSlotId;
    }

    public void deactivate() {
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}
