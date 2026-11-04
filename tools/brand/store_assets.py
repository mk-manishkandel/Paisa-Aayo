"""Google Play / F-Droid listing images: icon, feature graphic and captioned phone screenshots.

Usage (from the repo root, same venv as make_assets.py):
  .venv/bin/python tools/brand/store_assets.py <dir with full-size 1080x2400 screenshots>
Then render each generated SVG with rsvg-convert (the script prints the commands it runs).
"""
import base64, os, subprocess, sys
from mark import mark, tosvg, shaped
import pathops
from fontTools.pens.transformPen import TransformPen

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
NOTO = os.path.join(ROOT, "app/src/main/res/font/noto_sans_devanagari.ttf")
PLEX = os.path.join(ROOT, "app/src/main/res/font/ibm_plex_sans.ttf")
OUT = os.path.join(ROOT, "fastlane/metadata/android")
GOLD, INK, PAPER, MUTED, EDGE = "#F59E0B", "#0F172A", "#F8FAFC", "#94A3B8", "#334155"

_, _, CUT = mark()
MARK = tosvg(CUT)
MX0, MY0, MX1, MY1 = CUT.bounds


def text(font, t, wght, size, x, baseline, fill, anchor="start"):
    rec, gs, upm, adv = shaped(font, t, wght)
    sc = size / upm
    if anchor == "middle":
        x -= adv * sc / 2
    p = pathops.Path()
    rec.replay(TransformPen(p.getPen(), (sc, 0, 0, -sc, x, baseline)))
    return f'<path fill="{fill}" d="{tosvg(p)}"/>'


def mark_at(x, y, height):
    s = height / (MY1 - MY0)
    return f'<g transform="translate({x - s * MX0:.1f} {y - s * MY0:.1f}) scale({s:.4f})"><path fill="{GOLD}" d="{MARK}"/></g>'


def render(svg, png, w=None):
    path = png[:-4] + ".svg"
    open(path, "w").write(svg)
    cmd = ["rsvg-convert", path, "-o", png] + (["-w", str(w)] if w else [])
    subprocess.run(cmd, check=True)
    os.remove(path)
    print("wrote", os.path.relpath(png, ROOT))


def icon():
    # Full-bleed square: Play applies its own rounded mask.
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512">'
           f'<rect width="108" height="108" fill="{INK}"/>'
           f'<g transform="translate(54 54) scale(1.3) translate(-54 -54)"><path fill="{GOLD}" d="{MARK}"/></g></svg>')
    for lang in ("en-US", "ne-NP"):
        os.makedirs(f"{OUT}/{lang}/images", exist_ok=True)
        render(svg, f"{OUT}/{lang}/images/icon.png")


def feature_graphic():
    for lang, sub, font, wght in (
        ("en-US", "Hear every payment out loud", PLEX, 500),
        ("ne-NP", "हरेक भुक्तानी ठूलो स्वरमा", NOTO, 600),
    ):
        svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1024 500" width="1024" height="500">'
               f'<rect width="1024" height="500" fill="{INK}"/>'
               + mark_at(96, 130, 240)
               + text(PLEX, "Paisa Aayo", 600, 92, 400, 228, PAPER)
               + text(NOTO, "पैसा आयो", 700, 64, 404, 318, GOLD)
               + text(font, sub, wght, 30, 404, 388, MUTED)
               + '</svg>')
        render(svg, f"{OUT}/{lang}/images/featureGraphic.png")


SHOTS = {
    "en-US": [
        ("en_05_home.png", "Hear every payment out loud", PLEX, 600),
        ("en_06_history.png", "Know what came in, day by day", PLEX, 600),
        ("en_02_welcome.png", "No internet, no ads, no account", PLEX, 600),
        ("en_07_settings.png", "Nepali or English, your choice", PLEX, 600),
    ],
    "ne-NP": [
        ("ne_05_home.png", "हरेक भुक्तानी ठूलो स्वरमा", NOTO, 700),
        ("ne_06_history.png", "दिनदिनैको हिसाब एकै ठाउँमा", NOTO, 700),
        ("ne_07_settings.png", "नेपाली वा अंग्रेजी, तपाईंको रोजाइ", NOTO, 700),
    ],
}


def screenshots(src):
    W, H, PW, X, Y, R = 1080, 1920, 860, 110, 330, 44
    PH = PW * 2400 // 1080
    for lang, items in SHOTS.items():
        d = f"{OUT}/{lang}/images/phoneScreenshots"
        os.makedirs(d, exist_ok=True)
        for i, (name, caption, font, wght) in enumerate(items, 1):
            data = base64.b64encode(open(os.path.join(src, name), "rb").read()).decode()
            svg = (f'<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" '
                   f'viewBox="0 0 {W} {H}" width="{W}" height="{H}">'
                   f'<rect width="{W}" height="{H}" fill="{INK}"/>'
                   + mark_at(W / 2 - 34, 70, 64)
                   + text(font, caption, wght, 62, W / 2, 248, PAPER, anchor="middle")
                   + f'<clipPath id="c"><rect x="{X}" y="{Y}" width="{PW}" height="{PH}" rx="{R}"/></clipPath>'
                   f'<rect x="{X - 6}" y="{Y - 6}" width="{PW + 12}" height="{PH + 12}" rx="{R + 6}" fill="{EDGE}"/>'
                   f'<image clip-path="url(#c)" x="{X}" y="{Y}" width="{PW}" height="{PH}" '
                   f'xlink:href="data:image/png;base64,{data}"/></svg>')
            render(svg, f"{d}/{i}.png")


if __name__ == "__main__":
    icon()
    feature_graphic()
    screenshots(sys.argv[1])
