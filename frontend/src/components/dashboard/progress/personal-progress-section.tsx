"use client";

import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useToast } from "@/components/ui/toast";
import { ErrorState } from "@/components/ui/error-state";
import { TextField } from "@/components/ui/text-field";
import { ProgressFilterBar, type ProgressSelectOption } from "@/components/dashboard/progress/progress-filter-bar";
import { ProgressCardList } from "@/components/dashboard/progress/progress-card-list";
import { fetchPersonalProgresses } from "@/lib/progress-api";
import { fetchStudentCourseRecords } from "@/lib/dashboard-api";
import type { StudentCourseListItemResponse } from "@/types/dashboard";
import type { PersonalProgressResponse, ProgressCursor } from "@/types/progress";

type PersonalProgressSectionProps = {
  role: "TEACHER" | "ASSISTANT";
};

type StudentGroup = {
  studentId: string;
  name: string;
  records: StudentCourseListItemResponse[];
};

const SEARCH_DEBOUNCE_MS = 300;

export function PersonalProgressSection({ role }: PersonalProgressSectionProps) {
  const { showToast } = useToast();
  const [searchValue, setSearchValue] = useState("");
  const [searchResults, setSearchResults] = useState<StudentGroup[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedStudent, setSelectedStudent] = useState<StudentGroup | null>(null);
  const [selectedRecordId, setSelectedRecordId] = useState("");
  const [items, setItems] = useState<PersonalProgressResponse[]>([]);
  const [nextCursor, setNextCursor] = useState<ProgressCursor | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const searchRequestId = useRef(0);

  const loadStudents = useCallback(async (keyword?: string, requestId?: number) => {
    try {
      setError(null);
      setSearchLoading(true);
      const trimmedKeyword = keyword?.trim();
      const response = await fetchStudentCourseRecords({
        status: "ACTIVE",
        page: 0,
        size: 100,
        keyword: trimmedKeyword && trimmedKeyword.length > 0 ? trimmedKeyword : undefined
      });
      const groups = groupStudentRecords(response.items);
      if (requestId && requestId !== searchRequestId.current) {
        return;
      }
      setSearchResults(groups);
    } catch (err) {
      const message = err instanceof Error ? err.message : "학생 목록을 불러오지 못했습니다.";
      setError(message);
      showToast("error", message);
    } finally {
      setSearchLoading(false);
    }
  }, [showToast]);

  const loadProgresses = useCallback(
    async ({ cursor, append }: { cursor?: ProgressCursor | null; append: boolean }) => {
      if (!selectedRecordId) {
        return;
      }
      try {
        if (append) {
          setLoadingMore(true);
        } else {
          setLoading(true);
        }
        const response = await fetchPersonalProgresses({ recordId: selectedRecordId, cursor });
        setItems((prev) => (append ? [...prev, ...response.items] : response.items));
        setNextCursor(response.nextCursor ?? null);
      } catch (err) {
        const message = err instanceof Error ? err.message : "개인 진도를 불러오지 못했습니다.";
        setError(message);
        showToast("error", message);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [selectedRecordId, showToast]
  );

  useEffect(() => {
    const trimmed = searchValue.trim();
    if (!trimmed) {
      setSearchResults([]);
      return;
    }
    if (selectedStudent && trimmed === selectedStudent.name) {
      return;
    }
    const timer = setTimeout(() => {
      const requestId = ++searchRequestId.current;
      void loadStudents(trimmed, requestId);
    }, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [loadStudents, searchValue, selectedStudent]);

  useEffect(() => {
    if (!selectedStudent) {
      setSelectedRecordId("");
      return;
    }
    if (selectedStudent.records.length === 1) {
      setSelectedRecordId(selectedStudent.records[0]?.recordId ?? "");
    } else {
      setSelectedRecordId("");
    }
  }, [selectedStudent]);

  useEffect(() => {
    if (!selectedRecordId) {
      setItems([]);
      setNextCursor(null);
      return;
    }
    void loadProgresses({ append: false });
  }, [loadProgresses, selectedRecordId]);

  const courseOptions = useMemo<ProgressSelectOption[]>(
    () =>
      selectedStudent?.records
        .map((record) => ({
          value: record.recordId ?? "",
          label: record.courseName ?? "반"
        }))
        .filter((option) => option.value.length > 0) ?? [],
    [selectedStudent]
  );

  if (error && searchResults.length === 0 && !selectedStudent) {
    return (
      <ErrorState
        title="학생 목록을 불러오지 못했습니다"
        description={error}
        onRetry={() => void loadStudents(searchValue)}
      />
    );
  }

  return (
    <div className="space-y-6">
      <div className="relative rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm">
        <TextField
          label="학생 검색"
          placeholder="학생 이름을 입력하세요"
          value={searchValue}
          onChange={(event) => {
            const value = event.target.value;
            setSearchValue(value);
            if (selectedStudent && value !== selectedStudent.name) {
              setSelectedStudent(null);
              setSelectedRecordId("");
              setItems([]);
              setNextCursor(null);
            }
          }}
        />
        {searchLoading ? <p className="mt-2 text-xs text-slate-400">검색 중...</p> : null}
        {searchValue.trim().length > 0 && searchResults.length > 0 ? (
          <div className="absolute left-5 right-5 top-full z-10 mt-2 max-h-60 overflow-y-auto rounded-2xl border border-slate-200 bg-white p-2 shadow-xl">
            {searchResults.map((group) => (
              <button
                key={group.studentId}
                type="button"
                onClick={() => {
                  searchRequestId.current += 1;
                  setSelectedStudent(group);
                  setSearchValue(group.name);
                  setSearchResults([]);
                }}
                className="w-full rounded-xl px-4 py-3 text-left text-sm hover:bg-slate-50"
              >
                <p className="font-semibold text-slate-900">{group.name}</p>
                <p className="text-xs text-slate-500">
                  {group.records.map((record) => record.courseName ?? "반").join(", ")}
                </p>
              </button>
            ))}
          </div>
        ) : null}
      </div>

      {selectedStudent && courseOptions.length > 1 ? (
        <ProgressFilterBar
          label="반 선택"
          placeholder="반을 선택하세요"
          value={selectedRecordId}
          options={courseOptions}
          onChange={setSelectedRecordId}
          disabled={courseOptions.length === 0}
        />
      ) : null}

      {loading && items.length === 0 ? (
        <ErrorState title="진도 데이터를 불러오는 중" description="잠시만 기다려 주세요." />
      ) : (
        <ProgressCardList
          items={items}
          emptyMessage={
            selectedRecordId
              ? "선택한 학생의 기록이 없습니다."
              : selectedStudent
                ? "반을 선택해 주세요."
                : "학생을 검색해 선택해 주세요."
          }
          hasMore={Boolean(nextCursor)}
          loadingMore={loadingMore}
          onLoadMore={() => void loadProgresses({ cursor: nextCursor, append: true })}
        />
      )}
    </div>
  );
}

function groupStudentRecords(records: StudentCourseListItemResponse[]): StudentGroup[] {
  const map = new Map<string, StudentGroup>();
  records.forEach((record) => {
    if (!record.studentMemberId) {
      return;
    }
    const existing = map.get(record.studentMemberId);
    if (existing) {
      existing.records.push(record);
      return;
    }
    map.set(record.studentMemberId, {
      studentId: record.studentMemberId,
      name: record.studentName ?? "학생",
      records: [record]
    });
  });
  return Array.from(map.values());
}
