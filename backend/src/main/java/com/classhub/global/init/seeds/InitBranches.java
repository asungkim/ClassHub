package com.classhub.global.init.seeds;

import com.classhub.domain.company.company.model.VerifiedStatus;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Default Branch seed definitions used for initial data bootstrapping.
 */
public final class InitBranches {

        private InitBranches() {
        }

        public static List<BranchSeed> seeds() {
                List<BranchSeed> seeds = new ArrayList<>();

                seeds.addAll(buildAcademyBranches(
                                "러셀",
                                List.of("강남", "대치", "목동", "부천", "분당", "영통", "중계", "평촌",
                                                "대구", "대전", "센텀", "울산", "광주", "원주", "전주", "청주"),
                                Set.of("전주"),
                                null));

                seeds.addAll(buildAcademyBranches(
                                "두각",
                                List.of("본관", "태성관", "S관", "우전관", "비전관", "하늘관",
                                                "오름관", "오름관3", "창비관", "진학관", "이룸관",
                                                "K관", "누리관", "입시센터", "분당"),
                                Set.of("분당"),
                                null));

                seeds.addAll(buildAcademyBranches(
                                "시대인재",
                                List.of("대치", "목동", "반포", "분당", "대전"),
                                Set.of(),
                                null));

                seeds.addAll(buildAcademyBranches(
                                "미래탐구",
                                List.of("대치", "성북", "중계", "목동", "마포", "동작", "광진",
                                                "분당", "미사", "물금", "화명", "사직", "금정",
                                                "해운대", "센텀", "송도", "전주"),
                                Set.of("전주"),
                                null));

                return List.copyOf(seeds);
        }

        private static List<BranchSeed> buildAcademyBranches(
                        String companyName,
                        List<String> branchNames,
                        Set<String> unverifiedNames,
                        String creatorEmail) {
                Set<String> unverified = new HashSet<>(unverifiedNames);
                List<BranchSeed> seeds = new ArrayList<>();
                for (String branchName : branchNames) {
                        VerifiedStatus status = unverified.contains(branchName)
                                        ? VerifiedStatus.UNVERIFIED
                                        : VerifiedStatus.VERIFIED;
                        seeds.add(new BranchSeed(companyName, branchName, status, creatorEmail));
                }
                return seeds;
        }

        public record BranchSeed(
                        String companyName,
                        String name,
                        VerifiedStatus verifiedStatus,
                        String creatorEmail) {
        }
}
