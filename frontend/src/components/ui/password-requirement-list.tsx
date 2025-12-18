import clsx from "clsx";

type PasswordRequirementListProps = {
  password: string;
};

type Requirement = {
  id: string;
  label: string;
  test: (password: string) => boolean;
};

const requirements: Requirement[] = [
  {
    id: "length",
    label: "8자 이상 64자 이하",
    test: (pwd) => pwd.length >= 8 && pwd.length <= 64
  },
  {
    id: "letter",
    label: "영문 포함",
    test: (pwd) => /[A-Za-z]/.test(pwd)
  },
  {
    id: "number",
    label: "숫자 포함",
    test: (pwd) => /\d/.test(pwd)
  },
  {
    id: "special",
    label: "특수문자 포함",
    test: (pwd) => /[!@#$%^&*()_+\-={}:;"'`~<>,.?/\\|\[\]]/.test(pwd)
  }
];

export function PasswordRequirementList({ password }: PasswordRequirementListProps) {
  return (
    <ul className="space-y-1.5 text-xs">
      {requirements.map((req) => {
        const isMet = req.test(password);
        return (
          <li key={req.id} className="flex items-center gap-2">
            <CheckIcon
              className={clsx(
                "h-4 w-4 transition-colors",
                isMet ? "text-emerald-600" : "text-slate-300"
              )}
            />
            <span
              className={clsx(
                "transition-colors",
                isMet ? "text-emerald-700 font-medium" : "text-slate-500"
              )}
            >
              {req.label}
            </span>
          </li>
        );
      })}
    </ul>
  );
}

function CheckIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" {...props}>
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}
