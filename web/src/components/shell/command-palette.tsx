"use client";

import { Command } from "cmdk";
import { ClipboardCheck, FileSearch, LogOut, Monitor, Moon, Plus, Sparkles, Sun } from "lucide-react";
import { useRouter } from "next/navigation";

import { useTheme } from "@/components/theme/theme-provider";
import { navItems, settingsItem } from "@/config/navigation";
import { useProfile } from "@/lib/api/hooks";
import { skillRows } from "@/lib/domain/profile";
import { logout } from "@/lib/api/client";

const itemClass =
  "flex cursor-pointer items-center gap-3 rounded-control px-3 py-2 text-small text-foreground aria-selected:bg-accent [&_svg]:size-4 [&_svg]:text-muted-foreground";

/**
 * Paleta de comandos (⌘/Ctrl + K): navegar, trocar de tema e sair. A lista de ações cresce com as telas;
 * Ações: registrar sessão (n) e abrir check-in (c).
 */
export function CommandPalette({
  open,
  onOpenChange,
  onRegisterSession,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onRegisterSession: () => void;
}) {
  const router = useRouter();
  const { setTheme } = useTheme();
  const { data: profile } = useProfile();
  const skills = skillRows(profile?.snapshots ?? []);
  const evidences = (profile?.snapshots ?? []).flatMap((s) => s.evidences).slice(0, 40);
  const goTo = (q: string) => run(() => router.push(`/perfil/encontramos?q=${encodeURIComponent(q)}`));

  function run(action: () => void | Promise<void>) {
    onOpenChange(false);
    void action();
  }

  return (
    <Command.Dialog
      open={open}
      onOpenChange={onOpenChange}
      label="Paleta de comandos"
      overlayClassName="fixed inset-0 z-50 bg-black/60"
      contentClassName="fixed left-1/2 top-[15vh] z-50 w-[min(92vw,36rem)] -translate-x-1/2 overflow-hidden rounded-modal border border-border bg-popover text-popover-foreground shadow-overlay"
    >
      <Command.Input
        placeholder="Para onde vamos?"
        className="w-full border-b border-border bg-transparent px-4 py-3.5 text-body outline-none placeholder:text-muted-foreground"
      />
      <Command.List className="max-h-80 overflow-y-auto p-2">
        <Command.Empty className="px-3 py-6 text-center text-small text-muted-foreground">Nada encontrado.</Command.Empty>

        <Command.Group heading="Navegar" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
          {[...navItems, settingsItem].map(({ href, label, icon: Icon, shortcut }) => (
            <Command.Item key={href} value={`ir para ${label}`} onSelect={() => run(() => router.push(href))} className={itemClass}>
              <Icon strokeWidth={1.5} aria-hidden />
              <span className="flex-1">{label}</span>
              {shortcut ? <kbd className="num text-micro text-muted-foreground">g {shortcut}</kbd> : null}
            </Command.Item>
          ))}
        </Command.Group>

        <Command.Group heading="Ações" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
          <Command.Item value="registrar sessão de estudo" onSelect={() => run(onRegisterSession)} className={itemClass}>
            <Plus strokeWidth={1.5} aria-hidden />
            <span className="flex-1">Registrar sessão</span>
            <kbd className="num text-micro text-muted-foreground">n</kbd>
          </Command.Item>
          <Command.Item value="abrir check-in semanal" onSelect={() => run(() => router.push("/progresso/checkin"))} className={itemClass}>
            <ClipboardCheck strokeWidth={1.5} aria-hidden />
            <span className="flex-1">Abrir check-in</span>
            <kbd className="num text-micro text-muted-foreground">c</kbd>
          </Command.Item>
        </Command.Group>

        {skills.length > 0 ? (
          <Command.Group heading="Habilidades" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
            {skills.map((skill) => (
              <Command.Item key={skill.key} value={`habilidade ${skill.name}`} onSelect={() => goTo(skill.name)} className={itemClass}>
                <Sparkles strokeWidth={1.5} aria-hidden />
                <span className="flex-1">{skill.name}</span>
              </Command.Item>
            ))}
          </Command.Group>
        ) : null}

        {evidences.length > 0 ? (
          <Command.Group heading="Evidências" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
            {evidences.map((e) => (
              <Command.Item key={e.id} value={`evidência ${e.title}`} onSelect={() => (e.skill ? goTo(e.skill) : run(() => router.push("/perfil/encontramos")))} className={itemClass}>
                <FileSearch strokeWidth={1.5} aria-hidden />
                <span className="flex-1 truncate">{e.title}</span>
              </Command.Item>
            ))}
          </Command.Group>
        ) : null}

        <Command.Group heading="Tema" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
          <Command.Item value="tema escuro" onSelect={() => run(() => setTheme("dark"))} className={itemClass}>
            <Moon strokeWidth={1.5} aria-hidden /> Tema escuro
          </Command.Item>
          <Command.Item value="tema claro" onSelect={() => run(() => setTheme("light"))} className={itemClass}>
            <Sun strokeWidth={1.5} aria-hidden /> Tema claro
          </Command.Item>
          <Command.Item value="tema do sistema" onSelect={() => run(() => setTheme("system"))} className={itemClass}>
            <Monitor strokeWidth={1.5} aria-hidden /> Tema do sistema
          </Command.Item>
        </Command.Group>

        <Command.Group heading="Conta" className="text-micro text-muted-foreground [&_[cmdk-group-heading]]:px-3 [&_[cmdk-group-heading]]:py-1.5">
          <Command.Item
            value="sair"
            onSelect={() =>
              run(async () => {
                await logout();
                router.replace("/login");
              })
            }
            className={itemClass}
          >
            <LogOut strokeWidth={1.5} aria-hidden /> Sair
          </Command.Item>
        </Command.Group>
      </Command.List>
    </Command.Dialog>
  );
}
