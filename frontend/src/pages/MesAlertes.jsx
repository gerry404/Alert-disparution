import {
  ActionIcon, Alert, Anchor, Box, Button, Container, Grid, Group, Loader, Select, Stack, Switch, Text, Title, Tooltip,
} from '@mantine/core'
import { IconTrash } from '@tabler/icons-react'
import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { Alertes } from '../api'
import { useAuth } from '../auth'
import { informer, Vide } from '../components/ui'
import { ilYa } from '../format'
import { useQuartiers, useRequete } from '../hooks'

/** "Mes alertes" (5.8) : zones suivies + notifications recues. */
export default function MesAlertes() {
  const { utilisateur, pret } = useAuth()
  const navigate = useNavigate()
  const quartiers = useQuartiers()
  const zones = useRequete(() => Alertes.zones(), [], { actif: !!utilisateur })
  const [page, setPage] = useState(0)
  const notifs = useRequete(() => Alertes.notifications(page), [page], { actif: !!utilisateur })
  const [ajout, setAjout] = useState(null)
  const [erreur, setErreur] = useState('')

  if (!pret) return null
  if (!utilisateur) return <Navigate to="/connexion?retour=/alertes" replace />

  const suivis = new Set((zones.data ?? []).map((z) => z.quartierId))

  async function action(fn, message) {
    setErreur('')
    try {
      await fn()
      if (message) informer(message)
    } catch (e) {
      setErreur(e.message)
    }
  }

  const ajouter = () => action(async () => {
    const z = await Alertes.ajouter(Number(ajout))
    zones.setData((l) => [...l.filter((x) => x.id !== z.id), z])
    setAjout(null)
  }, 'Zone ajoutée')

  const basculer = (z) => action(async () => {
    const maj = await Alertes.activer(z.id, !z.actif)
    zones.setData((l) => l.map((x) => (x.id === z.id ? maj : x)))
  })

  const retirer = (z) => action(async () => {
    await Alertes.supprimer(z.id)
    zones.setData((l) => l.filter((x) => x.id !== z.id))
  })

  async function ouvrir(n) {
    if (!n.lue) {
      Alertes.marquerLue(n.id).catch(() => {})
      notifs.setData((p) => ({ ...p, contenu: p.contenu.map((x) => (x.id === n.id ? { ...x, lue: true } : x)) }))
      window.dispatchEvent(new Event('notifications-lues'))
    }
    if (n.lien) navigate(n.lien)
  }

  const toutLire = () => action(async () => {
    await Alertes.toutMarquerLu()
    notifs.setData((p) => ({ ...p, contenu: p.contenu.map((x) => ({ ...x, lue: true })) }))
    window.dispatchEvent(new Event('notifications-lues'))
  })

  return (
    <Container size="lg" pt={40}>
      <Title order={1}>Mes alertes</Title>
      <Text c="dimmed" mt={4} mb="xl" maw={620}>
        Choisissez les quartiers où vous vivez, travaillez ou passez souvent. Vous serez prévenu dès qu'une
        disparition y est publiée. Aucune localisation GPS n'est utilisée.
      </Text>

      {erreur && <Alert color="brique" mb="lg">{erreur}</Alert>}

      <Grid gutter={{ base: 32, md: 56 }}>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Box className="rubrique">
            <Title order={3} mb="md">Quartiers suivis</Title>
            <Group align="flex-end" gap="xs" mb="md" wrap="nowrap">
              <Select
                flex={1}
                placeholder="Ajouter un quartier"
                searchable
                data={quartiers.filter((q) => !suivis.has(q.id)).map((q) => ({ value: String(q.id), label: q.nom }))}
                value={ajout}
                onChange={setAjout}
                nothingFoundMessage="Aucun quartier"
              />
              <Button onClick={ajouter} disabled={!ajout}>Suivre</Button>
            </Group>

            {zones.chargement && !zones.data && <Loader size="sm" color="dark" />}
            {zones.data?.length === 0 && <Text size="sm" c="dimmed">Vous ne suivez encore aucun quartier.</Text>}
            <Stack gap={0}>
              {zones.data?.map((z) => (
                <Group key={z.id} justify="space-between" py="sm" className="filet-fin" wrap="nowrap">
                  <div>
                    <Text fw={600} c={z.actif ? undefined : 'dimmed'}>{z.quartier}</Text>
                    <Text size="xs" c="dimmed">{z.arrondissement}</Text>
                  </div>
                  <Group gap="xs" wrap="nowrap">
                    <Switch checked={z.actif} onChange={() => basculer(z)} aria-label={`Alertes ${z.quartier}`} />
                    <Tooltip label="Ne plus suivre">
                      <ActionIcon variant="subtle" color="gray" onClick={() => retirer(z)} aria-label="Supprimer">
                        <IconTrash size={16} />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                </Group>
              ))}
            </Stack>
          </Box>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Box className="rubrique">
            <Group justify="space-between" mb="md">
              <Title order={3}>Notifications</Title>
              {notifs.data?.contenu.some((n) => !n.lue) && (
                <Anchor component="button" size="sm" onClick={toutLire}>Tout marquer comme lu</Anchor>
              )}
            </Group>
            {notifs.data?.total === 0 && <Vide titre="Rien pour l'instant">Les alertes de vos quartiers apparaîtront ici.</Vide>}
            <Stack gap={0}>
              {notifs.data?.contenu.map((n) => (
                <Box
                  key={n.id}
                  py="sm"
                  px="xs"
                  className="filet-fin"
                  onClick={() => ouvrir(n)}
                  style={{ cursor: n.lien ? 'pointer' : 'default', borderLeft: n.lue ? '3px solid transparent' : '3px solid var(--mantine-color-brique-7)' }}
                >
                  <Group justify="space-between" wrap="nowrap" align="flex-start">
                    <Text fw={n.lue ? 500 : 700} size="sm">{n.titre}</Text>
                    <Text size="xs" c="dimmed" style={{ whiteSpace: 'nowrap' }}>{ilYa(n.creeLe)}</Text>
                  </Group>
                  <Text size="sm" c="dimmed">{n.message}</Text>
                </Box>
              ))}
            </Stack>
            {notifs.data?.totalPages > 1 && (
              <Group justify="space-between" mt="md">
                <Button variant="subtle" size="xs" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Plus récentes</Button>
                <Button variant="subtle" size="xs" disabled={page + 1 >= notifs.data.totalPages} onClick={() => setPage((p) => p + 1)}>Plus anciennes</Button>
              </Group>
            )}
          </Box>
          <Text size="xs" c="dimmed" mt="lg">
            Pour l'instant les alertes arrivent ici. L'envoi par SMS est prévu dès que son coût sera validé.{' '}
            <Anchor component={Link} to="/dossiers" size="xs">Voir les avis en cours</Anchor>
          </Text>
        </Grid.Col>
      </Grid>
    </Container>
  )
}
