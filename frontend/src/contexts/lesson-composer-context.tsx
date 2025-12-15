"use client";

import { createContext, useContext, useMemo, useReducer } from "react";
import type { ReactNode } from "react";

type SubmissionStatus = "idle" | "creatingShared" | "creatingPersonal" | "error" | "success";

export type SharedLessonFormValues = {
  courseId?: string;
  date: string;
  title: string;
  content: string;
};

export type PersonalLessonFormValues = {
  studentProfileId: string;
  date: string;
  content: string;
};

type SubmissionFailure = {
  studentId: string;
  message: string;
};

export type LessonComposerState = {
  isOpen: boolean;
  selectedCourseId?: string;
  selectedCourseName?: string;
  sharedLessonForm: SharedLessonFormValues;
  personalEntries: Record<string, PersonalLessonFormValues>;
  selectedStudentIds: string[];
  submission: {
    status: SubmissionStatus;
    failures: SubmissionFailure[];
  };
};

type PrefillPayload = Partial<
  SharedLessonFormValues & {
    courseName?: string;
  }
>;

type LessonComposerAction =
  | { type: "OPEN" }
  | { type: "CLOSE" }
  | { type: "RESET" }
  | { type: "PREFILL"; payload?: PrefillPayload }
  | { type: "SELECT_COURSE"; payload: { courseId?: string; courseName?: string } }
  | { type: "SET_SHARED_FIELD"; field: keyof SharedLessonFormValues; value: string | undefined }
  | { type: "SET_SELECTED_STUDENTS"; payload: { studentIds: string[]; defaultDate?: string } }
  | { type: "UPDATE_PERSONAL_ENTRY"; payload: { studentId: string; data: Partial<PersonalLessonFormValues> } }
  | {
      type: "SET_SUBMISSION";
      payload: Partial<{
        status: SubmissionStatus;
        failures: SubmissionFailure[];
      }>;
    };

type LessonComposerContextValue = {
  state: LessonComposerState;
  openComposer: () => void;
  closeComposer: () => void;
  resetComposer: () => void;
  prefillSharedLesson: (payload?: PrefillPayload) => void;
  selectCourse: (payload: { courseId?: string; courseName?: string }) => void;
  setSharedField: (field: keyof SharedLessonFormValues, value: string | undefined) => void;
  setSelectedStudents: (studentIds: string[]) => void;
  updatePersonalEntry: (studentId: string, data: Partial<PersonalLessonFormValues>) => void;
  updateSubmission: (payload: Partial<{ status: SubmissionStatus; failures: SubmissionFailure[] }>) => void;
};

const LessonComposerContext = createContext<LessonComposerContextValue | undefined>(undefined);

function getTodayISODate() {
  return new Date().toISOString().split("T")[0];
}

function createInitialState(): LessonComposerState {
  return {
    isOpen: false,
    selectedCourseId: undefined,
    selectedCourseName: undefined,
    sharedLessonForm: {
      courseId: undefined,
      date: getTodayISODate(),
      title: "",
      content: ""
    },
    personalEntries: {},
    selectedStudentIds: [],
    submission: {
      status: "idle",
      failures: []
    }
  };
}

function lessonComposerReducer(state: LessonComposerState, action: LessonComposerAction): LessonComposerState {
  switch (action.type) {
    case "OPEN":
      return { ...state, isOpen: true };
    case "CLOSE":
      return { ...state, isOpen: false };
    case "RESET":
      return createInitialState();
    case "PREFILL": {
      const payload = action.payload ?? {};
      const courseChanged = Boolean(payload.courseId && payload.courseId !== state.selectedCourseId);

      const nextSharedForm: SharedLessonFormValues = {
        ...state.sharedLessonForm,
        ...(payload.date ? { date: payload.date } : {}),
        ...(payload.title !== undefined ? { title: payload.title } : {}),
        ...(payload.content !== undefined ? { content: payload.content } : {})
      };

      if (payload.courseId !== undefined) {
        nextSharedForm.courseId = payload.courseId;
      }

      return {
        ...state,
        selectedCourseId: payload.courseId ?? state.selectedCourseId,
        selectedCourseName: payload.courseName ?? state.selectedCourseName,
        sharedLessonForm: nextSharedForm,
        personalEntries: courseChanged ? {} : state.personalEntries,
        selectedStudentIds: courseChanged ? [] : state.selectedStudentIds,
        submission: courseChanged ? { status: "idle", failures: [] } : state.submission
      };
    }
    case "SELECT_COURSE": {
      const { courseId, courseName } = action.payload;
      return {
        ...state,
        selectedCourseId: courseId,
        selectedCourseName: courseName,
        sharedLessonForm: {
          ...state.sharedLessonForm,
          courseId
        },
        personalEntries: {},
        selectedStudentIds: [],
        submission: { status: "idle", failures: [] }
      };
    }
    case "SET_SHARED_FIELD": {
      const { field, value } = action;
      const nextShared = { ...state.sharedLessonForm };
      if (field === "courseId") {
        nextShared.courseId = value;
      } else if (field === "date") {
        nextShared.date = value ?? getTodayISODate();
      } else if (field === "title") {
        nextShared.title = value ?? "";
      } else if (field === "content") {
        nextShared.content = value ?? "";
      }
      return {
        ...state,
        sharedLessonForm: nextShared
      };
    }
    case "SET_SELECTED_STUDENTS": {
      const { studentIds, defaultDate } = action.payload;
      const nextEntries: Record<string, PersonalLessonFormValues> = { ...state.personalEntries };

      Object.keys(nextEntries).forEach((studentId) => {
        if (!studentIds.includes(studentId)) {
          delete nextEntries[studentId];
        }
      });

      studentIds.forEach((studentId) => {
        if (!nextEntries[studentId]) {
          nextEntries[studentId] = {
            studentProfileId: studentId,
            date: defaultDate ?? state.sharedLessonForm.date,
            content: ""
          };
        }
      });

      return {
        ...state,
        selectedStudentIds: studentIds,
        personalEntries: nextEntries
      };
    }
    case "UPDATE_PERSONAL_ENTRY": {
      const { studentId, data } = action.payload;
      const existing = state.personalEntries[studentId] ?? {
        studentProfileId: studentId,
        date: state.sharedLessonForm.date,
        content: ""
      };
      return {
        ...state,
        personalEntries: {
          ...state.personalEntries,
          [studentId]: {
            ...existing,
            ...data
          }
        }
      };
    }
    case "SET_SUBMISSION": {
      const { payload } = action;
      return {
        ...state,
        submission: {
          status: payload.status ?? state.submission.status,
          failures: payload.failures ?? state.submission.failures
        }
      };
    }
    default:
      return state;
  }
}

export function LessonComposerProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(lessonComposerReducer, undefined, createInitialState);

  const contextValue = useMemo<LessonComposerContextValue>(
    () => ({
      state,
      openComposer: () => dispatch({ type: "OPEN" }),
      closeComposer: () => dispatch({ type: "CLOSE" }),
      resetComposer: () => dispatch({ type: "RESET" }),
      prefillSharedLesson: (payload?: PrefillPayload) => dispatch({ type: "PREFILL", payload }),
      selectCourse: (payload) => dispatch({ type: "SELECT_COURSE", payload }),
      setSharedField: (field, value) => dispatch({ type: "SET_SHARED_FIELD", field, value }),
      setSelectedStudents: (studentIds) =>
        dispatch({
          type: "SET_SELECTED_STUDENTS",
          payload: { studentIds, defaultDate: state.sharedLessonForm.date }
        }),
      updatePersonalEntry: (studentId, data) =>
        dispatch({ type: "UPDATE_PERSONAL_ENTRY", payload: { studentId, data } }),
      updateSubmission: (payload) => dispatch({ type: "SET_SUBMISSION", payload })
    }),
    [state]
  );

  return <LessonComposerContext.Provider value={contextValue}>{children}</LessonComposerContext.Provider>;
}

export function useLessonComposer() {
  const context = useContext(LessonComposerContext);
  if (!context) {
    throw new Error("useLessonComposer must be used within a LessonComposerProvider");
  }
  return context;
}
