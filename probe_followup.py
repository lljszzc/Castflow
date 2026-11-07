"""Read-only pagination, episode, audio range and auth-boundary checks."""
import json
import pathlib
import re
import subprocess
from probe_apple import fetch, OUT

root = pathlib.Path(__file__).resolve().parent
html = fetch('https://podcasts.apple.com/cn/new')['body'].decode('utf-8')
asset = re.search(r'<script type="module" crossorigin src="([^"]+)"', html).group(1)
js = fetch('https://podcasts.apple.com' + asset)['body'].decode('utf-8')
token = re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', js).group()
show = json.loads((OUT / 'show.json').read_text(encoding='utf-8'))['data'][0]
episodes = show['relationships']['episodes']
ep = episodes['data'][0]
base = 'https://amp-api.podcasts.apple.com'
jobs = {
    'episode': base + ep['href'],
    'episodes_page2': base + episodes['next'],
    'search_groups_corrected': base + '/v1/catalog/cn/search/groups?term=history&types=podcasts,podcast-episodes&limit=5',
    'positions_corrected_unauthenticated': base + '/v1/me/playback/positions/podcast-episodes?ids=' + ep['id'],
}
summary = {}
for name, url in jobs.items():
    r = fetch(url, token)
    data = json.loads(r['body']) if r['body'] else None
    (OUT / (name + '.json')).write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
    summary[name] = {'url': url, 'status': r['status'], 'bytes': len(r['body']), 'errors': data.get('errors') if isinstance(data, dict) else None}

audio = ep['attributes']['assetUrl']
# Cap downloaded bytes even if an origin ignores the Range request.
p = subprocess.run(['curl.exe', '-L', '--silent', '--show-error', '--connect-timeout', '10', '--max-time', '25', '--range', '0-4095', '--max-filesize', '65536', '-D', str(OUT / 'audio-headers.txt'), '-o', str(OUT / 'audio-sample.bin'), '-w', '%{http_code}', audio], capture_output=True)
summary['audio_range'] = {'url': audio, 'status': p.stdout.decode(), 'error': p.stderr.decode(errors='replace'), 'bytes': (OUT / 'audio-sample.bin').stat().st_size if (OUT / 'audio-sample.bin').exists() else 0}
(OUT / 'followup-summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
