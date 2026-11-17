package fr.kapture.api.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import fr.kapture.api.dto.EntreeChronologie;
import jakarta.annotation.PreDestroy;

/**
 * Registre des flux Server-Sent Events (v2, {@code docs/V2.md} §5.3) et
 * publication des événements.
 *
 * <ul>
 *   <li><b>Overlays</b> : émetteurs regroupés par session
 *       ({@code GET /api/sessions/{id}/flux}), reçoivent {@code variante} et
 *       {@code commande}.</li>
 *   <li><b>Consoles</b> : {@code GET /api/flux}, reçoivent {@code chronologie}.</li>
 * </ul>
 *
 * <p>Thread-safety : listes {@link CopyOnWriteArrayList} dans une
 * {@link ConcurrentHashMap}, envois synchronisés par émetteur, retrait
 * idempotent ({@link AtomicBoolean}). Un émetteur mort est retiré sur
 * {@code onCompletion} / {@code onTimeout} / {@code onError} ou dès qu'un
 * envoi lève une {@link IOException} ; le ping {@code :ping} toutes les 15 s
 * sert aussi à détecter les clients partis (le serveur ne le voit qu'en
 * écrivant).</p>
 *
 * <p>Le traitement de retrait fourni par l'appelant (fermer l'affichage
 * ouvert, publier {@code CONNEXION}) est exécuté sur un thread dédié : il ne
 * tourne jamais dans le contexte transactionnel d'une requête (en
 * particulier pas dans un {@code afterCommit}, où un accès JPA rejoindrait
 * une transaction déjà validée).</p>
 */
@Service
public class FluxService {

    private static final Logger LOG = LoggerFactory.getLogger(FluxService.class);

    /** Intervalle du commentaire {@code :ping}. */
    static final long PERIODE_PING_MS = 15_000;

    private final Map<Integer, List<Abonne>> overlays = new ConcurrentHashMap<>();
    private final List<Abonne> consoles = new CopyOnWriteArrayList<>();

    /** Traitements de retrait, exécutés hors des threads de requête, dans l'ordre. */
    private final ExecutorService retraits = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "flux-retraits");
        thread.setDaemon(true);
        return thread;
    });

    /** Un client SSE : son émetteur, sa session (null pour une console) et le traitement à son retrait. */
    private record Abonne(SseEmitter emetteur, Integer sessionId, Runnable surRetrait, AtomicBoolean retire) {
    }

    /**
     * Inscrit un overlay de la session et lui envoie tout de suite
     * l'événement {@code variante} (variante courante). {@code surRetrait}
     * est appelé (sur un thread dédié) quand cet overlay disparaît, une seule
     * fois.
     */
    public SseEmitter abonnerOverlay(Integer sessionId, Object varianteInitiale, Runnable surRetrait) {
        Abonne abonne = new Abonne(new SseEmitter(0L), sessionId, surRetrait, new AtomicBoolean());
        overlays.computeIfAbsent(sessionId, id -> new CopyOnWriteArrayList<>()).add(abonne);
        brancherCallbacks(abonne);
        envoyer(abonne, evenement("variante", varianteInitiale));
        return abonne.emetteur();
    }

    /** Inscrit une console ; un commentaire initial ouvre la réponse tout de suite. */
    public SseEmitter abonnerConsole() {
        Abonne abonne = new Abonne(new SseEmitter(0L), null, null, new AtomicBoolean());
        consoles.add(abonne);
        brancherCallbacks(abonne);
        envoyer(abonne, SseEmitter.event().comment("connecte"));
        return abonne.emetteur();
    }

    /** Nombre d'overlays connectés à une session. */
    public int nombreOverlays(Integer sessionId) {
        List<Abonne> liste = overlays.get(sessionId);
        return liste == null ? 0 : liste.size();
    }

    /** Envoie un événement nommé à tous les overlays d'une session, après commit. */
    public void publierAuxOverlays(Integer sessionId, String nom, Object donnee) {
        apresCommit(() -> {
            List<Abonne> liste = overlays.get(sessionId);
            if (liste != null) {
                liste.forEach(a -> envoyer(a, evenement(nom, donnee)));
            }
        });
    }

    /**
     * Publie une entrée de chronologie aux consoles, après commit : la
     * console ne peut pas relire une ligne pas encore validée.
     */
    public void publierChronologie(EntreeChronologie entree) {
        apresCommit(() -> consoles.forEach(a -> envoyer(a, evenement("chronologie", entree))));
    }

    /** Commentaire {@code :ping} à tous les clients toutes les 15 s. */
    @Scheduled(fixedRate = PERIODE_PING_MS, initialDelay = PERIODE_PING_MS)
    public void ping() {
        consoles.forEach(a -> envoyer(a, SseEmitter.event().comment("ping")));
        overlays.values().forEach(liste -> liste.forEach(a -> envoyer(a, SseEmitter.event().comment("ping"))));
    }

    @PreDestroy
    void arreter() {
        retraits.shutdownNow();
    }

    /**
     * Exécute l'action après le commit de la transaction courante, ou tout de
     * suite s'il n'y en a pas.
     */
    static void apresCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    private static SseEmitter.SseEventBuilder evenement(String nom, Object donnee) {
        return SseEmitter.event().name(nom).data(donnee, MediaType.APPLICATION_JSON);
    }

    private void brancherCallbacks(Abonne abonne) {
        abonne.emetteur().onCompletion(() -> retirer(abonne));
        abonne.emetteur().onTimeout(() -> retirer(abonne));
        abonne.emetteur().onError(erreur -> retirer(abonne));
    }

    /** Envoi protégé : une {@link IOException} signifie un client parti → retrait. */
    private void envoyer(Abonne abonne, SseEmitter.SseEventBuilder evenement) {
        if (abonne.retire().get()) {
            return;
        }
        try {
            synchronized (abonne) {
                abonne.emetteur().send(evenement);
            }
        } catch (IOException | IllegalStateException e) {
            retirer(abonne);
            try {
                abonne.emetteur().completeWithError(e);
            } catch (RuntimeException ignoree) {
                // émetteur déjà terminé
            }
        }
    }

    /** Retrait idempotent, puis traitement de l'appelant sur le thread dédié. */
    private void retirer(Abonne abonne) {
        if (!abonne.retire().compareAndSet(false, true)) {
            return;
        }
        if (abonne.sessionId() == null) {
            consoles.remove(abonne);
            return;
        }
        List<Abonne> liste = overlays.get(abonne.sessionId());
        if (liste != null) {
            liste.remove(abonne);
        }
        if (abonne.surRetrait() != null) {
            retraits.execute(() -> {
                try {
                    abonne.surRetrait().run();
                } catch (RuntimeException e) {
                    LOG.warn("Traitement de déconnexion de la session {} en échec", abonne.sessionId(), e);
                }
            });
        }
    }
}
