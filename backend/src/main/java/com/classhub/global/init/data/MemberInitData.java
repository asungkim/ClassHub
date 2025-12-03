package com.classhub.global.init.data;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.classhub.global.init.BootstrapSeedContext;
import com.classhub.global.init.SeedKeys;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
public class MemberInitData extends BaseInitData {

    private static final String DEFAULT_PASSWORD = "Classhub!234";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapSeedContext seedContext;

    public MemberInitData(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            BootstrapSeedContext seedContext
    ) {
        super("member-seed", 100);
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedContext = seedContext;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);
        for (MemberSeed seed : buildSeeds()) {
            UUID teacherId = resolveTeacherId(seed.teacherKey());
            Member member = memberRepository.findByEmail(seed.email())
                    .map(existing -> updateMember(existing, seed, teacherId, force ? encodedPassword : null))
                    .orElseGet(() -> createMember(seed, teacherId, encodedPassword));
            member = memberRepository.save(member);
            seedContext.storeMember(seed.key(), member);
        }
    }

    private Member updateMember(Member member, MemberSeed seed, UUID teacherId, String encodedPassword) {
        member.changeName(seed.name());
        member.changeRole(seed.role());
        member.assignTeacher(teacherId);
        member.activate();
        if (encodedPassword != null) {
            member.changePassword(encodedPassword);
        }
        return member;
    }

    private Member createMember(MemberSeed seed, UUID teacherId, String encodedPassword) {
        return Member.builder()
                .email(seed.email())
                .name(seed.name())
                .role(seed.role())
                .password(encodedPassword)
                .teacherId(teacherId)
                .build();
    }

    private UUID resolveTeacherId(String teacherKey) {
        if (teacherKey == null) {
            return null;
        }
        return seedContext.getRequiredMember(teacherKey).getId();
    }

    private List<MemberSeed> buildSeeds() {
        List<MemberSeed> seeds = new ArrayList<>();
        seeds.add(new MemberSeed(SeedKeys.SUPERADMIN, "admin@classhub.dev", "Super Admin", MemberRole.SUPERADMIN, null));
        seeds.add(new MemberSeed(SeedKeys.TEACHER_ALPHA, "teacher_alpha@classhub.dev", "Alice Teacher", MemberRole.TEACHER, null));
        seeds.add(new MemberSeed(SeedKeys.TEACHER_BETA, "teacher_beta@classhub.dev", "Ben Teacher", MemberRole.TEACHER, null));

        for (int i = 1; i <= 3; i++) {
            seeds.add(new MemberSeed(
                    SeedKeys.assistantKey(SeedKeys.TEACHER_ALPHA, i),
                    "assistant_alpha_" + i + "@classhub.dev",
                    "Alpha Assistant " + i,
                    MemberRole.ASSISTANT,
                    SeedKeys.TEACHER_ALPHA
            ));
            seeds.add(new MemberSeed(
                    SeedKeys.assistantKey(SeedKeys.TEACHER_BETA, i),
                    "assistant_beta_" + i + "@classhub.dev",
                    "Beta Assistant " + i,
                    MemberRole.ASSISTANT,
                    SeedKeys.TEACHER_BETA
            ));
        }

        seeds.add(new MemberSeed(
                SeedKeys.studentMemberKey(SeedKeys.TEACHER_ALPHA),
                "student_alpha_main@classhub.dev",
                "Alpha Student Account",
                MemberRole.STUDENT,
                null
        ));
        seeds.add(new MemberSeed(
                SeedKeys.studentMemberKey(SeedKeys.TEACHER_BETA),
                "student_beta_main@classhub.dev",
                "Beta Student Account",
                MemberRole.STUDENT,
                null
        ));
        return seeds;
    }

    private record MemberSeed(
            String key,
            String email,
            String name,
            MemberRole role,
            String teacherKey
    ) {
    }
}
