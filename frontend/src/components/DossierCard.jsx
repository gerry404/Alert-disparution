import { AspectRatio, Box, Card, Group, Image, Skeleton, Stack, Text, Title } from '@mantine/core'
import {
  IconBackpack, IconBox, IconCreditCard, IconDeviceLaptop, IconDeviceMobile, IconFileText, IconId, IconKey, IconPaw,
} from '@tabler/icons-react'
import { createElement } from 'react'
import { Link } from 'react-router-dom'
import { ilYa, initiales, TYPES } from '../format'
import { StatutBadge } from './ui'

// pictogramme selon la categorie de l'objet (libelles de la migration V2)
const ICONES = [
  [/t[ée]l[ée]phone/i, IconDeviceMobile],
  [/sac|portefeuille/i, IconBackpack],
  [/cl[ée]s/i, IconKey],
  [/identit|passeport/i, IconId],
  [/bancaire/i, IconCreditCard],
  [/document/i, IconFileText],
  [/ordinateur|tablette/i, IconDeviceLaptop],
  [/animal/i, IconPaw],
]

function iconeDe(categorie) {
  return ICONES.find(([motif]) => motif.test(categorie || ''))?.[1] ?? IconBox
}

export function Vignette({ dossier, ratio = 4 / 3, taille = 44 }) {
  return (
    <AspectRatio ratio={ratio}>
      {dossier.photo ? (
        <Image src={dossier.photo} alt={dossier.titre} fit="cover" loading="lazy" />
      ) : (
        <div className="vignette-vide" style={{ fontSize: taille }}>
          {dossier.type === 'PERSONNE'
            ? initiales(dossier.nomComplet || dossier.titre)
            : createElement(iconeDe(dossier.categorie), { size: taille * 1.6, stroke: 1.3 })}
        </div>
      )}
    </AspectRatio>
  )
}

function sousTitre(d) {
  if (d.type === 'PERSONNE') {
    const sexe = d.sexe === 'F' ? 'femme' : d.sexe === 'M' ? 'homme' : null
    return [d.age && `${d.age} ans`, sexe].filter(Boolean).join(', ')
  }
  return [d.categorie, d.marque, d.couleur].filter(Boolean).join(' · ')
}

export default function DossierCard({ dossier: d }) {
  const type = TYPES[d.type]
  return (
    <Card
      component={Link}
      to={`/d/${d.slug}`}
      padding={0}
      className="carte-dossier"
      data-resolu={d.statut === 'RESOLU' || undefined}
    >
      <Vignette dossier={d} />
      <Stack gap={4} p="md">
        <Group justify="space-between" gap="xs">
          <Text className="surtitre" c={type.couleur} fw={600}>{type.surtitre}</Text>
          {d.statut !== 'PUBLIE' && <StatutBadge statut={d.statut} type={d.type} />}
        </Group>
        <Title order={3} lineClamp={2}>{d.titre}</Title>
        {sousTitre(d) && <Text size="sm" c="dimmed">{sousTitre(d)}</Text>}
        <Text size="sm" mt={6}>
          {d.quartier} <Text span c="dimmed">· {ilYa(d.dateFaits)}</Text>
        </Text>
      </Stack>
    </Card>
  )
}

export function CarteSquelette() {
  return (
    <Card padding={0}>
      <Skeleton height={0} style={{ paddingTop: '75%' }} radius={0} />
      <Box p="md">
        <Skeleton height={10} width="30%" mb={10} />
        <Skeleton height={18} width="85%" mb={8} />
        <Skeleton height={12} width="50%" />
      </Box>
    </Card>
  )
}
