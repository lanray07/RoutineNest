"""Compose original brand artwork and real, unmodified app captures into store layouts."""
import argparse
import hashlib
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageOps
from generate_assets import font
ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'store-assets/premium'


def lines(text, selected, width):
    words = list(text) if ' ' not in text else text.split(' ')
    join = '' if ' ' not in text else ' '
    result, current = [], ''
    for word in words:
        candidate = current + (join if current else '') + word
        if selected.getlength(candidate) > width and current:
            result.append(current); current = word
        else: current = candidate
    if current: result.append(current)
    if any(selected.getlength(x) > width for x in result):
        raise ValueError('Unbreakable copy exceeds layout width')
    return result


def text_block(draw, text, x, y, width, size, locale, color, bold=False, max_lines=6):
    while size >= 18:
        selected = font(size, bold, locale)
        try: wrapped = lines(text, selected, width)
        except ValueError: size -= 2; continue
        if len(wrapped) <= max_lines: break
        size -= 2
    else: raise ValueError('Copy cannot fit safely')
    height = round(size * 1.35)
    for line in wrapped:
        draw.text((x,y), line, font=selected, fill=color)
        y += height
    return y


def icon(size):
    s = 4; width = size*s
    layer = Image.new('RGBA',(width,width)); draw = ImageDraw.Draw(layer)
    # Same nest/check silhouette as the application, with optical stroke tuning.
    draw.ellipse((width*.08,width*.08,width*.92,width*.92),fill='#174E3D')
    draw.arc((width*.22,width*.25,width*.78,width*.76),15,165,fill='#F6F1E7',width=round(width*.050))
    draw.arc((width*.28,width*.24,width*.72,width*.61),15,165,fill='#BBD4BF',width=round(width*.042))
    points=[(width*.39,width*.38),(width*.48,width*.47),(width*.66,width*.28)]
    stroke=round(width*.048)
    draw.line(points,fill='#D1AC72',width=stroke,joint='curve')
    for x,y in points:draw.ellipse((x-stroke/2,y-stroke/2,x+stroke/2,y+stroke/2),fill='#D1AC72')
    return layer.resize((size,size),Image.Resampling.LANCZOS)


def export(locale):
    resource=json.loads((ROOT/f'localization/drafts/{locale}.json').read_text(encoding='utf-8'))
    destination=OUT/locale; destination.mkdir(parents=True,exist_ok=True)
    # Generated abstract artwork is a background, never a screenshot or simulated UI.
    for mode in ['light','dark']:
        background=Image.open(OUT/f'source/{mode}-master.png').convert('RGB')
        promo=ImageOps.fit(background,(1024,500),method=Image.Resampling.LANCZOS)
        draw=ImageDraw.Draw(promo); color='#174E3D' if mode=='light' else '#F5F0E5'
        draw.text((62,159),'RoutineNest',font=font(67,True),fill=color)
        text_block(draw,resource['welcome'],65,263,430,29,locale,color,max_lines=3)
        draw.line((65,383,129,383),fill='#C39B61',width=3)
        promo.save(destination/f'promotional-{mode}-1024x500.png')
    copy=json.loads((ROOT/f'store-assets/metadata/{locale}/screenshot-text.json').read_text(encoding='utf-8'))
    manifest=[]
    for number, concept in enumerate(copy,1):
        source=ROOT/f'store-assets/screenshots/raw/{locale}/tablet/{concept["id"]}.png'
        if not source.exists():manifest.append(dict(id=concept['id'],status='real_capture_pending'));continue
        raw=Image.open(source).convert('RGB')
        if raw.size!=(1920,1200):raise ValueError(f'Unexpected capture dimensions {raw.size}')
        # UI image only receives uniform scaling; no text, controls or data are replaced.
        dark=concept['id']=='07'; background='#102B22' if dark else '#F4F1E9'
        color='#F4F1E9' if dark else '#174E3D'; muted='#BBCDBE' if dark else '#52675B'
        image=Image.new('RGB',(1920,1200),background); draw=ImageDraw.Draw(image)
        draw.text((76,64),'RoutineNest',font=font(38,True),fill=color)
        draw.line((76,132,145,132),fill='#C39B61',width=4)
        y=text_block(draw,concept['headline'],76,252,335,58,locale,color,True,6)
        text_block(draw,concept['caption'],78,y+45,335,31,locale,muted,max_lines=5)
        draw.text((78,1080),f'{number:02d} / {len(copy):02d}',font=font(26),fill=muted)
        screen=raw.resize((1420,888),Image.Resampling.LANCZOS)
        shadow=Image.new('RGBA',image.size);sd=ImageDraw.Draw(shadow)
        sd.rectangle((435,222,1875,1130),fill=(0,0,0,48 if dark else 26));shadow=shadow.filter(ImageFilter.GaussianBlur(18))
        image=Image.alpha_composite(image.convert('RGBA'),shadow)
        image.paste(screen,(440,227))
        file=destination/'tablet'/f'{concept["id"]}.png';file.parent.mkdir(exist_ok=True);image.convert('RGB').save(file)
        manifest.append(dict(id=concept['id'],status='captured_visual_review_pending',source=str(source.relative_to(ROOT)),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),output=str(file.relative_to(ROOT)),dimensions='1920x1200'))
    (destination/'capture-provenance.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
    return sum(x['status'].startswith('captured') for x in manifest)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--locale');args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True);icons=OUT/'icons';icons.mkdir(exist_ok=True)
    for size in [114,512]:icon(size).save(icons/f'icon-{size}.png')
    (icons/'icon.svg').write_text('<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 108 108"><circle cx="54" cy="54" r="45" fill="#174E3D"/><g fill="none" stroke-linecap="round"><path d="M27 58 Q54 87 81 58" stroke="#F6F1E7" stroke-width="5.4"/><path d="M33 47 Q54 70 75 47" stroke="#BBD4BF" stroke-width="4.5"/><path d="M42 41 L52 51 L71 30" stroke="#D1AC72" stroke-width="5.2"/></g></svg>',encoding='utf-8')
    locales=[args.locale] if args.locale else list(json.loads((ROOT/'store-assets/listing-translations.json').read_text(encoding='utf-8')))
    total=sum(export(locale) for locale in locales)
    print(f'{len(locales)} localized artwork sets; {total} genuine screenshot layouts exported. Missing captures remain explicit.')
if __name__=='__main__':main()
