import { useCallback, useEffect, useRef, useState } from 'react'
import { Referentiel } from './api'

/**
 * Charge une ressource et suit son etat. Annule la requete precedente quand
 * les dependances changent (pas de reponse obsolete qui ecrase la recente).
 *
 *   const { data, erreur, chargement, recharger, setData } = useRequete(signal => Api.x(..., { signal }), [dep])
 */
export function useRequete(charger, deps, { actif = true } = {}) {
  const [etat, setEtat] = useState({ data: undefined, erreur: null, chargement: actif })
  const [version, setVersion] = useState(0)
  const chargerRef = useRef(charger)
  chargerRef.current = charger

  useEffect(() => {
    if (!actif) return
    const controleur = new AbortController()
    setEtat((e) => ({ ...e, erreur: null, chargement: true }))
    chargerRef.current(controleur.signal)
      .then((data) => setEtat({ data, erreur: null, chargement: false }))
      .catch((erreur) => {
        if (erreur.name !== 'AbortError') setEtat({ data: undefined, erreur, chargement: false })
      })
    return () => controleur.abort()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, version, actif])

  const recharger = useCallback(() => setVersion((v) => v + 1), [])
  const setData = useCallback((maj) => setEtat((e) => ({
    ...e, data: typeof maj === 'function' ? maj(e.data) : maj,
  })), [])

  return { ...etat, recharger, setData }
}

// Donnees de reference : chargees une fois par session
const cache = new Map()

function useReference(cle, charger) {
  const [liste, setListe] = useState(cache.get(cle) ?? [])
  useEffect(() => {
    if (cache.has(cle)) return
    charger().then((data) => {
      cache.set(cle, data)
      setListe(data)
    }).catch(() => {})
  }, [cle, charger])
  return liste
}

export const useQuartiers = () => useReference('quartiers', Referentiel.quartiers)
export const useCategories = () => useReference('categories', Referentiel.categories)
