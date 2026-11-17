-- KAPTURE v2 : overlay, variantes du bouton, échanges avec le jeu, suivi (docs/V2.md §3).
-- Joué seulement sur un volume neuf (docker-entrypoint-initdb.d), après 01-schema.sql et 02-seed.sql.
-- Sur une base existante, spring.jpa.hibernate.ddl-auto=update crée ces tables au démarrage de l'API.
-- Le catalogue de variantes est inséré par l'API au démarrage si la table est vide (§5.2).

CREATE TABLE variante (               -- catalogue de variantes du bouton
  id         INT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(40)  NOT NULL UNIQUE,     -- ex. INCITATION_FORTE
  nom        VARCHAR(100) NOT NULL,
  intention  VARCHAR(20)  NOT NULL,            -- INCITER | NEUTRE | DISSUADER
  config     JSON         NOT NULL             -- ConfigBouton (§5.1)
);

CREATE TABLE affichage (              -- chaque variante effectivement affichée par l'overlay
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id   INT         NOT NULL,
  variante_id  INT         NULL,               -- NULL = configuration personnalisée
  config       JSON        NOT NULL,           -- copie exacte de ce qui a été rendu
  origine      VARCHAR(20) NOT NULL,           -- DEFAUT | OPERATEUR
  debut        DATETIME(3) NOT NULL,
  fin          DATETIME(3) NULL,               -- rempli au changement de variante ou à la déconnexion
  FOREIGN KEY (session_id)  REFERENCES session(id),
  FOREIGN KEY (variante_id) REFERENCES variante(id)
);

CREATE TABLE commande (               -- ordre envoyé au jeu par l'application
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id  INT          NOT NULL,
  type        VARCHAR(30)  NOT NULL,           -- DECLENCHER_BUG
  parametres  JSON         NOT NULL,           -- { "titre": "BUG3" }
  statut      VARCHAR(20)  NOT NULL,           -- ENVOYEE | EXECUTEE | ECHEC
  message     VARCHAR(200) NULL,               -- raison d'un échec
  cree_a      DATETIME(3)  NOT NULL,
  traite_a    DATETIME(3)  NULL,
  FOREIGN KEY (session_id) REFERENCES session(id)
);

CREATE TABLE apparition (             -- bug apparu dans le jeu
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id    INT          NOT NULL,
  titre         VARCHAR(100) NOT NULL,         -- BUG1 … BUG5
  origine       VARCHAR(20)  NOT NULL,         -- JEU (spontané) | COMMANDE (déclenché par l'application)
  commande_id   BIGINT       NULL,
  affichage_id  BIGINT       NULL,             -- variante affichée à cet instant
  payload       JSON         NULL,             -- état du jeu
  timestamp     DATETIME(3)  NOT NULL,
  FOREIGN KEY (session_id)   REFERENCES session(id),
  FOREIGN KEY (commande_id)  REFERENCES commande(id),
  FOREIGN KEY (affichage_id) REFERENCES affichage(id)
);

CREATE TABLE interaction (            -- comportement du joueur face au bouton
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  session_id    INT          NOT NULL,
  affichage_id  BIGINT       NULL,
  type          VARCHAR(20)  NOT NULL,         -- SURVOL | OUVERTURE | ANNULATION
  details       JSON         NULL,
  timestamp     DATETIME(3)  NOT NULL,
  FOREIGN KEY (session_id)   REFERENCES session(id),
  FOREIGN KEY (affichage_id) REFERENCES affichage(id)
);

ALTER TABLE evenement
  ADD COLUMN affichage_id  BIGINT NULL,        -- variante affichée quand le joueur a signalé
  ADD COLUMN apparition_id BIGINT NULL,        -- apparition que le signalement couvre (si connue)
  ADD FOREIGN KEY (affichage_id)  REFERENCES affichage(id),
  ADD FOREIGN KEY (apparition_id) REFERENCES apparition(id);
