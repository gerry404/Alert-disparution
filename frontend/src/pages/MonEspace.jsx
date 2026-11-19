import { Alert, Anchor, Badge, Box, Button, Collapse, Container, Grid, Group, Stack, Text, Timeline, Title } from '@mantine/core'
import { useEffect, useState } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Dossiers } from '../api'
import { useAuth } from '../auth'
import { Vignette } from '../components/DossierCard'
import { informer, ModalMotif, StatutBadge, Vide } from '../components/ui'
import { dateHeure, STATUTS, TYPES } from '../format'

function LigneDossier({ d, onRetrouve }) {
  const [ouvert, setOuvert] = useState(false)
  const i = d.infos
  const visible = !['EN_ATTENTE', 'REJETE'].includes(i.statut) && !d.mineur

  return (
    <Box py="lg" className="filet-fin">
      <Grid gutter="md" align="flex-start">
        <Grid.Col span="content">
          <Box w={80}><Vignette dossier={i} ratio={1} taille={24} /></Box>
        </Grid.Col>
        <Grid.Col span="auto">
          <Group gap="xs">
            <Text className="surtitre" c={TYPES[i.type].couleur} fw={600}>{TYPES[i.type].surtitre}</Text>
            <StatutBadge statut={i.statut} />
            {d.mineur && <Badge color="gray">Mineur · non public</Badge>}
          </Group>
          <Text ff="heading" fz={20} fw={600} mt={2}>{i.titre}</Text>
          <Text size="xs" c="dimmed" ff="monospace">{i.reference} · envoyé le {dateHeure(d.dateSoumission)}</Text>
          <Group gap="md" mt="sm">
            {visible && <Anchor component={Link} to={`/d/${i.slug}`} size="sm" fw={600}>Voir l'avis</Anchor>}
            <Anchor component="button" size="sm" c="dimmed" onClick={() => setOuvert((o) => !o)}>
              {ouvert ? 'Masquer l’historique' : 'Historique'}
            </Anchor>
          </Group>
        </Grid.Col>
        {i.statut === 'PUBLIE' && (
          <Grid.Col span={{ base: 12, xs: 'content' }}>
            <Button variant="default" size="sm" onClick={() => onRetrouve(d)}>C'est retrouvé</Button>
          </Grid.Col>
        )}
      </Grid>
      <Collapse expanded={ouvert}>
        <Timeline bulletSize={10} lineWidth={1} mt="md" ml={4}>
          {d.historique.map((h, k) => (
            <Timeline.Item key={k} color={STATUTS[h.nouveau].couleur} title={<Text size="sm" fw={600}>{STATUTS[h.nouveau].libelle}</Text>}>
              <Text size="xs" c="dimmed">{dateHeure(h.date)}</Text>
              {h.motif && <Text size="sm">{h.motif}</Text>}
            </Timeline.Item>
          ))}
        </Timeline>
      </Collapse>
    </Box>
  )
}

export default function MonEspace() {
  const { utilisateur, pret } = useAuth()
  const [params] = useSearchParams()
  const [dossiers, setDossiers] = useState(null)
  const [erreur, setErreur] = useState('')
  const [retrouve, setRetrouve] = useState(null)

  useEffect(() => {
    if (utilisateur) Dossiers.mesDossiers().then(setDossiers).catch((e) => setErreur(e.message))
  }, [utilisateur])

  if (!pret) return null
  if (!utilisateur) return <Navigate to="/connexion?retour=/moi" replace />

  async function declarerRetrouve(motif) {
    const maj = await Dossiers.declarerRetrouve(retrouve.infos.id, motif)
    setDossiers((l) => l.map((d) => (d.infos.id === maj.infos.id ? maj : d)))
    informer('Bonne nouvelle. Un modérateur va vérifier avant de clôturer l’avis.')
  }

  return (
    <Container size="md" pt={40}>
      <Group justify="space-between" align="flex-end" mb="xl">
        <div>
          <Title order={1}>Mes signalements</Title>
          <Text c="dimmed" mt={4}>Bonjour {utilisateur.prenom || utilisateur.nom}.</Text>
        </div>
        <Button component={Link} to="/signaler" color="brique.7">Nouveau signalement</Button>
      </Group>

      {params.get('nouveau') && (
        <Alert color="teal" mb="lg">Signalement envoyé. Il sera mis en ligne dès qu'un modérateur l'aura relu.</Alert>
      )}
      {erreur && <Alert color="brique" mb="lg">{erreur}</Alert>}

      {dossiers?.length === 0 && <Vide titre="Vous n'avez encore rien signalé" />}

      <Stack gap={0} className={dossiers?.length ? 'rubrique' : ''}>
        {dossiers?.map((d) => <LigneDossier key={d.infos.id} d={d} onRetrouve={setRetrouve} />)}
      </Stack>

      <ModalMotif
        ouvert={!!retrouve}
        titre="Signaler la retrouvaille"
        description="Expliquez en quelques mots comment la personne ou l'objet a été retrouvé. L'avis passera en vérification avant d'être clôturé."
        libelleBouton="Envoyer"
        onValider={declarerRetrouve}
        onFermer={() => setRetrouve(null)}
      />
    </Container>
  )
}
