"use client";

import { createContext, useCallback, useContext, useEffect, useState } from "react";

import { THEME_KEY, type ThemePreference } from "./theme-script";

type ThemeContextValue = { theme: ThemePreference; setTheme: (theme: ThemePreference) => void };

const ThemeContext = createContext<ThemeContextValue | null>(null);

function read(): ThemePreference {
  try {
    const stored = localStorage.getItem(THEME_KEY);
    return stored === "light" || stored === "system" ? stored : "dark";
  } catch {
    return "dark";
  }
}

function apply(theme: ThemePreference) {
  const dark = theme === "dark" || (theme === "system" && matchMedia("(prefers-color-scheme: dark)").matches);
  document.documentElement.classList.toggle("dark", dark);
}

/** Guarda a preferência de tema (escuro, claro ou do sistema) e aplica a classe `dark` no `<html>`. */
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<ThemePreference>("dark");

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect -- o storage só existe no navegador.
    setThemeState(read());
  }, []);

  // No modo "sistema", acompanha a mudança do sistema operacional.
  useEffect(() => {
    if (theme !== "system") return;
    const media = matchMedia("(prefers-color-scheme: dark)");
    const onChange = () => apply("system");
    media.addEventListener("change", onChange);
    return () => media.removeEventListener("change", onChange);
  }, [theme]);

  const setTheme = useCallback((next: ThemePreference) => {
    setThemeState(next);
    apply(next);
    try {
      localStorage.setItem(THEME_KEY, next);
    } catch {
      /* sem storage: vale só nesta visita */
    }
  }, []);

  return <ThemeContext value={{ theme, setTheme }}>{children}</ThemeContext>;
}

export function useTheme(): ThemeContextValue {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error("useTheme precisa estar dentro de <ThemeProvider>.");
  return ctx;
}
