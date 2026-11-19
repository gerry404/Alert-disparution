import { Alert, Button, Fieldset, Radio, Select, SimpleGrid, Stack, Switch, Text, Textarea, TextInput } from '@mantine/core'
import { useState } from 'react'
import { Dossiers } from '../api'
import { formaterTelephone, maintenantLocal } from '../format'
import { useQuartiers } from '../hooks'

export default function FormTemoignage({ dossier, onEnvoye }) {
  const quartiers = useQuartiers()
  const [f, setF] = useState({
    type: 'APERCU',
    dateFaits: maintenantLocal(),
    quartierId: null,
    lieuPrecis: '',
    description: '',
    nomTemoin: '',
    telephoneTemoin: '',
    anonyme: true,
  })
  const [erreurs, setErreurs] = useState({})
  const [message, setMessage] = useState('')
  const [envoi, setEnvoi] = useState(false)

  const set = (cle, v) => setF((x) => ({ ...x, [cle]: v }))
  const champ = (cle) => (e) => set(cle, e.currentTarget.value)

  async function envoyer(e) {
    e.preventDefault()
    setEnvoi(true)
    setErreurs({})
    setMessage('')
    try {
      await Dossiers.temoigner(dossier.id, { ...f, dateFaits: `${f.dateFaits}:00` })
      onEnvoye()
    } catch (err) {
      setErreurs(err.erreurs)
      setMessage(err.message)
    } finally {
      setEnvoi(false)
    }
  }

  const sujet = dossier.type === 'PERSONNE' ? 'cette personne' : 'cet objet'

  return (
    <form onSubmit={envoyer}>
      <Stack>
        <Text size="sm" c="dimmed">À propos de : <Text span fw={600} c="var(--mantine-color-text)">{dossier.titre}</Text></Text>

        <Radio.Group value={f.type} onChange={(v) => set('type', v)}>
          <Stack gap="xs">
            <Radio value="APERCU" label={`J'ai vu ${sujet}`} />
            <Radio value="INFORMATION" label="J'ai une autre information" />
          </Stack>
        </Radio.Group>

        <SimpleGrid cols={{ base: 1, sm: 2 }}>
          <TextInput
            type="datetime-local"
            label="Quand ?"
            value={f.dateFaits}
            max={maintenantLocal()}
            onChange={champ('dateFaits')}
            error={erreurs.dateFaits}
            required
          />
          <Select
            label="Dans quel quartier ?"
            placeholder="Choisir"
            searchable
            clearable
            data={quartiers.map((q) => ({ value: String(q.id), label: q.nom }))}
            value={f.quartierId}
            onChange={(v) => set('quartierId', v)}
          />
        </SimpleGrid>

        <TextInput
          label="Repère"
          description="Devant quelle boutique, quel carrefour…"
          value={f.lieuPrecis}
          onChange={champ('lieuPrecis')}
        />

        <Textarea
          label="Ce que vous avez vu ou appris"
          autosize
          minRows={4}
          value={f.description}
          onChange={champ('description')}
          error={erreurs.description}
          required
        />

        <Fieldset legend="Pour qu'on puisse vous rappeler" variant="filled">
          <Stack gap="sm">
            <SimpleGrid cols={{ base: 1, sm: 2 }}>
              <TextInput
                label="Téléphone"
                type="tel"
                placeholder="6XX XX XX XX"
                value={f.telephoneTemoin}
                onChange={(e) => set('telephoneTemoin', formaterTelephone(e.currentTarget.value))}
                inputMode="numeric"
                error={erreurs.telephoneTemoin}
                required
              />
              <TextInput label="Nom (facultatif)" value={f.nomTemoin} onChange={champ('nomTemoin')} />
            </SimpleGrid>
            <Switch
              label="Ne pas afficher mon nom sur l'avis"
              checked={f.anonyme}
              onChange={(e) => set('anonyme', e.currentTarget.checked)}
            />
            <Text size="xs" c="dimmed">
              Votre numéro n'est jamais publié ni donné à la famille. Seul un modérateur peut le voir.
            </Text>
          </Stack>
        </Fieldset>

        {message && <Alert color="brique">{message}</Alert>}

        <Button type="submit" size="md" loading={envoi}>Envoyer</Button>
      </Stack>
    </form>
  )
}
