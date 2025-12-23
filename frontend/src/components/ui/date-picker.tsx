"use client";

import { forwardRef, ComponentPropsWithRef } from "react";
import ReactDatePicker, { registerLocale } from "react-datepicker";
import { ko } from "date-fns/locale";
import "react-datepicker/dist/react-datepicker.css";
import clsx from "clsx";

// 한국어 로케일 등록
registerLocale("ko", ko);

type ReactDatePickerPropsType = ComponentPropsWithRef<typeof ReactDatePicker>;

export type DatePickerProps = Omit<ReactDatePickerPropsType, "onChange" | "selected"> & {
  value?: string; // YYYY-MM-DD 형식
  onChange?: (date: string) => void; // YYYY-MM-DD 형식
  error?: boolean;
  label?: string;
  required?: boolean;
};

export const DatePicker = ({ value, onChange, error, label, required, className, ...props }: DatePickerProps) => {
  // YYYY-MM-DD 문자열을 Date 객체로 변환
  const dateValue = value ? new Date(`${value}T00:00:00`) : null;

  // Date 객체를 YYYY-MM-DD 문자열로 변환
  const handleChange = (date: Date | [Date | null, Date | null] | null) => {
    // 배열인 경우 (selectsRange) - 첫 번째 날짜만 사용
    if (Array.isArray(date)) {
      const firstDate = date[0];
      if (!firstDate) {
        onChange?.("");
        return;
      }
      const year = firstDate.getFullYear();
      const month = String(firstDate.getMonth() + 1).padStart(2, "0");
      const day = String(firstDate.getDate()).padStart(2, "0");
      onChange?.(`${year}-${month}-${day}`);
      return;
    }

    // 단일 날짜
    if (!date) {
      onChange?.("");
      return;
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    onChange?.(`${year}-${month}-${day}`);
  };

  return (
    <div className="flex flex-col gap-1.5">
      {label && (
        <label className="text-sm font-semibold text-slate-700">
          {label}
          {required && <span className="ml-1 text-rose-500">*</span>}
        </label>
      )}
      <ReactDatePicker
        selected={dateValue}
        onChange={handleChange as any}
        dateFormat="yyyy/MM/dd"
        locale="ko"
        placeholderText="yyyy/mm/dd"
        className={clsx(
          "w-full rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900",
          "focus:outline-none focus:ring-2 focus:ring-blue-200",
          error && "border-rose-300 focus:ring-rose-200",
          className
        )}
        calendarClassName="!font-sans"
        {...(props as any)}
      />
    </div>
  );
};
