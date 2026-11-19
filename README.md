# Alerte Disparition

Plateforme de signalement et de recherche de personnes et d'objets disparus (pilote Douala).

```
conception/   diagramme de classes (PlantUML)
backend/      API Spring Boot 4 / Java 21 (H2 en dev, MySQL en prod, Flyway)
frontend/     React 19 + Vite + Mantine
```

## Lancer

```
cd backend  && mvnw spring-boot:run      # http://localhost:8081
cd frontend && npm install && npm run dev  # http://localhost:5173
```

Profil `dev` par défaut : base H2 dans `backend/data/`, schéma créé par Flyway, données de démo chargées
au premier démarrage. Pour repartir de zéro : supprimer `backend/data/`.

MySQL : `mvnw spring-boot:run -Dspring-boot.run.profiles=mysql` (variables `DB_URL`, `DB_USER`, `DB_PASSWORD`).

| Compte de démo | Téléphone | Mot de passe |
|---|---|---|
| Famille (Claire) | 699000000 | demo1234 |
| Famille (Yves, suit Ndokoti et Logbaba) | 677000000 | demo1234 |
| Modérateur | 600000000 | moderateur |
| Administrateur | 600000001 | admin1234 |

## Tests

```
cd backend && mvnw test
```

`CriteresAcceptationTest` rejoue les scénarios CA01 à CA15 du cahier des charges sur l'API réelle
(H2 + migrations). Tests unitaires : machine à états des statuts, moteur de rapprochement.

## Architecture du backend

Découpage **par fonctionnalité** : chaque package contient son entité, son repository, son service et son contrôleur.

| Package | Rôle |
|---|---|
| `dossier` | Agrégat `Dossier` : machine à états (`StatutDossier.suivants()`), historique écrit dans la même opération, verrou optimiste (`@Version`) |
| `temoignage` | Témoignage **sans compte**, anonyme par défaut ; accès à l'identité avec motif + journal |
| `alerte` | Abonnements par quartier, notifications, `CanalNotification` (SMS/WhatsApp = une nouvelle implémentation) |
| `rapprochement` | `MoteurRapprochement` (score explicable, sans dépendance Spring) + validation par un modérateur |
| `securite` | Sessions opaques : seule l'empreinte SHA-256 du token est stockée, expiration 30 jours |
| `utilisateur` | Inscription / connexion par téléphone, administration des comptes |
| `commun` | Erreurs métier → ProblemDetail (RFC 9457), pagination, limiteur de débit |

Choix notables :

- **Événements de domaine** : la publication d'un dossier émet `DossierPublie` ; les modules `alerte` et
  `rapprochement` réagissent *après le commit*. Une notification en échec n'annule jamais une décision.
- **Schéma versionné** : Flyway crée les tables, Hibernate ne fait que valider (`ddl-auto=validate`).
- **Anti-abus** : limitation des connexions, inscriptions et témoignages (par IP et par numéro) ;
  le format des photos est vérifié sur les octets du fichier, pas sur le Content-Type.
- **Protection des personnes** : mineurs jamais publics, photo jamais publiée pour un document sensible,
  lieu précis et coordonnées jamais exposés, temps de réponse identique que le numéro existe ou non.

## Règles du cahier des charges couvertes

RG01 à RG16, avec un écart assumé : **témoigner ne demande pas de compte** (décision du groupe ; le cahier
des charges l'excluait du MVP). Le numéro du témoin est obligatoire pour pouvoir le rappeler, mais jamais affiché.

## API

| Méthode | URL | Accès |
|---|---|---|
| GET | `/api/dossiers?type&quartierId&q&inclureResolus&page&taille` | public |
| GET | `/api/dossiers/{slug}` · `/api/dossiers/{id}/temoignages` | public |
| POST | `/api/dossiers/{id}/temoignages` | public (limité) |
| GET | `/api/referentiel/quartiers` · `/categories` · `/stats` | public |
| POST | `/api/auth/inscription` · `/connexion` · `/deconnexion` | — |
| GET/POST | `/api/moi/dossiers` (multipart) · `POST /api/moi/dossiers/{id}/retrouve` | connecté |
| GET/POST/PATCH/DELETE | `/api/moi/alertes` | connecté |
| GET/POST | `/api/moi/notifications` · `/non-lues` · `/{id}/lue` · `/lues` | connecté |
| GET/POST | `/api/moderation/dossiers` · `/{id}/{publier\|rejeter\|resoudre\|rouvrir\|masquer\|afficher}` | modérateur |
| GET/POST | `/api/moderation/temoignages` · `/{id}/{publier\|rejeter\|masquer\|identite}` | modérateur |
| GET/POST | `/api/moderation/correspondances` · `/{id}/{valider\|ecarter}` | modérateur |
| GET/PATCH | `/api/admin/utilisateurs` · `GET /api/admin/journal-acces` | admin |

## Pistes suivantes

- Canal SMS réel (après arbitrage du coût), envoi asynchrone avec reprise sur échec
- Tests front (Vitest + Testing Library) et CI GitHub Actions
- Diagrammes de cas d'utilisation et de séquence
