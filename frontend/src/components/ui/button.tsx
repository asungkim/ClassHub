import {
  ButtonHTMLAttributes,
  CSSProperties,
  ReactElement,
  ReactNode,
  cloneElement,
  isValidElement
} from "react";
import clsx from "clsx";
import { classhubTheme } from "@/theme/classhub-theme";

type ButtonVariant = "primary" | "secondary" | "ghost";

type ButtonProps = {
  variant?: ButtonVariant;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  asChild?: boolean;
} & ButtonHTMLAttributes<HTMLButtonElement>;

const baseClasses =
  "inline-flex items-center justify-center gap-2 rounded-xl font-semibold transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-offset-2";

const variantClassMap: Record<ButtonVariant, string> = {
  primary: "text-white shadow-lg",
  secondary: "border border-primary/40 text-primary bg-white",
  ghost: "text-slate-600 hover:text-primary hover:bg-primary/5"
};

export function Button({
  variant = "primary",
  leftIcon,
  rightIcon,
  asChild = false,
  className,
  style,
  ...props
}: ButtonProps) {
  const mergedStyle =
    variant === "primary"
      ? {
          backgroundImage: classhubTheme.colors.primary.gradient,
          boxShadow: classhubTheme.shadows.md,
          ...style
        }
      : style;

  if (asChild) {
    if (!isValidElement(props.children)) {
      throw new Error("Button with asChild expects a single React element child.");
    }
    const child = props.children as ReactElement;
    return cloneElement(child, {
      className: clsx(
        baseClasses,
        variantClassMap[variant],
        variant === "primary" && "text-white",
        variant !== "primary" && "bg-transparent",
        "h-12 px-5 text-sm md:text-base",
        child.props.className
      ),
      style: {
        ...(child.props.style ?? {}),
        ...(mergedStyle as CSSProperties)
      },
      children: (
        <>
          {leftIcon}
          {child.props.children}
          {rightIcon}
        </>
      )
    });
  }

  return (
    <button
      className={clsx(
        baseClasses,
        variantClassMap[variant],
        variant === "primary" && "text-white",
        variant !== "primary" && "bg-transparent",
        "h-12 px-5 text-sm md:text-base",
        className
      )}
      style={mergedStyle as CSSProperties}
      {...props}
    >
      {leftIcon}
      {props.children}
      {rightIcon}
    </button>
  );
}
