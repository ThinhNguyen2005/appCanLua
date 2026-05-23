"""Generate Cân Lúa launcher icons (webp) for all density buckets."""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = r"D:\appCanLua\app\src\main\res"

SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

GREEN_DARK = (27, 122, 58)
GREEN_MID = (43, 168, 74)
GOLD = (212, 160, 23)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient_color(t):
    if t <= 0.55:
        return lerp(GREEN_DARK, GREEN_MID, t / 0.55)
    return lerp(GREEN_MID, GOLD, (t - 0.55) / 0.45)


def make_background(size):
    img = Image.new("RGB", (size, size), GREEN_DARK)
    px = img.load()
    diag = math.sqrt(2) * size
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size)
            px[x, y] = gradient_color(min(1.0, max(0.0, t)))
    return img


def add_waves(img):
    size = img.width
    overlay = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)

    def wave_band(y_base, amp, alpha):
        pts = []
        for x in range(size + 1):
            t = x / size
            y = y_base + amp * math.sin(t * math.pi * 2)
            pts.append((x, y))
        pts.append((size, size))
        pts.append((0, size))
        draw.polygon(pts, fill=(255, 255, 255, alpha))

    wave_band(size * 0.80, size * 0.04, 30)
    wave_band(size * 0.89, size * 0.03, 40)
    return Image.alpha_composite(img.convert("RGBA"), overlay)


def draw_grain(draw, cx, cy, length, width, rot_deg, color):
    """Draw an ellipse at angle rot_deg around (cx, cy)."""
    rot = math.radians(rot_deg)
    cos_r, sin_r = math.cos(rot), math.sin(rot)
    pts = []
    steps = 36
    for i in range(steps):
        a = (i / steps) * math.pi * 2
        ex = (length / 2) * math.cos(a)
        ey = (width / 2) * math.sin(a)
        x = cx + ex * cos_r - ey * sin_r
        y = cy + ex * sin_r + ey * cos_r
        pts.append((x, y))
    draw.polygon(pts, fill=color)


def draw_rice_ear(img):
    size = img.width
    overlay = Image.new("RGBA", (size * 4, size * 4), (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    s = size * 4 / 108.0  # scale factor for 108-unit design space

    def U(v):
        return v * s

    # Stem
    stem_color = (254, 243, 199, 255)
    draw.polygon(
        [
            (U(52.5), U(32)),
            (U(55.5), U(32)),
            (U(55), U(82)),
            (U(53), U(82)),
        ],
        fill=stem_color,
    )

    # Top tip grain
    draw_grain(draw, U(54), U(28.5), U(13), U(9), 90, (255, 248, 225, 255))

    # Pairs: list of (y_center, dist_from_stem, length, width, angle, color)
    levels = [
        (36, 6, 14, 7, -35, (254, 243, 199, 255)),
        (46, 7, 15, 7, -30, (253, 230, 138, 255)),
        (56, 7, 16, 8, -25, (252, 211, 77, 255)),
        (66, 7, 16, 8, -20, (245, 197, 24, 255)),
        (75, 6, 14, 7, -12, (224, 168, 0, 255)),
    ]
    for y, dx, ln, wd, ang, col in levels:
        # Left
        draw_grain(draw, U(54 - dx - ln / 2 * math.cos(math.radians(ang))),
                   U(y + ln / 2 * math.sin(math.radians(ang))),
                   U(ln), U(wd), ang, col)
        # Right (mirror)
        draw_grain(draw, U(54 + dx + ln / 2 * math.cos(math.radians(ang))),
                   U(y + ln / 2 * math.sin(math.radians(ang))),
                   U(ln), U(wd), -ang, col)

    # Leaves at base
    leaf_color = (183, 230, 107, 255)
    draw.polygon(
        [(U(54), U(82)), (U(36), U(78)), (U(42), U(83)), (U(50), U(85)), (U(54), U(86))],
        fill=leaf_color,
    )
    draw.polygon(
        [(U(54), U(82)), (U(72), U(78)), (U(66), U(83)), (U(58), U(85)), (U(54), U(86))],
        fill=leaf_color,
    )

    overlay = overlay.resize((size, size), Image.LANCZOS)
    return Image.alpha_composite(img.convert("RGBA"), overlay)


def round_mask(size):
    mask = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse([0, 0, size * 4, size * 4], fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def square_mask(size):
    # Slightly rounded corners (like adaptive icon mask)
    radius = int(size * 0.22)
    mask = Image.new("L", (size * 4, size * 4), 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([0, 0, size * 4, size * 4], radius=radius * 4, fill=255)
    return mask.resize((size, size), Image.LANCZOS)


def build_icon(size, rounded=False):
    bg = make_background(size)
    bg = add_waves(bg)
    icon = draw_rice_ear(bg)
    mask = round_mask(size) if rounded else square_mask(size)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(icon, (0, 0), mask)
    return out


def main():
    for folder, size in SIZES.items():
        dst_dir = os.path.join(ROOT, folder)
        os.makedirs(dst_dir, exist_ok=True)

        square = build_icon(size, rounded=False)
        square.save(os.path.join(dst_dir, "ic_launcher.webp"), "WEBP", quality=95)

        round_ic = build_icon(size, rounded=True)
        round_ic.save(os.path.join(dst_dir, "ic_launcher_round.webp"), "WEBP", quality=95)

        print(f"{folder}: {size}x{size} OK")

    # Preview at 512
    preview = build_icon(512, rounded=False)
    preview.save(os.path.join(ROOT, "..", "..", "..", "icon_preview.png"))
    preview_round = build_icon(512, rounded=True)
    preview_round.save(os.path.join(ROOT, "..", "..", "..", "icon_preview_round.png"))
    print("Preview saved.")


if __name__ == "__main__":
    main()
