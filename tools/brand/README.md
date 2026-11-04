# Brand assets

The logo is generated, not hand-edited. To change it, edit `mark.py` (bubble shape, letter size)
and regenerate everything:

```bash
python3 -m venv .venv && .venv/bin/pip install fonttools uharfbuzz skia-pathops
.venv/bin/python tools/brand/make_assets.py \
  app/src/main/res/font/noto_sans_devanagari.ttf . app/src/main/res/font/ibm_plex_sans.ttf
rsvg-convert -w 512 docs/brand/icon.svg -o docs/brand/icon-512.png   # brew install librsvg
rsvg-convert docs/brand/banner.svg -o docs/brand/banner.png
```

This rewrites `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `ic_stat_listening.xml`
and `docs/brand/*.svg`. `ic_brand_mark.xml` reuses the launcher path inside a `<group>`
translated to its 48×48 crop; update it if the mark's bounds change. Keep the mark inside the
66 dp safe circle (the script prints the furthest point from the centre).
