// Libelles et petites fonctions d'affichage

export const TYPES = {
  PERSONNE: { libelle: 'Personne disparue', surtitre: 'Disparition', couleur: 'brique' },
  OBJET_PERDU: { libelle: 'Objet perdu', surtitre: 'Objet perdu', couleur: 'blue' },
  OBJET_TROUVE: { libelle: 'Objet trouvé', surtitre: 'Objet trouvé', couleur: 'teal' },
}

export const STATUTS = {
  EN_ATTENTE: { libelle: 'En attente', couleur: 'yellow' },
  PUBLIE: { libelle: 'Recherche en cours', couleur: 'brique' },
  EN_VERIFICATION: { libelle: 'En vérification', couleur: 'grape' },
  RESOLU: { libelle: 'Résolu', couleur: 'teal' },
  REJETE: { libelle: 'Rejeté', couleur: 'gray' },
}

const fmtDate = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
const fmtDateHeure = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

export const date = (iso) => (iso ? fmtDate.format(new Date(iso)) : '—')
export const dateHeure = (iso) => (iso ? fmtDateHeure.format(new Date(iso)) : '—')

export function ilYa(iso) {
  if (!iso) return ''
  const minutes = Math.round((Date.now() - new Date(iso).getTime()) / 60000)
  if (minutes < 60) return `il y a ${Math.max(minutes, 1)} min`
  const heures = Math.round(minutes / 60)
  if (heures < 24) return `il y a ${heures} h`
  const jours = Math.round(heures / 24)
  if (jours < 30) return `il y a ${jours} jour${jours > 1 ? 's' : ''}`
  return `le ${date(iso)}`
}

export function initiales(texte = '') {
  return texte
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((m) => m[0].toUpperCase())
    .join('')
}

// valeur pour un <input type="datetime-local"> (heure locale, sans secondes)
export function maintenantLocal() {
  const d = new Date()
  d.setMinutes(d.getMinutes() - d.getTimezoneOffset())
  return d.toISOString().slice(0, 16)
}
