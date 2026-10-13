import { useRef, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowUpRight, Package2 } from "lucide-react";
import { gsap } from "gsap";
import { buttonVariants } from "@/components/ui/buttonVariants";
import { cn } from "@/lib/utils";

export function Navbar() {
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  const line1Ref = useRef<HTMLSpanElement>(null);
  const line2Ref = useRef<HTMLSpanElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);

  const toggleMenu = () => {
    const opening = !isMenuOpen;
    setIsMenuOpen(opening);

    if (opening) {
      gsap.to(line1Ref.current, {
        rotate: 45,
        y: 6,
        duration: 0.3,
        ease: "power2.inOut",
      });
      gsap.to(line2Ref.current, {
        rotate: -45,
        y: -6,
        duration: 0.3,
        ease: "power2.inOut",
      });
      gsap.fromTo(
        menuRef.current,
        { autoAlpha: 0, y: -20 },
        { autoAlpha: 1, y: 0, duration: 0.35, ease: "power2.out" },
      );
    } else {
      gsap.to(line1Ref.current, {
        rotate: 0,
        y: 0,
        duration: 0.3,
        ease: "power2.inOut",
      });
      gsap.to(line2Ref.current, {
        rotate: 0,
        y: 0,
        duration: 0.3,
        ease: "power2.inOut",
      });
      gsap.to(menuRef.current, {
        autoAlpha: 0,
        y: -20,
        duration: 0.25,
        ease: "power2.in",
      });
    }
  };

  const closeMenu = () => {
    if (!isMenuOpen) return;
    toggleMenu();
  };

  return (
    <nav
      aria-label="Navegación principal"
      className="sticky top-0 z-50 w-full border-b bg-background"
    >
      <div className="flex h-18 items-center justify-between gap-6 px-4 md:px-6">
        <Link
          to="/"
          className="flex items-center gap-2"
          aria-label="LogisTrack - Ir a la página de inicio"
        >
          <Package2 className="h-6 w-6 text-emerald-500" aria-hidden="true" />
          <span className="font-bold text-xl">LogisTrack</span>
        </Link>

        <div
          className="hidden min-w-0 flex-1 items-center gap-1 overflow-x-auto whitespace-nowrap scrollbar-none md:flex [&::-webkit-scrollbar]:hidden"
          role="group"
          aria-label="Enlaces de navegación"
        >
          <a
            href="/#como-funciona"
            className="rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
          >
            Cómo funciona
          </a>
          <a
            href="/#para-quien"
            className="rounded-md px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground"
          >
            Para quién
          </a>
        </div>

        <div
          className="hidden items-center gap-2 md:flex"
          role="group"
          aria-label="Acciones de cuenta"
        >
          <Link
            to="/login"
            className={cn(buttonVariants({ variant: "ghost" }))}
          >
            Iniciar Sesión
          </Link>

          <Link to="/registro" className={cn(buttonVariants())}>
            Registrarse <ArrowUpRight className="ml-1 h-4 w-4" />
          </Link>
        </div>

        <button
          type="button"
          onClick={toggleMenu}
          aria-expanded={isMenuOpen}
          aria-controls="mobile-menu"
          aria-label={isMenuOpen ? "Cerrar menú" : "Abrir menú"}
          className="md:hidden relative h-10 w-10 flex items-center justify-center"
        >
          <span className="sr-only">
            {isMenuOpen ? "Cerrar menú" : "Abrir menú"}
          </span>
          <span className="relative block h-5 w-6">
            <span
              ref={line1Ref}
              className="absolute left-0 top-0 h-0.5 w-6 bg-foreground origin-center"
            />
            <span
              ref={line2Ref}
              className="absolute left-0 bottom-0 h-0.5 w-6 bg-foreground origin-center"
            />
          </span>
        </button>
      </div>

      <div
        id="mobile-menu"
        ref={menuRef}
        className={cn(
          "md:hidden fixed inset-0 top-18 h-[calc(100vh-4.5rem)] w-full bg-background flex flex-col items-center justify-center gap-6",
          !isMenuOpen && "pointer-events-none",
        )}
        style={{ visibility: isMenuOpen ? "visible" : "hidden", opacity: 0 }}
        aria-label="Navegación móvil"
      >
        <Link
          to="/"
          onClick={closeMenu}
          className="mb-4 flex items-center gap-2 text-lg font-semibold"
        >
          <Package2 className="h-5 w-5 text-emerald-500" aria-hidden="true" />
          LogisTrack
        </Link>

        <a
          href="/#como-funciona"
          onClick={closeMenu}
          className="w-4/5 px-4 py-2 text-center text-lg text-muted-foreground"
        >
          Cómo funciona
        </a>

        <a
          href="/#para-quien"
          onClick={closeMenu}
          className="w-4/5 px-4 py-2 text-center text-lg text-muted-foreground"
        >
          Para quién
        </a>

        <Link
          to="/login"
          onClick={closeMenu}
          className={cn(
            buttonVariants({ variant: "ghost" }),
            "text-lg w-4/5 justify-center",
          )}
        >
          Iniciar Sesión
        </Link>

        <Link
          to="/registro"
          onClick={closeMenu}
          className={cn(buttonVariants(), "text-lg w-4/5 justify-center")}
        >
          Registrarse <ArrowUpRight className="ml-1 h-4 w-4" />
        </Link>
      </div>
    </nav>
  );
}
