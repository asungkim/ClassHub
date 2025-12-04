"use client";

import Link from "next/link";
import type { Route } from "next";

export type NavItem = {
  label: string;
  href: string;
};

type NavigationBarProps = {
  navItems: NavItem[];
  ctaLabel?: string;
  ctaHref?: string;
};

function isInternalRoute(href: string): href is Route {
  return href.startsWith("/");
}

export function NavigationBar({ navItems, ctaLabel, ctaHref }: NavigationBarProps) {
  return (
    <header className="border-b border-slate-200 bg-white/80 backdrop-blur">
      <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-4">
        <Link href="/" className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-base font-semibold text-white shadow">
            CH
          </div>
          <div>
            <p className="text-sm font-semibold text-slate-900">ClassHub</p>
            <p className="text-xs text-slate-500">Demo Console</p>
          </div>
        </Link>
        <nav className="hidden items-center gap-6 text-sm font-medium text-slate-500 md:flex">
          {navItems.map((item, index) => (
            <div key={`${item.label}-${item.href}-${index}`}>
              {isInternalRoute(item.href) ? (
                <Link href={item.href} className="hover:text-primary">
                  {item.label}
                </Link>
              ) : (
                <a href={item.href} className="hover:text-primary" target="_blank" rel="noreferrer">
                  {item.label}
                </a>
              )}
            </div>
          ))}
        </nav>
        {ctaLabel && ctaHref && (
          <>
            {isInternalRoute(ctaHref) ? (
              <Link
                href={ctaHref}
                className="hidden rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white shadow md:inline-flex"
              >
                {ctaLabel}
              </Link>
            ) : (
              <a
                href={ctaHref}
                className="hidden rounded-full bg-primary px-4 py-2 text-sm font-semibold text-white shadow md:inline-flex"
                target="_blank"
                rel="noreferrer"
              >
                {ctaLabel}
              </a>
            )}
          </>
        )}
      </div>
    </header>
  );
}
