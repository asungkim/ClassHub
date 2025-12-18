package com.classhub.global.init.data;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.StudentInfo;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.domain.member.repository.StudentInfoRepository;
import com.classhub.global.init.seeds.InitStudentInfos;
import com.classhub.global.init.seeds.InitStudentInfos.StudentInfoSeed;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile({"local", "test"})
public class StudentInfoInitData extends BaseInitData {

    private final MemberRepository memberRepository;
    private final StudentInfoRepository studentInfoRepository;

    public StudentInfoInitData(MemberRepository memberRepository,
                               StudentInfoRepository studentInfoRepository) {
        super("season2-student-info-seed", 110);
        this.memberRepository = memberRepository;
        this.studentInfoRepository = studentInfoRepository;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (StudentInfoSeed seed : InitStudentInfos.seeds()) {
            Optional<Member> member = memberRepository.findByEmail(seed.memberEmail());
            if (member.isEmpty()) {
                log.warn("Skipping StudentInfo seed. Member not found for email={}", seed.memberEmail());
                continue;
            }
            upsertStudentInfo(member.get(), seed, force);
        }
    }

    private void upsertStudentInfo(Member member, StudentInfoSeed seed, boolean force) {
        Optional<StudentInfo> existing = studentInfoRepository.findByMemberId(member.getId());
        if (existing.isPresent()) {
            if (force) {
                StudentInfo info = existing.get();
                info.updateSchoolName(seed.schoolName());
                info.updateGrade(seed.grade());
                info.updateBirthDate(seed.birthDate());
                info.updateParentPhone(seed.parentPhone());
            }
            return;
        }

        StudentInfo info = StudentInfo.create(
                member,
                seed.schoolName(),
                seed.grade(),
                seed.birthDate(),
                seed.parentPhone()
        );
        studentInfoRepository.save(info);
    }
}
