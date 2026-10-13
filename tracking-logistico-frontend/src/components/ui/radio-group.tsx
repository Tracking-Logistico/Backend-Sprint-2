import { cn } from "cn";

type RadioGroupProps = React.ComponentProps<"div">;

function RadioGroup({ className, children, ...props }: RadioGroupProps) {
  return (
    <div role="radiogroup" className={cn("grid gap-3", className)} {...props}>
      {children}
    </div>
  );
}

interface RadioGroupItemProps extends React.ComponentProps<"input"> {
  value: string;
}

function RadioGroupItem({ className, ...props }: RadioGroupItemProps) {
  return (
    <input
      type="radio"
      className={cn("size-4 accent-emerald-700", className)}
      {...props}
    />
  );
}

export { RadioGroup, RadioGroupItem };
