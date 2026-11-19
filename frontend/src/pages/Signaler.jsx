import {
  ActionIcon, Alert, Box, Button, Container, FileButton, Group, Image, NumberInput, Paper, Select, SimpleGrid,
  Stack, Stepper, Table, Text, Textarea, TextInput, Title, UnstyledButton,
} from '@mantine/core'
import { IconChevronRight, IconPhoto, IconX } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Dossiers } from '../api'
import { useAuth } from '../auth'
import { dateHeure, maintenantLocal, TYPES } from '../format'
import { useCategories, useQuartiers } from '../hooks'

const CHOIX = [
  { type: 'PERSONNE', titre: 'Une personne a disparu', texte: 'Un proche qu’on n’arrive plus à joindre.' },
  { type: 'OBJET_PERDU', titre: 'J’ai perdu quelque chose', texte: 'Téléphone, sac, papiers, clés…' },
  { type: 'OBJET_TROUVE', titre: 'J’ai trouvé quelque chose', texte: 'Pour que son propriétaire le récupère.' },
]

const VIDE = {
  type: '', titre: '', description: '', dateFaits: '', quartierId: null, categorieId: null, lieuPrecis: '',
  nomComplet: '', sexe: null, age: '', descriptionPhysique: '', circonstances: '',
  marque: '', modele: '', couleur: '', signesDistinctifs: '',
}

export default function Signaler() {
  const { utilisateur, pret } = useAuth()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const quartiers = useQuartiers()
  const categories = useCategories()

  const typeUrl = TYPES[params.get('type')] ? params.get('type') : ''
  const [etape, setEtape] = useState(typeUrl ? 1 : 0)
  const [f, setF] = useState({ ...VIDE, type: typeUrl })
  const [photo, setPhoto] = useState(null)
  const [apercu, setApercu] = useState(null)
  const [erreurs, setErreurs] = useState({})
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    if (!photo) return setApercu(null)
    const u = URL.createObjectURL(photo)
    setApercu(u)
    return () => URL.revokeObjectURL(u)
  }, [photo])

  if (!pret) return null

  if (!utilisateur) {
    return (
      <Container size="xs" pt={64}>
        <Title order={2}>Il faut un compte pour publier un avis</Title>
        <Text c="dimmed" mt="sm">
          Ça nous permet de vous prévenir quand quelqu'un témoigne, et ça limite les faux avis.
          Pour <b>témoigner</b>, en revanche, aucun compte n'est demandé.
        </Text>
        <Group mt="xl">
          <Button component={Link} to="/connexion?retour=/signaler">Se connecter</Button>
          <Button component={Link} to="/connexion?mode=inscription&retour=/signaler" variant="default">
            Créer un compte
          </Button>
        </Group>
      </Container>
    )
  }

  const estPersonne = f.type === 'PERSONNE'
  const set = (cle, v) => setF((x) => ({ ...x, [cle]: v }))
  const champ = (cle) => (e) => set(cle, e.currentTarget.value)
  const categorie = categories.find((c) => String(c.id) === f.categorieId)
  const quartier = quartiers.find((q) => String(q.id) === f.quartierId)

  function valider() {
    const err = {}
    if (etape === 1 && estPersonne && !f.nomComplet.trim()) err.nomComplet = 'Le nom est nécessaire'
    if (etape === 1 && !estPersonne && !f.categorieId) err.categorieId = 'Choisissez une catégorie'
    if (etape === 2 && !f.quartierId) err.quartierId = 'Choisissez un quartier'
    if (etape === 2 && !f.dateFaits) err.dateFaits = 'Indiquez la date'
    setErreurs(err)
    return Object.keys(err).length === 0
  }

  function suivant() {
    if (!valider()) return
    if (etape === 2 && !f.titre) {
      const t = estPersonne
        ? `Disparition de ${f.nomComplet}`
        : `${categorie?.libelle}${f.marque ? ' ' + f.marque : ''} ${f.type === 'OBJET_TROUVE' ? 'trouvé' : 'perdu'} à ${quartier?.nom}`
      set('titre', t)
    }
    setEtape((e) => e + 1)
  }

  async function publier() {
    if (!f.titre.trim()) return setErreurs({ titre: 'Il faut un titre' })
    setEnvoi(true)
    setMessage('')
    const data = new FormData()
    Object.entries(f).forEach(([k, v]) => {
      if (v !== '' && v !== null) data.append(k, k === 'dateFaits' ? `${v}:00` : v)
    })
    if (photo) data.append('photo', photo)
    try {
      await Dossiers.signaler(data)
      navigate('/moi?nouveau=1')
    } catch (err) {
      setErreurs(err.erreurs)
      setMessage(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Container size="sm" pt={40}>
      <Title order={1}>Nouveau signalement</Title>
      <Text c="dimmed" mt={4} mb="xl">Un modérateur le relira avant qu'il soit mis en ligne.</Text>

      <Stepper active={etape} size="sm" color="dark" mb="xl" allowNextStepsSelect={false} onStepClick={setEtape}>
        <Stepper.Step label="Type" />
        <Stepper.Step label="Description" />
        <Stepper.Step label="Où et quand" />
        <Stepper.Step label="Relecture" />
      </Stepper>

      {etape === 0 && (
        <Stack gap={0} className="rubrique">
          {CHOIX.map((c) => (
            <UnstyledButton
              key={c.type}
              py="lg"
              className="filet-fin"
              onClick={() => {
                set('type', c.type)
                setEtape(1)
              }}
            >
              <Group justify="space-between" wrap="nowrap">
                <div>
                  <Text className="surtitre" c={TYPES[c.type].couleur} fw={600}>{TYPES[c.type].surtitre}</Text>
                  <Text ff="heading" fz={22} fw={600}>{c.titre}</Text>
                  <Text size="sm" c="dimmed">{c.texte}</Text>
                </div>
                <IconChevronRight size={20} />
              </Group>
            </UnstyledButton>
          ))}
        </Stack>
      )}

      {etape === 1 && estPersonne && (
        <Stack>
          <TextInput label="Nom et prénoms" value={f.nomComplet} onChange={champ('nomComplet')} error={erreurs.nomComplet} data-autofocus required />
          <SimpleGrid cols={{ base: 1, xs: 2 }}>
            <NumberInput label="Âge (environ)" min={0} max={120} value={f.age} onChange={(v) => set('age', v)} />
            <Select
              label="Sexe"
              placeholder="Non précisé"
              clearable
              data={[{ value: 'F', label: 'Femme' }, { value: 'M', label: 'Homme' }]}
              value={f.sexe}
              onChange={(v) => set('sexe', v)}
            />
          </SimpleGrid>
          {f.age !== '' && Number(f.age) < 18 && (
            <Alert color="blue" title="Enfant mineur">
              L'avis ne sera pas affiché publiquement : il sera suivi de façon confidentielle par la modération.
              Si l'enfant est en danger, appelez tout de suite le 117.
            </Alert>
          )}
          <Textarea label="Signalement" description="Taille, corpulence, coiffure, vêtements portés" autosize minRows={3} value={f.descriptionPhysique} onChange={champ('descriptionPhysique')} />
          <TextInput label="Signes particuliers" description="Cicatrice, lunettes, tatouage…" value={f.signesDistinctifs} onChange={champ('signesDistinctifs')} />
        </Stack>
      )}

      {etape === 1 && !estPersonne && (
        <Stack>
          <Select
            label="Catégorie"
            placeholder="Choisir"
            data={categories.map((c) => ({ value: String(c.id), label: c.libelle }))}
            value={f.categorieId}
            onChange={(v) => set('categorieId', v)}
            error={erreurs.categorieId}
            required
          />
          {categorie?.sensible && (
            <Alert color="yellow" title="Document sensible">
              N'écrivez ni le numéro complet ni le nom entier. La photo ne sera pas publiée : le propriétaire
              devra prouver que c'est à lui.
            </Alert>
          )}
          <SimpleGrid cols={{ base: 1, xs: 3 }}>
            <TextInput label="Marque" value={f.marque} onChange={champ('marque')} />
            <TextInput label="Modèle" value={f.modele} onChange={champ('modele')} />
            <TextInput label="Couleur" value={f.couleur} onChange={champ('couleur')} />
          </SimpleGrid>
          <TextInput label="Signes particuliers" description="Coque, autocollant, rayure…" value={f.signesDistinctifs} onChange={champ('signesDistinctifs')} />
        </Stack>
      )}

      {etape === 2 && (
        <Stack>
          <SimpleGrid cols={{ base: 1, xs: 2 }}>
            <Select
              label={estPersonne ? 'Dernier quartier connu' : 'Quartier'}
              placeholder="Choisir"
              searchable
              data={quartiers.map((q) => ({ value: String(q.id), label: `${q.nom} (${q.arrondissement})` }))}
              value={f.quartierId}
              onChange={(v) => set('quartierId', v)}
              error={erreurs.quartierId}
              required
            />
            <TextInput
              type="datetime-local"
              label={estPersonne ? 'Disparu(e) depuis le' : f.type === 'OBJET_TROUVE' ? 'Trouvé le' : 'Perdu le'}
              value={f.dateFaits}
              max={maintenantLocal()}
              onChange={champ('dateFaits')}
              error={erreurs.dateFaits}
              required
            />
          </SimpleGrid>
          <TextInput
            label="Adresse ou repère exact"
            description="Seule la modération le verra. Le public ne voit que le quartier."
            value={f.lieuPrecis}
            onChange={champ('lieuPrecis')}
          />
          <Textarea
            label={estPersonne ? 'Que s’est-il passé ?' : 'Autres informations'}
            description={estPersonne ? 'Où allait la personne, avec qui, dans quel état…' : null}
            autosize
            minRows={4}
            value={estPersonne ? f.circonstances : f.description}
            onChange={champ(estPersonne ? 'circonstances' : 'description')}
          />
        </Stack>
      )}

      {etape === 3 && (
        <Stack>
          <TextInput label="Titre de l'avis" value={f.titre} onChange={champ('titre')} error={erreurs.titre} maxLength={120} />

          {apercu ? (
            <Box pos="relative" maw={260}>
              <Image src={apercu} alt="Aperçu" radius="sm" />
              <ActionIcon pos="absolute" top={8} right={8} variant="default" onClick={() => setPhoto(null)} aria-label="Retirer la photo">
                <IconX size={16} />
              </ActionIcon>
            </Box>
          ) : (
            <FileButton onChange={setPhoto} accept="image/jpeg,image/png,image/webp">
              {(props) => (
                <Button {...props} variant="default" leftSection={<IconPhoto size={18} />} w="fit-content">
                  Ajouter une photo
                </Button>
              )}
            </FileButton>
          )}
          <Text size="xs" c="dimmed" mt={-8}>Une photo récente et nette aide beaucoup. 5 Mo maximum.</Text>

          <Paper p="md" mt="sm">
            <Table>
              <Table.Tbody>
                <Table.Tr><Table.Td c="dimmed">Type</Table.Td><Table.Td>{TYPES[f.type].libelle}</Table.Td></Table.Tr>
                {estPersonne
                  ? <Table.Tr><Table.Td c="dimmed">Nom</Table.Td><Table.Td>{f.nomComplet}</Table.Td></Table.Tr>
                  : <Table.Tr><Table.Td c="dimmed">Catégorie</Table.Td><Table.Td>{categorie?.libelle}</Table.Td></Table.Tr>}
                <Table.Tr><Table.Td c="dimmed">Quartier</Table.Td><Table.Td>{quartier?.nom}</Table.Td></Table.Tr>
                <Table.Tr><Table.Td c="dimmed">Date</Table.Td><Table.Td>{dateHeure(f.dateFaits)}</Table.Td></Table.Tr>
              </Table.Tbody>
            </Table>
          </Paper>

          {message && <Alert color="brique">{message}</Alert>}
        </Stack>
      )}

      {etape > 0 && (
        <Group justify="space-between" mt="xl" pt="md" className="filet-fin">
          <Button variant="subtle" color="gray" onClick={() => setEtape((e) => e - 1)}>Retour</Button>
          {etape < 3 ? (
            <Button onClick={suivant}>Continuer</Button>
          ) : (
            <Button color="brique.7" onClick={publier} loading={envoi}>Envoyer pour relecture</Button>
          )}
        </Group>
      )}
    </Container>
  )
}
