import { HTMLAttributes } from "react";
import clsx from "clsx";

type SkeletonProps = HTMLAttributes<HTMLDivElement>;

export function Skeleton({ className, ...props }: SkeletonProps) {
  return (
    <div
      className={clsx("animate-pulse rounded-md bg-gray-200", className)}
      {...props}
    />
  );
}
