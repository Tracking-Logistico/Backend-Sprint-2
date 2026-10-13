interface PasswordStrengthMeterProps {
  label: string;
  width: string;
}

export function PasswordStrengthMeter({
  label,
  width,
}: PasswordStrengthMeterProps) {
  const colorClass =
    label === "Fuerte"
      ? "bg-emerald-600"
      : label === "Media"
        ? "bg-amber-500"
        : label === "Débil"
          ? "bg-rose-500"
          : "bg-slate-300";

  return (
    <div className="space-y-1.5">
      <div className="flex justify-between text-xs text-slate-500">
        <span>Fortaleza de contraseña</span>
        <span className="font-medium text-slate-700">{label}</span>
      </div>
      <div
        role="progressbar"
        aria-label="Fortaleza de contraseña"
        aria-valuemin={0}
        aria-valuemax={100}
        aria-valuetext={label}
        className="h-1.5 w-full overflow-hidden rounded-full bg-slate-100"
      >
        <div
          className={`h-full transition-all duration-300 ${colorClass}`}
          style={{ width }}
        />
      </div>
    </div>
  );
}
