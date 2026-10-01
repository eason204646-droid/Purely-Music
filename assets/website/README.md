# Purely Music homepage

Deploy `index.html` together with `assets/website/`. This is a native static
HTML/CSS/JavaScript page. Fonts, icons, images and animation libraries are local;
there is no package manager, build step or runtime CDN dependency.

## Design

Record-poster composition: oversized Outfit typography, a transparent vinyl
sculpture, cool silver-green surfaces, the app's red artwork, floating navigation
and rounded inset frames. The desktop feature grid has one tall album panel and
two shorter panels. Mobile uses a single column and simplified album poses.

The original navigation anchors, GitHub/download/help/support links and Mulan PSL
v2 attribution are retained. Feature copy follows the README and playback code.
Release details live on GitHub Releases rather than a hardcoded version badge.
Cover images and lyric/waveform examples are clearly described as illustrations.

Progressive enhancement includes a two-cover carousel, lyric-style toggle,
constant-speed audio-format marquee with a pause button, GSAP hero entrance and
ScrollTrigger character reveals. The listening-story title pins only at desktop
widths. Reduced-motion preferences disable entrances, marquee, pinning and
scrubbing. Static content stays readable without JavaScript or GSAP.

## Assets and licenses

- `vinyl-sculpture.webp`: conceptual transparent artwork generated using Codex's
  built-in ImageGen and exported as WebP with alpha. It is not an app screenshot.
  Original: Codex generated-images directory, task
  `01a0f6ed-8ee6-7373-b934-f60cd115a7aa`, image
  `exec-08993ba5-7c61-4560-ab01-a5dc813771fb.png`.
- `album-red.webp`: from `app/src/main/res/drawable/onboarding_album_art.png`.
- `album-coast.webp`: from `app/src/main/res/drawable/default_cover.jpg`.
- `app-icon.webp`: from `app/src/main/ic_launcher-playstore.png`.
- `icons/*.svg`: [Tabler Icons](https://github.com/tabler/tabler-icons) outline
  icons, with stroke width adjusted to 1.5. MIT license: `icons/LICENSE`.
- `fonts/outfit-variable.woff2`: [Outfit](https://github.com/google/fonts/tree/main/ofl/outfit)
  variable font, converted to WOFF2 using fontTools and Brotli.
- `fonts/noto-sans-sc-subset.woff2`: [Noto Sans SC](https://github.com/google/fonts/tree/main/ofl/notosanssc)
  variable font, subset using fontTools to the characters in the HTML, CSS and
  JavaScript, plus printable ASCII. Include new characters when editing copy.
  Both font families use the SIL Open Font License; copies are in `fonts/`.
- `vendor/gsap.min.js` and `vendor/ScrollTrigger.min.js`: GSAP 3.15.0, copied
  from the official npm `gsap` distribution with copyright headers retained.
  License/source links are in `vendor/NOTICE.txt`.

### ImageGen prompt

Use case: stylized-concept. Asset type: transparent hero artwork for Purely Music, a Chinese open source Android local music player landing page with high-end editorial typography. Primary request: a single sculptural black vinyl record floating at an elegant oblique three-quarter angle, dramatically wrapped behind and around by one broad fluid vermilion red translucent silk ribbon suggesting sound and the application's existing red album artwork. Style: exquisite physical 3D still life, photographed material quality, sophisticated and minimal, not a logo or icon, tangible grooves with anisotropic reflections. Composition: one large vinyl disc, seen nearly face-on but subtly tilted, centrally placed, occupies 75 percent of the 1536x1536 square frame. A small solid vermilion paper label in the center with no text. One red silk ribbon arcs upward to top-right and flows softly downward left behind the disc, elegant rather than messy, enough ribbon to make an expressive silhouette. The disc remains clearly readable as a music record. Palette: near-black graphite vinyl, soft silver highlights, vermilion #dc4b40 silk, no other colors. Lighting: soft studio lighting from upper left, beautifully refined dark grooves, the red material glows softly from transmitted light, no neon. Background: genuinely transparent alpha channel, isolated object; no rectangular canvas, no scene background, no floor plane, no cast shadow outside the objects. Constraints: no text, no logos, no typography, no phones, no headphones, no extra discs, no UI, no gradient backdrop, no checkerboard baked into the image, no watermark. This is an artwork to sit beside huge typography, so keep the center quiet and silhouette strong. Deliver a premium square transparent-background raster image.
