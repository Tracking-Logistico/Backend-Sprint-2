import type { HTMLAttributes } from "react";
import { cn } from "cn";

export function Avatar({
  className,
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "relative flex size-14 shrink-0 overflow-hidden rounded-full bg-sidebar-primary/20 text-sidebar-primary",
        className,
      )}
      {...props}
    />
  );
}

export function AvatarFallback({
  className,
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={cn(
        "flex size-full items-center justify-center font-semibold",
        className,
      )}
      {...props}
    />
  );
}
