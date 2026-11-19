import { Anchor, Container } from '@mantine/core'
import { lazy } from 'react'
import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth'
import Layout from './components/Layout'
import { Vide } from './components/ui'
import Accueil from './pages/Accueil'
import Fiche from './pages/Fiche'
import Recherche from './pages/Recherche'

// Les pages publiques (celles qu'on ouvre depuis un lien WhatsApp) sont dans le bundle principal ;
// le reste n'est telecharge qu'a la demande : moins de donnees mobiles au premier chargement.
const Signaler = lazy(() => import('./pages/Signaler'))
const Connexion = lazy(() => import('./pages/Connexion'))
const MonEspace = lazy(() => import('./pages/MonEspace'))
const MesAlertes = lazy(() => import('./pages/MesAlertes'))
const Moderation = lazy(() => import('./pages/Moderation'))
const Administration = lazy(() => import('./pages/Administration'))

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<Accueil />} />
            <Route path="dossiers" element={<Recherche />} />
            <Route path="d/:slug" element={<Fiche />} />
            <Route path="signaler" element={<Signaler />} />
            <Route path="connexion" element={<Connexion />} />
            <Route path="moi" element={<MonEspace />} />
            <Route path="alertes" element={<MesAlertes />} />
            <Route path="moderation" element={<Moderation />} />
            <Route path="admin" element={<Administration />} />
            <Route
              path="*"
              element={
                <Container size="lg" pt={40}>
                  <Vide titre="Page introuvable">
                    <Anchor component={Link} to="/">Retour à l'accueil</Anchor>
                  </Vide>
                </Container>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
