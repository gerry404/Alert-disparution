-- A lancer UNE fois avec root (Laragon : root sans mot de passe) :
--   mysql -uroot < scripts/creer-base.sql
-- Utilisateur dedie : l'application ne se connecte jamais en root.

CREATE DATABASE IF NOT EXISTS alerte_disparition CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'alerte'@'localhost' IDENTIFIED BY 'alerte';
CREATE USER IF NOT EXISTS 'alerte'@'127.0.0.1' IDENTIFIED BY 'alerte';
CREATE USER IF NOT EXISTS 'alerte'@'::1' IDENTIFIED BY 'alerte';

GRANT ALL PRIVILEGES ON alerte_disparition.* TO 'alerte'@'localhost', 'alerte'@'127.0.0.1', 'alerte'@'::1';
FLUSH PRIVILEGES;
