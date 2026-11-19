import { Alert, Anchor, Button, Container, PasswordInput, SimpleGrid, Stack, Text, TextInput, Title } from '@mantine/core'
import { useState } from 'react'
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { Auth } from '../api'
import { useAuth } from '../auth'
import { formaterTelephone } from '../format'

const VIDE = { nom: '', prenom: '', telephone: '', email: '', motDePasse: '', confirmation: '' }

function validerLocalement(f, inscription) {
  const err = {}
  const chiffres = f.telephone.replace(/\D/g, '')
  if (!chiffres) err.telephone = 'Indiquez votre numéro'
  else if (inscription && !/^6\d{8}$/.test(chiffres)) err.telephone = 'Numéro camerounais à 9 chiffres commençant par 6'
  if (!f.motDePasse) err.motDePasse = 'Indiquez votre mot de passe'
  if (inscription) {
    if (!f.nom.trim()) err.nom = 'Indiquez votre nom'
    if (f.motDePasse && f.motDePasse.length < 8) err.motDePasse = '8 caractères au moins'
    if (f.confirmation !== f.motDePasse) err.confirmation = 'Les deux mots de passe ne sont pas identiques'
    if (f.email && !/^\S+@\S+\.\S+$/.test(f.email)) err.email = 'Adresse e-mail invalide'
  }
  return err
}

export default function Connexion() {
  const [params, setParams] = useSearchParams()
  const navigate = useNavigate()
  const { utilisateur, ouvrirSession } = useAuth()
  const inscription = params.get('mode') === 'inscription'
  const retourDemande = params.get('retour')
  const retour = retourDemande?.startsWith('/') && !retourDemande.startsWith('//') ? retourDemande : null

  const [f, setF] = useState(VIDE)
  const [erreurs, setErreurs] = useState({})
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)

  // deja connecte (onglet rouvert, bouton retour du navigateur...) : rien a faire ici
  if (utilisateur && !envoi) return <Navigate to={retour || '/moi'} replace />

  function maj(cle, valeur) {
    setF((x) => ({ ...x, [cle]: valeur }))
    if (erreurs[cle]) setErreurs((x) => ({ ...x, [cle]: undefined }))
    if (message) setMessage('')
  }
  const champ = (cle) => (e) => maj(cle, e.currentTarget.value)

  function basculer() {
    setParams((p) => {
      const n = new URLSearchParams(p)
      if (inscription) n.delete('mode')
      else n.set('mode', 'inscription')
      return n
    }, { replace: true })
    setF((x) => ({ ...VIDE, telephone: x.telephone })) // on garde le numero deja tape
    setErreurs({})
    setMessage('')
  }

  async function envoyer(e) {
    e.preventDefault()
    const locales = validerLocalement(f, inscription)
    if (Object.keys(locales).length) {
      setErreurs(locales)
      return
    }

    setEnvoi(true)
    setErreurs({})
    setMessage('')
    try {
      const session = inscription
        ? await Auth.inscription({
          nom: f.nom.trim(), prenom: f.prenom.trim(), telephone: f.telephone, email: f.email.trim(), motDePasse: f.motDePasse,
        })
        : await Auth.connexion(f.telephone, f.motDePasse)
      ouvrirSession(session)
      const parDefaut = session.utilisateur.role === 'DECLARANT' ? '/moi' : '/moderation'
      navigate(retour || parDefaut, { replace: true })
    } catch (err) {
      setErreurs(err.erreurs ?? {})
      setMessage(err.code === 'TELEPHONE_DEJA_UTILISE'
        ? 'Ce numéro a déjà un compte. Connectez-vous plutôt.'
        : err.message)
      setEnvoi(false)
    }
  }

  return (
    <Container size={460} pt={{ base: 32, sm: 56 }} pb={40}>
      <Title order={1} fz={36}>{inscription ? 'Créer un compte' : 'Connexion'}</Title>
      <Text c="dimmed" mt={6} mb="xl">
        {inscription
          ? 'Pour publier un avis et être prévenu quand quelqu’un témoigne. Pour témoigner, pas besoin de compte.'
          : 'Pour retrouver vos signalements et vos alertes.'}
      </Text>

      <form onSubmit={envoyer} noValidate>
        <Stack>
          {inscription && (
            <SimpleGrid cols={{ base: 1, xs: 2 }}>
              <TextInput label="Nom" value={f.nom} onChange={champ('nom')} error={erreurs.nom}
                autoComplete="family-name" withAsterisk data-autofocus />
              <TextInput label="Prénom" value={f.prenom} onChange={champ('prenom')} autoComplete="given-name" />
            </SimpleGrid>
          )}

          <TextInput
            label="Numéro de téléphone"
            type="tel"
            inputMode="numeric"
            leftSection={<Text size="sm" c="dimmed">+237</Text>}
            leftSectionWidth={56}
            placeholder="6XX XX XX XX"
            value={f.telephone}
            onChange={(e) => maj('telephone', formaterTelephone(e.currentTarget.value))}
            error={erreurs.telephone}
            autoComplete="tel-national"
            withAsterisk
            autoFocus={!inscription}
          />

          {inscription && (
            <TextInput label="E-mail" description="Facultatif" type="email" value={f.email} onChange={champ('email')}
              error={erreurs.email} autoComplete="email" />
          )}

          <PasswordInput
            label="Mot de passe"
            description={inscription ? '8 caractères au moins' : null}
            value={f.motDePasse}
            onChange={champ('motDePasse')}
            error={erreurs.motDePasse}
            autoComplete={inscription ? 'new-password' : 'current-password'}
            withAsterisk
          />

          {inscription && (
            <PasswordInput
              label="Confirmer le mot de passe"
              value={f.confirmation}
              onChange={champ('confirmation')}
              error={erreurs.confirmation}
              autoComplete="new-password"
              withAsterisk
            />
          )}

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

    </Container>
  )
}
