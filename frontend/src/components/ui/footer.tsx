"use client";

import Link from "next/link";

type FooterLink = {
  label: string;
  href: string;
};

type FooterSection = {
  title: string;
  links: FooterLink[];
};

type FooterProps = {
  sections: FooterSection[];
};

export function Footer({ sections }: FooterProps) {
  return (
    <footer className="border-t border-slate-200 bg-white/80">
      <div className="mx-auto grid max-w-6xl gap-10 px-4 py-12 md:grid-cols-4">
        <div className="space-y-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-primary text-base font-semibold text-white shadow">
              CH
            </div>
            <div>
              <p className="text-lg font-semibold text-slate-900">ClassHub</p>
              <p className="text-xs text-slate-500">함께 더 나은 수업을</p>
            </div>
          </div>
          <p className="text-sm text-slate-500">
            강사·조교·학생을 하나로 묶어주는 운영 플랫폼. 초대부터 수업 관리까지 한 곳에서 완료하세요.
          </p>
        </div>
        {sections.map((section, sectionIndex) => (
          <div key={`${section.title}-${sectionIndex}`} className="space-y-3">
            <h4 className="text-sm font-semibold text-slate-800">{section.title}</h4>
            <ul className="space-y-2 text-sm text-slate-500">
              {section.links.map((link, linkIndex) => (
                <li key={`${section.title}-${link.label}-${linkIndex}`}>
                  <Link href={link.href} className="hover:text-primary">
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <div className="border-t border-slate-200/60 bg-white/60 px-4 py-4 text-center text-xs text-slate-500">
        © {new Date().getFullYear()} ClassHub. All rights reserved.
      </div>
    </footer>
  );
}
