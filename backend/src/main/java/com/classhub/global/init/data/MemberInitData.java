package com.classhub.global.init.data;

import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.init.seeds.InitMembers;
import com.classhub.global.init.seeds.InitMembers.MemberSeed;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "test"})
public class MemberInitData extends BaseInitData {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberInitData(MemberRepository memberRepository,
                          PasswordEncoder passwordEncoder) {
        super("season2-member-seed", 30);
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    protected void doInitialize(boolean force) {
        for (MemberSeed seed : InitMembers.seeds()) {
            updateMember(seed, force);
        }
    }

    private void updateMember(MemberSeed seed, boolean force) {
        Optional<Member> existing = memberRepository.findByEmail(seed.email());
        if (existing.isPresent()) {
            Member member = existing.get();
            member.activate();
            if (force) {
                member.changeName(seed.name());
                member.changeRole(seed.role());
                member.changePhoneNumber(seed.phoneNumber());
                member.changePassword(passwordEncoder.encode(seed.rawPassword()));
            }
            return;
        }

        Member newMember = Member.builder()
                .email(seed.email())
                .password(passwordEncoder.encode(seed.rawPassword()))
                .name(seed.name())
                .phoneNumber(seed.phoneNumber())
                .role(seed.role())
                .build();
        memberRepository.save(newMember);
    }
}
