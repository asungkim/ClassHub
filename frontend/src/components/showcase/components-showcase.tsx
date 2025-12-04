"use client";

import { useState } from "react";
import { classhubTheme } from "@/theme/classhub-theme";
import { SectionHeading } from "@/components/ui/section-heading";
import { Card } from "@/components/ui/card";
import { Logo } from "@/components/ui/logo";
import { Button } from "@/components/ui/button";
import { TextField } from "@/components/ui/text-field";
import { Checkbox } from "@/components/ui/checkbox";
import { Carousel } from "@/components/ui/carousel";
import { Hero } from "@/components/ui/hero";
import { NavigationBar } from "@/components/ui/navigation-bar";
import { Footer } from "@/components/ui/footer";

type ColorSwatchProps = {
  label: string;
  value: string;
  description: string;
};

const colorSwatches: ColorSwatchProps[] = [
  { label: "Primary", value: classhubTheme.colors.primary.main, description: "핵심 액션" },
  { label: "Primary Light", value: classhubTheme.colors.primary.light, description: "호버/서브" },
  { label: "Secondary", value: classhubTheme.colors.secondary.main, description: "보조 액션" },
  { label: "Card", value: classhubTheme.colors.background.card, description: "카드 배경" },
  { label: "Text", value: classhubTheme.colors.text.primary, description: "본문" },
  { label: "Border", value: classhubTheme.colors.border.main, description: "입력 필드" }
];

const carouselSlides = [
  {
    id: "teacher-program",
    eyebrow: "Program",
    title: "Teacher Master Class",
    description: "수업 커리큘럼 템플릿과 조교 관리 팁을 모아 매주 공유합니다.",
    meta: "Every Friday · 2PM",
    ctaLabel: "참여 예약",
    background: "linear-gradient(135deg, #5B5FED 0%, #9D4EDD 100%)"
  },
  {
    id: "student-insight",
    eyebrow: "Insight",
    title: "학생별 레슨 리포트",
    description: "AI가 정리한 개인별 학습 속도와 차주 추천 액션을 확인하세요.",
    meta: "Beta",
    ctaLabel: "리포트 보기",
    background: "linear-gradient(135deg, #73E0A9 0%, #5B68DF 60%, #FF8AE2 100%)"
  },
  {
    id: "assistant-lounge",
    eyebrow: "Community",
    title: "Assistant Lounge",
    description: "다른 캠퍼스 조교들과 질문을 주고받고 우수 사례를 공유합니다.",
    meta: "24/7",
    ctaLabel: "라운지 입장",
    background: "linear-gradient(135deg, #FFB347 0%, #FFCC33 100%)"
  }
];

const heroIllustration = (
  <div className="rounded-3xl bg-slate-900/90 p-6 text-white shadow-lg">
    <p className="text-xs uppercase tracking-[0.4em] text-white/60">Progress</p>
    <div className="mt-4 space-y-4">
      {[68, 82, 54].map((value, idx) => (
        <div key={value}>
          <div className="flex items-center justify-between text-xs text-white/80">
            <span>{["Invitation", "Lesson", "Clinic"][idx]}</span>
            <span>{value}%</span>
          </div>
          <div className="mt-2 h-2 rounded-full bg-white/20">
            <div
              className="h-2 rounded-full bg-white"
              style={{ width: `${value}%`, opacity: 0.8 }}
            />
          </div>
        </div>
      ))}
    </div>
  </div>
);

const demoNavItems = [
  { label: "Overview", href: "#" },
  { label: "Members", href: "#" },
  { label: "Lessons", href: "#" },
  { label: "Reports", href: "#" }
];

const demoFooterSections = [
  {
    title: "Product",
    links: [
      { label: "Timeline", href: "#" },
      { label: "Analytics", href: "#" },
      { label: "Notifications", href: "#" }
    ]
  },
  {
    title: "Company",
    links: [
      { label: "About", href: "#" },
      { label: "Careers", href: "#" },
      { label: "Contact", href: "#" }
    ]
  },
  {
    title: "Support",
    links: [
      { label: "Docs", href: "#" },
      { label: "Status", href: "#" },
      { label: "Community", href: "#" }
    ]
  }
];

export function ComponentsShowcase() {
  const [email, setEmail] = useState("");
  const [isSubscribed, setIsSubscribed] = useState(true);

  return (
    <div className="space-y-12 py-8">
      <SectionHeading
        eyebrow="Design System"
        title="ClassHub 컴포넌트 컬렉션"
        description="프로젝트 전반에서 재사용할 로고, 카드, 버튼, 입력 필드, 상태 카드 등을 한곳에서 미리 확인할 수 있는 페이지입니다."
      />

      <Card>
        <div className="flex flex-col gap-8 lg:flex-row">
          <div className="flex-1 space-y-6">
            <Logo />
            <p className="text-base text-slate-500">
              ClassHub 테마는 {classhubTheme.colors.primary.main}와 {classhubTheme.colors.secondary.main}을 중심으로
              모던한 교육 서비스 경험을 제공합니다. 모든 공통 컴포넌트는 아래 토큰을 기반으로 스타일링됩니다.
            </p>
            <div className="grid gap-4 sm:grid-cols-2">
              {colorSwatches.map((swatch) => (
                <ColorSwatch key={swatch.label} {...swatch} />
              ))}
            </div>
          </div>

          <div className="flex-1">
            <div
              className="rounded-3xl p-6 text-slate-900"
              style={{
                backgroundImage: classhubTheme.colors.background.gradient
              }}
            >
              <p className="text-sm font-semibold uppercase tracking-[0.2em] text-slate-600">Typography</p>
              <h3 className="mt-4 text-3xl font-semibold" style={{ fontFamily: classhubTheme.typography.fontFamily.korean }}>
                깔끔하고 명확한 메시지를
                <br />
                학원 구성원에게 전달하세요.
              </h3>
              <p className="mt-4 text-sm text-slate-600">
                기본 폰트는 {classhubTheme.typography.fontFamily.primary.split(",")[0]} 를 포함하는 산세리프 계열입니다.
              </p>
            </div>
          </div>
        </div>
      </Card>

      <div className="space-y-4">
        <SectionHeading
          eyebrow="Carousel"
          title="추천 프로그램 슬라이드"
          description="메타정보와 CTA를 포함한 공통 Carousel 컴포넌트 예시입니다."
        />
        <Carousel slides={carouselSlides} />
      </div>

      <div className="space-y-4">
        <SectionHeading
          eyebrow="Hero"
          title="Hero 컴포넌트"
          description="가장 중요한 메시지를 강조하는 Hero 레이아웃입니다."
        />
        <Hero
          eyebrow="Next Chapter"
          title="ClassHub로 학원 운영을 자동화하세요."
          description="초대 발송부터 Student Profile 업데이트까지 하나의 대시보드에서 빠르게 처리해 보세요."
          primaryCta={{ label: "무료 체험 시작", href: "#" }}
          secondaryCta={{ label: "제품 투어 보기", href: "#" }}
          illustration={heroIllustration}
        />
      </div>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Buttons" description="주요/보조 액션 버튼">
          <div className="flex flex-wrap gap-3">
            <Button>Primary Action</Button>
            <Button variant="secondary">Secondary Action</Button>
            <Button variant="ghost">Ghost Action</Button>
          </div>
        </Card>

        <Card title="간단한 가입 폼" description="Input · Checkbox · Button 조합 예시">
          <form className="space-y-4">
            <TextField
              label="이메일"
              placeholder="teacher@classhub.dev"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              icon={<MailIcon />}
            />
            <Checkbox
              label="업데이트 소식 수신"
              checked={isSubscribed}
              onChange={(event) => setIsSubscribed(event.target.checked)}
            />
            <Button type="submit" className="w-full">
              계정 생성
            </Button>
          </form>
        </Card>
      </div>

      <Card
        title="이미지 카드 예제"
        description="Card 컴포넌트 media prop을 활용해 썸네일을 포함 할 수 있습니다."
        media={
          <div
            className="h-56 w-full bg-cover bg-center"
            style={{
              backgroundImage:
                "url('https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?auto=format&fit=crop&w=1200&q=60')"
            }}
          >
            <div className="flex h-full w-full items-end bg-gradient-to-t from-slate-900/80 to-transparent p-6">
              <p className="text-sm font-semibold uppercase tracking-[0.3em] text-white/70">STUDENT FOCUS</p>
            </div>
          </div>
        }
      >
        <p className="text-base text-slate-600">
          다음 주 시작하는 Personal Lesson 캠프를 소개하는 카드입니다. 이미지 영역은 media prop으로 구성하여
          공통 카드 레이아웃과 동일한 패딩/타이포를 유지하면서도 시각적 요소를 쉽게 변경할 수 있습니다.
        </p>
        <div className="mt-4 flex flex-wrap gap-3">
          <Button>상세 보기</Button>
          <Button variant="secondary">참가자 목록</Button>
        </div>
      </Card>

      <Card title="Status Cards" description="진행 현황/통계에 사용할 미니 카드">
        <div className="grid gap-4 md:grid-cols-3">
          <StatCard title="신규 초대" value="24" caption="+8 오늘" />
          <StatCard title="대기 중 학생" value="12" caption="2건 검토 필요" />
          <StatCard title="활성 강좌" value="7" caption="이번 주 32 세션" />
        </div>
      </Card>

      <div className="space-y-4">
        <SectionHeading
          eyebrow="Layout"
          title="Navigation & Footer"
          description="레이아웃 전반에서 재사용할 상단/하단 바 구성 예시입니다."
        />
        <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow">
          <NavigationBar navItems={demoNavItems} ctaLabel="Open Console" ctaHref="#" />
          <div className="px-6 py-6 text-sm text-slate-500">
            NavBar는 로고, 네비게이션, CTA 구조를 유지하며 투명/불투명 배경으로 확장 가능합니다.
          </div>
          <Footer sections={demoFooterSections} />
        </div>
      </div>
    </div>
  );
}

function ColorSwatch({ label, value, description }: ColorSwatchProps) {
  return (
    <div className="flex items-center gap-3 rounded-2xl border border-slate-100 bg-slate-50/80 p-3">
      <span className="h-10 w-10 rounded-xl border" style={{ backgroundColor: value, borderColor: value }} />
      <div>
        <p className="text-sm font-semibold text-slate-900">{label}</p>
        <p className="text-xs text-slate-500">{description}</p>
        <p className="text-[11px] text-slate-400">{value}</p>
      </div>
    </div>
  );
}

type StatCardProps = {
  title: string;
  value: string;
  caption: string;
};

function StatCard({ title, value, caption }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-100 bg-white p-5 shadow-sm">
      <p className="text-sm text-slate-500">{title}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-900">{value}</p>
      <p className="mt-2 text-xs text-primary">{caption}</p>
    </div>
  );
}

function MailIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8m-18 8h18a2 2 0 002-2V8a2 2 0 00-2-2H3a2 2 0 00-2 2v6a2 2 0 002 2z" />
    </svg>
  );
}
