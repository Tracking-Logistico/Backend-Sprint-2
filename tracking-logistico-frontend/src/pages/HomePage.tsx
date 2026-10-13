import { Header } from "@/components/Header";
import { HowItWorks } from "@/components/HowItWorks";
import { IntendedAudience } from "@/components/IntendedAudience";
import { Footer } from "@/components/Footer";

export const HomePage = () => {
  return (
    <main>
      <Header />
      <HowItWorks />
      <IntendedAudience />
      <Footer />
    </main>
  );
};
