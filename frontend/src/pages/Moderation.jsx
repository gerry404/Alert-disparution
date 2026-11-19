import {
  Alert, Anchor, Badge, Blockquote, Box, Button, Container, Grid, Group, List, Loader, Menu, Paper, SimpleGrid,
  Stack, Tabs, Text, Title,
} from '@mantine/core'
import { IconDots } from '@tabler/icons-react'
import { useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { Moderation as Api } from '../api'
import { useAuth } from '../auth'
import { Vignette } from '../components/DossierCard'
import { informer, ModalMotif, StatutBadge, Vide } from '../components/ui'
import { dateHeure, TYPES } from '../format'
import { useRequete } from '../hooks'

// decision -> texte de la modale de motif
const DECISIONS = {
  rejeter: { titre: 'Refuser la publication', bouton: 'Refuser', danger: true },
  resoudre: { titre: 'Clôturer l’avis', bouton: 'Clôturer', texte: 'Sur quels éléments la retrouvaille a-t-elle été confirmée ?' },
  rouvrir: { titre: 'Remettre en recherche', bouton: 'Remettre en ligne', texte: 'La retrouvaille n’est pas confirmée : l’avis redevient actif.' },
  masquer: { titre: 'Suspendre la visibilité', bouton: 'Suspendre', danger: true, texte: 'L’avis disparaît du site sans changer de statut. Le motif est conservé dans l’historique.' },
}

export default function Moderation() {
  const { estModerateur, pret } = useAuth()
  const dossiers = useRequete(() => Api.dossiers(), [], { actif: estModerateur })
  const temoignages = useRequete(() => Api.temoignages(), [], { actif: estModerateur })
  const correspondances = useRequete(() => Api.correspondances(), [], { actif: estModerateur })
  const [decision, setDecision] = useState(null)
  const [erreur, setErreur] = useState('')

  if (!pret) return null
  if (!estModerateur) return <Navigate to="/connexion?retour=/moderation" replace />

  async function decider(id, type, motif) {
    const maj = await Api.decider(id, type, motif)
    // le dossier reste dans la liste tant qu'il est a traiter (ex : masque mais toujours en attente)
    const resteATraiter = ['EN_ATTENTE', 'EN_VERIFICATION'].includes(maj.infos.statut)
    dossiers.setData((l) => l.flatMap((d) => (d.infos.id !== id ? [d] : resteATraiter ? [maj] : [])))
    informer(type === 'publier' ? 'Avis publié' : 'Décision enregistrée')
  }

  const compte = (r) => (r.data ? r.data.length : '…')
  const cfg = decision && DECISIONS[decision.type]

  return (
    <Container size="md" pt={40}>
      <Title order={1}>Modération</Title>
      <Text c="dimmed" mt={4} mb="xl">Rien n'est publié sans passer par ici.</Text>

      {erreur && <Alert color="brique" mb="lg" withCloseButton onClose={() => setErreur('')}>{erreur}</Alert>}

      <Tabs defaultValue="dossiers" color="dark" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="dossiers">Dossiers ({compte(dossiers)})</Tabs.Tab>
          <Tabs.Tab value="temoignages">Témoignages ({compte(temoignages)})</Tabs.Tab>
          <Tabs.Tab value="correspondances">Correspondances ({compte(correspondances)})</Tabs.Tab>
        </Tabs.List>

        <Tabs.Panel value="dossiers" pt="md">
          <OngletDossiers
            requete={dossiers}
            onPublier={(id) => decider(id, 'publier', '').catch((e) => setErreur(e.message))}
            onDecision={(id, type) => setDecision({ id, type })}
          />
        </Tabs.Panel>
        <Tabs.Panel value="temoignages" pt="md">
          <OngletTemoignages requete={temoignages} onErreur={setErreur} />
        </Tabs.Panel>
        <Tabs.Panel value="correspondances" pt="md">
          <OngletCorrespondances requete={correspondances} onErreur={setErreur} />
        </Tabs.Panel>
      </Tabs>

      <ModalMotif
        ouvert={!!decision}
        titre={cfg?.titre}
        description={cfg?.texte}
        libelleBouton={cfg?.bouton}
        danger={cfg?.danger}
        onValider={(motif) => decider(decision.id, decision.type, motif)}
        onFermer={() => setDecision(null)}
      />
    </Container>
  )
}

function Chargement({ requete, vide, children }) {
  if (requete.chargement && !requete.data) return <Group justify="center" py="xl"><Loader size="sm" color="dark" /></Group>
  if (requete.erreur) return <Alert color="brique">{requete.erreur.message}</Alert>
  if (requete.data?.length === 0) return <Vide titre={vide} />
  return children
}

// ------------------------------------------------------------------ dossiers

function OngletDossiers({ requete, onPublier, onDecision }) {
  const liste = requete.data ?? []
  const groupes = [
    ['À relire avant publication', liste.filter((d) => d.infos.statut === 'EN_ATTENTE')],
    ['Retrouvailles à vérifier', liste.filter((d) => d.infos.statut === 'EN_VERIFICATION')],
  ]
  return (
    <Chargement requete={requete} vide="Aucun dossier à traiter">
      {groupes.map(([titre, items]) => items.length > 0 && (
        <Box key={titre} mt="lg">
          <Text fw={600} size="xs" tt="uppercase" c="dimmed" style={{ letterSpacing: '0.06em' }}>{titre}</Text>
          {items.map((d) => <CarteDossier key={d.infos.id} d={d} onPublier={onPublier} onDecision={onDecision} />)}
        </Box>
      ))}
    </Chargement>
  )
}

function CarteDossier({ d, onPublier, onDecision }) {
  const i = d.infos
  const verif = i.statut === 'EN_VERIFICATION'
  const dernier = d.historique.at(-1)
  return (
    <Box py="lg" className="filet-fin">
      <Grid gutter="md">
        <Grid.Col span="content"><Box w={88}><Vignette dossier={i} ratio={1} taille={26} /></Box></Grid.Col>
        <Grid.Col span="auto">
          <Group gap="xs">
            <Text className="surtitre" c={TYPES[i.type].couleur} fw={600}>{TYPES[i.type].surtitre}</Text>
            <StatutBadge statut={i.statut} type={i.type} />
            {d.mineur && <Badge color="brique">Mineur</Badge>}
            {d.masque && <Badge color="gray">Masqué</Badge>}
          </Group>
          <Text ff="heading" fz={20} fw={600} mt={2}>{i.titre}</Text>
          <Text size="sm" c="dimmed">
            {i.quartier} · {dateHeure(i.dateFaits)} · {d.declarant} ({d.telephoneDeclarant})
          </Text>
          {d.lieuPrecis && <Text size="sm" mt={4}>Lieu exact (privé) : {d.lieuPrecis}</Text>}
          {(i.circonstances || i.description) && <Text size="sm" mt={4} lineClamp={3}>{i.circonstances || i.description}</Text>}
          {verif && dernier?.motif && (
            <Blockquote color="grape" mt="sm" p="sm" fz="sm">« {dernier.motif} » — {dernier.auteur}</Blockquote>
          )}
          {d.mineur && !verif && (
            <Text size="xs" c="dimmed" mt="xs">Même validé, ce dossier restera invisible du public (RG05) et ne déclenchera pas d'alerte.</Text>
          )}
        </Grid.Col>
      </Grid>
      <Group justify="flex-end" mt="sm" gap="xs">
        {verif ? (
          <>
            <Button variant="default" size="xs" onClick={() => onDecision(i.id, 'rouvrir')}>Non confirmé</Button>
            <Button color="teal" size="xs" onClick={() => onDecision(i.id, 'resoudre')}>Confirmer et clôturer</Button>
          </>
        ) : (
          <>
            <Button variant="default" size="xs" onClick={() => onDecision(i.id, 'rejeter')}>Refuser</Button>
            <Button size="xs" onClick={() => onPublier(i.id)}>Publier</Button>
          </>
        )}
        <Menu position="bottom-end" withinPortal>
          <Menu.Target>
            <Button variant="subtle" color="gray" size="xs" px={6} aria-label="Plus d'actions"><IconDots size={16} /></Button>
          </Menu.Target>
          <Menu.Dropdown>
            <Menu.Item color="red" onClick={() => onDecision(i.id, 'masquer')} disabled={d.masque}>Suspendre la visibilité</Menu.Item>
          </Menu.Dropdown>
        </Menu>
      </Group>
    </Box>
  )
}

// --------------------------------------------------------------- temoignages

function OngletTemoignages({ requete, onErreur }) {
  const [identites, setIdentites] = useState({})
  const [demande, setDemande] = useState(null)

  async function traiter(id, action) {
    try {
      await Api.traiterTemoignage(id, action)
      requete.setData((l) => l.filter((t) => t.temoignage.id !== id))
      informer(action === 'publier' ? 'Témoignage publié, la famille est prévenue' : 'Témoignage écarté')
    } catch (e) {
      onErreur(e.message)
    }
  }

  return (
    <Chargement requete={requete} vide="Aucun témoignage en attente">
      <Stack gap={0}>
        {requete.data?.map((t) => {
          const tm = t.temoignage
          const id = identites[tm.id]
          return (
            <Box key={tm.id} py="lg" className="filet-fin">
              <Text size="xs" c="dimmed">
                {tm.type === 'APERCU' ? 'Aperçu' : 'Information'} · reçu le {dateHeure(t.dateSoumission)} · à propos de{' '}
                <Text span fw={600} c="var(--mantine-color-text)">{t.dossierTitre}</Text> ({t.dossierReference})
              </Text>
              <Text mt={6}>{tm.description}</Text>
              <Text size="sm" c="dimmed" mt={6}>
                Faits du {dateHeure(tm.dateFaits)}{tm.quartier && ` · ${tm.quartier}`}{t.lieuPrecis && ` · ${t.lieuPrecis}`}
              </Text>
              <Group justify="space-between" mt="sm" gap="xs">
                <Text size="sm">
                  {id ? (
                    <>{id.nom || 'Sans nom'} · <Anchor href={`tel:${id.telephone}`} fw={600}>{id.telephone}</Anchor></>
                  ) : (
                    <>
                      <Text span c="dimmed">{t.anonyme ? 'Témoin anonyme' : `Signé « ${tm.auteur} »`} · </Text>
                      <Anchor component="button" size="sm" onClick={() => setDemande(tm.id)}>voir les coordonnées</Anchor>
                    </>
                  )}
                </Text>
                <Group gap="xs">
                  <Button variant="default" size="xs" onClick={() => traiter(tm.id, 'rejeter')}>Écarter</Button>
                  <Button size="xs" onClick={() => traiter(tm.id, 'publier')}>Publier</Button>
                </Group>
              </Group>
            </Box>
          )
        })}
      </Stack>
      <ModalMotif
        ouvert={!!demande}
        titre="Coordonnées du témoin"
        description="Cette consultation sera inscrite au journal, avec votre nom et le motif."
        libelleBouton="Afficher"
        onValider={async (motif) => {
          const r = await Api.identite(demande, motif)
          setIdentites((x) => ({ ...x, [demande]: r }))
        }}
        onFermer={() => setDemande(null)}
      />
    </Chargement>
  )
}

// ----------------------------------------------------------- correspondances

function OngletCorrespondances({ requete, onErreur }) {
  async function trancher(id, valider) {
    try {
      await Api.trancher(id, valider)
      requete.setData((l) => l.filter((c) => c.id !== id))
      informer(valider ? 'Les deux personnes sont prévenues. Organisez la vérification.' : 'Correspondance écartée')
    } catch (e) {
      onErreur(e.message)
    }
  }

  return (
    <Chargement requete={requete} vide="Aucune correspondance à examiner">
      <Text size="sm" c="dimmed" mb="md">
        Rapprochements automatiques entre objets perdus et trouvés. Un score élevé n'est pas une preuve :
        vérifiez la propriété avant de mettre les personnes en relation.
      </Text>
      <Stack>
        {requete.data?.map((c) => (
          <Paper key={c.id} p="md">
            <Group justify="space-between" mb="sm">
              <Text fw={700} ff="monospace">Score {c.score}/100</Text>
              <Text size="xs" c="dimmed">détecté le {dateHeure(c.creeLe)}</Text>
            </Group>
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              {[['Perdu', c.perdu, c.declarantPerdu], ['Trouvé', c.trouve, c.declarantTrouve]].map(([label, d, qui]) => (
                <Box key={label}>
                  <Text className="surtitre" c={TYPES[d.type].couleur} fw={600}>{label}</Text>
                  <Anchor component={Link} to={`/d/${d.slug}`} fw={600}>{d.titre}</Anchor>
                  <Text size="sm" c="dimmed">
                    {[d.marque, d.modele, d.couleur].filter(Boolean).join(' · ')} — {d.quartier}, {dateHeure(d.dateFaits)}
                  </Text>
                  {d.signesDistinctifs && <Text size="sm">{d.signesDistinctifs}</Text>}
                  <Text size="xs" c="dimmed" mt={4}>Déclaré par {qui}</Text>
                </Box>
              ))}
            </SimpleGrid>
            <List size="sm" mt="sm" c="dimmed">
              {c.raisons.map((r) => <List.Item key={r}>{r}</List.Item>)}
            </List>
            <Group justify="flex-end" mt="sm" gap="xs">
              <Button variant="default" size="xs" onClick={() => trancher(c.id, false)}>Ce n'est pas le même</Button>
              <Button size="xs" onClick={() => trancher(c.id, true)}>Valider et prévenir</Button>
            </Group>
          </Paper>
        ))}
      </Stack>
    </Chargement>
  )
}
