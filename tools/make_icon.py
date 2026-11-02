#!/usr/bin/env python3
"""
Material 3 Expressive calculator icon generator (no external assets).

Draws the icon from scratch with Pillow:
  * background : violet -> magenta diagonal gradient + two soft light clouds
  * glyph      : a white calculator card with a tinted display and a 3x2 keypad,
                 the last key ("=") painted in the accent magenta
  * output     : adaptive icon layers (foreground / background / monochrome),
                 legacy launcher PNGs, round icon and a 512px store icon.

Everything is drawn in the 108 x 108 dp adaptive icon canvas. The card and all
keys stay inside the 66 dp safe-zone circle, so no launcher mask crops them.

Run:  python3 tools/make_icon.py
"""

from __future__ import annotations

import os

from PIL import Image, ImageDraw, ImageFilter

# --------------------------------------------------------------------------
# Design constants (in dp, the adaptive icon canvas is 108 x 108 dp)
# --------------------------------------------------------------------------
DP = 108.0

GRADIENT_START = (0x5B, 0x3F, 0xE8)   # deep violet
GRADIENT_END = (0xE8, 0x3F, 0x8C)     # expressive magenta

CARD = (255, 255, 255, 255)
DISPLAY = (0x53, 0x3A, 0xD6)          # violet display bar
KEY = (0x72, 0x5C, 0xEC)              # violet keys
KEY_ACCENT = (0xE8, 0x3F, 0x8C)       # magenta "=" key

# Card geometry (centred, corners ~32.6 dp from the centre: inside the safe zone)
CARD_BOX = (32.0, 29.0, 76.0, 79.0)
CARD_RADIUS = 11.0

DISPLAY_BOX = (38.0, 35.0, 70.0, 41.5)
DISPLAY_RADIUS = 3.25

KEY_COLUMNS = (40.5, 54.0, 67.5)
KEY_ROWS = (55.0, 68.0)
KEY_RADIUS = 4.0

DENSITIES = {
    "mdpi": 1.0,
    "hdpi": 1.5,
    "xhdpi": 2.0,
    "xxhdpi": 3.0,
    "xxxhdpi": 4.0,
}

SUPERSAMPLE = 4

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "..", "app", "src", "main", "res")
DOCS = os.path.join(HERE, "..", "docs")


# ------------------------------------------------------------------ helpers
def lerp(a: float, b: float, t: float) -> int:
    return int(round(a + (b - a) * t))


def diagonal_gradient(size: int) -> Image.Image:
    """Tiny gradient computed per pixel, then upscaled with bicubic (fast + smooth)."""
    n = 96
    small = Image.new("RGB", (n, n))
    pixels = small.load()
    for y in range(n):
        for x in range(n):
            t = (x + y) / (2.0 * (n - 1))
            # ease-in-out so the violet dominates a little longer
            t = t * t * (3.0 - 2.0 * t)
            pixels[x, y] = (
                lerp(GRADIENT_START[0], GRADIENT_END[0], t),
                lerp(GRADIENT_START[1], GRADIENT_END[1], t),
                lerp(GRADIENT_START[2], GRADIENT_END[2], t),
            )
    return small.resize((size, size), Image.BICUBIC)


def soft_glow(size: int, cx: float, cy: float, radius: float, alpha: int) -> Image.Image:
    """Concentric translucent ellipses -> a cheap, smooth radial glow."""
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    steps = 70
    for i in range(steps, 0, -1):
        t = i / steps
        rr = radius * t
        draw.ellipse(
            [cx - rr, cy - rr, cx + rr, cy + rr],
            fill=(255, 255, 255, int(alpha * (1.0 - t) ** 2)),
        )
    return layer


def background_layer(size: int) -> Image.Image:
    """Full bleed gradient + soft light clouds (top-left white, bottom-right warm)."""
    base = diagonal_gradient(size).convert("RGBA")
    unit = size / DP

    top = soft_glow(size, 18 * unit, 12 * unit, 66 * unit, 60)
    base = Image.alpha_composite(base, top)

    warm = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(warm)
    steps = 60
    r = 52 * unit
    cx, cy = 96 * unit, 100 * unit
    for i in range(steps, 0, -1):
        t = i / steps
        rr = r * t
        draw.ellipse(
            [cx - rr, cy - rr, cx + rr, cy + rr],
            fill=(255, 214, 120, int(34 * (1.0 - t) ** 2)),
        )
    warm = warm.filter(ImageFilter.GaussianBlur(2 * unit))
    return Image.alpha_composite(base, warm)


# -------------------------------------------------------------------- glyph
def draw_card(
    canvas: Image.Image,
    size: int,
    *,
    scale: float = 1.0,
    mono: bool = False,
) -> None:
    """Draws the calculator card, scaled around the canvas centre."""
    unit = size / DP
    cx = cy = DP / 2.0

    def sx(x: float) -> float:
        return (cx + (x - cx) * scale) * unit

    def sy(y: float) -> float:
        return (cy + (y - cy) * scale) * unit

    def box(b: tuple[float, float, float, float]) -> list[float]:
        return [sx(b[0]), sy(b[1]), sx(b[2]), sy(b[3])]

    card = box(CARD_BOX)
    radius = CARD_RADIUS * unit * scale

    # --- card -----------------------------------------------------------------
    card_layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(card_layer).rounded_rectangle(card, radius=radius, fill=CARD)

    # Soft shadow so the card separates from the gradient.
    shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        [card[0], card[1] + 2.0 * unit, card[2], card[3] + 2.0 * unit],
        radius=radius,
        fill=(24, 0, 60, 105),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(3.2 * unit))
    canvas.alpha_composite(shadow)

    if mono:
        # Themed icons: crisp white silhouette of the outline + keypad.
        draw = ImageDraw.Draw(canvas)
        stroke = max(1, round(2.6 * unit * scale))
        draw.rounded_rectangle(
            card, radius=radius, outline=(255, 255, 255, 255), width=stroke
        )
        draw.rounded_rectangle(
            box(DISPLAY_BOX), radius=DISPLAY_RADIUS * unit * scale, fill=(255, 255, 255, 255)
        )
        key_r = KEY_RADIUS * unit * scale
        for row_index, row in enumerate(KEY_ROWS):
            for col_index, column in enumerate(KEY_COLUMNS):
                px, py = sx(column), sy(row)
                if col_index == len(KEY_COLUMNS) - 1 and row_index == len(KEY_ROWS) - 1:
                    # "=" key: drawn a touch larger, matching the coloured icon.
                    rr = key_r * 1.15
                else:
                    rr = key_r
                draw.ellipse([px - rr, py - rr, px + rr, py + rr], fill=(255, 255, 255, 255))
        return

    canvas.alpha_composite(card_layer)

    draw = ImageDraw.Draw(canvas)
    draw.rounded_rectangle(
        box(DISPLAY_BOX),
        radius=DISPLAY_RADIUS * unit * scale,
        fill=DISPLAY,
    )

    key_r = KEY_RADIUS * unit * scale
    for row_index, row in enumerate(KEY_ROWS):
        for col_index, column in enumerate(KEY_COLUMNS):
            px, py = sx(column), sy(row)
            accent = col_index == len(KEY_COLUMNS) - 1 and row_index == len(KEY_ROWS) - 1
            rr = key_r * 1.15 if accent else key_r
            draw.ellipse(
                [px - rr, py - rr, px + rr, py + rr],
                fill=KEY_ACCENT if accent else KEY,
            )


# ------------------------------------------------------------------- output
def rounded_mask(size: int, radius_ratio: float) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=int(size * radius_ratio), fill=255
    )
    return mask


def circle_mask(size: int) -> Image.Image:
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, size - 1, size - 1], fill=255)
    return mask


def render(
    size: int,
    *,
    with_background: bool,
    glyph_scale: float,
    mask: Image.Image | None = None,
    round_icon: bool = False,
    mono: bool = False,
) -> Image.Image:
    s = size * SUPERSAMPLE
    if with_background:
        canvas = background_layer(s)
    else:
        canvas = Image.new("RGBA", (s, s), (0, 0, 0, 0))

    draw_card(canvas, s, scale=glyph_scale, mono=mono)
    canvas = canvas.resize((size, size), Image.LANCZOS)

    if mask is not None:
        canvas.putalpha(circle_mask(size) if round_icon else mask)

    return canvas


def save(image: Image.Image, path: str) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    image.save(path, "PNG", optimize=True)
    print("  ->", os.path.relpath(path))


def main() -> None:
    print("Generating Material 3 Expressive calculator icon")

    for density, factor in DENSITIES.items():
        folder = os.path.join(RES, f"mipmap-{density}")
        legacy = round(48 * factor)
        adaptive = round(108 * factor)

        # Adaptive icon layers (Android 8+)
        save(
            render(adaptive, with_background=False, glyph_scale=1.0),
            os.path.join(folder, "ic_launcher_foreground.png"),
        )
        save(
            background_layer(adaptive * SUPERSAMPLE).resize((adaptive, adaptive), Image.LANCZOS),
            os.path.join(folder, "ic_launcher_background.png"),
        )

        # Monochrome layer for Android 13 themed icons (tinted by the system)
        save(
            render(adaptive, with_background=False, glyph_scale=1.0, mono=True),
            os.path.join(folder, "ic_launcher_monochrome.png"),
        )

        # Legacy icons (Android 7 and below)
        legacy_mask = rounded_mask(legacy, 0.22)
        save(
            render(legacy, with_background=True, glyph_scale=1.28, mask=legacy_mask),
            os.path.join(folder, "ic_launcher.png"),
        )
        save(
            render(
                legacy,
                with_background=True,
                glyph_scale=1.28,
                mask=legacy_mask,
                round_icon=True,
            ),
            os.path.join(folder, "ic_launcher_round.png"),
        )

    # Play Store / README artwork: full square, no mask (the store applies its own).
    save(render(512, with_background=True, glyph_scale=1.15), os.path.join(DOCS, "icon-512.png"))

    print("Done.")


if __name__ == "__main__":
    main()
