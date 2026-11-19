// Petit client pour l'API Spring. Le token est garde dans le localStorage.

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
    /* navigation privee : tant pis, la session ne survivra pas au rechargement */
  }
}

export class ApiError extends Error {
  constructor(status, message, erreurs) {
    super(message)
    this.status = status
    this.erreurs = erreurs || {}
  }
}

async function requete(methode, url, corps) {
  const headers = {}
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let body
  if (corps instanceof FormData) {
    body = corps
  } else if (corps !== undefined) {
    headers['Content-Type'] = 'application/json'
    body = JSON.stringify(corps)
  }

  let res
  try {
    res = await fetch(`/api${url}`, { method: methode, headers, body })
  } catch {
    throw new ApiError(0, 'Connexion impossible. Vérifiez votre réseau.')
  }

  if (res.status === 204) return null
  const texte = await res.text()
  const data = texte ? JSON.parse(texte) : null

  if (!res.ok) {
    if (res.status === 401 && token) setToken(null)
    const message = data?.message || (res.status === 401 ? 'Veuillez vous connecter.' : 'Une erreur est survenue.')
    throw new ApiError(res.status, message, data?.erreurs)
  }
  return data
}

export const api = {
  get: (url) => requete('GET', url),
  post: (url, corps) => requete('POST', url, corps ?? {}),
}

// --- appels metier ---

export const Auth = {
  connexion: (telephone, motDePasse) => api.post('/auth/login', { telephone, motDePasse }),
  inscription: (donnees) => api.post('/auth/register', donnees),
  moi: () => api.get('/auth/me'),
  deconnexion: () => api.post('/auth/logout'),
}

export const Dossiers = {
  rechercher: (filtres = {}) => {
    const params = new URLSearchParams()
    Object.entries(filtres).forEach(([k, v]) => {
      if (v !== '' && v !== null && v !== undefined && v !== false) params.set(k, v)
    })
    const qs = params.toString()
    return api.get(`/dossiers${qs ? `?${qs}` : ''}`)
  },
  fiche: (slug) => api.get(`/dossiers/${encodeURIComponent(slug)}`),
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

export const Moderation = {
  dossiers: () => api.get('/moderation/dossiers'),
  action: (id, action, motif) => api.post(`/moderation/dossiers/${id}/${action}`, { motif }),
  temoignages: () => api.get('/moderation/temoignages'),
  publierTemoignage: (id) => api.post(`/moderation/temoignages/${id}/publier`),
  rejeterTemoignage: (id) => api.post(`/moderation/temoignages/${id}/rejeter`),
  identite: (id, motif) => api.post(`/moderation/temoignages/${id}/identite`, { motif }),
}
