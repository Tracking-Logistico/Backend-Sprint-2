import { CheckCircle2, PackageCheck, Truck } from "lucide-react";

export const HowItWorks = () => {
  const steps = [
    {
      number: "01",
      title: "Registra tu envío",
      description:
        "Guarda la información esencial de cada paquete en segundos.",
      icon: PackageCheck,
    },
    {
      number: "02",
      title: "Actualiza el estado",
      description:
        "Mantén una línea de tiempo clara desde la salida hasta la entrega.",
      icon: Truck,
    },
    {
      number: "03",
      title: "Consulta el progreso",
      description:
        "Comparte el avance y toma decisiones con información confiable.",
      icon: CheckCircle2,
    },
  ];

  return (
    <section
      id="como-funciona"
      className="mx-auto max-w-7xl scroll-mt-24 px-6 py-20 md:px-10 md:py-28"
      aria-labelledby="process-title"
    >
      <div className="max-w-2xl">
        <p className="text-sm font-semibold uppercase tracking-[0.2em] text-primary">
          Cómo funciona
        </p>
        <h2
          id="process-title"
          className="mt-3 text-3xl font-semibold tracking-tight text-foreground md:text-5xl"
        >
          Del movimiento a la certeza.
        </h2>
        <p className="mt-5 text-lg leading-8 text-muted-foreground">
          Una experiencia directa para que el seguimiento deje de ser una
          búsqueda entre mensajes y se convierta en una decisión informada.
        </p>
      </div>
      <div className="mt-14 grid gap-px overflow-hidden rounded-2xl border bg-muted md:grid-cols-3">
        {steps.map(({ number, title, description, icon: Icon }) => (
          <article key={number} className="bg-card p-7 md:p-9">
            <div className="flex items-center justify-between">
              <span className="font-mono text-sm text-muted-foreground">{number}</span>
              <Icon className="h-6 w-6 text-primary" />
            </div>
            <h3 className="mt-16 text-xl font-semibold text-foreground">
              {title}
            </h3>
            <p className="mt-3 leading-7 text-muted-foreground">{description}</p>
          </article>
        ))}
      </div>
    </section>
  );
};
