export const THEME_KEY = "rota-theme";
export type ThemePreference = "dark" | "light" | "system";

/**
 * Roda no `<head>`, antes da primeira pintura, para a página não piscar no tema errado.
 * Sem preferência salva (ou com o storage bloqueado), vale o escuro, o padrão do produto.
 */
export const themeScript = `(function(){try{var t=localStorage.getItem("${THEME_KEY}")||"dark";var d=t==="dark"||(t==="system"&&matchMedia("(prefers-color-scheme: dark)").matches);document.documentElement.classList.toggle("dark",d)}catch(e){document.documentElement.classList.add("dark")}})()`;
