"""Read-only account probe. Paste only the media-user-token locally at the prompt.

Secrets are passed to curl on stdin, never argv, files, or output. Only fixed
Apple HTTPS hosts are contacted; authenticated redirects are not followed.
"""
import argparse
import datetime
import getpass
import json
import pathlib
import re
import subprocess
from probe_apple import fetch

HOST = 'https://amp-api.podcasts.apple.com'
PATHS = {
    'storefront': '/v1/me/storefront',
    'library_shows': '/v1/me/library/podcasts?limit=5',
    'saved_episodes': '/v1/me/library/podcast-episodes?limit=5',
    'channels': '/v1/me/library/podcast-channels?limit=5',
    'recently_played': '/v1/me/recent/played/podcast-episodes?limit=5',
    'continue_playing': '/v1/me/continue-playing/podcast-episodes?limit=5',
}

def config_quote(value):
    if any(ord(c) < 32 or ord(c) == 127 for c in value):
        raise ValueError('Control characters are not allowed.')
    return '"' + value.replace('\\', '\\\\').replace('"', '\\"') + '"'

def request(path, developer_token, user_token):
    if path not in PATHS.values():
        raise ValueError('Only the fixed read-only probe paths are allowed.')
    headers = {
        'Authorization': 'Bearer ' + developer_token,
        'Origin': 'https://podcasts.apple.com',
        'Referer': 'https://podcasts.apple.com/',
    }
    if user_token:
        headers['media-user-token'] = user_token
    config = '\n'.join('header = ' + config_quote(k + ': ' + v) for k, v in headers.items())
    proc = subprocess.run(
        ['curl.exe', '--disable', '--config', '-', '--silent', '--compressed',
         '--proto', '=https', '--connect-timeout', '10', '--max-time', '25',
         '--max-filesize', '2097152', '--write-out', '\n%{http_code}', HOST + path],
        input=config.encode(), capture_output=True, timeout=30,
    )
    body, _, status = proc.stdout.rpartition(b'\n')
    result = {'http_status': status.decode('ascii', errors='replace'), 'transport_exit': proc.returncode}
    try:
        payload = json.loads(body)
    except ValueError:
        result['json'] = False
        return result
    result['json'] = True
    if isinstance(payload, dict):
        data = payload.get('data')
        result['returned_count'] = len(data) if isinstance(data, list) else None
        result['has_next_page'] = bool(payload.get('next'))
        result['error_codes'] = [str(e.get('code', '')) for e in payload.get('errors', []) if isinstance(e, dict)]
        if path == PATHS['storefront'] and isinstance(data, list) and data:
            country = data[0].get('id', '')
            if re.fullmatch('[a-zA-Z]{2}', country):
                result['storefront'] = country
    return result

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--anonymous', action='store_true', help='Run negative controls without account credentials.')
    args = parser.parse_args()
    page = fetch('https://podcasts.apple.com/cn/new')
    if page['status'] != '200':
        raise RuntimeError('Public page unavailable.')
    asset = re.search(r'<script type="module" crossorigin src="([^"]+)"', page['body'].decode()).group(1)
    if not re.fullmatch(r'/assets/[A-Za-z0-9_~.\-]+\.js', asset):
        raise RuntimeError('Unexpected public script path.')
    js = fetch('https://podcasts.apple.com' + asset)['body'].decode()
    developer_token = re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', js).group()
    user_token = ''
    if not args.anonymous:
        print('Paste only media-user-token from your Apple Podcasts test session.')
        print('Input is hidden. It is sent only to amp-api.podcasts.apple.com and is not saved.')
        user_token = getpass.getpass('media-user-token: ').strip()
        if not user_token or any(c.isspace() for c in user_token) or ';' in user_token:
            raise ValueError('Expected one token value, not a Cookie header.')
    report = {
        'checked_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
        'mode': 'anonymous_control' if args.anonymous else 'account_token_read_only',
        'probes': {},
    }
    for name, path in PATHS.items():
        try:
            result = request(path, developer_token, user_token)
        except (subprocess.TimeoutExpired, OSError):
            result = {'transport_error': True}
        report['probes'][name] = result
        print(name + ': ' + json.dumps(result))
    dest = pathlib.Path(__file__).resolve().parent / 'research' / ('account-control.json' if args.anonymous else 'account-summary.json')
    dest.parent.mkdir(exist_ok=True)
    dest.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print('Redacted report saved: ' + str(dest))

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print('Cancelled.')
    except Exception as exc:
        # Avoid printing exception arguments which might contain request data.
        print('Probe stopped: ' + type(exc).__name__)
        raise SystemExit(1)
