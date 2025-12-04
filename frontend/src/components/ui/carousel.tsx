"use client";

import { useEffect, useState } from "react";
import clsx from "clsx";
import { classhubTheme } from "@/theme/classhub-theme";
import { Button } from "@/components/ui/button";

export type CarouselSlide = {
  id: string;
  eyebrow?: string;
  title: string;
  description?: string;
  meta?: string;
  ctaLabel?: string;
  onCtaClick?: () => void;
  background?: string;
};

type CarouselProps = {
  slides: CarouselSlide[];
  autoPlay?: boolean;
};

export function Carousel({ slides, autoPlay = false }: CarouselProps) {
  const [index, setIndex] = useState(0);
  const activeSlide = slides[index];

  useEffect(() => {
    if (!autoPlay) return;
    const timer = setInterval(() => {
      setIndex((prev) => (prev + 1) % slides.length);
    }, 6000);
    return () => clearInterval(timer);
  }, [slides.length, autoPlay]);

  return (
    <div className="rounded-3xl border border-white/40 bg-white/70 p-8 shadow-lg backdrop-blur">
      <div
        className="rounded-3xl p-8 text-white shadow-xl"
        style={{
          background: activeSlide.background ?? classhubTheme.colors.primary.gradient
        }}
      >
        <div className="flex flex-col gap-4">
          {activeSlide.eyebrow && <p className="text-xs uppercase tracking-[0.25em] opacity-80">{activeSlide.eyebrow}</p>}
          <h3 className="text-3xl font-semibold leading-snug">{activeSlide.title}</h3>
          {activeSlide.description && <p className="text-base text-white/80">{activeSlide.description}</p>}
          <div className="flex flex-wrap items-center gap-4">
            {activeSlide.ctaLabel && (
              <Button onClick={activeSlide.onCtaClick} className="rounded-full bg-white/15 px-6 text-sm font-semibold">
                {activeSlide.ctaLabel}
              </Button>
            )}
            {activeSlide.meta && <span className="text-xs uppercase tracking-widest text-white/70">{activeSlide.meta}</span>}
          </div>
        </div>
      </div>

      <div className="mt-6 flex flex-wrap items-center justify-between gap-4">
        <div className="flex gap-2">
          {slides.map((slide, i) => (
            <button
              key={slide.id}
              className={clsx(
                "h-2.5 rounded-full transition-all",
                i === index ? "w-8 bg-primary" : "w-2.5 bg-slate-300 hover:bg-slate-400"
              )}
              onClick={() => setIndex(i)}
              aria-label={`${slide.title}로 이동`}
            />
          ))}
        </div>
        <div className="flex gap-3">
          <button
            className="rounded-full border border-slate-200 bg-white p-2 shadow hover:bg-slate-50"
            onClick={() => setIndex((prev) => (prev - 1 + slides.length) % slides.length)}
            aria-label="이전 슬라이드"
          >
            <ArrowLeftIcon />
          </button>
          <button
            className="rounded-full border border-slate-200 bg-white p-2 shadow hover:bg-slate-50"
            onClick={() => setIndex((prev) => (prev + 1) % slides.length)}
            aria-label="다음 슬라이드"
          >
            <ArrowRightIcon />
          </button>
        </div>
      </div>
    </div>
  );
}

function ArrowLeftIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 6l-6 6 6 6" />
    </svg>
  );
}

function ArrowRightIcon() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4 text-slate-600" viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M9 6l6 6-6 6" />
    </svg>
  );
}
