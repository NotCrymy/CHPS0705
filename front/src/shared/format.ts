export function dateHeure(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString("fr-FR", { dateStyle: "short", timeStyle: "medium" });
}

export function heure(iso: string) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleTimeString("fr-FR") + "." + String(d.getMilliseconds()).padStart(3, "0");
}

export function duree(ms: number) {
  if (ms < 1000) return `${ms} ms`;
  const s = Math.round(ms / 1000);
  if (s < 60) return `${s} s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `${m} min ${String(s % 60).padStart(2, "0")}`;
  return `${Math.floor(m / 60)} h ${String(m % 60).padStart(2, "0")}`;
}

export const pourcent = (v: number | null) => (v == null ? "—" : `${Math.round(v * 100)} %`);

/** « il y a 12 s », pour le flux en direct. */
export function ilYa(iso: string, maintenant = Date.now()) {
  const s = Math.max(0, Math.round((maintenant - Date.parse(iso)) / 1000));
  if (s < 5) return "à l'instant";
  if (s < 60) return `il y a ${s} s`;
  const m = Math.floor(s / 60);
  if (m < 60) return `il y a ${m} min`;
  return `il y a ${Math.floor(m / 60)} h`;
}
