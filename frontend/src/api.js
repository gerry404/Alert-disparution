// Client de l'API Spring. Le token de session est garde dans le localStorage.

const CLE_TOKEN = 'ad_token'

export function getToken() {
  try {
    return localStorage.getItem(CLE_TOKEN)
  } catch {
    return null
  }
}

export function setToken(token) {
  try {
    if (token) localStorage.setItem(CLE_TOKEN, token)
    else localStorage.removeItem(CLE_TOKEN)
  } catch {
    /* navigation privee : la session ne survivra pas au rechargement */
  }
}

/** Erreur API au format ProblemDetail : { status, detail, code, erreurs } */
export class ApiError extends Error {
  constructor(status, message, code, erreurs) {
    super(message)
    this.status = status
    this.code = code
    this.erreurs = erreurs || {}
  }
}

const MESSAGES = {
  401: 'Votre session a expiré. Reconnectez-vous.',
  403: 'Vous n’avez pas accès à cette action.',
  429: 'Trop de tentatives. Réessayez dans quelques minutes.',
  500: 'Le serveur a rencontré un problème. Réessayez.',
}

async function requete(methode, url, corps, { signal } = {}) {
  const headers = { Accept: 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let body
  if (corps instanceof FormData) body = corps
  else if (corps !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corps)
  }

  let res
  try {
    res = await fetch(`/api${url}`, { method: methode, headers, body, signal })
  } catch (e) {
    if (e.name === 'AbortError') throw e
    throw new ApiError(0, 'Connexion impossible. Vérifiez votre réseau.', 'RESEAU')
  }

  const texte = await res.text()
  let data = null
  try {
    data = texte ? JSON.parse(texte) : null
  } catch {
    /* corps non JSON */
  }

  if (!res.ok) {
    if (res.status === 401 && token) {
      setToken(null)
      window.dispatchEvent(new Event('session-expiree'))
    }
    throw new ApiError(res.status, data?.detail || MESSAGES[res.status] || 'Une erreur est survenue.',
      data?.code, data?.erreurs)
  }
  return data
}

const api = {
  get: (url, opts) => requete('GET', url, undefined, opts),
  post: (url, corps) => requete('POST', url, corps ?? {}),
  patch: (url, corps) => requete('PATCH', url, corps),
  delete: (url) => requete('DELETE', url),
}

function qs(params) {
  const p = new URLSearchParams()
  Object.entries(params).forEach(([k, v]) => {
    if (v !== '' && v !== null && v !== undefined && v !== false) p.set(k, v)
  })
  const s = p.toString()
  return s ? `?${s}` : ''
}

// ------------------------------------------------------------------ metier

export const Auth = {
  connexion: (telephone, motDePasse) => api.post('/auth/connexion', { telephone, motDePasse }),
  inscription: (donnees) => api.post('/auth/inscription', donnees),
  moi: () => api.get('/auth/moi'),
  deconnexion: () => api.post('/auth/deconnexion'),
}

export const Dossiers = {
  rechercher: (filtres = {}, opts) => api.get(`/dossiers${qs(filtres)}`, opts),
  fiche: (slug) => api.get(`/dossiers/${encodeURIComponent(slug)}`),
  temoignages: (id) => api.get(`/dossiers/${id}/temoignages`),
  temoigner: (id, donnees) => api.post(`/dossiers/${id}/temoignages`, donnees),
  mesDossiers: () => api.get('/moi/dossiers'),
  signaler: (formData) => api.post('/moi/dossiers', formData),
  declarerRetrouve: (id, motif) => api.post(`/moi/dossiers/${id}/retrouve`, { motif }),
}

export const Referentiel = {
  quartiers: () => api.get('/referentiel/quartiers'),
  categories: () => api.get('/referentiel/categories'),
  stats: () => api.get('/referentiel/stats'),
}

export const Alertes = {
  zones: () => api.get('/moi/alertes'),
  ajouter: (quartierId) => api.post('/moi/alertes', { quartierId }),
  activer: (id, actif) => api.patch(`/moi/alertes/${id}`, { actif }),
  supprimer: (id) => api.delete(`/moi/alertes/${id}`),
  notifications: (page = 0) => api.get(`/moi/notifications?page=${page}`),
  nonLues: () => api.get('/moi/notifications/non-lues'),
  marquerLue: (id) => api.post(`/moi/notifications/${id}/lue`),
  toutMarquerLu: () => api.post('/moi/notifications/lues'),
}

export const Moderation = {
  dossiers: () => api.get('/moderation/dossiers'),
  decider: (id, decision, motif) => api.post(`/moderation/dossiers/${id}/${decision}`, { motif }),
  temoignages: () => api.get('/moderation/temoignages'),
  traiterTemoignage: (id, action) => api.post(`/moderation/temoignages/${id}/${action}`),
  identite: (id, motif) => api.post(`/moderation/temoignages/${id}/identite`, { motif }),
  correspondances: () => api.get('/moderation/correspondances'),
  trancher: (id, valider) => api.post(`/moderation/correspondances/${id}/${valider ? 'valider' : 'ecarter'}`),
}

export const Admin = {
  utilisateurs: (q, page = 0) => api.get(`/admin/utilisateurs${qs({ q, page })}`),
  modifier: (id, changements) => api.patch(`/admin/utilisateurs/${id}`, changements),
  journal: (page = 0) => api.get(`/admin/journal-acces?page=${page}`),
}
