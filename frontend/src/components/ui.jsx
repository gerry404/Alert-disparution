import { Badge, Button, Group, Modal, Stack, Text, Textarea, Title } from '@mantine/core'
import { notifications } from '@mantine/notifications'
import { useEffect, useState } from 'react'
import { libelleStatut, STATUTS, TYPES } from '../format'

export function TypeBadge({ type }) {
  const t = TYPES[type]
  return <Badge color={t.couleur} variant="light">{t.libelle}</Badge>
}

export function StatutBadge({ statut, type }) {
  return <Badge color={STATUTS[statut].couleur} variant="dot">{libelleStatut(statut, type)}</Badge>
}

export function Vide({ titre, children }) {
  return (
    <Stack align="center" gap={6} py={60} ta="center">
      <Title order={3}>{titre}</Title>
      {children && <Text c="dimmed" maw={420}>{children}</Text>}
    </Stack>
  )
}

export function informer(message, couleur = 'teal') {
  notifications.show({ message, color: couleur, withBorder: true })
}

/** Modale "motif" : rejeter, resoudre, voir une identite, declarer retrouve... */
export function ModalMotif({ ouvert, titre, description, libelleBouton, danger, onValider, onFermer }) {
  const [motif, setMotif] = useState('')
  const [envoi, setEnvoi] = useState(false)
  const [erreur, setErreur] = useState(null)

  useEffect(() => {
    if (ouvert) {
      setMotif('')
      setErreur(null)
    }
  }, [ouvert])

  async function valider(e) {
    e.preventDefault()
    if (!motif.trim()) return setErreur('Le motif est obligatoire')
    setEnvoi(true)
    try {
      await onValider(motif.trim())
      onFermer()
    } catch (err) {
      setErreur(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <Modal opened={ouvert} onClose={onFermer} title={<Text fw={600}>{titre}</Text>}>
      <form onSubmit={valider}>
        <Stack>
          {description && <Text size="sm" c="dimmed">{description}</Text>}
          <Textarea
            label="Motif"
            autosize
            minRows={3}
            value={motif}
            onChange={(e) => setMotif(e.currentTarget.value)}
            error={erreur}
            data-autofocus
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={onFermer}>Annuler</Button>
            <Button type="submit" color={danger ? 'brique' : undefined} loading={envoi}>{libelleBouton}</Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  )
}
