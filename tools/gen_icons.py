#!/usr/bin/env python3
"""Generate the Oregon Trail launcher icons.

The icon is ASCII art: the in-game covered wagon, rendered in a monospace
font as glowing green phosphor on black, with a shell prompt underneath.
The wagon itself is read straight from the engine's ``Ascii.kt`` so the icon
always matches what the game draws.

Usage:
    python3 tools/gen_icons.py
"""
import os
import re

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
ASCII_KT = os.path.join(ROOT, "engine", "src", "main", "kotlin",
                        "com", "oregontrail", "engine", "Ascii.kt")

FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"

BG = (11, 26, 11, 255)
GLOW = (30, 150, 40, 255)
FG = (140, 255, 140, 255)

# Adaptive icons are 108dp with the visible area being the inner ~72dp.
LEGACY_SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
ADAPTIVE_SIZES = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}


def wagon_art():
    """Pulls ``wagonSmall`` out of the engine so the icon matches the game."""
    src = open(ASCII_KT, encoding="utf-8").read()
    body = re.search(r"val wagonSmall: List<String> = listOf\((.*?)\)", src, re.S).group(1)
    lines = []
    for raw in re.findall(r'"((?:[^"\\]|\\.)*)"', body):
        lines.append(raw.replace('\\"', '"').replace("\\\\", "\\"))
    # A shell prompt so the icon reads as a terminal.
    lines += ["> _"]
    return lines


def render(size, background, content_frac=66.0 / 108.0):
    art = wagon_art()
    img = Image.new("RGBA", (size, size), background)
    if background[3] == 0:
        layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    else:
        layer = img

    # Content stays inside the adaptive-icon safe zone.
    content = int(size * content_frac)
    top_frac = 0.5

    font = None
    px = size
    while px > 4:
        f = ImageFont.truetype(FONT_PATH, px)
        lh = sum(f.getmetrics())
        widest = max(f.getlength(l) for l in art)
        if widest <= content and lh * len(art) <= content:
            font = f
            break
        px -= 1

    if font is None:
        font = ImageFont.truetype(FONT_PATH, 4)
    lh = sum(font.getmetrics())
    block_h = lh * len(art)
    top = int((size - block_h) * top_frac)
    stroke = max(1, round(px * 0.09))

    def draw_all(dst, color, stroked=False):
        for i, line in enumerate(art):
            w = font.getlength(line)
            x = (size - w) / 2.0
            y = top + i * lh
            if stroked:
                dst.text((x, y), line, font=font, fill=color,
                         stroke_width=stroke, stroke_fill=color)
            else:
                dst.text((x, y), line, font=font, fill=color)

    # A faint phosphor bloom, kept tight so thin glyphs never merge.
    glow_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw_all(ImageDraw.Draw(glow_layer), GLOW)
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(radius=max(1, px * 0.07)))
    img = Image.alpha_composite(img, glow_layer)

    draw_all(ImageDraw.Draw(img), FG, stroked=True)
    return img


def round_mask(size):
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse((2, 2, size - 3, size - 3), fill=255)
    return mask


def main():
    for name, size in LEGACY_SIZES.items():
        out = os.path.join(RES, "mipmap-" + name)
        os.makedirs(out, exist_ok=True)
        icon = render(size, BG, content_frac=0.84)
        icon.save(os.path.join(out, "ic_launcher.png"))
        rnd = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        rnd.paste(icon, (0, 0), round_mask(size))
        rnd.save(os.path.join(out, "ic_launcher_round.png"))
        print("legacy  ", name, size)

    for name, size in ADAPTIVE_SIZES.items():
        out = os.path.join(RES, "mipmap-" + name)
        os.makedirs(out, exist_ok=True)
        render(size, (0, 0, 0, 0)).save(os.path.join(out, "ic_launcher_foreground.png"))
        print("adaptive", name, size)


if __name__ == "__main__":
    main()
