/** Um template remonta a cada navegação: o conteúdo entra com um fade curto (200 ms). `prefers-reduced-motion` o desliga (globals.css). */
export default function DashboardTemplate({ children }: { children: React.ReactNode }) {
  return <div className="animate-in fade-in-0 slide-in-from-bottom-1 duration-200">{children}</div>;
}
