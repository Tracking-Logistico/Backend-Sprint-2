import { ArrowRight } from "lucide-react";
import { Link } from "react-router-dom";
import { buttonVariants } from "@/components/ui/buttonVariants";
import { cn } from "@/lib/utils";

export const Footer = () => {
  return (
    <footer className="mx-auto flex max-w-7xl flex-col gap-7 px-6 py-20 md:flex-row md:items-center md:justify-between md:px-10 md:py-24">
      <div>
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-primary">
          El siguiente paso
        </p>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight text-foreground">
          Empieza a tener el control de tus envíos.
        </h2>
      </div>
      <Link
        to="/registro"
        className={cn(
          buttonVariants({ size: "lg" }),
          "w-fit gap-2 bg-primary text-primary-foreground hover:bg-primary/85",
        )}
      >
        Crear una cuenta <ArrowRight className="h-4 w-4" />
      </Link>
    </footer>
  );
};
