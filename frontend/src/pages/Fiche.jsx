import {
  ActionIcon, Alert, Anchor, Box, Button, Container, CopyButton, Divider, Grid, Group, Modal, Skeleton,
  Table, Text, Timeline, Title, Tooltip,
} from '@mantine/core'
import { IconArrowLeft, IconBrandFacebook, IconBrandWhatsapp, IconCheck, IconLink } from '@tabler/icons-react'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { Dossiers } from '../api'
import { Vignette } from '../components/DossierCard'
import { informer, StatutBadge, Vide } from '../components/ui'
import { date, dateHeure, ilYa, TYPES } from '../format'
import FormTemoignage from './FormTemoignage'
import { useRequete } from '../hooks'

export default function Fiche() {
  const { slug } = useParams()
  const [modal, setModal] = useState(false)
  const { data: d, erreur } = useRequete(() => Dossiers.fiche(slug), [slug])
  const { data: temoignages = [] } = useRequete(() => Dossiers.temoignages(d.id), [d?.id], { actif: !!d })

  if (erreur) {
    return (
      <Container size="lg" pt={40}>
        <Vide titre={erreur.status === 404 ? 'Cet avis n’est pas (ou plus) en ligne' : 'Erreur'}>
          {erreur.status === 404 ? <Anchor component={Link} to="/dossiers">Voir les avis en cours</Anchor> : erreur.message}
        </Vide>
      </Container>
    )
  }

  if (!d) {
    return (
      <Container size="lg" pt={40}>
        <Grid gutter={48}>
          <Grid.Col span={{ base: 12, md: 5 }}><Skeleton height={380} /></Grid.Col>
          <Grid.Col span={{ base: 12, md: 7 }}>
            <Skeleton height={14} width="25%" mb="md" />
            <Skeleton height={40} width="80%" mb="md" />
            <Skeleton height={120} />
          </Grid.Col>
        </Grid>
      </Container>
    )
  }

  const type = TYPES[d.type]
  const estPersonne = d.type === 'PERSONNE'
  const actif = d.statut === 'PUBLIE' || d.statut === 'EN_VERIFICATION'
  const url = window.location.href
  const texteWhatsApp = `${estPersonne ? 'AVIS DE RECHERCHE' : type.libelle.toUpperCase()} : ${d.titre} (${d.quartier}, ${d.ville}). Si vous avez une information : ${url}`

  const lignes = (estPersonne
    ? [
        ['Nom', d.nomComplet],
        ['Âge', d.age && `${d.age} ans`],
        ['Sexe', d.sexe === 'F' ? 'Femme' : d.sexe === 'M' ? 'Homme' : null],
        ['Signalement', d.descriptionPhysique],
        ['Signes particuliers', d.signesDistinctifs],
      ]
    : [
        ['Catégorie', d.categorie],
        ['Marque', d.marque],
        ['Modèle', d.modele],
        ['Couleur', d.couleur],
        ['Signes particuliers', d.signesDistinctifs],
      ]
  ).filter(([, v]) => v)

  return (
    <Container size="lg" pt={32}>
      <Anchor component={Link} to="/dossiers" c="dimmed" size="sm">
        <Group gap={4}><IconArrowLeft size={14} /> Tous les avis</Group>
      </Anchor>

      {d.statut === 'RESOLU' && (
        <Alert color="teal" mt="md" title="Dossier résolu">Merci à celles et ceux qui ont partagé et témoigné.</Alert>
      )}
      {d.statut === 'EN_VERIFICATION' && (
        <Alert color="grape" mt="md">
          Une retrouvaille a été signalée, elle est en cours de vérification. L'avis reste ouvert en attendant.
        </Alert>
      )}

      <Grid gutter={{ base: 24, md: 56 }} mt="lg">
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Box style={{ border: '1px solid var(--mantine-color-default-border)' }}>
            <Vignette dossier={d} ratio={1} taille={96} />
          </Box>
          {!d.photo && d.categorie && !estPersonne && (
            <Text size="xs" c="dimmed" mt={6}>Pas de photo publique, pour protéger les informations de l'objet.</Text>
          )}
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Group gap="sm">
            <Text className="surtitre" c={type.couleur} fw={600}>{type.surtitre}</Text>
            <StatutBadge statut={d.statut} type={d.type} />
          </Group>
          <Title order={1} mt="xs">{d.titre}</Title>
          <Text size="sm" c="dimmed" mt="xs" ff="monospace">
            {d.reference} · en ligne {ilYa(d.datePublication)}
          </Text>

          <Group mt="xl" gap={40} className="filet-fin" pt="md">
            <div>
              <Text size="xs" c="dimmed">
                {estPersonne ? 'Dernier endroit connu' : d.type === 'OBJET_TROUVE' ? 'Trouvé à' : 'Perdu à'}
              </Text>
              <Text fw={600} size="lg">{d.quartier}, {d.ville}</Text>
            </div>
            <div>
              <Text size="xs" c="dimmed">{estPersonne ? 'Depuis le' : 'Le'}</Text>
              <Text fw={600} size="lg">{dateHeure(d.dateFaits)}</Text>
            </div>
          </Group>

          {(d.circonstances || d.description) && (
            <Text mt="lg" style={{ whiteSpace: 'pre-line' }}>{d.circonstances || d.description}</Text>
          )}

          {actif && (
            <Box mt="xl">
              <Button size="md" color="brique.7" onClick={() => setModal(true)} fullWidth maw={360}>
                {d.type === 'OBJET_TROUVE' ? 'C’est à moi / j’ai une info' : 'J’ai vu quelque chose'}
              </Button>
              <Text size="xs" c="dimmed" mt={6}>Pas besoin de compte. Votre nom n'est pas affiché.</Text>
            </Box>
          )}

          <Group mt="xl" gap="xs">
            <Text size="sm" fw={600} mr="xs">Faire circuler :</Text>
            <Button
              component="a"
              href={`https://wa.me/?text=${encodeURIComponent(texteWhatsApp)}`}
              target="_blank"
              rel="noreferrer"
              variant="default"
              leftSection={<IconBrandWhatsapp size={18} color="#25d366" />}
            >
              WhatsApp
            </Button>
            <Tooltip label="Facebook">
              <ActionIcon
                component="a"
                href={`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(url)}`}
                target="_blank"
                rel="noreferrer"
                variant="default"
                size={36}
                aria-label="Partager sur Facebook"
              >
                <IconBrandFacebook size={18} />
              </ActionIcon>
            </Tooltip>
            <CopyButton value={url}>
              {({ copied, copy }) => (
                <Tooltip label={copied ? 'Copié' : 'Copier le lien'}>
                  <ActionIcon variant="default" size={36} onClick={copy} aria-label="Copier le lien">
                    {copied ? <IconCheck size={18} /> : <IconLink size={18} />}
                  </ActionIcon>
                </Tooltip>
              )}
            </CopyButton>
          </Group>
        </Grid.Col>
      </Grid>

      <Grid gutter={{ base: 24, md: 56 }} mt={56}>
        <Grid.Col span={{ base: 12, md: 5 }}>
          <Box className="rubrique">
            <Title order={3} mb="sm">Description</Title>
            <Table verticalSpacing="sm" layout="fixed">
              <Table.Tbody>
                {lignes.map(([k, v]) => (
                  <Table.Tr key={k}>
                    <Table.Td w="40%"><Text size="sm" c="dimmed">{k}</Text></Table.Td>
                    <Table.Td><Text size="sm" fw={500}>{v}</Text></Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
          </Box>
        </Grid.Col>

        <Grid.Col span={{ base: 12, md: 7 }}>
          <Box className="rubrique">
            <Title order={3} mb="md">Témoignages ({temoignages.length})</Title>
            {temoignages.length === 0 ? (
              <Text c="dimmed" size="sm">Aucun témoignage publié pour l'instant.</Text>
            ) : (
              <Timeline bulletSize={14} lineWidth={1}>
                {temoignages.map((t) => (
                  <Timeline.Item
                    key={t.id}
                    title={
                      <Text size="sm" fw={600}>
                        {t.type === 'APERCU' ? 'Aperçu' : 'Information'}
                        {t.quartier && <Text span c="dimmed" fw={400}> · {t.quartier}</Text>}
                        <Text span c="dimmed" fw={400}> · {date(t.dateFaits)}</Text>
                      </Text>
                    }
                  >
                    <Text size="sm" mt={4}>{t.description}</Text>
                    <Text size="xs" c="dimmed" mt={4} fs="italic">{t.auteur}</Text>
                  </Timeline.Item>
                ))}
              </Timeline>
            )}
            <Divider my="lg" />
            <Text size="xs" c="dimmed">
              Ne cherchez pas à intervenir vous-même. En cas de danger, appelez le 117.
            </Text>
          </Box>
        </Grid.Col>
      </Grid>

      <Modal opened={modal} onClose={() => setModal(false)} title={<Text fw={600}>Transmettre une information</Text>} size="lg">
        <FormTemoignage
          dossier={d}
          onEnvoye={() => {
            setModal(false)
            informer('Merci. Votre témoignage sera relu avant d’être publié.')
          }}
        />
      </Modal>
    </Container>
  )
}
