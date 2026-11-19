import { Anchor, Box, Button, Container, Grid, Group, SimpleGrid, Stack, Text, TextInput, Title } from '@mantine/core'
import { IconArrowRight, IconSearch } from '@tabler/icons-react'
import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Dossiers } from '../api'
import DossierCard, { CarteSquelette } from '../components/DossierCard'

function Rubrique({ type, titre, lien }) {
  const [liste, setListe] = useState(null)
  useEffect(() => {
    Dossiers.rechercher({ type, taille: 3 }).then((p) => setListe(p.contenu)).catch(() => setListe([]))
  }, [type])

  if (liste?.length === 0) return null
  return (
    <Box className="rubrique" mt={56}>
      <Group justify="space-between" align="baseline" mb="lg">
        <Title order={2} className="titre-affiche" fz={32}>{titre}</Title>
        <Anchor component={Link} to={lien} size="sm" fw={600} className="lien-souligne">
          Tout voir
        </Anchor>
      </Group>
      <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="lg">
        {liste === null
          ? [1, 2, 3].map((i) => <CarteSquelette key={i} />)
          : liste.map((d) => <DossierCard key={d.id} dossier={d} />)}
      </SimpleGrid>
    </Box>
  )
}

export default function Accueil() {
  const navigate = useNavigate()
  const [q, setQ] = useState('')

  function rechercher(e) {
    e.preventDefault()
    navigate(`/dossiers${q.trim() ? `?q=${encodeURIComponent(q.trim())}` : ''}`)
  }

  return (
    <>
    {/* Bandeau : rond-point Deido, statue « La Nouvelle Liberte » */}
    <Box className="bandeau-accueil">
      <Container size="lg" py={{ base: 48, md: 88 }} pos="relative">
      <Stack gap="lg" align="center" ta="center" maw={680} mx="auto">
        <Title order={1} className="titre-affiche" fz={{ base: 40, sm: 56 }}>
          Quelqu'un manque à l'appel ?<br />
          tu peut aider à le retrouver.
        </Title>
        <Text size="lg" c="rgba(255,255,255,0.88)" maw={560}>
          Les avis de recherche circulent sur WhatsApp puis se perdent. Ici, ils restent au même
          endroit, vérifiés, et chacun peut transmettre ce qu'il a vu sans donner son nom.
        </Text>

        <form onSubmit={rechercher} style={{ width: '100%', maxWidth: 560 }}>
          <Group gap="xs" wrap="nowrap">
            <TextInput
              flex={1}
              size="md"
              radius="xl"
              placeholder="Un nom, un objet, un quartier…"
              leftSection={<IconSearch size={18} />}
              value={q}
              onChange={(e) => setQ(e.currentTarget.value)}
              aria-label="Rechercher"
            />
            <Button type="submit" size="md" variant="white" color="dark">Chercher</Button>
          </Group>
        </form>

        <Group gap="sm" justify="center">
          <Button component={Link} to="/signaler" color="brique.7">Signaler une disparition</Button>
          <Button component={Link} to="/signaler?type=OBJET_TROUVE" variant="white" color="dark">J'ai trouvé un objet</Button>
        </Group>
      </Stack>
      </Container>
    </Box>

    <Container size="lg">

      <Rubrique type="PERSONNE" titre="Personnes recherchées" lien="/dossiers?type=PERSONNE" />
      <Rubrique type="OBJET_PERDU" titre="Objets perdus" lien="/dossiers?type=OBJET_PERDU" />
      <Rubrique type="OBJET_TROUVE" titre="Objets trouvés" lien="/dossiers?type=OBJET_TROUVE" />

      <Box className="rubrique" mt={72}>
        <Title order={2} mb="lg" ta="center" className="titre-affiche" fz={32}>Comment ça se passe</Title>
        <SimpleGrid cols={{ base: 1, sm: 3 }} spacing={40} ta="center">
          <Stack gap={6}>
            <Text ff="heading" fz={40} lh={1} c="dimmed">1</Text>
            <Text fw={600}>La famille publie l'avis</Text>
            <Text size="sm" c="dimmed">
              Un compte suffit, avec un numéro de téléphone. Photo, description, dernier endroit où la
              personne a été vue.
            </Text>
          </Stack>
          <Stack gap={6}>
            <Text ff="heading" fz={40} lh={1} c="dimmed">2</Text>
            <Text fw={600}>Un modérateur relit</Text>
            <Text size="sm" c="dimmed">
              Rien n'est mis en ligne sans vérification. Les dossiers d'enfants ne sont jamais affichés
              publiquement.
            </Text>
          </Stack>
          <Stack gap={6}>
            <Text ff="heading" fz={40} lh={1} c="dimmed">3</Text>
            <Text fw={600}>Le quartier ouvre l'œil</Text>
            <Text size="sm" c="dimmed">
              Le lien se partage sur WhatsApp. Celui qui a vu quelque chose le dit en une minute, sans compte.
            </Text>
          </Stack>
        </SimpleGrid>
      </Box>

      <Grid mt={72} gutter={40} className="rubrique" pt="lg">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Title order={2} className="titre-affiche" fz={32}>Parler sans s'exposer</Title>
        </Grid.Col>
        <Grid.Col span={{ base: 12, md: 7 }}>
          <Stack gap="md">
            <Text>
              Beaucoup de gens savent quelque chose mais se taisent, par peur d'avoir des problèmes.
              C'est pour ça que le témoignage est anonyme par défaut.
            </Text>
            <Text c="dimmed" size="sm">
              Votre numéro sert uniquement à ce qu'un modérateur puisse vous rappeler. Il n'est jamais affiché,
              pas même à la famille, et chaque fois qu'un modérateur le consulte, c'est enregistré.
            </Text>
            <Anchor component={Link} to="/dossiers" fw={600}>
              <Group gap={6}>Voir les avis en cours <IconArrowRight size={16} /></Group>
            </Anchor>
          </Stack>
        </Grid.Col>
      </Grid>
    </Container>
    </>
  )
}
