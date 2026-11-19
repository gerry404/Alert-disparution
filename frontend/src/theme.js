import { createTheme } from '@mantine/core'

// Inspiration : les avis de recherche imprimes et la presse locale.
// Encre noire, papier creme, un seul rouge reserve aux disparitions de personnes.

const encre = [
  '#f5f3ef', '#e7e3dc', '#cdc6bb', '#b1a898', '#998e7b',
  '#8a7e69', '#82765f', '#6f644f', '#3a342b', '#1c1915',
]

const brique = [
  '#fdeeec', '#f6dad6', '#ebb1a9', '#e08679', '#d66251',
  '#d04b38', '#ce3f2b', '#b6311f', '#a3291a', '#8f1f12',
]

export const theme = createTheme({
  primaryColor: 'encre',
  primaryShade: { light: 9, dark: 1 },
  colors: { encre, brique },
  black: '#1c1915',
  white: '#fffdf9',
  defaultRadius: 'sm',
  radius: { xs: '2px', sm: '3px', md: '5px', lg: '8px', xl: '12px' },
  fontFamily: '"IBM Plex Sans", system-ui, sans-serif',
  fontFamilyMonospace: '"IBM Plex Mono", ui-monospace, monospace',
  headings: {
    fontFamily: '"Newsreader", Georgia, serif',
    fontWeight: '600',
    sizes: {
      h1: { fontSize: 'clamp(2rem, 5vw, 3rem)', lineHeight: '1.05' },
      h2: { fontSize: '1.6rem', lineHeight: '1.15' },
      h3: { fontSize: '1.2rem', lineHeight: '1.25' },
    },
  },
  components: {
    Button: { defaultProps: { fw: 600 } },
    Badge: { defaultProps: { radius: 'xs', variant: 'light' } },
    Card: { defaultProps: { withBorder: true, radius: 'sm' } },
    Paper: { defaultProps: { withBorder: true } },
    Modal: { defaultProps: { radius: 'md', centered: true } },
  },
})
