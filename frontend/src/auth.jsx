import { createContext, useContext, useEffect, useState } from 'react'
import { Auth, getToken, setToken } from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [utilisateur, setUtilisateur] = useState(null)
  const [pret, setPret] = useState(!getToken())

  // au chargement, on verifie que le token est toujours valable
  useEffect(() => {
    if (!getToken()) return
    Auth.moi()
      .then(setUtilisateur)
      .catch(() => setToken(null))
      .finally(() => setPret(true))
  }, [])

  function ouvrirSession(session) {
    setToken(session.token)
    setUtilisateur(session.utilisateur)
  }

  async function deconnecter() {
    try {
      await Auth.deconnexion()
    } catch {
      /* deja expire */
    }
    setToken(null)
    setUtilisateur(null)
  }

  const estModerateur = utilisateur?.role === 'MODERATEUR' || utilisateur?.role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ utilisateur, pret, ouvrirSession, deconnecter, estModerateur }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  return useContext(AuthContext)
}
