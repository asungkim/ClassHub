"use client";

import { useState, useMemo } from "react";
import { useRoleGuard } from "@/hooks/use-role-guard";
import { DashboardShell } from "@/components/dashboard/dashboard-shell";
import { Button } from "@/components/ui/button";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/shared/empty-state";
import { LoadingSkeleton } from "@/components/shared/loading-skeleton";
import { CourseFormModal } from "@/components/course/course-form-modal";
import { useCourses, useToggleCourse } from "@/hooks/use-courses";
import { getApiErrorMessage } from "@/lib/api-error";
import type { components } from "@/types/openapi";

type CourseResponse = components["schemas"]["CourseResponse"];
type ActiveFilter = "all" | "active" | "inactive";

export default function CoursesPage() {
  const { canRender, fallback } = useRoleGuard("TEACHER");
  const [activeFilter, setActiveFilter] = useState<ActiveFilter>("all");
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingCourse, setEditingCourse] = useState<CourseResponse | null>(null);

  const activeValue = useMemo(() => {
    if (activeFilter === "all") return undefined;
    return activeFilter === "active";
  }, [activeFilter]);

  const coursesQuery = useCourses(activeValue);
  const toggleCourse = useToggleCourse();

  if (!canRender) {
    return fallback;
  }

  const courses = coursesQuery.data ?? [];
  const isRefreshing = coursesQuery.isFetching && !coursesQuery.isLoading;
  const errorMessage = coursesQuery.isError
    ? getApiErrorMessage(coursesQuery.error, "반 목록을 불러오지 못했습니다.")
    : "";

  const handleOpenCreateModal = () => {
    setEditingCourse(null);
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (course: CourseResponse) => {
    setEditingCourse(course);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingCourse(null);
  };

  const handleToggle = (course: CourseResponse) => {
    if (!course.id) return;
    const isActive = course.isActive !== false;
    toggleCourse.mutate(course.id, isActive);
  };

  return (
    <DashboardShell
      title="반 관리"
      subtitle="수업하는 반을 생성하고 관리할 수 있습니다."
    >
      <div className="space-y-6">
        {/* Filter Bar */}
        <div className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:flex-row md:items-center md:justify-between">
          <div className="flex items-center gap-2">
            <button
              onClick={() => setActiveFilter("all")}
              className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
                activeFilter === "all"
                  ? "bg-blue-100 text-blue-700"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              전체
            </button>
            <button
              onClick={() => setActiveFilter("active")}
              className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
                activeFilter === "active"
                  ? "bg-blue-100 text-blue-700"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              활성
            </button>
            <button
              onClick={() => setActiveFilter("inactive")}
              className={`rounded-lg px-4 py-2 text-sm font-medium transition ${
                activeFilter === "inactive"
                  ? "bg-blue-100 text-blue-700"
                  : "bg-slate-100 text-slate-600 hover:bg-slate-200"
              }`}
            >
              비활성
            </button>
          </div>

          <Button onClick={handleOpenCreateModal}>새 반 만들기</Button>
        </div>

        {/* Course List */}
        {coursesQuery.isLoading ? (
          <LoadingSkeleton rows={3} />
        ) : coursesQuery.isError ? (
          <ErrorState
            title="목록을 불러오지 못했습니다"
            description={errorMessage}
            retryLabel="다시 시도"
            onRetry={() => coursesQuery.refetch()}
          />
        ) : courses.length === 0 ? (
          <EmptyState
            message="아직 생성된 반이 없습니다"
            description="첫 반을 만들어보세요"
            icon={
              <svg
                xmlns="http://www.w3.org/2000/svg"
                className="h-12 w-12"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={1.5}
                  d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                />
              </svg>
            }
          />
        ) : (
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
            {courses.map((course) => (
              <div key={course.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="space-y-4">
                  <div className="flex items-start justify-between">
                    <h3 className="text-lg font-semibold text-slate-900">{course.name}</h3>
                    <span
                      className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                        course.isActive !== false
                          ? "bg-blue-100 text-blue-700"
                          : "bg-gray-100 text-gray-600"
                      }`}
                    >
                      {course.isActive !== false ? "활성" : "비활성"}
                    </span>
                  </div>

                  <div className="space-y-2 text-sm text-slate-600">
                    <div className="flex items-center gap-2">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"
                        />
                      </svg>
                      <span>{course.company}</span>
                    </div>

                    <div className="flex items-center gap-2">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
                        />
                      </svg>
                      <span>
                        {course.daysOfWeek
                          ?.map((day) =>
                            day === "MONDAY"
                              ? "월"
                              : day === "TUESDAY"
                              ? "화"
                              : day === "WEDNESDAY"
                              ? "수"
                              : day === "THURSDAY"
                              ? "목"
                              : day === "FRIDAY"
                              ? "금"
                              : day === "SATURDAY"
                              ? "토"
                              : "일"
                          )
                          .join(", ")}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <svg
                        xmlns="http://www.w3.org/2000/svg"
                        className="h-4 w-4"
                        fill="none"
                        viewBox="0 0 24 24"
                        stroke="currentColor"
                      >
                        <path
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          strokeWidth={2}
                          d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                        />
                      </svg>
                      <span>
                        {course.startTime} - {course.endTime}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center gap-2 pt-2">
                    <Button variant="ghost" onClick={() => handleOpenEditModal(course)} className="flex-1">
                      수정
                    </Button>
                    <Button
                      variant="ghost"
                      onClick={() => handleToggle(course)}
                      disabled={toggleCourse.isPending}
                      className="flex-1"
                    >
                      {course.isActive !== false ? "비활성화" : "활성화"}
                    </Button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Empty State 버튼 */}
      {courses.length === 0 && !coursesQuery.isLoading && !coursesQuery.isError && (
        <div className="mt-6 text-center">
          <Button onClick={handleOpenCreateModal}>첫 반 만들기</Button>
        </div>
      )}

      {/* Course Form Modal */}
      <CourseFormModal
        open={isModalOpen}
        onClose={handleCloseModal}
        editingCourse={editingCourse}
      />
    </DashboardShell>
  );
}
