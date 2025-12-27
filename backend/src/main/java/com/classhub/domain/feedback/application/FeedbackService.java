package com.classhub.domain.feedback.application;

import com.classhub.domain.feedback.dto.request.FeedbackCreateRequest;
import com.classhub.domain.feedback.dto.response.FeedbackResponse;
import com.classhub.domain.feedback.dto.response.FeedbackWriterResponse;
import com.classhub.domain.feedback.model.Feedback;
import com.classhub.domain.feedback.model.FeedbackStatus;
import com.classhub.domain.feedback.repository.FeedbackRepository;
import com.classhub.domain.member.model.Member;
import com.classhub.domain.member.repository.MemberRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public FeedbackResponse createFeedback(UUID memberId, FeedbackCreateRequest request) {
        String content = trimContent(request.content());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        if (member.isDeleted()) {
            throw new BusinessException(RsCode.MEMBER_INACTIVE);
        }
        Feedback feedback = Feedback.builder()
                .memberId(memberId)
                .content(content)
                .status(FeedbackStatus.SUBMITTED)
                .build();
        Feedback saved = feedbackRepository.save(feedback);
        return FeedbackResponse.from(saved, FeedbackWriterResponse.from(member));
    }

    public PageResponse<FeedbackResponse> getFeedbacksForAdmin(FeedbackStatus status, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<Feedback> feedbackPage = status == null
                ? feedbackRepository.findAll(pageable)
                : feedbackRepository.findAllByStatus(status, pageable);
        if (feedbackPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        Map<UUID, FeedbackWriterResponse> writerMap = loadWriterMap(feedbackPage.getContent());
        Page<FeedbackResponse> dtoPage = feedbackPage.map(feedback ->
                FeedbackResponse.from(feedback, requireWriter(writerMap, feedback.getMemberId()))
        );
        return PageResponse.from(dtoPage);
    }

    public PageResponse<FeedbackResponse> getMyFeedbacks(UUID memberId, FeedbackStatus status, int page, int size) {
        Pageable pageable = buildPageable(page, size);
        Page<Feedback> feedbackPage = status == null
                ? feedbackRepository.findAllByMemberId(memberId, pageable)
                : feedbackRepository.findAllByMemberIdAndStatus(memberId, status, pageable);
        if (feedbackPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        FeedbackWriterResponse writer = FeedbackWriterResponse.from(member);
        Page<FeedbackResponse> dtoPage = feedbackPage.map(feedback -> FeedbackResponse.from(feedback, writer));
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public FeedbackResponse resolveFeedback(UUID feedbackId, UUID adminId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(RsCode.FEEDBACK_NOT_FOUND::toException);
        if (feedback.getStatus() == FeedbackStatus.RESOLVED) {
            throw new BusinessException(RsCode.FEEDBACK_ALREADY_RESOLVED);
        }
        feedback.resolve(adminId, null);
        Feedback saved = feedbackRepository.save(feedback);
        Member member = memberRepository.findById(saved.getMemberId())
                .orElseThrow(RsCode.MEMBER_NOT_FOUND::toException);
        return FeedbackResponse.from(saved, FeedbackWriterResponse.from(member));
    }

    private String trimContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return content.trim();
    }

    private Pageable buildPageable(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new BusinessException(RsCode.BAD_REQUEST);
        }
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private Map<UUID, FeedbackWriterResponse> loadWriterMap(List<Feedback> feedbacks) {
        Set<UUID> memberIds = feedbacks.stream()
                .map(Feedback::getMemberId)
                .collect(Collectors.toSet());
        return memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, FeedbackWriterResponse::from));
    }

    private FeedbackWriterResponse requireWriter(Map<UUID, FeedbackWriterResponse> writerMap, UUID memberId) {
        FeedbackWriterResponse writer = writerMap.get(memberId);
        if (writer == null) {
            throw new BusinessException(RsCode.MEMBER_NOT_FOUND);
        }
        return writer;
    }
}
