import { HTMLAttributes } from "react";
import clsx from "clsx";

type BadgeVariant = "default" | "success" | "secondary" | "destructive";

type BadgeProps = {
  variant?: BadgeVariant;
} & HTMLAttributes<HTMLSpanElement>;

const variantClassMap: Record<BadgeVariant, string> = {
  default: "bg-blue-50 text-blue-700 border-blue-200",
  success: "bg-green-50 text-green-700 border-green-200",
  secondary: "bg-gray-50 text-gray-700 border-gray-200",
  destructive: "bg-red-50 text-red-700 border-red-200"
};

export function Badge({ variant = "default", className, ...props }: BadgeProps) {
  return (
    <span
      className={clsx(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors",
        variantClassMap[variant],
        className
      )}
      {...props}
    />
  );
}
