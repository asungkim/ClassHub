package com.classhub.domain.studentcourse.model;

import com.classhub.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "student_course_record",
        indexes = {
                @Index(name = "idx_scr_student", columnList = "student_member_id"),
                @Index(name = "idx_scr_course", columnList = "course_id"),
                @Index(name = "idx_scr_assistant", columnList = "assistant_member_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentCourseRecord extends BaseEntity {

    @Column(name = "student_member_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID studentMemberId;

    @Column(name = "course_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID courseId;

    @Column(name = "assistant_member_id", columnDefinition = "BINARY(16)")
    private UUID assistantMemberId;

    @Column(name = "default_clinic_slot_id", columnDefinition = "BINARY(16)")
    private UUID defaultClinicSlotId;

    @Column(name = "teacher_notes", columnDefinition = "TEXT")
    private String teacherNotes;

    @Builder
    private StudentCourseRecord(UUID studentMemberId,
                                UUID courseId,
                                UUID assistantMemberId,
                                UUID defaultClinicSlotId,
                                String teacherNotes) {
        this.studentMemberId = studentMemberId;
        this.courseId = courseId;
        this.assistantMemberId = assistantMemberId;
        this.defaultClinicSlotId = defaultClinicSlotId;
        this.teacherNotes = teacherNotes;
    }

    public static StudentCourseRecord create(UUID studentMemberId,
                                             UUID courseId,
                                             UUID assistantMemberId,
                                             UUID defaultClinicSlotId,
                                             String teacherNotes) {
        return StudentCourseRecord.builder()
                .studentMemberId(studentMemberId)
                .courseId(courseId)
                .assistantMemberId(assistantMemberId)
                .defaultClinicSlotId(defaultClinicSlotId)
                .teacherNotes(teacherNotes)
                .build();
    }

    public void updateAssistant(UUID assistantMemberId) {
        this.assistantMemberId = assistantMemberId;
    }

    public void updateDefaultClinicSlot(UUID clinicSlotId) {
        this.defaultClinicSlotId = clinicSlotId;
    }

    public void updateTeacherNotes(String notes) {
        this.teacherNotes = notes;
    }
}
