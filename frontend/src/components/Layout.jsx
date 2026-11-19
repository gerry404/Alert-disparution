import {
  ActionIcon, Anchor, Box, Burger, Button, Container, Divider, Drawer, Group, SimpleGrid, Stack, Text,
  useComputedColorScheme, useMantineColorScheme,
} from '@mantine/core'
import { useDisclosure } from '@mantine/hooks'
import { IconMoon, IconSun } from '@tabler/icons-react'
import { useEffect } from 'react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

function Logo() {
  return (
    <Link to="/" style={{ lineHeight: 1 }}>
      <Text ff="heading" fw={700} fz={24} style={{ letterSpacing: '-0.02em' }}>
        Alerte<Text span inherit c="brique.7">·</Text>Disparition
      </Text>
    </Link>
  )
}

function LienNav({ to, children }) {
  return (
    <NavLink to={to} style={{ textDecoration: 'none' }}>
      {({ isActive }) => (
        <Text fw={isActive ? 600 : 500} c={isActive ? undefined : 'dimmed'} className={isActive ? 'lien-souligne' : ''}>
          {children}
        </Text>
      )}
    </NavLink>
  )
}

export default function Layout() {
  const { utilisateur, estModerateur, deconnecter } = useAuth()
  const [menu, { toggle, close }] = useDisclosure(false)
  const { setColorScheme } = useMantineColorScheme()
  const schema = useComputedColorScheme('light')
  const location = useLocation()
  const navigate = useNavigate()

  useEffect(() => {
    close()
    window.scrollTo(0, 0)
  }, [location.pathname, close])

  async function seDeconnecter() {
    await deconnecter()
    navigate('/')
  }

  const liens = (
    <>
      <LienNav to="/dossiers">Consulter les avis</LienNav>
      {utilisateur && <LienNav to="/moi">Mes signalements</LienNav>}
      {estModerateur && <LienNav to="/moderation">Modération</LienNav>}
    </>
  )

  const compte = utilisateur ? (
    <Anchor component="button" c="dimmed" onClick={seDeconnecter}>Déconnexion</Anchor>
  ) : (
    <LienNav to="/connexion">Connexion</LienNav>
  )

  const basculerTheme = (
    <ActionIcon
      variant="subtle"
      color="gray"
      onClick={() => setColorScheme(schema === 'dark' ? 'light' : 'dark')}
      aria-label="Changer de thème"
    >
      {schema === 'dark' ? <IconSun size={18} /> : <IconMoon size={18} />}
    </ActionIcon>
  )

  const date = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
    .format(new Date())

  return (
    <Box mih="100vh" style={{ display: 'flex', flexDirection: 'column' }}>
      <Box component="header" style={{ borderBottom: '1px solid var(--mantine-color-default-border)' }}>
        <Container size="lg">
          <Group justify="space-between" h={36} visibleFrom="sm">
            <Text size="xs" c="dimmed" tt="capitalize">{date} · Douala</Text>
            <Text size="xs" c="dimmed">
              Danger immédiat : <Anchor href="tel:117" size="xs" fw={600} c="brique.7">117</Anchor> police ·{' '}
              <Anchor href="tel:113" size="xs" fw={600} c="brique.7">113</Anchor> gendarmerie
            </Text>
          </Group>
          <Divider visibleFrom="sm" />
          <Group justify="space-between" h={64}>
            <Logo />
            <Group gap="lg" visibleFrom="md">
              {liens}
              {compte}
              {basculerTheme}
              <Button component={Link} to="/signaler" color="brique.7">Faire un signalement</Button>
            </Group>
            <Group gap="xs" hiddenFrom="md">
              {basculerTheme}
              <Burger opened={menu} onClick={toggle} aria-label="Menu" size="sm" />
            </Group>
          </Group>
        </Container>
      </Box>

      <Drawer opened={menu} onClose={close} position="right" size="80%" title={<Logo />} hiddenFrom="md">
        <Stack gap="lg" mt="md">
          {liens}
          {compte}
          <Button component={Link} to="/signaler" color="brique.7" fullWidth>Faire un signalement</Button>
          <Text size="sm" c="dimmed">
            Danger immédiat : <Anchor href="tel:117" fw={600}>117</Anchor> (police)
          </Text>
        </Stack>
      </Drawer>

      <Box component="main" style={{ flex: 1 }}>
        <Outlet />
      </Box>

      <Box component="footer" mt={80} py={40} style={{ borderTop: '3px solid var(--filet)' }}>
        <Container size="lg">
          <SimpleGrid cols={{ base: 1, sm: 3 }} spacing="xl">
            <Stack gap="xs">
              <Logo />
              <Text size="sm" c="dimmed">
                Plateforme citoyenne de centralisation des avis de recherche. Projet pilote, Douala.
              </Text>
            </Stack>
            <Stack gap={4}>
              <Text fw={600} size="sm">Ce que nous ne sommes pas</Text>
              <Text size="sm" c="dimmed">
                Nous ne remplaçons ni la police, ni la gendarmerie. Ne confrontez jamais vous-même une personne.
              </Text>
            </Stack>
            <Stack gap={4}>
              <Text fw={600} size="sm">Numéros d'urgence</Text>
              <Text size="sm" c="dimmed">Police 117 · Gendarmerie 113 · Pompiers 118</Text>
            </Stack>
          </SimpleGrid>
        </Container>
      </Box>
    </Box>
  )
}
