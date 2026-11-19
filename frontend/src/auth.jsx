import { createContext, useCallback, useContext, useEffect, useState } from 'react'
import { Auth, getToken, setToken } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [utilisateur, setUtilisateur] = useState(null)
  const [pret, setPret] = useState(!getToken())

  useEffect(() => {
    if (!getToken()) return
    Auth.moi()
      .then(setUtilisateur)
      .catch(() => setToken(null))
      .finally(() => setPret(true))
  }, [])

  // un 401 n'importe ou dans l'app (token expire, compte desactive) ferme la session
  useEffect(() => {
    const surExpiration = () => setUtilisateur(null)
    window.addEventListener('session-expiree', surExpiration)
    return () => window.removeEventListener('session-expiree', surExpiration)
  }, [])

  const ouvrirSession = useCallback((session) => {
    setToken(session.token)
    setUtilisateur(session.utilisateur)
  }, [])

  const deconnecter = useCallback(async () => {
    try {
      await Auth.deconnexion()
    } catch {
      /* deja expire */
    }
    setToken(null)
    setUtilisateur(null)
  }, [])

  const role = utilisateur?.role
  const valeur = {
    utilisateur,
    pret,
    ouvrirSession,
    deconnecter,
    estModerateur: role === 'MODERATEUR' || role === 'ADMIN',
    estAdmin: role === 'ADMIN',
  }

  return <AuthContext.Provider value={valeur}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
