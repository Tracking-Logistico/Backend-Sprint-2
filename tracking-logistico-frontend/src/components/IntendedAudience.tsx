import { MapPin, Users } from "lucide-react";

export const IntendedAudience = () => {
  return (
    <section
      id="para-quien"
      className="scroll-mt-24 border-y bg-muted"
      aria-labelledby="audience-title"
    >
      <div className="mx-auto grid max-w-7xl gap-12 px-6 py-20 md:px-10 md:py-24 lg:grid-cols-[0.8fr_1.2fr] lg:items-center">
        <div>
          <p className="text-sm font-semibold uppercase tracking-[0.2em] text-primary">
            Para quién es
          </p>
          <h2
            id="audience-title"
            className="mt-3 text-3xl font-semibold tracking-tight text-foreground md:text-4xl"
          >
            Diseñado para coordinar, no para complicar.
          </h2>
        </div>
        <div className="grid gap-8 sm:grid-cols-2">
          <div className="border-l-2 border-primary pl-5">
            <Users className="h-6 w-6 text-primary" />
            <h3 className="mt-5 font-semibold text-foreground">
              Personas y equipos
            </h3>
            <p className="mt-2 leading-7 text-muted-foreground">
              Para quienes necesitan consultar envíos sin depender de procesos
              dispersos.
            </p>
          </div>
          <div className="border-l-2 border-primary pl-5">
            <MapPin className="h-6 w-6 text-primary" />
            <h3 className="mt-5 font-semibold text-foreground">
              Operaciones logísticas
            </h3>
            <p className="mt-2 leading-7 text-muted-foreground">
              Para centralizar estados y ofrecer una comunicación más clara a
              cada cliente.
            </p>
          </div>
        </div>
      </div>
    </section>
  );
};
