#!/usr/bin/env python3
"""Convert simple SVG (paths only) to Android VectorDrawable XML."""
import xml.etree.ElementTree as ET
import sys


def strip_ns(tag: str) -> str:
    return tag.split("}", 1)[-1] if "}" in tag else tag


def hex_to_argb(h: str) -> str:
    h = h.lstrip("#")
    if len(h) == 6:
        return "#FF" + h.upper()
    if len(h) == 8:
        return "#" + h.upper()
    return "#FF000000"


def emit_path(lines, d: str, fill: str, fill_alpha):
    color = hex_to_argb(fill)
    lines.append("    <path")
    lines.append(f'        android:fillColor="{color}"')
    if fill_alpha is not None:
        lines.append(f'        android:fillAlpha="{fill_alpha}"')
    d_esc = d.replace("&", "&amp;")
    lines.append(f'        android:pathData="{d_esc}" />')


def walk(el, lines, inherited_alpha=None):
    tag = strip_ns(el.tag)
    if tag == "defs":
        return
    if tag == "g":
        opacity = el.get("opacity")
        ga = inherited_alpha
        if opacity is not None:
            try:
                ga = float(opacity) * (inherited_alpha or 1.0)
            except ValueError:
                ga = inherited_alpha
        for c in el:
            walk(c, lines, ga)
        return
    if tag == "path":
        d = el.get("d")
        fill = el.get("fill", "#000000")
        if not d or fill == "none":
            return
        opacity = el.get("opacity")
        fa = None
        if opacity is not None:
            try:
                fa = float(opacity) * (inherited_alpha or 1.0)
            except ValueError:
                fa = inherited_alpha
        elif inherited_alpha is not None:
            fa = inherited_alpha
        emit_path(lines, d, fill, fa)
        return
    for c in el:
        walk(c, lines, inherited_alpha)


def main():
    if len(sys.argv) < 3:
        print("usage: svg_to_vector.py input.svg output.xml", file=sys.stderr)
        sys.exit(1)
    inp, outp = sys.argv[1], sys.argv[2]
    tree = ET.parse(inp)
    root = tree.getroot()
    vb = root.get("viewBox", "0 0 800 800").split()
    vw, vh = int(float(vb[2])), int(float(vb[3]))
    lines: list[str] = []
    lines.append('<?xml version="1.0" encoding="utf-8"?>')
    lines.append('<vector xmlns:android="http://schemas.android.com/apk/res/android"')
    lines.append('    android:width="24dp"')
    lines.append('    android:height="24dp"')
    lines.append(f'    android:viewportWidth="{vw}"')
    lines.append(f'    android:viewportHeight="{vh}">')
    for child in root:
        walk(child, lines, None)
    lines.append("</vector>")
    out = "\n".join(lines) + "\n"
    with open(outp, "w", encoding="utf-8") as f:
        f.write(out)
    print(outp)


if __name__ == "__main__":
    main()
