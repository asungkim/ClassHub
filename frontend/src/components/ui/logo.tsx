type LogoProps = {
  label?: string;
};

export function Logo({ label = "CH" }: LogoProps) {
  return (
    <div className="flex items-center gap-3">
      <div
        className="flex h-16 w-16 items-center justify-center rounded-2xl text-2xl font-semibold text-white shadow-lg"
        style={{
          backgroundImage: "linear-gradient(135deg, #5B5FED 0%, #9D4EDD 100%)"
        }}
      >
        {label}
      </div>
      <div>
        <p className="text-lg font-semibold text-slate-900">ClassHub</p>
        <p className="text-sm text-slate-500">Frontend System</p>
      </div>
    </div>
  );
}
