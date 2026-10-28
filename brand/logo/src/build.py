"""Generates Repz logo source SVGs. Geometry is documented in brand/LOGO.md."""
# Dumbbell on a 320x160 grid, centre (160, 80). Each part: (x, y, w, h); rx = w/2 (pill ends).
BAR = (100, 70, 120, 20)
PLATES = {  # left side; right side is mirrored around x=160
    "inner":  (72, 16, 28, 128),
    "middle": (40, 32, 24, 96),
    "outer":  (12, 56, 20, 48),
}
def mirror(p):
    x, y, w, h = p
    return (320 - x - w, y, w, h)

def rect(p, id_, fill):
    x, y, w, h = p
    return f'<rect id="{id_}" x="{x}" y="{y}" width="{w}" height="{h}" rx="{min(w,h)/2:g}" fill="{fill}"/>'

def dumbbell(fill):
    parts = [rect(BAR, "bar", fill)]
    for name, p in PLATES.items():
        parts.append(rect(p, f"plate-{name}-left", fill))
        parts.append(rect(mirror(p), f"plate-{name}-right", fill))
    return '<g id="symbol">' + "".join(parts) + "</g>"

# Wordmark "repz": monoline, stroke 22, round caps/joins, bowl radius 31 (centreline).
# Local coords: x-height outer 0..84 (centreline 11..73, middle 42); p descender centreline 113 (outer 124).
# Letter gaps 10. Width 0..343. All diagonals 45 degrees.
STROKE = 22
WORD_PATHS = [
    "M11 73 V42 A31 31 0 0 1 42 11 H57",                            # r
    "M89 42 H151 A31 31 0 1 0 141.9 63.9",                         # e
    "M183 11 V113 M183 42 A31 31 0 1 1 245 42 A31 31 0 1 1 183 42",# p
    "M277 11 H339 L277 73 H339",                                   # z  (x 266..350 outer)
]
WORD_W, WORD_H = 350, 124
def wordmark(fill):
    d = " ".join(WORD_PATHS)
    return (f'<g id="wordmark"><path d="{d}" fill="none" stroke="{fill}" stroke-width="{STROKE}" '
            f'stroke-linecap="round" stroke-linejoin="round"/></g>')

def svg(vb, body, title):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{vb.split()[2]}" '
            f'height="{vb.split()[3]}"><title>{title}</title>{body}</svg>\n')

def write(name, content):
    open(name, "w").write(content)

C = "#000000"
write("symbol.svg", svg("0 0 320 160", dumbbell(C), "Repz symbol"))
write("wordmark.svg", svg(f"0 0 {WORD_W} {WORD_H}", wordmark(C), "Repz wordmark"))
# Stacked: wordmark at 0.7 (245 wide) centred under the symbol, 24 units below the plates.
write("lockup-stacked.svg", svg("0 0 320 260",
      dumbbell(C) + f'<g transform="translate({(320 - WORD_W * 0.7) / 2:g} 168) scale(0.7)">{wordmark(C)}</g>',
      "Repz logo stacked"))
# Horizontal: symbol at 0.6, wordmark at 0.75 with its x-height centred on the bar.
write("lockup-horizontal.svg", svg("0 0 470 116",
      f'<g transform="translate(-7 4) scale(0.6)">{dumbbell(C)}</g>'
      f'<g transform="translate(206 20) scale(0.75)">{wordmark(C)}</g>', "Repz logo horizontal"))
