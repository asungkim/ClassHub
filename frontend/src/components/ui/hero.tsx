"use client";

import { ReactNode } from "react";
import Link from "next/link";
import { Button } from "@/components/ui/button";

type HeroProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  primaryCta?: { label: string; href: string };
  secondaryCta?: { label: string; href: string };
  illustration?: ReactNode;
};

export function Hero({ eyebrow, title, description, primaryCta, secondaryCta, illustration }: HeroProps) {
  return (
    <div className="overflow-hidden rounded-3xl border border-white/60 bg-white/80 p-6 shadow-lg backdrop-blur lg:flex lg:items-center">
      <div className="flex-1 space-y-4">
        {eyebrow && <p className="text-xs font-semibold uppercase tracking-[0.3em] text-primary">{eyebrow}</p>}
        <h1 className="text-4xl font-semibold text-slate-900">{title}</h1>
        {description && <p className="text-base text-slate-600">{description}</p>}
        <div className="flex flex-wrap gap-3">
          {primaryCta && (
            <Button asChild>
              <Link href={primaryCta.href}>{primaryCta.label}</Link>
            </Button>
          )}
          {secondaryCta && (
            <Button variant="secondary" asChild>
              <Link href={secondaryCta.href}>{secondaryCta.label}</Link>
            </Button>
          )}
        </div>
      </div>
      {illustration && <div className="mt-8 flex-1 lg:mt-0 lg:pl-10">{illustration}</div>}
    </div>
  );
}
