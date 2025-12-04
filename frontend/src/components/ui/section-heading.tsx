type SectionHeadingProps = {
  eyebrow?: string;
  title: string;
  description?: string;
};

export function SectionHeading({ eyebrow, title, description }: SectionHeadingProps) {
  return (
    <div className="space-y-2">
      {eyebrow && <p className="text-xs font-semibold uppercase tracking-[0.2em] text-primary/70">{eyebrow}</p>}
      <h2 className="text-2xl font-semibold text-slate-900">{title}</h2>
      {description && <p className="text-base text-slate-500">{description}</p>}
    </div>
  );
}
