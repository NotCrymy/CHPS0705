import { onBeforeUnmount, ref, type Ref } from "vue";

export type Handlers = Record<string, (data: any) => void>;

/**
 * Ouvre un flux SSE (EventSource gère la reconnexion). Les données sont parsées en JSON.
 * Renvoie une fonction de fermeture.
 */
export function ouvrirFlux(url: string, handlers: Handlers, connecte?: Ref<boolean>) {
  const source = new EventSource(url);
  source.onopen = () => connecte && (connecte.value = true);
  source.onerror = () => connecte && (connecte.value = false);
  for (const [event, handler] of Object.entries(handlers)) {
    source.addEventListener(event, (e) => {
      try {
        handler(JSON.parse((e as MessageEvent).data));
      } catch (err) {
        console.warn(`[kapture] événement « ${event} » illisible`, err);
      }
    });
  }
  return () => {
    source.close();
    if (connecte) connecte.value = false;
  };
}

/** Version composant : fermée automatiquement au démontage. */
export function useFlux(url: string, handlers: Handlers) {
  const connecte = ref(false);
  const fermer = ouvrirFlux(url, handlers, connecte);
  onBeforeUnmount(fermer);
  return { connecte, fermer };
}
