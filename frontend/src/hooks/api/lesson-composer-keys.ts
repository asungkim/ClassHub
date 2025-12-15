const baseKey = ["lesson-composer"] as const;

export const lessonComposerQueryKeys = {
  all: baseKey,
  courses: (isActive?: boolean) => [...baseKey, "courses", { isActive: isActive ?? "all" }] as const,
  courseStudents: (courseId?: string | null) =>
    [...baseKey, "course-students", courseId ?? "unselected"] as const
};
