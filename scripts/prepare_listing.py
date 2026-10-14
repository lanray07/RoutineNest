"""Export authored marketplace translations with explicit native-review provenance."""
import csv
import json
from pathlib import Path
ROOT = Path(__file__).resolve().parents[1]

def main():
    source = json.loads((ROOT / 'store-assets/listing-translations.json').read_text(encoding='utf-8'))
    matrix = []
    for locale, values in source.items():
        assert len(values) == 5, locale
        title, short, full, bullets, keywords = values
        assert 0 < len(title) <= 200 and 0 < len(short) <= 1200 and len(short.encode('utf-8')) <= 2000
        assert 0 < len(full) <= 4000 and 3 <= len(bullets.splitlines()) <= 5
        out = ROOT / 'store-assets/metadata' / locale
        out.mkdir(parents=True, exist_ok=True)
        for filename, text in zip(('title.txt', 'short-description.txt', 'full-description.txt', 'features.txt', 'keywords.txt'), values):
            (out / filename).write_text(text + '\n', encoding='utf-8')
        (out / 'release-notes.txt').write_text('RoutineNest 0.2.0\n', encoding='utf-8')
        resource = json.loads((ROOT / f'localization/drafts/{locale}.json').read_text(encoding='utf-8'))
        if locale.startswith('en-'):
            heads = ['Daily routines, a calmer rhythm', 'Your English assistant', 'Morning, evening, focus', 'Every step counts', 'A quieter way to plan', 'Your routine, your way']
            captions = ['A daily checklist built around you.', 'Type a command. Speak where supported.', 'Start with a template. Make it yours.', 'See weekly progress and scheduled-day streaks.', 'Your routine data stays on your device.', 'Choose your appearance and interface language.']
        else:
            heads = [title.split(': ', 1)[-1].split(chr(0xff1a), 1)[-1], resource['assistant_heading'], resource['collection_heading'], resource['weekly_progress'], resource['today_heading'], resource['settings_heading']]
            captions = [resource['today_subheading'], resource['command'], resource['templates'], resource['progress_subheading'], resource['collection_subheading'], resource['privacy_heading']]
        concepts = [dict(id=identifier, headline=head, caption=caption) for identifier, head, caption in zip(('01','03','04','05','07','08'), heads, captions)]
        (out / 'screenshot-text.json').write_text(json.dumps(concepts, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
        for pending in list(out.glob('*.pending')) + [out / 'PENDING.md']:
            if pending.exists(): pending.unlink()
        (out / 'REVIEW.md').write_text('Assistant-authored translation and factual/technical review. Native editorial review remains pending. Command grammar/replies are English-only. Premium purchasing is disabled. Screens must be actual captured UI; captions do not establish UI localization. Search intent is unmeasured.\n', encoding='utf-8')
        matrix.extend([locale, term.strip(), 'relevant title/description/keyword field', 'search-intent hypothesis; unmeasured', 'native editorial review pending'] for term in keywords.split(','))
    with (ROOT / 'store-assets/ASO_KEYWORD_MATRIX.csv').open('w', encoding='utf-8', newline='') as stream:
        writer = csv.writer(stream); writer.writerow(('locale','search_intent','placement','evidence','review')); writer.writerows(matrix)
    print(f'{len(source)} Amazon marketplace copy sets exported within field limits; native review recorded separately.')
if __name__ == '__main__': main()
