import type {
  Bug, Commande, ConfigBouton, EntreeChronologie, OrigineAffichage, OrigineApparition, Page,
  SessionEtat, StatVariante, TypeInteraction, Variante,
} from "./types";

/* Couche HTTP unique. Le navigateur parle à la même origine (Nginx ou proxy Vite) : pas de CORS. */

export class ApiError extends Error {
  constructor(public status: number, message: string) {
    super(message);
  }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      ...init,
      headers: init?.body ? { "Content-Type": "application/json", ...init.headers } : init?.headers,
    });
  } catch {
    throw new ApiError(0, "Impossible de joindre l'API.");
  }
  if (!response.ok) {
    let message = response.statusText;
    try {
      const data = await response.json();
      message = data.message ?? message;
    } catch { /* corps non JSON */ }
    throw new ApiError(response.status, message);
  }
  if (response.status === 204) return undefined as T;
  return response.json() as Promise<T>;
}

const post = <T>(url: string, body: unknown) => request<T>(url, { method: "POST", body: JSON.stringify(body) });
const now = () => new Date().toISOString();

export const api = {
  /* ── v1 ── */
  listerBugs: (params: Record<string, string | number>) =>
    request<Page<Bug>>(`/api/bugs?${new URLSearchParams(Object.entries(params).map(([k, v]) => [k, String(v)]))}`),

  creerBug: (sessionId: number, body: Omit<Bug, "id" | "session_id">) =>
    post<{ id: number }>(`/api/sessions/${sessionId}/bugs`, body),

  /* ── v2 : console ── */
  variantes: () => request<Variante[]>("/api/variantes"),
  sessions: () => request<SessionEtat[]>("/api/sessions"),
  assignerVariante: (sessionId: number, choix: { variante_id: number } | { config: ConfigBouton }) =>
    request<SessionEtat>(`/api/sessions/${sessionId}/variante`, { method: "PUT", body: JSON.stringify(choix) }),
  declencherBug: (sessionId: number, titre: string) =>
    post<Commande>(`/api/sessions/${sessionId}/commandes`, { type: "DECLENCHER_BUG", parametres: { titre } }),
  chronologie: (sessionId: number) => request<EntreeChronologie[]>(`/api/sessions/${sessionId}/chronologie`),
  suiviVariantes: (sessionId?: number) =>
    request<StatVariante[]>(`/api/suivi/variantes${sessionId ? `?session_id=${sessionId}` : ""}`),

  /* ── v2 : overlay ── */
  confirmerAffichage: (sessionId: number, b: { variante_id: number | null; config: ConfigBouton; origine: OrigineAffichage }) =>
    post<{ id: number }>(`/api/sessions/${sessionId}/affichages`, { ...b, timestamp: now() }),
  signalerApparition: (sessionId: number, b: {
    titre: string; origine: OrigineApparition; commande_id: number | null; affichage_id: number | null;
    payload: Record<string, unknown>;
  }) => post<{ id: number }>(`/api/sessions/${sessionId}/apparitions`, { ...b, timestamp: now() }),
  interaction: (sessionId: number, b: { type: TypeInteraction; affichage_id: number | null; details?: Record<string, unknown> }) =>
    post<{ id: number }>(`/api/sessions/${sessionId}/interactions`, { details: {}, ...b, timestamp: now() }),
  resultatCommande: (commandeId: number, succes: boolean, message: string | null) =>
    post<Commande>(`/api/commandes/${commandeId}/resultat`, { succes, message }),
};
