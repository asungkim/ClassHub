package com.classhub.domain.company.company.application;

import com.classhub.domain.company.company.dto.response.CompanyResponse;
import com.classhub.domain.company.company.model.Company;
import com.classhub.domain.company.company.model.CompanyType;
import com.classhub.domain.company.company.model.VerifiedStatus;
import com.classhub.domain.company.company.repository.CompanyRepository;
import com.classhub.global.exception.BusinessException;
import com.classhub.global.response.PageResponse;
import com.classhub.global.response.RsCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyQueryService {

    private final CompanyRepository companyRepository;

    public PageResponse<CompanyResponse> getCompaniesForTeacher(
            UUID teacherId,
            VerifiedStatus status,
            CompanyType type,
            String keyword,
            Pageable pageable
    ) {
        VerifiedStatus resolvedStatus = status != null ? status : VerifiedStatus.VERIFIED;
        UUID creatorFilter = resolvedStatus == VerifiedStatus.UNVERIFIED ? teacherId : null;

        Page<Company> page = companyRepository.searchCompanies(resolvedStatus, type, keyword, creatorFilter, pageable);
        return PageResponse.from(page.map(CompanyResponse::from));
    }

    public PageResponse<CompanyResponse> getCompaniesForAdmin(
            VerifiedStatus status,
            CompanyType type,
            String keyword,
            Pageable pageable
    ) {
        Page<Company> page = companyRepository.searchCompanies(status, type, keyword, null, pageable);
        return PageResponse.from(page.map(CompanyResponse::from));
    }

    public CompanyResponse getCompany(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(RsCode.COMPANY_NOT_FOUND::toException);
        return CompanyResponse.from(company);
    }
}
