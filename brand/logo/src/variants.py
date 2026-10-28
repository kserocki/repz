"""Builds colour variants, app icons and the animation storyboard from the outlined masters."""
import re, os
M = "master"
def body(name):
    s = open(f"{M}/{name}.svg").read()
    return re.search(r"<defs/>(.*)</svg>", s, re.S).group(1).replace('fill="#000000"', 'fill="{c}"')
SYM, STACK, HOR = body("symbol"), body("lockup-stacked"), body("lockup-horizontal")
CYAN, DEEP, MIST = "#00C2E0", "#00687A", "#A6EEFF"
INK, PAPER, WHITE = "#0E1416", "#F5FAFB", "#FFFFFF"

def out(path, vb, inner, bg=None, title="Repz logo", rx=0):
    w, h = vb.split()[2:]
    bgr = f'<rect width="{w}" height="{h}" rx="{rx}" fill="{bg}"/>' if bg else ""
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{vb}" width="{w}" height="{h}">'
                          f'<title>{title}</title>{bgr}{inner}</svg>\n')

# Colourways (transparent background = for use on that surface; "-tile" = with background baked in)
ways = {"white": WHITE, "cyan": CYAN, "deep": DEEP, "ink": INK, "black": "#000000"}
for n, c in ways.items():
    out(f"color/symbol-{n}.svg", "0 0 320 160", SYM.format(c=c))
    out(f"color/lockup-stacked-{n}.svg", "0 0 320 260", STACK.format(c=c))
    out(f"color/lockup-horizontal-{n}.svg", "0 0 470 116", HOR.format(c=c))
pad = lambda inner, dx, dy: f'<g transform="translate({dx} {dy})">{inner}</g>'
out("color/lockup-stacked-on-ink.svg", "0 0 400 340", pad(STACK.format(c=CYAN), 40, 40), INK)
out("color/lockup-stacked-on-cyan.svg", "0 0 400 340", pad(STACK.format(c=INK), 40, 40), CYAN)
out("color/lockup-stacked-on-paper.svg", "0 0 400 340", pad(STACK.format(c=DEEP), 40, 40), PAPER)

# App icons. Symbol scaled so its 296-unit width fills 60 % of the icon (inside the Android 66 dp safe zone).
def icon(size, fg, bg, rx=0):
    s = size * 0.60 / 296
    dx = (size - 320 * s) / 2
    dy = (size - 160 * s) / 2
    return f'<g transform="translate({dx:.2f} {dy:.2f}) scale({s:.4f})">{SYM.format(c=fg)}</g>', bg
for name, fg, bg in [("ios-light", INK, CYAN), ("ios-dark", CYAN, INK), ("ios-tinted", "#E8E8E8", "#000000")]:
    inner, b = icon(1024, fg, bg); out(f"app-icon/{name}-1024.svg", "0 0 1024 1024", inner, b, f"Repz {name}")
inner, _ = icon(108, INK, None); out("app-icon/android-foreground-108.svg", "0 0 108 108", inner, None, "foreground")
out("app-icon/android-background-108.svg", "0 0 108 108", "", CYAN, "background")
inner, _ = icon(108, "#000000", None); out("app-icon/android-monochrome-108.svg", "0 0 108 108", inner, None, "monochrome")
inner, _ = icon(512, INK, CYAN); out("app-icon/preview-squircle-512.svg", "0 0 512 512", inner, CYAN, "preview", rx=112)

# Animation storyboard: 6 frames on ink (bar -> inner -> middle -> outer -> settle -> wordmark)
sym = open(f"{M}/symbol.svg").read()
parts = dict(re.findall(r'<path id="([^"]+)" fill="#000000" d="([^"]+)"/>', sym))
def frame(ids, word=False, offsets=None):
    offsets = offsets or {}
    g = ""
    for i in ids:
        dx = offsets.get(i, 0)
        g += f'<path transform="translate({dx} 0)" fill="{CYAN}" d="{parts[i]}"/>'
    if word:
        w = re.search(r'<g transform="translate\(([^)]*)\) scale\(0.7\)">', open("src/lockup-stacked.svg").read())
        wm = re.search(r"<defs/>(.*)</svg>", open(f"{M}/wordmark.svg").read(), re.S).group(1).replace("#000000", CYAN)
        g += f'<g transform="translate({w.group(1).replace(" ", " ")}) scale(0.7)">{wm}</g>'
    return g
L = lambda n: [f"plate-{n}-left", f"plate-{n}-right"]
frames = [
    ("1 · bar grows from centre", frame(["bar"])),
    ("2 · inner plates slam in", frame(["bar"] + L("inner"), offsets={"plate-inner-left": -6, "plate-inner-right": 6})),
    ("3 · middle plates", frame(["bar"] + L("inner") + L("middle"), offsets={"plate-middle-left": -6, "plate-middle-right": 6})),
    ("4 · outer collars", frame(["bar"] + L("inner") + L("middle") + L("outer"), offsets={"plate-outer-left": -6, "plate-outer-right": 6})),
    ("5 · settle (bounce)", frame(["bar"] + L("inner") + L("middle") + L("outer"))),
    ("6 · wordmark rises", frame(["bar"] + L("inner") + L("middle") + L("outer"), word=True)),
]
cells = ""
for i, (label, g) in enumerate(frames):
    x = (i % 3) * 400; y = (i // 3) * 380
    cells += (f'<g transform="translate({x} {y})"><rect width="380" height="360" rx="24" fill="{INK}"/>'
              f'<g transform="translate(30 40)">{g}</g>'
              f'<text x="24" y="340" fill="{MIST}" font-family="Helvetica, Arial" font-size="18">{label}</text></g>')
out("motion/splash-storyboard.svg", "0 0 1180 740", cells, None, "Repz splash storyboard")
