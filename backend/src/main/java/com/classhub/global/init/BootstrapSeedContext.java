package com.classhub.global.init;

import com.classhub.domain.course.model.Course;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.studentprofile.model.StudentProfile;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "dev"})
public class BootstrapSeedContext {

    private final Map<String, Member> membersByKey = new ConcurrentHashMap<>();
    private final Map<UUID, Member> membersById = new ConcurrentHashMap<>();
    private final Map<String, Course> coursesByKey = new ConcurrentHashMap<>();
    private final Map<UUID, Course> coursesById = new ConcurrentHashMap<>();
    private final Map<String, StudentProfile> studentProfilesByKey = new ConcurrentHashMap<>();

    public void storeMember(String key, Member member) {
        membersByKey.put(key, member);
        membersById.put(member.getId(), member);
    }

    public Member getRequiredMember(String key) {
        return Optional.ofNullable(membersByKey.get(key))
                .orElseThrow(() -> new IllegalStateException("Member seed not found for key: " + key));
    }

    public Optional<Member> findMember(String key) {
        return Optional.ofNullable(membersByKey.get(key));
    }

    public Optional<Member> findMember(UUID id) {
        return Optional.ofNullable(membersById.get(id));
    }

    public void storeCourse(String key, Course course) {
        coursesByKey.put(key, course);
        coursesById.put(course.getId(), course);
    }

    public Course getRequiredCourse(String key) {
        return Optional.ofNullable(coursesByKey.get(key))
                .orElseThrow(() -> new IllegalStateException("Course seed not found for key: " + key));
    }

    public Optional<Course> findCourse(UUID id) {
        return Optional.ofNullable(coursesById.get(id));
    }

    public void storeStudentProfile(String key, StudentProfile studentProfile) {
        studentProfilesByKey.put(key, studentProfile);
    }

    public StudentProfile getRequiredStudentProfile(String key) {
        return Optional.ofNullable(studentProfilesByKey.get(key))
                .orElseThrow(() -> new IllegalStateException("StudentProfile seed not found for key: " + key));
    }

    public Collection<StudentProfile> getAllStudentProfiles() {
        return studentProfilesByKey.values();
    }
}
