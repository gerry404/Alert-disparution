import { Alert, Box, Button, Container, Group, SegmentedControl, Select, SimpleGrid, Switch, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconSearch } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Dossiers } from '../api'
import DossierCard, { CarteSquelette } from '../components/DossierCard'
import { Vide } from '../components/ui'
import { useQuartiers } from '../hooks'

export default function Recherche() {
  const [params, setParams] = useSearchParams()
  const quartiers = useQuartiers()
  const [saisie, setSaisie] = useState(params.get('q') || '')
  const [saisieStable] = useDebouncedValue(saisie, 350)
  const [resultats, setResultats] = useState(null)
  const [erreur, setErreur] = useState('')

  const type = params.get('type') || ''
  const quartierId = params.get('quartierId') || null
  const inclureResolus = params.get('inclureResolus') === 'true'

  function changer(cle, valeur) {
    setParams((p) => {
      const n = new URLSearchParams(p)
      if (valeur) n.set(cle, valeur)
      else n.delete(cle)
      return n
    }, { replace: true })
  }

  useEffect(() => {
    if ((params.get('q') || '') !== saisieStable.trim()) changer('q', saisieStable.trim())
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [saisieStable])

  useEffect(() => {
    setResultats(null)
    setErreur('')
    Dossiers.rechercher({ type, quartierId, q: params.get('q') || '', inclureResolus })
      .then(setResultats)
      .catch((e) => {
        setErreur(e.message)
        setResultats([])
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [params])

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
            value={quartierId}
            onChange={(v) => changer('quartierId', v)}
            style={{ flex: '1 1 200px' }}
          />
        </Group>
        <Group justify="space-between" mt="md" gap="md">
          <SegmentedControl
            value={type}
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
            checked={inclureResolus}
            onChange={(e) => changer('inclureResolus', e.currentTarget.checked ? 'true' : '')}
          />
        </Group>
      </Box>

      {erreur && <Alert color="brique" mb="lg">{erreur}</Alert>}

      {resultats?.length > 0 && (
        <Text size="sm" c="dimmed" mb="md">
          {resultats.length} résultat{resultats.length > 1 ? 's' : ''}
        </Text>
      )}

      <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="lg">
        {resultats === null
          ? [1, 2, 3, 4, 5, 6].map((i) => <CarteSquelette key={i} />)
          : resultats.map((d) => <DossierCard key={d.id} dossier={d} />)}
      </SimpleGrid>

      {resultats?.length === 0 && !erreur && (
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
