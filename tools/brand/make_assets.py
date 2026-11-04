"""Regenerate every logo asset. Run from the repo root: see tools/brand/README.md."""
import sys, pathops
import mark as build
from mark import *
R, PLEX = sys.argv[2], sys.argv[3]
GOLD, INK = "#F59E0B", "#0F172A"
shape, g, cut = mark()
d = tosvg(cut)
x0,y0,x1,y1 = cut.bounds
print("mark bounds", [round(v,1) for v in (x0,y0,x1,y1)])
import math
far = max(math.hypot(px-54,py-54) for c in [cut] for (verb,pts) in c for (px,py) in pts)
print("max dist from centre", round(far,1))
def vec(w, vp, paths):
    body="\n".join(f'    <path android:fillColor="{c}" android:pathData="{p}" />' for c,p in paths)
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Paisa Aayo mark: a speech bubble saying "रु". Source: docs/brand/mark.svg -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="{w}dp" android:height="{w}dp"
    android:viewportWidth="{vp}" android:viewportHeight="{vp}">
{body}
</vector>
'''
res=R+"/app/src/main/res/drawable/"
open(res+"ic_launcher_foreground.xml","w").write(vec(108,108,[(GOLD,d)]))
open(res+"ic_launcher_monochrome.xml","w").write(vec(108,108,[("#FF000000",d)]))
# status bar icon: fit mark into 24x24 with 1dp padding
s = 22/max(x1-x0,y1-y0); small=pathops.Path()
cut.draw(TransformPen(small.getPen(), (s,0,0,s, 12-s*(x0+x1)/2, 12-s*(y0+y1)/2)))
open(res+"ic_stat_listening.xml","w").write(vec(24,24,[("#FFFFFF",tosvg(small))]))
# brand svgs
mark_svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x0-1:.1f} {y0-1:.1f} {x1-x0+2:.1f} {y1-y0+2:.1f}"><title>Paisa Aayo</title><path fill="{GOLD}" d="{d}"/></svg>\n'
open(R+"/docs/brand/mark.svg","w").write(mark_svg)
icon_svg=f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108" width="512" height="512"><title>Paisa Aayo</title><rect width="108" height="108" rx="24" fill="{INK}"/><g transform="translate(54 54) scale(1.32) translate(-54 -54)"><path fill="{GOLD}" d="{d}"/></g></svg>\n'
open(R+"/docs/brand/icon.svg","w").write(icon_svg)
# wordmark as outlines so it renders anywhere without fonts
def text(fontp, t, wght, size, x, baseline, fill):
    rec,gs,upm,adv = shaped(fontp, t, wght); sc=size/upm
    p=pathops.Path(); rec.replay(TransformPen(p.getPen(), (sc,0,0,-sc,x,baseline)))
    return f'<path fill="{fill}" d="{tosvg(p)}"/>', adv*sc
W,H=1280,640
bm = 4.8
mx, my = 130, H/2
en, enw = text(PLEX, "Paisa Aayo", 600, 112, 470, 300, "#F8FAFC")
ne, new = text(build.FONT, "पैसा आयो", 700, 76, 474, 400, GOLD)
sub, subw = text(PLEX, "A payment speaker for Android shops in Nepal", 400, 30, 474, 478, "#94A3B8")
banner=f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}"><title>Paisa Aayo · पैसा आयो</title>
<rect width="{W}" height="{H}" fill="{INK}"/>
<g transform="translate({mx - bm*x0:.1f} {my - bm*(y0+y1)/2:.1f}) scale({bm})"><path fill="{GOLD}" d="{d}"/></g>
{en}{ne}{sub}
</svg>
'''
open(R+"/docs/brand/banner.svg","w").write(banner)
