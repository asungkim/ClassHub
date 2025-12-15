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
  | { type: "PREFILL"; payload?: PrefillPayload };

type LessonComposerContextValue = {
  state: LessonComposerState;
  openComposer: () => void;
  closeComposer: () => void;
  resetComposer: () => void;
  prefillSharedLesson: (payload?: PrefillPayload) => void;
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
      const courseChanged =
        payload.courseId && payload.courseId !== state.selectedCourseId ? true : false;

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
      prefillSharedLesson: (payload?: PrefillPayload) => dispatch({ type: "PREFILL", payload })
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
