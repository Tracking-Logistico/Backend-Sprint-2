import { ArrowRight, Clock3, PackageCheck, ShieldCheck } from "lucide-react";
import { buttonVariants } from "@/components/ui/buttonVariants";
import { cn } from "@/lib/utils";
import { Link } from "react-router-dom";

export const Header = () => {
  return (
    <header
      className="overflow-hidden border-b bg-sidebar text-sidebar-foreground"
      aria-labelledby="hero-title"
    >
      <div className="mx-auto grid max-w-7xl gap-14 px-6 py-20 md:px-10 md:py-28 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:gap-20">
        <div className="max-w-2xl">
          <p className="mb-6 flex items-center gap-3 text-sm font-semibold uppercase tracking-[0.22em] text-sidebar-primary">
            <span className="h-px w-10 bg-sidebar-primary" />
            Visibilidad que avanza contigo
          </p>
          <h1
            id="hero-title"
            className="max-w-3xl text-5xl font-semibold tracking-tight md:text-7xl"
          >
            Cada paquete tiene una historia.{" "}
            <span className="text-sidebar-primary">Síguela.</span>
          </h1>
          <p className="mt-7 max-w-xl text-lg leading-8 text-sidebar-foreground/75">
            LogisTrack reúne en un solo lugar el estado de tus envíos para que
            sepas qué está pasando, dónde está y cuál es el siguiente paso.
          </p>
          <div className="mt-9 flex flex-col gap-3 sm:flex-row">
            <Link
              to="/registro"
              className={cn(
                buttonVariants({ size: "lg" }),
                "gap-2 bg-sidebar-primary text-sidebar-primary-foreground hover:bg-sidebar-primary/85",
              )}
            >
              Comenzar ahora <ArrowRight className="h-4 w-4" />
            </Link>
            <a
              href="#como-funciona"
              className={cn(
                buttonVariants({ variant: "outline", size: "lg" }),
                "border-sidebar-border bg-transparent text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground",
              )}
            >
              Conocer el proceso
            </a>
          </div>
          <div className="mt-12 flex flex-wrap gap-x-7 gap-y-3 text-sm text-sidebar-foreground/60">
            <span className="flex items-center gap-2">
              <ShieldCheck className="h-4 w-4 text-sidebar-primary" /> Información
              centralizada
            </span>
            <span className="flex items-center gap-2">
              <Clock3 className="h-4 w-4 text-sidebar-primary" /> Seguimiento simple
            </span>
          </div>
        </div>

        <div className="relative mx-auto w-full max-w-lg lg:mx-0 lg:justify-self-end">
          <div className="absolute -right-20 -top-20 h-64 w-64 rounded-full border border-sidebar-primary/20" />
          <div className="relative overflow-hidden rounded-2xl border border-sidebar-border bg-sidebar-accent shadow-2xl">
            <div className="flex items-center justify-between border-b border-sidebar-border px-5 py-4">
              <span className="text-sm font-medium text-sidebar-foreground/75">
                Vista del envío
              </span>
              <span className="flex items-center gap-2 text-xs text-sidebar-primary">
                <span className="h-2 w-2 rounded-full bg-sidebar-primary" /> En
                tránsito
              </span>
            </div>
            <div className="p-6">
              <div className="flex items-start justify-between gap-5">
                <div>
                  <p className="text-xs uppercase tracking-widest text-sidebar-foreground/50">
                    Código de seguimiento
                  </p>
                  <p className="mt-2 font-mono text-lg text-sidebar-foreground">
                    LGT-4829-MX
                  </p>
                </div>
                <PackageCheck className="h-7 w-7 text-sidebar-primary" />
              </div>
              <div className="my-8 flex items-center gap-3 text-sm">
                <div className="h-3 w-3 rounded-full bg-sidebar-primary ring-4 ring-sidebar-primary/15" />
                <div className="h-px flex-1 bg-gradient-to from-sidebar-primary via-sidebar-primary/60 to-sidebar-border" />
                <div className="h-3 w-3 rounded-full border border-sidebar-border bg-sidebar-accent" />
              </div>
              <div className="flex justify-between text-sm">
                <div>
                  <p className="text-sidebar-foreground/50">Origen</p>
                  <p className="mt-1 text-sidebar-foreground/85">Bogotá</p>
                </div>
                <div className="text-right">
                  <p className="text-sidebar-foreground/50">Destino</p>
                  <p className="mt-1 text-sidebar-foreground/85">Medellín</p>
                </div>
              </div>
              <div className="mt-7 grid grid-cols-2 gap-3">
                <div className="rounded-lg bg-sidebar/70 p-3">
                  <p className="text-xs text-sidebar-foreground/50">Última actualización</p>
                  <p className="mt-1 text-sm text-sidebar-foreground/85">Hace 18 min</p>
                </div>
                <div className="rounded-lg bg-sidebar/70 p-3">
                  <p className="text-xs text-sidebar-foreground/50">Entrega estimada</p>
                  <p className="mt-1 text-sm text-sidebar-foreground/85">24 Jun, 2025</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
};
