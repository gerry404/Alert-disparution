import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth'
import Layout from './components/Layout'
import { Container } from '@mantine/core'
import { Vide } from './components/ui'
import Accueil from './pages/Accueil'
import Connexion from './pages/Connexion'
import Fiche from './pages/Fiche'
import Moderation from './pages/Moderation'
import MonEspace from './pages/MonEspace'
import Recherche from './pages/Recherche'
import Signaler from './pages/Signaler'

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
            <Route path="moderation" element={<Moderation />} />
            <Route
              path="*"
              element={
                <Container size="lg" pt={40}>
                  <Vide titre="Page introuvable">
                    <Link to="/" className="lien">Retour à l'accueil</Link>
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
