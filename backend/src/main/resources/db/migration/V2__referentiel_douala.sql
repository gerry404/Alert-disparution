-- Donnees de reference du pilote : Douala (Littoral) et categories d'objets.
-- Elles font partie du schema : presentes en dev, en test et en production.

INSERT INTO regions (id, nom) VALUES (1, 'Littoral');
INSERT INTO villes (id, nom, region_id) VALUES (1, 'Douala', 1);

INSERT INTO quartiers (nom, arrondissement, ville_id) VALUES
    ('Akwa', 'Douala 1er', 1),
    ('Bonanjo', 'Douala 1er', 1),
    ('Bonapriso', 'Douala 1er', 1),
    ('Deido', 'Douala 1er', 1),
    ('Bali', 'Douala 1er', 1),
    ('New Bell', 'Douala 2e', 1),
    ('Nkololoun', 'Douala 2e', 1),
    ('Ndokoti', 'Douala 3e', 1),
    ('Logbaba', 'Douala 3e', 1),
    ('Yassa', 'Douala 3e', 1),
    ('PK 14', 'Douala 3e', 1),
    ('Bonabéri', 'Douala 4e', 1),
    ('Mabanda', 'Douala 4e', 1),
    ('Bepanda', 'Douala 5e', 1),
    ('Makepe', 'Douala 5e', 1),
    ('Kotto', 'Douala 5e', 1),
    ('Logpom', 'Douala 5e', 1),
    ('Bonamoussadi', 'Douala 5e', 1),
    ('Ndogbong', 'Douala 5e', 1),
    ('Manoka', 'Douala 6e', 1);

INSERT INTO categories (libelle, sensible) VALUES
    ('Téléphone', FALSE),
    ('Sac / Portefeuille', FALSE),
    ('Clés', FALSE),
    ('Carte nationale d''identité', TRUE),
    ('Passeport', TRUE),
    ('Carte bancaire', TRUE),
    ('Documents administratifs', TRUE),
    ('Ordinateur / Tablette', FALSE),
    ('Animal', FALSE),
    ('Autre', FALSE);
