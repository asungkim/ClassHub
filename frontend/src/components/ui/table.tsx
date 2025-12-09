import { HTMLAttributes, TdHTMLAttributes, ThHTMLAttributes } from "react";
import clsx from "clsx";

export function Table({ className, ...props }: HTMLAttributes<HTMLTableElement>) {
  return (
    <div className="relative w-full overflow-auto">
      <table
        className={clsx("w-full caption-bottom text-sm", className)}
        {...props}
      />
    </div>
  );
}

export function TableHeader({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return <thead className={clsx("[&_tr]:border-b", className)} {...props} />;
}

export function TableBody({ className, ...props }: HTMLAttributes<HTMLTableSectionElement>) {
  return (
    <tbody className={clsx("[&_tr:last-child]:border-0", className)} {...props} />
  );
}

export function TableRow({ className, ...props }: HTMLAttributes<HTMLTableRowElement>) {
  return (
    <tr
      className={clsx(
        "border-b transition-colors hover:bg-gray-50/50 data-[state=selected]:bg-gray-50",
        className
      )}
      {...props}
    />
  );
}

export function TableHead({ className, ...props }: ThHTMLAttributes<HTMLTableCellElement>) {
  return (
    <th
      className={clsx(
        "h-12 px-4 text-left align-middle font-medium text-gray-500 [&:has([role=checkbox])]:pr-0",
        className
      )}
      {...props}
    />
  );
}

export function TableCell({ className, ...props }: TdHTMLAttributes<HTMLTableCellElement>) {
  return (
    <td
      className={clsx("p-4 align-middle [&:has([role=checkbox])]:pr-0", className)}
      {...props}
    />
  );
}
