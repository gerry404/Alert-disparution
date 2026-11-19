import 'leaflet/dist/leaflet.css'
import { Circle, CircleMarker, MapContainer, TileLayer, Tooltip } from 'react-leaflet'
import { date } from '../format'

const ROUGE = '#b6311f'
const ORANGE = '#d97706'

/**
 * Petite carte de la fiche : la zone du quartier (cercle), jamais une adresse precise (RG14),
 * et les quartiers ou des temoins disent avoir vu la personne ou l'objet.
 * Chargee a la demande (Leaflet ~40 Ko gzip) : elle ne pese pas sur le premier affichage.
 */
export default function CarteZone({ dossier, temoignages = [] }) {
  const centre = [dossier.latitude, dossier.longitude]
  const vus = temoignages.filter((t) => t.latitude && t.longitude)
  // avec des temoignages : on cadre pour voir a la fois la zone et les points signales
  const cadrage = vus.length
    ? { bounds: [centre, ...vus.map((t) => [t.latitude, t.longitude])], boundsOptions: { padding: [36, 36], maxZoom: 14 } }
    : { center: centre, zoom: 14 }

  return (
    <div className="carte-zone">
      <MapContainer
        key={vus.length} // les props de Leaflet sont figees au montage : on recadre quand les temoignages arrivent
        {...cadrage}
        scrollWheelZoom={false}
        dragging={!window.matchMedia('(pointer: coarse)').matches} // sur mobile : le doigt fait defiler la page
        attributionControl
        style={{ height: '100%', width: '100%' }}
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
          maxZoom={17}
        />
        <Circle center={centre} radius={600} pathOptions={{ color: ROUGE, fillColor: ROUGE, fillOpacity: 0.18, weight: 2 }}>
          <Tooltip direction="top" permanent className="carte-etiquette">{dossier.quartier}</Tooltip>
        </Circle>
        {vus.map((t) => (
          <CircleMarker
            key={t.id}
            center={[t.latitude, t.longitude]}
            radius={7}
            pathOptions={{ color: '#fff', weight: 2, fillColor: ORANGE, fillOpacity: 1 }}
          >
            <Tooltip direction="top">
              {t.type === 'APERCU' ? 'Aperçu' : 'Information'} · {t.quartier} · {date(t.dateFaits)}
            </Tooltip>
          </CircleMarker>
        ))}
      </MapContainer>
    </div>
  )
}
