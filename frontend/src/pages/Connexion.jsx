import { Alert, Anchor, Button, Container, Paper, PasswordInput, SimpleGrid, Stack, Text, TextInput, Title } from '@mantine/core'
import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Auth } from '../api'
import { useAuth } from '../auth'

export default function Connexion() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const { ouvrirSession } = useAuth()
  const inscription = params.get('mode') === 'inscription'
  const retour = params.get('retour')?.startsWith('/') ? params.get('retour') : '/moi'

  const [f, setF] = useState({ nom: '', prenom: '', telephone: '', email: '', motDePasse: '' })
  const [erreurs, setErreurs] = useState({})
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)

  const champ = (cle) => (e) => setF((x) => ({ ...x, [cle]: e.currentTarget.value }))

  function basculer() {
    setParams((p) => {
      const n = new URLSearchParams(p)
      if (inscription) n.delete('mode')
      else n.set('mode', 'inscription')
      return n
    }, { replace: true })
    setErreurs({})
    setMessage('')
  }

  async function envoyer(e) {
    e.preventDefault()
    setEnvoi(true)
    setErreurs({})
    setMessage('')
    try {
      const session = inscription ? await Auth.inscription(f) : await Auth.connexion(f.telephone, f.motDePasse)
      ouvrirSession(session)
      const modo = session.utilisateur.role !== 'DECLARANT'
      navigate(modo && retour === '/moi' ? '/moderation' : retour)
    } catch (err) {
      setErreurs(err.erreurs)
      setMessage(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Container size={440} pt={56}>
      <Title order={1} fz={36}>{inscription ? 'Créer un compte' : 'Connexion'}</Title>
      <Text c="dimmed" mt={6} mb="xl">
        {inscription ? 'Pour publier un avis et suivre ce qui se passe.' : 'Pour retrouver vos signalements.'}
      </Text>

      <form onSubmit={envoyer}>
        <Stack>
          {inscription && (
            <SimpleGrid cols={2}>
              <TextInput label="Nom" value={f.nom} onChange={champ('nom')} error={erreurs.nom} autoComplete="family-name" required />
              <TextInput label="Prénom" value={f.prenom} onChange={champ('prenom')} autoComplete="given-name" />
            </SimpleGrid>
          )}
          <TextInput
            label="Téléphone"
            type="tel"
            leftSection={<Text size="sm" c="dimmed">+237</Text>}
            leftSectionWidth={52}
            placeholder="6XX XX XX XX"
            value={f.telephone}
            onChange={champ('telephone')}
            error={erreurs.telephone}
            autoComplete="tel-national"
            required
          />
          {inscription && (
            <TextInput label="E-mail (facultatif)" type="email" value={f.email} onChange={champ('email')} error={erreurs.email} />
          )}
          <PasswordInput
            label="Mot de passe"
            description={inscription ? '8 caractères au moins' : null}
            value={f.motDePasse}
            onChange={champ('motDePasse')}
            error={erreurs.motDePasse}
            autoComplete={inscription ? 'new-password' : 'current-password'}
            required
          />
          {message && <Alert color="brique">{message}</Alert>}
          <Button type="submit" size="md" loading={envoi} mt="xs">
            {inscription ? 'Créer le compte' : 'Se connecter'}
          </Button>
        </Stack>
      </form>

      <Text size="sm" mt="lg" c="dimmed">
        {inscription ? 'Vous avez déjà un compte ?' : 'Pas encore de compte ?'}{' '}
        <Anchor component="button" type="button" onClick={basculer} fw={600}>
          {inscription ? 'Se connecter' : 'En créer un'}
        </Anchor>
      </Text>

      {!inscription && (
        <Paper p="sm" mt="xl" bg="var(--mantine-color-default-hover)" withBorder={false}>
          <Text size="xs" ff="monospace">
            Démo · familles 699000000 ou 677000000 / demo1234<br />
            Démo · modérateur 600000000 / moderateur<br />
            Démo · admin 600000001 / admin1234
          </Text>
        </Paper>
      )}
    </Container>
  )
}
