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

/** "Recherche en cours" n'a de sens que pour une personne ou un objet perdu. */
export function libelleStatut(statut, type) {
  if (statut === 'PUBLIE' && type === 'OBJET_TROUVE') return 'Propriétaire recherché'
  if (statut === 'RESOLU' && type === 'OBJET_TROUVE') return 'Rendu'
  if (statut === 'RESOLU' && type === 'OBJET_PERDU') return 'Récupéré'
  if (statut === 'RESOLU' && type === 'PERSONNE') return 'Retrouvé(e)'
  return STATUTS[statut].libelle
}

const fmtDate = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })
const fmtDateHeure = new Intl.DateTimeFormat('fr-FR', {
  day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
})

/**
 * "2026-11-19T19:03:30.164123" -> Date. On coupe aux millisecondes : au-dela, Safari renvoie
 * "Invalid Date" et Intl.DateTimeFormat plante (ecran blanc sur iPhone).
 */
export function lireDate(iso) {
  if (!iso) return null
  const d = new Date(String(iso).replace(/(\.\d{3})\d+/, '$1'))
  return Number.isNaN(d.getTime()) ? null : d
}

export const date = (iso) => { const d = lireDate(iso); return d ? fmtDate.format(d) : '—' }
export const dateHeure = (iso) => { const d = lireDate(iso); return d ? fmtDateHeure.format(d) : '—' }

export function ilYa(iso) {
  const d = lireDate(iso)
  if (!d) return ''
  const minutes = Math.round((Date.now() - d.getTime()) / 60000)
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

/**
 * Saisie d'un numero camerounais : on garde 9 chiffres groupes comme on les lit ("699 00 00 00").
 * "+237 699...", "00237 699..." et "237699..." sont ramenes au numero national.
 */
export function formaterTelephone(saisie) {
  let chiffres = saisie.replace(/\D/g, '')
  const indicatif = /^\s*(\+|00)/.test(saisie) || chiffres.length > 9
  if (chiffres.startsWith('00237')) chiffres = chiffres.slice(5)
  else if (indicatif && chiffres.startsWith('237')) chiffres = chiffres.slice(3)
  chiffres = chiffres.slice(0, 9)
  return [chiffres.slice(0, 3), chiffres.slice(3, 5), chiffres.slice(5, 7), chiffres.slice(7, 9)]
    .filter(Boolean).join(' ')
}
