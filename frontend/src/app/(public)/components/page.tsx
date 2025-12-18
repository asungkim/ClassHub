import type { Metadata } from "next";
import { ComponentsShowcase } from "@/components/showcase/components-showcase";
import { classhubTheme } from "@/theme/classhub-theme";

export const metadata: Metadata = {
  title: "ClassHub Components",
  description: "공통 UI 컴포넌트와 테마 데모 페이지"
};

export default function ComponentsPage() {
  return (
    <div className="space-y-10">
      <div className="rounded-3xl border border-slate-100 bg-white/70 p-8 shadow-sm backdrop-blur">
        <h1 className="text-3xl font-semibold text-slate-900">{classhubTheme.name}</h1>
        <p className="mt-2 text-sm text-slate-500">{classhubTheme.description}</p>
      </div>
      <ComponentsShowcase />
    </div>
  );
}
