import { useEffect, useState } from 'react'
import { Referentiel } from './api'

// cache simple : les quartiers et categories ne changent pas pendant la session
const cache = {}

function useListe(cle, charger) {
  const [liste, setListe] = useState(cache[cle] || [])
  useEffect(() => {
    if (cache[cle]) return
    charger()
      .then((data) => {
        cache[cle] = data
        setListe(data)
      })
      .catch(() => {})
  }, [cle, charger])
  return liste
}

export const useQuartiers = () => useListe('quartiers', Referentiel.quartiers)
export const useCategories = () => useListe('categories', Referentiel.categories)
