"""Original geometric RoutineNest identity. Editable SVG sources + PNG exports."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "store-assets/identity"
OUT.mkdir(parents=True, exist_ok=True)
FONT = Path("C:/Windows/Fonts/segoeui.ttf")
BOLD = Path("C:/Windows/Fonts/segoeuib.ttf")

def font(size, bold=False, locale="en-GB"):
    language = locale.split("-")[0]
    candidates = {
        "zh": ["msyhbd.ttc" if bold else "msyh.ttc", "msjh.ttc"],
        "ja": ["YuGothB.ttc" if bold else "YuGothM.ttc", "msgothic.ttc"],
        "ko": ["malgunbd.ttf" if bold else "malgun.ttf"],
        "hi": ["NirmalaB.ttf" if bold else "Nirmala.ttf", "nirmala.ttf"],
        "ar": ["segoeuib.ttf" if bold else "segoeui.ttf", "arial.ttf"]
    }
    if language in candidates:
        for name in candidates[language]:
            path = Path("C:/Windows/Fonts") / name
            if path.exists(): return ImageFont.truetype(str(path), size)
        raise ValueError(f"No suitable installed font for {locale}. Configure a licensed locale font before export.")
    path = BOLD if bold else FONT
    return ImageFont.truetype(str(path), size) if path.exists() else ImageFont.load_default(size=size)

def mark(size, dark=False):
    scale = 4
    image = Image.new("RGBA", (size * scale, size * scale))
    draw = ImageDraw.Draw(image)
    def box(*points): return tuple(round(p * size * scale / 108) for p in points)
    green = "#B0D7BB" if dark else "#205B47"
    cream = "#131D18" if dark else "#F6F4ED"
    draw.ellipse(box(9,9,99,99), fill=green)
    draw.arc(box(23,33,85,78), 15,165, fill=cream, width=round(size*scale*5/108))
    draw.arc(box(29,27,79,65), 15,165, fill=cream, width=round(size*scale*5/108))
    draw.line([box(40,39),box(51,50),box(70,30)], fill=cream, width=round(size*scale*5/108), joint="curve")
    return image.resize((size,size), Image.Resampling.LANCZOS)

def run():
    for dark in [False, True]:
        mode = "dark" if dark else "light"
        for size in [114,512]: mark(size,dark).save(OUT / f"icon-{mode}-{size}.png")
        bg, green = ("#131D18","#B0D7BB") if dark else ("#F6F4ED","#205B47")
        (OUT / f"icon-{mode}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108"><circle cx="54" cy="54" r="45" fill="{green}"/><g fill="none" stroke="{bg}" stroke-width="5" stroke-linecap="round"><path d="M27 58 Q54 87 81 58 M33 47 Q54 70 75 47 M40 39 L51 50 L70 30"/></g></svg>')
        promo = Image.new("RGB",(1024,500),bg); promo.paste(mark(230,dark),(65,135),mark(230,dark)); draw=ImageDraw.Draw(promo)
        draw.text((340,150),"RoutineNest",font=font(68,True),fill=green); draw.text((343,244),"Make room for a better day.",font=font(32),fill=green)
        promo.save(OUT / f"promotional-art-{mode}-1024x500.png")
        # Editable marketing layout template; contains no simulated app screenshots.
        (OUT / f"screenshot-background-{mode}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="1280" height="800"><rect width="1280" height="800" fill="{bg}"/><circle cx="1190" cy="-20" r="240" fill="{green}" opacity=".10"/><text x="64" y="75" font-family="Segoe UI,sans-serif" font-weight="700" font-size="44" fill="{green}">HEADLINE</text><text x="64" y="124" font-family="Segoe UI,sans-serif" font-size="24" fill="{green}">CAPTION</text></svg>')
    illustrations = {
        "onboarding":"M40,130 Q120,195 200,130 M55,108 Q120,163 185,108 M75,88 Q120,123 165,88 M100,64 L117,81 L150,46",
        "empty_state":"M65,150 L175,150 L175,55 L65,55 Z M85,82 L115,82 M85,106 L155,106 M85,130 L140,130",
        "premium_growth":"M120,160 L120,75 M120,110 Q70,100 70,55 Q120,50 120,110 M120,135 Q175,125 175,80 Q120,80 120,135 M70,170 Q120,200 175,170"
    }
    for name,path in illustrations.items():
        for mode,green,bg in [("light","#205B47","#DCEADE"),("dark","#B0D7BB","#284B3C")]:
            svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="240" height="220" viewBox="0 0 240 220"><rect x="15" y="10" width="210" height="200" rx="44" fill="{bg}"/><path d="{path}" fill="none" stroke="{green}" stroke-width="7" stroke-linecap="round" stroke-linejoin="round"/></svg>'
            (OUT / f"{name}-{mode}.svg").write_text(svg)
        xml=f'<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="240dp" android:height="220dp" android:viewportWidth="240" android:viewportHeight="220"><path android:fillColor="#DCEADE" android:pathData="M45,10 L195,10 Q225,10 225,40 L225,180 Q225,210 195,210 L45,210 Q15,210 15,180 L15,40 Q15,10 45,10 Z"/><path android:fillColor="@android:color/transparent" android:strokeColor="#205B47" android:strokeWidth="7" android:strokeLineCap="round" android:strokeLineJoin="round" android:pathData="{path}"/></vector>'
        (ROOT / f"app/src/main/res/drawable/{name}.xml").write_text(xml)
        dark_xml=xml.replace("#DCEADE","#284B3C").replace("#205B47","#B0D7BB")
        dark_dir = ROOT / "app/src/main/res/drawable-night"; dark_dir.mkdir(exist_ok=True)
        (dark_dir / f"{name}.xml").write_text(dark_xml)
    categories={"morning":"M30,60 L78,60 M54,48 L54,20 M35,35 L25,25 M73,35 L83,25 M32,60 A22,22 0,0 1,76 60", "evening":"M73,20 A35,35 0,1 0,88 78 A30,30 0,0 1,73 20", "focus":"M25,25 L40,25 M25,25 L25,40 M83,25 L68,25 M83,25 L83,40 M25,83 L40,83 M25,83 L25,68 M83,83 L68,83 M83,83 L83,68 M54,40 A14,14 0,1 0,54 68 A14,14 0,1 0,54 40"}
    for name,path in categories.items():
        (OUT / f"category-{name}.svg").write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108"><path d="{path}" fill="none" stroke="#205B47" stroke-width="5" stroke-linecap="round"/></svg>')
    (OUT / "DESIGN.md").write_text("# RoutineNest identity\n\nOriginal nest/check mark and geometric illustrations generated by scripts/generate_assets.py. Palette: forest #205B47, linen #F6F4ED, sage #DCEADE; dark forest #131D18. Typography: Android system sans; Segoe UI in Windows marketing renders, editable through SVG. PNG icons use transparent backgrounds. These are simple original geometric assets, not commissioned complex illustrations. Review and align the exported mark and adaptive icon at final brand approval. Marketing backgrounds are templates; real screen captures are required.\n")
    print("Original SVG sources, icons and promotional artwork exported to",OUT)

if __name__ == "__main__": run()
