# KAPTURE FAIRPLAY Analytics — POC

Projet de l'équipe **Maki**. POC : insertion de bugs par API dans une base cloisonnée, puis consultation filtrée et triée. Détails dans [Specs.md](Specs.md), contrat de l'API dans [API.md](API.md).

**v2** ([docs/V2.md](docs/V2.md)) : overlay permanent et transparent par-dessus le jeu, bouton de signalement piloté à distance (variantes d'apparence pour inciter ou dissuader), échanges bidirectionnels avec le jeu (apparitions de bugs, déclenchement à la demande) et suivi de chaque variante affichée.

| Brique | Techno | Dossier |
| --- | --- | --- |
| Backend | Java 21 / Spring Boot (REST + SSE) | `api/` |
| Base de données | MariaDB (+ Adminer) | `docker-compose.yml` |
| Front-office | Vue 3 + Vite + TypeScript, servi par Nginx | `front/` |
| SDK d'intégration jeu | JS sans dépendance | `front/public/sdk/kapture-overlay.js` |

## Prérequis

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (inclut Docker Compose)
- [JDK 21](https://adoptium.net/temurin/releases/?version=21), **uniquement en mode dev** (en prod, le jar est compilé dans Docker)
- Maven : **rien à installer**, le wrapper `./mvnw` est fourni dans `api/`

Extensions VS Code conseillées :

- Extension Pack for Java (`vscjava.vscode-java-pack`)
- Spring Boot Extension Pack (`vmware.vscode-boot-dev-pack`)
- Docker (`ms-azuretools.vscode-docker`)

## Démarrage rapide (prod)

```bash
cp .env.exemple .env                      # adapter les mots de passe
docker compose --profile api up -d --build
```

Tout tourne dans Docker : MariaDB, l'API, le front (Nginx) et Adminer.

| URL | Page |
| --- | --- |
| http://localhost:8081/ | Console : pilotage des sessions (variante du bouton, déclenchement de bugs, flux en direct) |
| http://localhost:8081/suivi | Console : suivi par variante et chronologie d'une session |
| http://localhost:8081/signalements | Console : liste des bugs, filtres, tri, pagination |
| http://localhost:8081/demo/?session=42&user=Storm_X9 | Jeu de démo « Néon Drift » avec l'overlay intégré, et à droite le panneau opérateur (variantes du bouton, déclenchement de bugs, flux en direct) |
| http://localhost:8081/overlay/?session=42&user=Storm_X9 | Overlay seul (chargé dans une iframe par le SDK) |
| http://localhost:8080/api/bugs | API (JSON) |
| http://localhost:8082/ | Adminer (exploration de la base) |

> Les sessions de test sont les **41 à 48** (`db/init/02-seed.sql`). Depuis la console, le bouton « Ouvrir le jeu de démo » ouvre le jeu sur la session sélectionnée.

## Mode dev vs mode prod

Un seul réglage : `API_UPSTREAM` dans `.env` indique à Nginx à qui relayer les appels `/api/*`.

| | **Prod** (tout Docker) | **Dev** (API en local) |
| --- | --- | --- |
| API | conteneur `api` | `./mvnw spring-boot:run` (JDK 21) |
| `API_UPSTREAM` | `api:8080` (défaut) | `host.docker.internal:8080` |
| Démarrage | `docker compose --profile api up -d --build` | `docker compose up -d` + `cd api && ./mvnw spring-boot:run` |
| Après un changement Java | rebuild de l'image (~1 min) | relancer Maven (~3 s) |

### Prod — tout Docker

```bash
docker compose --profile api up -d --build   # à relancer après un changement dans api/
```

Le build est en deux étapes (`api/Dockerfile`) : Maven compile le jar dans une image de build, l'image finale ne contient que le JRE. Les dépendances Maven forment une couche de cache : modifier une classe ne re-télécharge rien.

### Dev — API en local

```bash
# 1. le front vise l'API locale
Dans `.env` : `API_UPSTREAM=host.docker.internal:8080`
docker compose up -d --force-recreate front

# 2. l'API, à relancer à chaque changement (Ctrl-C puis relancer)
cd api && ./mvnw spring-boot:run             # Windows : mvnw.cmd spring-boot:run
```

L'API lit `../.env` pour se connecter à MariaDB (`localhost:3306`).

**Port 8080 partagé** : un seul des deux peut tourner à la fois — arrêter le conteneur (`docker compose stop api`) avant de lancer `./mvnw`, et inversement. Pour repasser en prod : `docker compose --profile api up -d` (le front reprend `api:8080` après un `docker compose up -d --force-recreate front` si `API_UPSTREAM` a changé).

## Front (v2)

Projet Vite multi-pages dans `front/` :

| Page | Point d'entrée | Rôle |
| --- | --- | --- |
| Console `/` | `index.html`, `src/console/` | Pilotage, Suivi, Signalements (vue-router) |
| Overlay `/overlay/` | `overlay/index.html`, `src/overlay/` | Bouton de signalement + formulaire, fond transparent |
| Démo `/demo/` | `demo/index.html`, `src/demo/` | Jeu canvas qui intègre l'overlay via le SDK |

Le composant `src/components/ReportButton.vue` est partagé entre l'overlay et l'aperçu de la console : ce que voit l'opérateur est ce que voit le joueur.

**Développement du front** (rechargement à chaud, appels `/api` relayés vers l'API) :

```bash
cd front && npm install
npm run dev                                  # http://localhost:5173 ; API visée : http://localhost:8080
VITE_API=http://localhost:8090 npm run dev   # viser une autre API
npm run build                                # vérification des types + build dans dist/
```

En Docker, l'image du front compile le projet (`node`) puis sert `dist/` avec Nginx. Nginx désactive le tampon sur les flux SSE (`/api/flux`, `/api/sessions/{id}/flux`) et renvoie `index.html` pour les routes de la console.

### Intégrer l'overlay dans un jeu web

```html
<script src="https://<front>/sdk/kapture-overlay.js"></script>
<script>
  const kapture = KaptureOverlay.mount({
    baseUrl: "https://<front>",
    session: 42,
    user: "Storm_X9",
    getState: () => ({ niveau: 3, score: 1250 }),          // joint à chaque signalement
    onTriggerBug: (titre) => jeu.declencher(titre),        // bug demandé par la console ; false = échec
    onReportOpened: () => jeu.pause(),
    onReportClosed: () => jeu.reprendre(),
  });
  kapture.bugAppeared("BUG3", { x: 120, y: 80 });          // le jeu signale un bug apparu de lui-même
</script>
```

Le SDK pose une iframe plein écran transparente ; elle ne capte la souris que sur le bouton et le formulaire, le reste des clics atteint le jeu. Protocole `postMessage` complet : [docs/V2.md](docs/V2.md).

## API

JSON uniquement, plus deux flux SSE. Contrat complet (champs, paramètres, messages d'erreur) : [API.md](API.md) et [docs/V2.md](docs/V2.md).

| Méthode | Route | Rôle |
| --- | --- | --- |
| `POST` | `/api/sessions/{id}/bugs` | insère un bug (`affichage_id`, `apparition_id` facultatifs) → `201` + `{"id": …}` |
| `GET` | `/api/bugs` | liste filtrée, triée, paginée : `type_bug`, `titre`, `id_utilisateur`, `session_id`, `depuis`, `jusqu_a`, `tri`, `ordre`, `page`, `taille` |
| `GET` | `/api/variantes` | catalogue des variantes du bouton |
| `GET` | `/api/sessions` | sessions avec état de connexion de l'overlay et variante courante |
| `PUT` | `/api/sessions/{id}/variante` | assigne une variante (`variante_id` ou `config`), poussée en direct à l'overlay |
| `POST` | `/api/sessions/{id}/commandes` | demande au jeu de déclencher un bug (`409` si aucun overlay connecté) |
| `POST` | `/api/commandes/{id}/resultat` | l'overlay rend le résultat d'une commande |
| `POST` | `/api/sessions/{id}/affichages` · `apparitions` · `interactions` | collecte : variante affichée, bug apparu, survol / ouverture / abandon |
| `GET` | `/api/sessions/{id}/chronologie` | tout ce qui s'est passé dans la session, trié |
| `GET` | `/api/suivi/variantes` | statistiques par variante (`session_id` facultatif) |
| `GET` | `/api/sessions/{id}/flux` | SSE vers l'overlay : `variante`, `commande` |
| `GET` | `/api/flux` | SSE vers la console : `chronologie` |

Les erreurs ont un format unique `{"message": "…"}` en français. Cloisonnement : les bugs ne portent que le pseudonyme — les données nominatives de la table `testeur` ne sortent jamais par l'API.

## Commandes utiles

| Commande | Effet |
| --- | --- |
| `docker compose --profile api up -d --build` | Tout lancer, API comprise (en rebuildant) |
| `docker compose up -d` | Lancer db + front + adminer (mode dev, API lancée à part) |
| `docker compose stop api` / `start api` | Libérer / reprendre le port 8080 (bascule dev/prod) |
| `docker compose down` | Arrêter les conteneurs, **les données sont conservées** |
| `docker compose down -v` | Arrêter **et supprimer les données** (rejoue schéma + seed au démarrage) |