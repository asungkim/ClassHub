package com.classhub.domain.assignment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.classhub.domain.assignment.dto.AssistantAssignmentStatusFilter;
import com.classhub.domain.assignment.dto.response.AssistantAssignmentResponse;
import com.classhub.domain.assignment.model.TeacherAssistantAssignment;
import com.classhub.domain.assignment.repository.TeacherAssistantAssignmentRepository;
import com.classhub.domain.invitation.model.Invitation;
import com.classhub.domain.invitation.model.InvitationRole;
import com.classhub.domain.invitation.model.InvitationStatus;
import com.classhub.domain.invitation.repository.InvitationRepository;
import com.classhub.domain.invitation.dto.response.InvitationSummaryResponse;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.model.MemberRole;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssistantManagementServiceTest {

    @Mock
    private TeacherAssistantAssignmentRepository assignmentRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private AssistantManagementService assistantManagementService;

    @Captor
    private ArgumentCaptor<TeacherAssistantAssignment> assignmentCaptor;

    private UUID teacherId;
    private UUID assistantId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        assistantId = UUID.randomUUID();
    }

    @Test
    void getAssistantAssignments_shouldUseActiveFilter() {
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());

        Page<TeacherAssistantAssignment> page = new PageImpl<>(
                List.of(assignment),
                PageRequest.of(0, 10),
                1
        );
        when(assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNull(eq(teacherId), any()))
                .thenReturn(page);
        Member assistant = buildAssistantMember(assistantId, "Assistant One");
        when(memberRepository.findAllById(any())).thenReturn(List.of(assistant));

        PageResponse<AssistantAssignmentResponse> result =
                assistantManagementService.getAssistantAssignments(
                        teacherId,
                        AssistantAssignmentStatusFilter.ACTIVE,
                        0,
                        10
                );

        assertThat(result.content()).hasSize(1);
        AssistantAssignmentResponse response = result.content().get(0);
        assertThat(response.assignmentId()).isEqualTo(assignment.getId());
        assertThat(response.assistant().name()).isEqualTo("Assistant One");
        verify(assignmentRepository).findByTeacherMemberIdAndDeletedAtIsNull(eq(teacherId), any());
    }

    @Test
    void getAssistantAssignments_shouldUseInactiveFilter() {
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        assignment.disable();
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        Page<TeacherAssistantAssignment> page = new PageImpl<>(
                List.of(assignment),
                PageRequest.of(0, 10),
                1
        );
        when(assignmentRepository.findByTeacherMemberIdAndDeletedAtIsNotNull(eq(teacherId), any()))
                .thenReturn(page);
        when(memberRepository.findAllById(any())).thenReturn(List.of(buildAssistantMember(assistantId, "Assistant Two")));

        PageResponse<AssistantAssignmentResponse> result =
                assistantManagementService.getAssistantAssignments(
                        teacherId,
                        AssistantAssignmentStatusFilter.INACTIVE,
                        0,
                        10
                );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isActive()).isFalse();
        verify(assignmentRepository).findByTeacherMemberIdAndDeletedAtIsNotNull(eq(teacherId), any());
    }

    @Test
    void getAssistantAssignments_shouldUseAllFilter() {
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        ReflectionTestUtils.setField(assignment, "id", UUID.randomUUID());
        when(assignmentRepository.findByTeacherMemberId(eq(teacherId), any()))
                .thenReturn(new PageImpl<>(List.of(assignment), PageRequest.of(0, 10), 1));
        when(memberRepository.findAllById(any())).thenReturn(List.of(buildAssistantMember(assistantId, "Assistant Three")));

        assistantManagementService.getAssistantAssignments(
                teacherId,
                AssistantAssignmentStatusFilter.ALL,
                0,
                10
        );

        verify(assignmentRepository).findByTeacherMemberId(eq(teacherId), any());
    }

    @Test
    void updateAssistantStatus_shouldDisableAssignment_whenEnabledFalse() {
        UUID assignmentId = UUID.randomUUID();
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        when(assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(memberRepository.findById(assistantId))
                .thenReturn(Optional.of(buildAssistantMember(assistantId, "Assistant")));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AssistantAssignmentResponse response = assistantManagementService.updateAssistantStatus(
                teacherId,
                assignmentId,
                false
        );

        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().isActive()).isFalse();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void updateAssistantStatus_shouldEnableAssignment_whenEnabledTrue() {
        UUID assignmentId = UUID.randomUUID();
        TeacherAssistantAssignment assignment = TeacherAssistantAssignment.create(teacherId, assistantId);
        assignment.disable();
        ReflectionTestUtils.setField(assignment, "id", assignmentId);
        when(assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId))
                .thenReturn(Optional.of(assignment));
        when(memberRepository.findById(assistantId))
                .thenReturn(Optional.of(buildAssistantMember(assistantId, "Assistant")));
        when(assignmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AssistantAssignmentResponse response = assistantManagementService.updateAssistantStatus(
                teacherId,
                assignmentId,
                true
        );

        verify(assignmentRepository).save(assignmentCaptor.capture());
        assertThat(assignmentCaptor.getValue().isActive()).isTrue();
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void updateAssistantStatus_shouldThrow_whenAssignmentNotFound() {
        UUID assignmentId = UUID.randomUUID();
        when(assignmentRepository.findByIdAndTeacherMemberId(assignmentId, teacherId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> assistantManagementService.updateAssistantStatus(teacherId, assignmentId, true))
                .isInstanceOf(BusinessException.class);

        verify(assignmentRepository, never()).save(any());
    }

    @Test
    void getAssistantInvitations_shouldFilterByStatus() {
        Invitation invitation = buildInvitation(teacherId, "CODE-700", InvitationStatus.PENDING);
        Page<Invitation> page = new PageImpl<>(List.of(invitation), PageRequest.of(0, 10), 1);
        when(invitationRepository.findBySenderIdAndInviteeRoleAndStatus(
                eq(teacherId),
                eq(InvitationRole.ASSISTANT),
                eq(InvitationStatus.PENDING),
                any()
        )).thenReturn(page);

        PageResponse<InvitationSummaryResponse> result = assistantManagementService.getAssistantInvitations(
                teacherId,
                InvitationStatus.PENDING,
                0,
                10
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).code()).isEqualTo("CODE-700");
        verify(invitationRepository).findBySenderIdAndInviteeRoleAndStatus(
                eq(teacherId),
                eq(InvitationRole.ASSISTANT),
                eq(InvitationStatus.PENDING),
                any()
        );
    }

    @Test
    void getAssistantInvitations_shouldFallbackToAll_whenStatusNull() {
        Invitation invitation = buildInvitation(teacherId, "CODE-701", InvitationStatus.PENDING);
        Page<Invitation> page = new PageImpl<>(List.of(invitation), PageRequest.of(0, 10), 1);
        when(invitationRepository.findBySenderIdAndInviteeRole(
                eq(teacherId),
                eq(InvitationRole.ASSISTANT),
                any()
        )).thenReturn(page);

        assistantManagementService.getAssistantInvitations(
                teacherId,
                null,
                0,
                10
        );

        verify(invitationRepository).findBySenderIdAndInviteeRole(
                eq(teacherId),
                eq(InvitationRole.ASSISTANT),
                any()
        );
    }

    private Member buildAssistantMember(UUID memberId, String name) {
        Member member = Member.builder()
                .email(name.replace(" ", "").toLowerCase() + "@classhub.com")
                .password("encoded")
                .name(name)
                .phoneNumber("01012345678")
                .role(MemberRole.ASSISTANT)
                .build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private Invitation buildInvitation(UUID senderId, String code, InvitationStatus status) {
        Invitation invitation = Invitation.builder()
                .senderId(senderId)
                .targetEmail("assistant@classhub.com")
                .inviteeRole(InvitationRole.ASSISTANT)
                .status(status)
                .code(code)
                .expiredAt(LocalDateTime.now().plusDays(7))
                .build();
        ReflectionTestUtils.setField(invitation, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(invitation, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(invitation, "updatedAt", LocalDateTime.now());
        return invitation;
    }
}
