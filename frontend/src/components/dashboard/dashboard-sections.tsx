"use client";

import clsx from "clsx";
import { Card } from "@/components/ui/card";

type DashboardSectionsProps = {
  role: "TEACHER" | "ASSISTANT" | "STUDENT" | "SUPERADMIN";
};

type SummaryItem = {
  label: string;
  value: string;
  helper: string;
  tone: "blue" | "green" | "orange" | "rose";
};

type ScheduleItem = {
  time: string;
  title: string;
  caption: string;
  tone: "blue" | "green" | "purple" | "slate";
};

type ProgressItem = {
  label: string;
  value: string;
  helper: string;
  tone: "green" | "slate" | "blue" | "indigo";
};

export function DashboardSections({ role }: DashboardSectionsProps) {
  const today = new Intl.DateTimeFormat("ko", {
    month: "long",
    day: "numeric",
    weekday: "long"
  }).format(new Date());

  const summary: SummaryItem[] = [
    { label: "전체 학생", value: "45명", helper: "연락 학생", tone: "blue" },
    { label: "진행 중인 반", value: "4개", helper: "활성 반", tone: "green" },
    { label: "이번 주 클리닉", value: "53건", helper: "예정 클리닉", tone: "orange" },
    { label: "미확인 알림", value: "3개", helper: "알림 대기", tone: "rose" }
  ];

  const schedule: ScheduleItem[] = [
    { time: "09:00", title: "수학 심화", caption: "합동의 예비 개념 | A반 12명", tone: "blue" },
    { time: "11:00", title: "입시 컨설팅", caption: "개인별 면접 지도 | 개별 3건", tone: "green" },
    { time: "14:00", title: "수학 멘토링", caption: "확률과 통계 연습 | B반 10명", tone: "purple" },
    { time: "16:00", title: "클리닉 세션", caption: "개별 피드백 | 클리닉 전용", tone: "slate" }
  ];

  const progress: ProgressItem[] = [
    { label: "완료", value: "48건", helper: "완료된 클리닉", tone: "green" },
    { label: "대기", value: "5건", helper: "오늘 일정", tone: "slate" },
    { label: "숙제율", value: "92%", helper: "주간 과제 제출", tone: "blue" },
    { label: "출석률", value: "87%", helper: "최근 2주 기준", tone: "indigo" }
  ];

  const assistants = [
    { name: "김민주 조교", status: "근무 중", shift: "09:00 - 18:00" },
    { name: "박서준 조교", status: "오후 근무", shift: "13:00 - 21:00" }
  ];

  return (
    <div className="space-y-8">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <div>
          <p className="text-sm font-semibold text-slate-500">오늘</p>
          <h2 className="text-xl font-bold text-slate-900">{today}</h2>
        </div>
        <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
          {roleLabel(role)} 계정
        </span>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {summary.map((item) => (
          <Card key={item.label}>
            <div className="space-y-2">
              <p className="text-sm font-semibold text-slate-500">{item.label}</p>
              <div className="flex items-end gap-2">
                <span className="text-3xl font-bold text-slate-900">{item.value}</span>
              </div>
              <span className={clsx("inline-flex rounded-full px-3 py-1 text-xs font-semibold", toneClass(item.tone))}>
                {item.helper}
              </span>
            </div>
          </Card>
        ))}
      </div>

      <Card title="오늘의 일정" description="수업과 클리닉 진행 계획">
        <div className="divide-y divide-slate-100">
          {schedule.map((item) => (
            <div key={item.title} className="flex flex-col gap-2 py-3 md:flex-row md:items-center md:justify-between">
              <div className="flex items-center gap-3">
                <span
                  className={clsx(
                    "flex h-10 w-10 items-center justify-center rounded-xl text-xs font-bold",
                    toneIconClass(item.tone)
                  )}
                >
                  {item.time}
                </span>
                <div>
                  <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                  <p className="text-xs text-slate-500">{item.caption}</p>
                </div>
              </div>
              <button className="text-xs font-semibold text-blue-600 hover:text-blue-700">자세히 보기</button>
            </div>
          ))}
        </div>
      </Card>

      <Card title="이번 주 클리닉" description="완료율과 주요 지표">
        <div className="space-y-4">
          <div className="overflow-hidden rounded-2xl bg-purple-50">
            <div className="h-16 w-full bg-gradient-to-r from-purple-400 to-indigo-500 px-6 py-4 text-right text-lg font-bold text-white">
              48/53 완료율
            </div>
          </div>
          <div className="grid gap-3 md:grid-cols-2">
            {progress.map((item) => (
              <div
                key={item.label}
                className="flex items-center justify-between rounded-xl border border-slate-100 bg-white px-4 py-3"
              >
                <div>
                  <p className="text-sm font-semibold text-slate-800">{item.label}</p>
                  <p className="text-xs text-slate-500">{item.helper}</p>
                </div>
                <span className={clsx("text-sm font-bold", toneTextClass(item.tone))}>{item.value}</span>
              </div>
            ))}
          </div>
        </div>
      </Card>

      <Card title="조교 근무 현황" description="근무 중인 조교와 교대 일정">
        <div className="divide-y divide-slate-100">
          {assistants.map((assistant) => (
            <div key={assistant.name} className="flex flex-col gap-2 py-3 md:flex-row md:items-center md:justify-between">
              <div>
                <p className="text-sm font-semibold text-slate-900">{assistant.name}</p>
                <p className="text-xs text-slate-500">{assistant.shift}</p>
              </div>
              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                {assistant.status}
              </span>
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

function toneClass(tone: SummaryItem["tone"]) {
  switch (tone) {
    case "blue":
      return "bg-blue-50 text-blue-700";
    case "green":
      return "bg-emerald-50 text-emerald-700";
    case "orange":
      return "bg-amber-50 text-amber-700";
    case "rose":
      return "bg-rose-50 text-rose-700";
  }
}

function toneIconClass(tone: ScheduleItem["tone"]) {
  switch (tone) {
    case "blue":
      return "bg-blue-50 text-blue-700";
    case "green":
      return "bg-emerald-50 text-emerald-700";
    case "purple":
      return "bg-purple-50 text-purple-700";
    case "slate":
      return "bg-slate-100 text-slate-700";
  }
}

function toneTextClass(tone: ProgressItem["tone"]) {
  switch (tone) {
    case "green":
      return "text-emerald-700";
    case "slate":
      return "text-slate-700";
    case "blue":
      return "text-blue-700";
    case "indigo":
      return "text-indigo-700";
  }
}

function roleLabel(role: DashboardSectionsProps["role"]) {
  switch (role) {
    case "TEACHER":
      return "선생님";
    case "ASSISTANT":
      return "조교";
    case "STUDENT":
      return "학생";
    case "SUPERADMIN":
      return "슈퍼어드민";
  }
}
