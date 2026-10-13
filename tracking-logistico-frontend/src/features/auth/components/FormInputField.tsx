import type { InputHTMLAttributes, ReactNode } from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { cn } from "cn";

interface FormInputFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  name: string;
  error?: string;
  hint?: ReactNode;
  endAdornment?: ReactNode;
}

export function FormInputField({
  label,
  name,
  error,
  hint,
  id,
  className,
  endAdornment,
  ...props
}: FormInputFieldProps) {
  const inputId = id ?? name;

  return (
    <div className="space-y-2">
      <Label htmlFor={inputId} className="text-sm font-medium text-slate-800">
        {label}
        {props.required && (
          <span className="ml-1 text-rose-500" aria-hidden="true">
            *
          </span>
        )}
      </Label>
      <div className="relative">
        <Input
          id={inputId}
          name={name}
          aria-invalid={Boolean(error)}
          aria-describedby={
            error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined
          }
          className={cn(
            "h-11 rounded-lg border-slate-200 bg-white px-3 text-sm text-slate-950 placeholder:text-slate-400 focus-visible:border-emerald-500 focus-visible:ring-emerald-500/20",
            endAdornment && "pr-10",
            error &&
              "border-rose-400 focus-visible:border-rose-500 focus-visible:ring-rose-500/20",
            className,
          )}
          {...props}
        />
        {endAdornment && (
          <div className="absolute right-3 top-1/2 -translate-y-1/2">
            {endAdornment}
          </div>
        )}
      </div>
      {hint && !error && (
        <p id={`${inputId}-hint`} className="text-xs text-slate-500">
          {hint}
        </p>
      )}
      {error && (
        <p
          id={`${inputId}-error`}
          className="text-xs font-medium text-rose-600"
        >
          {error}
        </p>
      )}
    </div>
  );
}
