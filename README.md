# Alerte Disparition

Plateforme de signalement et de recherche de personnes et d'objets disparus (pilote Douala).

```
conception/   diagramme de classes (PlantUML)
backend/      API Spring Boot 4 (Java 21)
frontend/     React + Vite + Mantine
```

## Lancer le projet

Backend (port 8081) :

```
cd backend
mvnw spring-boot:run
```

Frontend (port 5173) :

```
cd frontend
npm install
npm run dev
```

Puis ouvrir http://localhost:5173

La base H2 est créée toute seule dans `backend/data/` avec des données de démo
(quartiers de Douala, catégories, quelques avis). Pour repartir de zéro : supprimer `backend/data/`.
Pour passer sur MySQL (Laragon) : voir les lignes commentées dans `backend/src/main/resources/application.properties`.

Comptes de démo :

| Rôle       | Téléphone | Mot de passe |
|------------|-----------|--------------|
| Famille    | 699000000 | demo1234     |
| Modérateur | 600000000 | moderateur   |

## Ce qui marche dans cette première version

- Consultation et recherche publique des avis (type, quartier, mots-clés), sans compte
- Inscription / connexion par numéro de téléphone
- Signalement en 4 étapes (personne, objet perdu, objet trouvé) avec photo
- Témoignage **sans compte**, anonyme par défaut (décision du groupe)
- Modération : publier / refuser un avis, publier / écarter un témoignage
- Accès aux coordonnées d'un témoin : motif obligatoire + enregistré (table `acces_identite`)
- Déclaration « retrouvé » -> En vérification -> le modérateur confirme (motif obligatoire) ou remet en ligne
- Historique de tous les changements de statut
- Mineurs : jamais affichés publiquement ; documents sensibles (CNI, carte bancaire…) : photo jamais publiée
- Partage WhatsApp / Facebook / lien

## Pas encore fait

- Alertes par zone (abonnements + notifications)
- Rapprochement automatique objets perdus / trouvés
- Administration (gestion des comptes, des quartiers, des catégories)

## API (résumé)

| Méthode | URL | Accès |
|---|---|---|
| GET | /api/dossiers?type=&quartierId=&q=&inclureResolus= | public |
| GET | /api/dossiers/{slug} | public |
| POST | /api/dossiers/{id}/temoignages | public |
| GET | /api/referentiel/quartiers, /categories, /stats | public |
| POST | /api/auth/register, /api/auth/login | public |
| GET / POST | /api/moi/dossiers (multipart) | connecté |
| POST | /api/moi/dossiers/{id}/retrouve | déclarant |
| GET | /api/moderation/dossiers, /temoignages | modérateur |
| POST | /api/moderation/dossiers/{id}/publier, rejeter, resoudre, rouvrir | modérateur |
| POST | /api/moderation/temoignages/{id}/publier, rejeter, identite | modérateur |
