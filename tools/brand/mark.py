"""The Paisa Aayo mark: a speech bubble with "रु" cut out of it, in the 108-unit adaptive-icon grid."""
import sys, io, math, pathops, uharfbuzz as hb
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen
from fontTools.pens.recordingPen import RecordingPen, DecomposingRecordingPen
from fontTools.svgLib.path import parse_path
FONT = sys.argv[1]
def ntos(v): return ("%.2f" % v).rstrip("0").rstrip(".")
_cache = {}
def shaped(font_path, text, wght, axes=None):
    key=(font_path,wght)
    if key not in _cache:
        f = instantiateVariableFont(TTFont(font_path), dict(axes or {}, wght=wght))
        b=io.BytesIO(); f.save(b); _cache[key]=(f,b.getvalue())
    f,data=_cache[key]
    font=hb.Font(hb.Face(data)); buf=hb.Buffer(); buf.add_str(text); buf.guess_segment_properties(); hb.shape(font,buf)
    gs=f.getGlyphSet(); order=f.getGlyphOrder(); rec=DecomposingRecordingPen(gs); x=0
    for i,p in zip(buf.glyph_infos, buf.glyph_positions):
        gs[order[i.codepoint]].draw(TransformPen(rec,(1,0,0,1,x+p.x_offset,p.y_offset))); x+=p.x_advance
    return rec, gs, f["head"].unitsPerEm, x
def place(rec, gs, cx, cy, height):
    bp=BoundsPen(gs); rec.replay(bp); x0,y0,x1,y1=bp.bounds; s=height/(y1-y0)
    path=pathops.Path(); rec.replay(TransformPen(path.getPen(), (s,0,0,-s, cx-s*(x0+x1)/2, cy+s*(y0+y1)/2)))
    return path
def svgpath(d):
    p=pathops.Path(); parse_path(d, p.getPen()); return p
def tosvg(p):
    pen=SVGPathPen(None, ntos=ntos); p.draw(pen); return pen.getCommands()
def mark(cx=55, cy=52, r=22.5, glyph_h=25.5, glyph_dy=1.0):
    # bubble: circle + a tail that leaves the lower-left like a hand-cut paper shape
    bubble = svgpath(f"M{cx-r},{cy} a{r},{r} 0 1 0 {2*r},0 a{r},{r} 0 1 0 {-2*r},0z")
    tail = svgpath(f"M{cx-17},{cy+8} C{cx-18.5},{cy+15.5} {cx-21.5},{cy+20} {cx-25},{cy+24} "
                   f"C{cx-18.5},{cy+24} {cx-12.5},{cy+22} {cx-7},{cy+18}z")
    shape = pathops.op(bubble, tail, pathops.PathOp.UNION)
    rec,gs,_,_ = shaped(FONT, "रु", 800)
    g = place(rec, gs, cx+0.6, cy+glyph_dy, glyph_h)
    g.simplify()
    return shape, g, pathops.op(shape, g, pathops.PathOp.DIFFERENCE)
if __name__ == "__main__":
    shape, g, cut = mark()
    print(tosvg(cut))
