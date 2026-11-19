import { Alert, Box, Button, Center, Container, Group, Pagination, SegmentedControl, Select, SimpleGrid, Switch, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconSearch } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Dossiers } from '../api'
import DossierCard, { CarteSquelette } from '../components/DossierCard'
import { Vide } from '../components/ui'
import { useQuartiers, useRequete } from '../hooks'

const TAILLE = 12

export default function Recherche() {
  const [params, setParams] = useSearchParams()
  const quartiers = useQuartiers()
  const [saisie, setSaisie] = useState(params.get('q') || '')
  const [saisieStable] = useDebouncedValue(saisie, 350)

  const filtres = {
    type: params.get('type') || '',
    quartierId: params.get('quartierId') || '',
    q: params.get('q') || '',
    inclureResolus: params.get('inclureResolus') === 'true',
    page: Number(params.get('page') || 1) - 1,
    taille: TAILLE,
  }

  // l'URL est la source de verite : un lien partage redonne exactement la meme recherche
  const { data, erreur, chargement } = useRequete(
    (signal) => Dossiers.rechercher(filtres, { signal }),
    [params.toString()],
  )

  function changer(cle, valeur, garderPage = false) {
    setParams((p) => {
      const n = new URLSearchParams(p)
      if (valeur) n.set(cle, valeur)
      else n.delete(cle)
      if (!garderPage) n.delete('page')
      return n
    }, { replace: cle !== 'page' })
  }

  // l'URL change depuis l'exterieur (lien "Consulter les avis", bouton precedent) : le champ suit
  useEffect(() => {
    setSaisie((s) => (s.trim() === filtres.q ? s : filtres.q))
  }, [filtres.q])

  useEffect(() => {
    if (filtres.q !== saisieStable.trim()) changer('q', saisieStable.trim())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [saisieStable])

  const resultats = data?.contenu

  return (
    <Container size="lg" pt={40}>
      <Title order={1} mb={4}>Avis en cours</Title>
      <Text c="dimmed" mb="xl">Personnes disparues, objets perdus et objets trouvés à Douala.</Text>

      <Box className="rubrique" pt="md" mb="xl">
        <Group align="flex-end" gap="md" wrap="wrap">
          <TextInput
            label="Mots-clés"
            placeholder="Nom, description, référence…"
            leftSection={<IconSearch size={16} />}
            value={saisie}
            onChange={(e) => setSaisie(e.currentTarget.value)}
            style={{ flex: '1 1 260px' }}
          />
          <Select
            label="Quartier"
            placeholder="Tous"
            clearable
            searchable
            data={quartiers.map((q) => ({ value: String(q.id), label: q.nom }))}
            value={filtres.quartierId || null}
            onChange={(v) => changer('quartierId', v)}
            style={{ flex: '1 1 200px' }}
          />
        </Group>
        <Group justify="space-between" mt="md" gap="md">
          <SegmentedControl
            value={filtres.type}
            onChange={(v) => changer('type', v)}
            data={[
              { value: '', label: 'Tout' },
              { value: 'PERSONNE', label: 'Personnes' },
              { value: 'OBJET_PERDU', label: 'Perdus' },
              { value: 'OBJET_TROUVE', label: 'Trouvés' },
            ]}
          />
          <Switch
            label="Afficher aussi les dossiers résolus"
            checked={filtres.inclureResolus}
            onChange={(e) => changer('inclureResolus', e.currentTarget.checked ? 'true' : '')}
          />
        </Group>
      </Box>

      {erreur && <Alert color="brique" mb="lg">{erreur.message}</Alert>}

      {data && data.total > 0 && (
        <Text size="sm" c="dimmed" mb="md">
          {data.total} résultat{data.total > 1 ? 's' : ''}
        </Text>
      )}

      <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="lg">
        {chargement && !resultats
          ? Array.from({ length: 6 }, (_, i) => <CarteSquelette key={i} />)
          : resultats?.map((d) => <DossierCard key={d.id} dossier={d} />)}
      </SimpleGrid>

      {data?.totalPages > 1 && (
        <Center mt="xl">
          <Pagination
            total={data.totalPages}
            value={data.page + 1}
            onChange={(p) => {
              changer('page', p > 1 ? String(p) : '', true)
              window.scrollTo({ top: 0, behavior: 'smooth' })
            }}
            color="dark"
          />
        </Center>
      )}

      {data?.total === 0 && (
        <Vide titre="Rien trouvé">
          Essayez un autre quartier ou d'autres mots.{' '}
          <Button variant="subtle" size="compact-sm" onClick={() => { setSaisie(''); setParams({}) }}>
            Tout effacer
          </Button>
        </Vide>
      )}
    </Container>
  )
}
