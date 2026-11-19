import { Alert, Badge, Button, Container, Group, Pagination, Select, Switch, Table, Tabs, Text, TextInput, Title } from '@mantine/core'
import { useDebouncedValue } from '@mantine/hooks'
import { IconSearch } from '@tabler/icons-react'
import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Admin } from '../api'
import { useAuth } from '../auth'
import { informer } from '../components/ui'
import { dateHeure } from '../format'
import { useRequete } from '../hooks'

const ROLES = [
  { value: 'DECLARANT', label: 'Déclarant' },
  { value: 'MODERATEUR', label: 'Modérateur' },
  { value: 'ADMIN', label: 'Administrateur' },
]

export default function Administration() {
  const { estAdmin, pret, utilisateur } = useAuth()
  if (!pret) return null
  if (!estAdmin) return <Navigate to="/" replace />

  return (
    <Container size="lg" pt={40}>
      <Title order={1}>Administration</Title>
      <Text c="dimmed" mt={4} mb="xl">Comptes, rôles et traçabilité des accès sensibles.</Text>
      <Tabs defaultValue="comptes" color="dark" keepMounted={false}>
        <Tabs.List>
          <Tabs.Tab value="comptes">Comptes</Tabs.Tab>
          <Tabs.Tab value="journal">Journal des accès</Tabs.Tab>
        </Tabs.List>
        <Tabs.Panel value="comptes" pt="lg"><Comptes moiId={utilisateur.id} /></Tabs.Panel>
        <Tabs.Panel value="journal" pt="lg"><Journal /></Tabs.Panel>
      </Tabs>
    </Container>
  )
}

function Comptes({ moiId }) {
  const [q, setQ] = useState('')
  const [qStable] = useDebouncedValue(q, 300)
  const [page, setPage] = useState(0)
  const [erreur, setErreur] = useState('')
  const { data, setData } = useRequete(() => Admin.utilisateurs(qStable, page), [qStable, page])

  async function modifier(u, changements) {
    setErreur('')
    try {
      const maj = await Admin.modifier(u.id, changements)
      setData((p) => ({ ...p, contenu: p.contenu.map((x) => (x.id === u.id ? maj : x)) }))
      informer('Compte mis à jour')
    } catch (e) {
      setErreur(e.message)
    }
  }

  return (
    <>
      <TextInput
        placeholder="Nom ou téléphone"
        leftSection={<IconSearch size={16} />}
        value={q}
        onChange={(e) => { setQ(e.currentTarget.value); setPage(0) }}
        maw={360}
        mb="md"
      />
      {erreur && <Alert color="brique" mb="md">{erreur}</Alert>}
      <Table.ScrollContainer minWidth={640}>
        <Table verticalSpacing="sm" highlightOnHover>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Nom</Table.Th>
              <Table.Th>Téléphone</Table.Th>
              <Table.Th>Inscrit le</Table.Th>
              <Table.Th>Rôle</Table.Th>
              <Table.Th>Actif</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {data?.contenu.map((u) => {
              const moi = u.id === moiId
              return (
                <Table.Tr key={u.id}>
                  <Table.Td>{u.nom} {moi && <Badge size="xs" color="gray">vous</Badge>}</Table.Td>
                  <Table.Td ff="monospace">{u.telephone}</Table.Td>
                  <Table.Td>{dateHeure(u.creeLe)}</Table.Td>
                  <Table.Td w={180}>
                    <Select size="xs" data={ROLES} value={u.role} disabled={moi} allowDeselect={false}
                      onChange={(role) => modifier(u, { role })} />
                  </Table.Td>
                  <Table.Td>
                    <Switch checked={u.actif} disabled={moi} onChange={(e) => modifier(u, { actif: e.currentTarget.checked })}
                      aria-label="Compte actif" />
                  </Table.Td>
                </Table.Tr>
              )
            })}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      {data?.totalPages > 1 && (
        <Group justify="center" mt="md">
          <Pagination total={data.totalPages} value={page + 1} onChange={(p) => setPage(p - 1)} color="dark" />
        </Group>
      )}
      <Text size="xs" c="dimmed" mt="md">Désactiver un compte le déconnecte immédiatement de tous ses appareils.</Text>
    </>
  )
}

function Journal() {
  const [page, setPage] = useState(0)
  const { data } = useRequete(() => Admin.journal(page), [page])
  return (
    <>
      <Text size="sm" c="dimmed" mb="md">
        Chaque fois qu'un modérateur affiche les coordonnées d'un témoin, la consultation est enregistrée ici.
      </Text>
      <Table.ScrollContainer minWidth={600}>
        <Table verticalSpacing="sm">
          <Table.Thead>
            <Table.Tr>
              <Table.Th>Date</Table.Th>
              <Table.Th>Modérateur</Table.Th>
              <Table.Th>Dossier</Table.Th>
              <Table.Th>Motif</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {data?.contenu.map((a, i) => (
              <Table.Tr key={i}>
                <Table.Td style={{ whiteSpace: 'nowrap' }}>{dateHeure(a.date)}</Table.Td>
                <Table.Td>{a.moderateur}</Table.Td>
                <Table.Td ff="monospace">{a.dossierReference}</Table.Td>
                <Table.Td>{a.motif}</Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>
      {data?.total === 0 && <Text c="dimmed" ta="center" py="xl">Aucun accès enregistré.</Text>}
      {data?.totalPages > 1 && (
        <Group justify="center" mt="md">
          <Button variant="subtle" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Précédent</Button>
          <Button variant="subtle" disabled={page + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}>Suivant</Button>
        </Group>
      )}
    </>
  )
}
