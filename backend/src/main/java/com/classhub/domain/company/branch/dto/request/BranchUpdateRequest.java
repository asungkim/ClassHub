package com.classhub.domain.company.branch.dto.request;

public record BranchUpdateRequest(
        String name,
        Boolean enabled
) {

    public boolean hasChanges() {
        return (name != null && !name.isBlank()) || enabled != null;
    }
}
