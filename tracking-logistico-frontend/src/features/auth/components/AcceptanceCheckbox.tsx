import type { ReactNode } from "react";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";

interface AcceptanceCheckboxProps {
  id: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
  children: ReactNode;
}

export function AcceptanceCheckbox({
  id,
  checked,
  onCheckedChange,
  children,
}: AcceptanceCheckboxProps) {
  return (
    <div className="flex items-start gap-3">
      <Checkbox
        id={id}
        checked={checked}
        onCheckedChange={(val) => onCheckedChange(Boolean(val))}
        className="mt-0.5 border-slate-300 data-checked:border-emerald-600 data-checked:bg-emerald-600 data-checked:text-white focus-visible:ring-emerald-500/30"
      />
      <Label
        htmlFor={id}
        className="cursor-pointer text-sm leading-snug font-normal text-slate-600"
      >
        {children}
      </Label>
    </div>
  );
}
