-- Schema initial. Compatible MySQL 8 et H2 (MODE=MySQL).
-- Hibernate est en ddl-auto=validate : toute divergence avec les entites fait echouer le demarrage.

CREATE TABLE regions (
    id  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom VARCHAR(80) NOT NULL,
    CONSTRAINT uk_regions_nom UNIQUE (nom)
);

CREATE TABLE villes (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom       VARCHAR(80) NOT NULL,
    region_id BIGINT      NOT NULL,
    CONSTRAINT fk_villes_region FOREIGN KEY (region_id) REFERENCES regions (id)
);

CREATE TABLE quartiers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom            VARCHAR(80) NOT NULL,
    arrondissement VARCHAR(40),
    ville_id       BIGINT      NOT NULL,
    CONSTRAINT fk_quartiers_ville FOREIGN KEY (ville_id) REFERENCES villes (id)
);

CREATE TABLE categories (
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    libelle  VARCHAR(80) NOT NULL,
    sensible BOOLEAN     NOT NULL DEFAULT FALSE
);

CREATE TABLE utilisateurs (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nom          VARCHAR(100) NOT NULL,
    prenom       VARCHAR(100),
    telephone    VARCHAR(20)  NOT NULL,
    email        VARCHAR(150),
    mot_de_passe VARCHAR(100) NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    actif        BOOLEAN      NOT NULL DEFAULT TRUE,
    cree_le      DATETIME(6)  NOT NULL,
    CONSTRAINT uk_utilisateurs_telephone UNIQUE (telephone),
    CONSTRAINT uk_utilisateurs_email UNIQUE (email)
);

CREATE TABLE sessions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id  BIGINT      NOT NULL,
    empreinte_token VARCHAR(64) NOT NULL,
    cree_le         DATETIME(6) NOT NULL,
    expire_le       DATETIME(6) NOT NULL,
    CONSTRAINT uk_sessions_empreinte UNIQUE (empreinte_token),
    CONSTRAINT fk_sessions_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs (id) ON DELETE CASCADE
);
CREATE INDEX ix_sessions_expire ON sessions (expire_le);

CREATE TABLE dossiers (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    version              BIGINT        NOT NULL DEFAULT 0,
    reference            VARCHAR(20)   NOT NULL,
    slug                 VARCHAR(80)   NOT NULL,
    type                 VARCHAR(20)   NOT NULL,
    statut               VARCHAR(20)   NOT NULL,
    masque               BOOLEAN       NOT NULL DEFAULT FALSE,
    declarant_id         BIGINT        NOT NULL,
    quartier_id          BIGINT        NOT NULL,
    categorie_id         BIGINT,
    titre                VARCHAR(120)  NOT NULL,
    description          VARCHAR(2000),
    date_faits           DATETIME(6)   NOT NULL,
    lieu_precis          VARCHAR(255),
    photo                VARCHAR(255),
    nom_complet          VARCHAR(150),
    sexe                 VARCHAR(1),
    age                  INTEGER,
    description_physique VARCHAR(1000),
    circonstances        VARCHAR(2000),
    marque               VARCHAR(80),
    modele               VARCHAR(80),
    couleur              VARCHAR(40),
    signes_distinctifs   VARCHAR(1000),
    date_soumission      DATETIME(6)   NOT NULL,
    date_publication     DATETIME(6),
    CONSTRAINT uk_dossiers_reference UNIQUE (reference),
    CONSTRAINT uk_dossiers_slug UNIQUE (slug),
    CONSTRAINT fk_dossiers_declarant FOREIGN KEY (declarant_id) REFERENCES utilisateurs (id),
    CONSTRAINT fk_dossiers_quartier FOREIGN KEY (quartier_id) REFERENCES quartiers (id),
    CONSTRAINT fk_dossiers_categorie FOREIGN KEY (categorie_id) REFERENCES categories (id)
);
-- recherche publique et rapprochement
CREATE INDEX ix_dossiers_type_statut ON dossiers (type, statut, date_publication);
CREATE INDEX ix_dossiers_quartier ON dossiers (quartier_id);
CREATE INDEX ix_dossiers_date_faits ON dossiers (date_faits);

CREATE TABLE historique_statuts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_id     BIGINT        NOT NULL,
    auteur_id      BIGINT,
    ancien_statut  VARCHAR(20),
    nouveau_statut VARCHAR(20)   NOT NULL,
    motif          VARCHAR(1000),
    date           DATETIME(6)   NOT NULL,
    CONSTRAINT fk_historique_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_historique_auteur FOREIGN KEY (auteur_id) REFERENCES utilisateurs (id)
);
CREATE INDEX ix_historique_dossier ON historique_statuts (dossier_id);

CREATE TABLE temoignages (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    dossier_id       BIGINT        NOT NULL,
    type             VARCHAR(20)   NOT NULL,
    date_faits       DATETIME(6)   NOT NULL,
    quartier_id      BIGINT,
    lieu_precis      VARCHAR(255),
    description      VARCHAR(2000) NOT NULL,
    nom_temoin       VARCHAR(100),
    telephone_temoin VARCHAR(20)   NOT NULL,
    anonyme          BOOLEAN       NOT NULL DEFAULT TRUE,
    statut           VARCHAR(20)   NOT NULL,
    traite_par_id    BIGINT,
    date_soumission  DATETIME(6)   NOT NULL,
    CONSTRAINT fk_temoignages_dossier FOREIGN KEY (dossier_id) REFERENCES dossiers (id),
    CONSTRAINT fk_temoignages_quartier FOREIGN KEY (quartier_id) REFERENCES quartiers (id),
    CONSTRAINT fk_temoignages_traite_par FOREIGN KEY (traite_par_id) REFERENCES utilisateurs (id)
);
CREATE INDEX ix_temoignages_dossier_statut ON temoignages (dossier_id, statut);
CREATE INDEX ix_temoignages_statut ON temoignages (statut);

CREATE TABLE acces_identites (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    temoignage_id BIGINT       NOT NULL,
    moderateur_id BIGINT       NOT NULL,
    motif         VARCHAR(255) NOT NULL,
    date          DATETIME(6)  NOT NULL,
    CONSTRAINT fk_acces_temoignage FOREIGN KEY (temoignage_id) REFERENCES temoignages (id),
    CONSTRAINT fk_acces_moderateur FOREIGN KEY (moderateur_id) REFERENCES utilisateurs (id)
);

CREATE TABLE abonnements_alerte (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    utilisateur_id BIGINT      NOT NULL,
    quartier_id    BIGINT      NOT NULL,
    actif          BOOLEAN     NOT NULL DEFAULT TRUE,
    cree_le        DATETIME(6) NOT NULL,
    CONSTRAINT uk_abonnement UNIQUE (utilisateur_id, quartier_id),
    CONSTRAINT fk_abonnements_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateurs (id) ON DELETE CASCADE,
    CONSTRAINT fk_abonnements_quartier FOREIGN KEY (quartier_id) REFERENCES quartiers (id)
);
CREATE INDEX ix_abonnements_quartier ON abonnements_alerte (quartier_id, actif);

CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    destinataire_id BIGINT       NOT NULL,
    type            VARCHAR(30)  NOT NULL,
    titre           VARCHAR(150) NOT NULL,
    message         VARCHAR(500) NOT NULL,
    lien            VARCHAR(255),
    lue             BOOLEAN      NOT NULL DEFAULT FALSE,
    cree_le         DATETIME(6)  NOT NULL,
    CONSTRAINT fk_notifications_destinataire FOREIGN KEY (destinataire_id) REFERENCES utilisateurs (id) ON DELETE CASCADE
);
CREATE INDEX ix_notifications_destinataire ON notifications (destinataire_id, lue, cree_le);

CREATE TABLE correspondances (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    objet_perdu_id  BIGINT       NOT NULL,
    objet_trouve_id BIGINT       NOT NULL,
    score           INTEGER      NOT NULL,
    raisons         VARCHAR(500) NOT NULL,
    statut          VARCHAR(20)  NOT NULL,
    traitee_par_id  BIGINT,
    cree_le         DATETIME(6)  NOT NULL,
    traitee_le      DATETIME(6),
    CONSTRAINT uk_correspondance UNIQUE (objet_perdu_id, objet_trouve_id),
    CONSTRAINT fk_corresp_perdu FOREIGN KEY (objet_perdu_id) REFERENCES dossiers (id),
    CONSTRAINT fk_corresp_trouve FOREIGN KEY (objet_trouve_id) REFERENCES dossiers (id),
    CONSTRAINT fk_corresp_traitee_par FOREIGN KEY (traitee_par_id) REFERENCES utilisateurs (id)
);
CREATE INDEX ix_correspondances_statut ON correspondances (statut);
