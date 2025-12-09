import { ReactNode } from "react";

type EmptyStateProps = {
  message: string;
  description?: string;
  icon?: ReactNode;
};

export function EmptyState({ message, description, icon }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      {icon && <div className="mb-4 text-gray-400">{icon}</div>}
      <p className="text-sm font-medium text-gray-900">{message}</p>
      {description && <p className="mt-1 text-sm text-gray-500">{description}</p>}
    </div>
  );
}
