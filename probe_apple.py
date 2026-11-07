"""Read-only probes against endpoints found in Apple's public web client.
No Apple Account credentials are accepted or stored.
"""
import concurrent.futures
import datetime
import json
import pathlib
import re
import subprocess
import urllib.parse

ROOT = pathlib.Path(__file__).resolve().parent
OUT = ROOT / 'research'
OUT.mkdir(exist_ok=True)

def fetch(url, token=None, extra=None):
    cmd = ['curl.exe', '--compressed', '--silent', '--show-error', '-L',
           '--connect-timeout', '10', '--max-time', '25', '-w', '\n%{http_code}', url]
    headers = {'Origin': 'https://podcasts.apple.com', 'Referer': 'https://podcasts.apple.com/'}
    if token:
        headers['Authorization'] = 'Bearer ' + token
    headers.update(extra or {})
    for k, v in headers.items():
        cmd += ['-H', f'{k}: {v}']
    p = subprocess.run(cmd, capture_output=True)
    body, _, status = p.stdout.rpartition(b'\n')
    return {'status': status.decode(), 'body': body, 'error': p.stderr.decode(errors='replace')}

def main():
    page = fetch('https://podcasts.apple.com/cn/new')
    html = page['body'].decode('utf-8')
    asset = re.search(r'<script type="module" crossorigin src="([^"]+)"', html).group(1)
    js = fetch('https://podcasts.apple.com' + asset)['body'].decode('utf-8')
    token = re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', js).group()
    urls = sorted(set(re.findall(r'https://amp-api\.podcasts\.apple\.com/[^\s"<>]+', html)))
    paths = sorted(set(re.findall(r'["`](/v1/[^"`]+)["`]', js)))
    (OUT / 'discovered-paths.json').write_text(json.dumps(paths, indent=2), encoding='utf-8')
    base = 'https://amp-api.podcasts.apple.com'
    jobs = {
        'charts': next(u for u in urls if '/charts?' in u and 'types=podcasts&' in u and 'genre=26&' in u and 'chart=top&' in u),
        'new': next(u for u in urls if '/groupings?' in u),
        'search': base + '/v1/catalog/cn/search?term=history&types=podcasts,podcast-episodes&limit=5',
        'search_groups': base + '/v1/catalog/cn/search/groups?term=history&limit=5',
        'library_unauthenticated': base + '/v1/me/library/podcasts?limit=1',
        'positions_unauthenticated': base + '/v1/me/playback/positions/podcast-episodes',
    }
    summary = {'checked_at': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'asset': asset, 'probes': {}}
    responses = {}
    def run(name, url):
        r = fetch(url, token)
        try:
            data = json.loads(r['body'])
        except ValueError:
            data = None
        if data is not None:
            (OUT / (name + '.json')).write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
        return name, {'url': url, 'status': r['status'], 'bytes': len(r['body']), 'keys': list(data) if isinstance(data, dict) else [], 'error': r['error']}, data
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for name, result, data in pool.map(lambda item: run(*item), jobs.items()):
            summary['probes'][name] = result
            responses[name] = data
    # Follow an actual resource ID from a successful chart response.
    def resources(value):
        if isinstance(value, dict):
            if value.get('type') == 'podcasts' and 'id' in value:
                yield value
            for child in value.values():
                yield from resources(child)
        elif isinstance(value, list):
            for child in value:
                yield from resources(child)
    first = next(resources(responses.get('charts')), None)
    if first:
        name, result, data = run('show', base + '/v1/catalog/cn/podcasts/' + first['id'] + '?include=episodes&extend=feedUrl')
        summary['probes'][name] = result
    # Negative control: same catalog endpoint with no bearer token.
    r = fetch(jobs['charts'])
    summary['probes']['charts_without_token'] = {'status': r['status'], 'bytes': len(r['body'])}
    (OUT / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(summary, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
