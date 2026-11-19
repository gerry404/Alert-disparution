-- Centre approximatif de chaque quartier, pour la carte de la fiche.
-- Volontairement au niveau du quartier : jamais d'adresse precise sur une page publique (RG14).

ALTER TABLE quartiers ADD COLUMN latitude DOUBLE;
ALTER TABLE quartiers ADD COLUMN longitude DOUBLE;

UPDATE quartiers SET latitude = 4.0480, longitude = 9.6990 WHERE nom = 'Akwa';
UPDATE quartiers SET latitude = 4.0425, longitude = 9.6890 WHERE nom = 'Bonanjo';
UPDATE quartiers SET latitude = 4.0300, longitude = 9.6950 WHERE nom = 'Bonapriso';
UPDATE quartiers SET latitude = 4.0620, longitude = 9.7080 WHERE nom = 'Deido';
UPDATE quartiers SET latitude = 4.0360, longitude = 9.7000 WHERE nom = 'Bali';
UPDATE quartiers SET latitude = 4.0380, longitude = 9.7150 WHERE nom = 'New Bell';
UPDATE quartiers SET latitude = 4.0450, longitude = 9.7250 WHERE nom = 'Nkololoun';
UPDATE quartiers SET latitude = 4.0480, longitude = 9.7370 WHERE nom = 'Ndokoti';
UPDATE quartiers SET latitude = 4.0420, longitude = 9.7700 WHERE nom = 'Logbaba';
UPDATE quartiers SET latitude = 4.0100, longitude = 9.7900 WHERE nom = 'Yassa';
UPDATE quartiers SET latitude = 4.0700, longitude = 9.8100 WHERE nom = 'PK 14';
UPDATE quartiers SET latitude = 4.0730, longitude = 9.6600 WHERE nom = 'Bonabéri';
UPDATE quartiers SET latitude = 4.0850, longitude = 9.6450 WHERE nom = 'Mabanda';
UPDATE quartiers SET latitude = 4.0640, longitude = 9.7280 WHERE nom = 'Bepanda';
UPDATE quartiers SET latitude = 4.0750, longitude = 9.7420 WHERE nom = 'Makepe';
UPDATE quartiers SET latitude = 4.0730, longitude = 9.7600 WHERE nom = 'Kotto';
UPDATE quartiers SET latitude = 4.0870, longitude = 9.7580 WHERE nom = 'Logpom';
UPDATE quartiers SET latitude = 4.0900, longitude = 9.7400 WHERE nom = 'Bonamoussadi';
UPDATE quartiers SET latitude = 4.0560, longitude = 9.7480 WHERE nom = 'Ndogbong';
UPDATE quartiers SET latitude = 3.9000, longitude = 9.6200 WHERE nom = 'Manoka';
