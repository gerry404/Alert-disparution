import { Alert, Anchor, Badge, Blockquote, Box, Button, Container, Grid, Group, Stack, Tabs, Text, Title } from '@mantine/core'
import { useCallback, useEffect, useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Moderation as Api } from '../api'
import { useAuth } from '../auth'
import { Vignette } from '../components/DossierCard'
import { informer, ModalMotif, StatutBadge, Vide } from '../components/ui'
import { dateHeure, TYPES } from '../format'

const ACTIONS = {
  rejeter: { titre: 'Refuser la publication', bouton: 'Refuser', danger: true },
  resoudre: { titre: 'Clôturer l’avis', bouton: 'Clôturer', texte: 'Sur quels éléments la retrouvaille a-t-elle été confirmée ?' },
  rouvrir: { titre: 'Remettre en recherche', bouton: 'Remettre en ligne', texte: 'La retrouvaille n’est pas confirmée : l’avis redevient actif.' },
}

function CarteDossier({ d, onPublier, onAction }) {
  const i = d.infos
  const verif = i.statut === 'EN_VERIFICATION'
  return (
    <Box py="lg" className="filet-fin">
      <Grid gutter="md">
        <Grid.Col span="content"><Box w={88}><Vignette dossier={i} ratio={1} taille={26} /></Box></Grid.Col>
        <Grid.Col span="auto">
          <Group gap="xs">
            <Text className="surtitre" c={TYPES[i.type].couleur} fw={600}>{TYPES[i.type].surtitre}</Text>
            <StatutBadge statut={i.statut} />
            {d.mineur && <Badge color="brique">Mineur</Badge>}
          </Group>
          <Text ff="heading" fz={20} fw={600} mt={2}>{i.titre}</Text>
          <Text size="sm" c="dimmed">
            {i.quartier} · {dateHeure(i.dateFaits)} · par {d.declarant} ({d.telephoneDeclarant})
          </Text>
          {d.lieuPrecis && <Text size="sm" mt={4}>Lieu exact (privé) : {d.lieuPrecis}</Text>}
          {(i.circonstances || i.description) && <Text size="sm" mt={4}>{i.circonstances || i.description}</Text>}
          {verif && d.historique.at(-1)?.motif && (
            <Blockquote color="grape" mt="sm" p="sm" fz="sm">{d.historique.at(-1).motif}</Blockquote>
          )}
          {d.mineur && !verif && (
            <Text size="xs" c="dimmed" mt="xs">Même validé, ce dossier restera invisible du public (règle RG05).</Text>
          )}
        </Grid.Col>
      </Grid>
      <Group justify="flex-end" mt="sm" gap="xs">
        {verif ? (
          <>
            <Button variant="default" size="xs" onClick={() => onAction('rouvrir')}>Non confirmé</Button>
            <Button color="teal" size="xs" onClick={() => onAction('resoudre')}>Confirmer et clôturer</Button>
          </>
        ) : (
          <>
            <Button variant="default" size="xs" onClick={() => onAction('rejeter')}>Refuser</Button>
            <Button size="xs" onClick={onPublier}>Publier</Button>
          </>
        )}
      </Group>
    </Box>
  )
}

export default function Moderation() {
  const { estModerateur, pret } = useAuth()
  const [dossiers, setDossiers] = useState(null)
  const [temoignages, setTemoignages] = useState(null)
  const [erreur, setErreur] = useState('')
  const [action, setAction] = useState(null)
  const [identites, setIdentites] = useState({})
  const [demandeIdentite, setDemandeIdentite] = useState(null)

  const charger = useCallback(() => {
    Api.dossiers().then(setDossiers).catch((e) => setErreur(e.message))
    Api.temoignages().then(setTemoignages).catch((e) => setErreur(e.message))
  }, [])

  useEffect(() => {
    if (estModerateur) charger()
  }, [estModerateur, charger])

  if (!pret) return null
  if (!estModerateur) return <Navigate to="/connexion?retour=/moderation" replace />

  async function executer(id, type, motif) {
    await Api.action(id, type, motif)
    setDossiers((l) => l.filter((d) => d.infos.id !== id))
    informer(type === 'publier' ? 'Avis publié' : 'Décision enregistrée')
  }

  async function traiterTemoignage(id, publier) {
    try {
      await (publier ? Api.publierTemoignage(id) : Api.rejeterTemoignage(id))
      setTemoignages((l) => l.filter((t) => t.temoignage.id !== id))
      informer(publier ? 'Témoignage publié' : 'Témoignage écarté')
    } catch (e) {
      setErreur(e.message)
    }
  }

  const aValider = dossiers?.filter((d) => d.infos.statut === 'EN_ATTENTE') ?? []
  const aVerifier = dossiers?.filter((d) => d.infos.statut === 'EN_VERIFICATION') ?? []
  const cfg = action && ACTIONS[action.type]

  return (
    <Container size="md" pt={40}>
      <Title order={1}>Modération</Title>
      <Text c="dimmed" mt={4} mb="xl">Rien n'est publié sans passer par ici.</Text>

      {erreur && <Alert color="brique" mb="lg">{erreur}</Alert>}

      <Tabs defaultValue="dossiers" color="dark">
        <Tabs.List>
          <Tabs.Tab value="dossiers">Dossiers ({dossiers?.length ?? '…'})</Tabs.Tab>
          <Tabs.Tab value="temoignages">Témoignages ({temoignages?.length ?? '…'})</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="dossiers" pt="md">
          {dossiers?.length === 0 && <Vide titre="Rien à traiter" />}
          {[['À relire avant publication', aValider], ['Retrouvailles à vérifier', aVerifier]].map(([titre, liste]) =>
            liste.length === 0 ? null : (
              <Box key={titre} mt="lg">
                <Text fw={600} size="sm" tt="uppercase" c="dimmed" style={{ letterSpacing: '0.05em' }}>{titre}</Text>
                {liste.map((d) => (
                  <CarteDossier
                    key={d.infos.id}
                    d={d}
                    onPublier={() => executer(d.infos.id, 'publier', '').catch((e) => setErreur(e.message))}
                    onAction={(type) => setAction({ id: d.infos.id, type })}
                  />
                ))}
              </Box>
            ),
          )}
        </Tabs.Panel>

        <Tabs.Panel value="temoignages" pt="md">
          {temoignages?.length === 0 && <Vide titre="Aucun témoignage en attente" />}
          <Stack gap={0}>
            {temoignages?.map((t) => {
              const tm = t.temoignage
              const id = identites[tm.id]
              return (
                <Box key={tm.id} py="lg" className="filet-fin">
                  <Text size="xs" c="dimmed">
                    {tm.type === 'APERCU' ? 'Aperçu' : 'Information'} · à propos de{' '}
                    <Text span fw={600} c="var(--mantine-color-text)">{t.dossierTitre}</Text> ({t.dossierReference})
                  </Text>
                  <Text mt={6}>{tm.description}</Text>
                  <Text size="sm" c="dimmed" mt={6}>
                    {dateHeure(tm.dateFaits)}
                    {tm.quartier && ` · ${tm.quartier}`}
                    {t.lieuPrecis && ` · ${t.lieuPrecis}`}
                  </Text>
                  <Group justify="space-between" mt="sm" gap="xs">
                    <Text size="sm">
                      {id ? (
                        <>
                          {id.nom || 'Sans nom'} · <Anchor href={`tel:${id.telephone}`} fw={600}>{id.telephone}</Anchor>
                        </>
                      ) : (
                        <>
                          <Text span c="dimmed">{t.anonyme ? 'Témoin anonyme' : 'Témoin non anonyme'} · </Text>
                          <Anchor component="button" size="sm" onClick={() => setDemandeIdentite(tm.id)}>
                            voir ses coordonnées
                          </Anchor>
                        </>
                      )}
                    </Text>
                    <Group gap="xs">
                      <Button variant="default" size="xs" onClick={() => traiterTemoignage(tm.id, false)}>Écarter</Button>
                      <Button size="xs" onClick={() => traiterTemoignage(tm.id, true)}>Publier</Button>
                    </Group>
                  </Group>
                </Box>
              )
            })}
          </Stack>
        </Tabs.Panel>
      </Tabs>

      <ModalMotif
        ouvert={!!action}
        titre={cfg?.titre}
        description={cfg?.texte}
        libelleBouton={cfg?.bouton}
        danger={cfg?.danger}
        onValider={(motif) => executer(action.id, action.type, motif)}
        onFermer={() => setAction(null)}
      />
      <ModalMotif
        ouvert={!!demandeIdentite}
        titre="Coordonnées du témoin"
        description="Cette consultation sera inscrite au journal, avec votre nom et le motif."
        libelleBouton="Afficher"
        onValider={async (motif) => {
          const r = await Api.identite(demandeIdentite, motif)
          setIdentites((x) => ({ ...x, [demandeIdentite]: r }))
        }}
        onFermer={() => setDemandeIdentite(null)}
      />
    </Container>
  )
}
