"""Extended API verification. Credentials stay in memory; reports are redacted.

--write-tests temporarily follows and saves public test content, then removes
only those additions. It also writes an existing playback position unchanged.
No purchases, subscriptions, reviews, or existing library removals are tested.
"""
import argparse
import datetime
import getpass
import json
import pathlib
import re
import subprocess
import time
from urllib.parse import urlsplit, parse_qs, urlencode
from probe_account import config_quote
from probe_apple import fetch

ROOT = pathlib.Path(__file__).resolve().parent
HOST = 'https://amp-api.podcasts.apple.com'
REPORT = ROOT / 'research' / 'extended-summary.json'
LIBRARIES = ('/v1/me/library/podcasts', '/v1/me/library/podcast-episodes')

class ProbeError(Exception):
    pass

def normalized_path(value):
    parsed = urlsplit(value)
    if parsed.scheme or parsed.netloc:
        if parsed.scheme != 'https' or parsed.netloc != 'amp-api.podcasts.apple.com':
            raise ProbeError('Unexpected pagination host')
    if not parsed.path.startswith('/v1/') or parsed.fragment:
        raise ProbeError('Unexpected API path')
    return parsed.path + ('?' + parsed.query if parsed.query else '')

class Client:
    def __init__(self, public, user):
        self.public, self.user = public, user
        self.mutation_targets = set()

    def call(self, path, method='GET', body=None):
        path = normalized_path(path)
        if method != 'GET' and (method, path) not in self.mutation_targets:
            raise ProbeError('Mutation is not in the explicit test target set')
        headers = {'Authorization': 'Bearer ' + self.public,
                   'media-user-token': self.user,
                   'Origin': 'https://podcasts.apple.com',
                   'Referer': 'https://podcasts.apple.com/',
                   'Cache-Control': 'no-cache'}
        if body is not None:
            headers['Content-Type'] = 'application/json'
        config = '\n'.join('header = ' + config_quote(k + ': ' + v) for k, v in headers.items())
        if body is not None:
            config += '\ndata = ' + config_quote(json.dumps(body))
        p = subprocess.run(['curl.exe', '--disable', '--config', '-', '--silent', '--compressed',
                            '--proto', '=https', '--request', method, '--connect-timeout', '10',
                            '--max-time', '25', '--max-filesize', '4194304',
                            '--write-out', '\n%{http_code}', HOST + path],
                           input=config.encode(), capture_output=True, timeout=30)
        raw, _, status = p.stdout.rpartition(b'\n')
        if p.returncode:
            raise ProbeError('Transport failure')
        code = int(status)
        data = json.loads(raw) if raw else {}
        return code, data

    def collection(self, path, max_pages=30):
        rows, seen = [], set()
        while path:
            path = normalized_path(path)
            if path in seen or len(seen) >= max_pages:
                raise ProbeError('Pagination incomplete; refusing mutation')
            seen.add(path)
            status, payload = self.call(path)
            if status != 200 or not isinstance(payload.get('data'), list):
                raise ProbeError('Collection did not return valid 200 data')
            rows.extend(payload['data'])
            path = payload.get('next')
        return rows, len(seen)

def numeric_ids(rows):
    ids = set()
    for row in rows:
        value = str(row.get('id', ''))
        if not re.fullmatch(r'\d+', value):
            raise ProbeError('Unexpected library identity format; refusing mutation')
        ids.add(value)
    return ids

def verify_membership(client, path, identifier, present):
    for attempt in range(5):
        rows, _ = client.collection(path + '?limit=30')
        if (identifier in numeric_ids(rows)) == present:
            return True
        if attempt < 4:
            time.sleep(1)
    return False

def roundtrip(client, path, identifier, baseline):
    if identifier in numeric_ids(baseline):
        return {'skipped': 'already_present'}
    if not re.fullmatch(r'\d+', identifier):
        raise ProbeError('Unexpected mutation resource ID')
    # Apple's URL builder appends a singleton ID to the path. The ids query
    # parameter is only used for multiple resources; collection POST is 405.
    target = path + '/' + identifier
    client.mutation_targets.update({('POST', target), ('DELETE', target)})
    result = {'test_resource_id': identifier}
    attempted = False
    try:
        attempted = True
        result['add_status'], _ = client.call(target, 'POST')
        result['add_visible'] = verify_membership(client, path, identifier, True)
    finally:
        if attempted:
            # Target was absent in the complete baseline. Cleanup is needed even
            # if the POST response was lost after the server applied it.
            try:
                result['remove_status'], _ = client.call(target, 'DELETE')
                result['cleanup_verified'] = verify_membership(client, path, identifier, False)
            except Exception:
                result['cleanup_verified'] = False
            result['passed'] = (result.get('add_status') in (200, 201, 202, 204)
                                and result.get('add_visible') is True
                                and result.get('cleanup_verified') is True)
            save_result('follow_roundtrip' if path == LIBRARIES[0] else 'save_roundtrip', result)
    if not result['cleanup_verified']:
        raise ProbeError('Cleanup not confirmed. See report test_resource_id; stop further writes.')
    return result

report = {}
def save_result(name, result):
    report.setdefault('checks', {})[name] = result
    REPORT.parent.mkdir(exist_ok=True)
    REPORT.write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(name + ': ' + json.dumps(result), flush=True)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write-tests', action='store_true')
    parser.add_argument('--position-only', action='store_true', help='Only repeat the unchanged playback-position write; skip follow/save mutations.')
    args = parser.parse_args()
    report.update({'probe_revision': 3, 'checked_at': datetime.datetime.now(datetime.timezone.utc).isoformat(),
                   'write_tests_requested': args.write_tests, 'checks': {}})
    html = fetch('https://podcasts.apple.com/cn/new')['body'].decode()
    asset = re.search(r'<script type="module" crossorigin src="([^"]+)"', html).group(1)
    if not re.fullmatch(r'/assets/[A-Za-z0-9_~.\-]+\.js', asset):
        raise ProbeError('Unexpected script path')
    source = fetch('https://podcasts.apple.com' + asset)['body'].decode()
    public = re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+', source).group()
    print('Input token locally; it is not saved. Only Apple Podcasts API is contacted.')
    if args.write_tests:
        print('Will temporarily follow/save test content, verify, then remove those additions.')
        print('Will write an existing playback position unchanged; no new listening position is created.')
    user = getpass.getpass('media-user-token: ').strip()
    if not user or any(c.isspace() for c in user) or ';' in user:
        raise ProbeError('Expected one token value')
    client = Client(public, user)
    status, payload = client.call('/v1/me/storefront')
    country = payload.get('data', [{}])[0].get('id', '')
    if status != 200 or not re.fullmatch(r'[a-z]{2}', country):
        raise ProbeError('Account storefront unavailable')
    save_result('storefront', {'status': status, 'country': country})
    shows, pages = client.collection(LIBRARIES[0] + '?limit=30')
    save_result('library_baseline', {'count': len(shows), 'pages': pages})
    saved, pages = client.collection(LIBRARIES[1] + '?limit=30')
    save_result('saved_baseline', {'count': len(saved), 'pages': pages})
    for key, path in [('recent_pagination', '/v1/me/recent/played/podcast-episodes'),
                      ('continue_pagination', '/v1/me/continue-playing/podcast-episodes'),
                      ('new_episodes', '/v1/me/library/new-episodes'),
                      ('latest_episodes', '/v1/me/library/latest-episodes')]:
        status, data = client.call(path + '?limit=5')
        result = {'status': status, 'count': len(data.get('data', []))}
        if status == 200 and data.get('next'):
            code2, data2 = client.call(data['next'])
            first_ids = {x['id'] for x in data.get('data', [])}
            second_ids = {x['id'] for x in data2.get('data', [])}
            result.update({'page2_status': code2, 'page2_count': len(second_ids),
                           'distinct_new_items': len(second_ids - first_ids)})
        save_result(key, result)
    # Search/catalog selectors are public and do not depend on personal data.
    status, search = client.call('/v1/catalog/' + country + '/search?term=history&types=podcasts&limit=10')
    candidates = search.get('results', {}).get('podcasts', {}).get('data', [])
    existing = numeric_ids(shows)
    candidate = next((x for x in candidates if str(x.get('id')) not in existing), None)
    if not candidate:
        raise ProbeError('No unused public test show found')
    show_id = str(candidate['id'])
    if not re.fullmatch(r'\d+', show_id):
        raise ProbeError('Unexpected test show ID')
    status, details = client.call('/v1/catalog/' + country + '/podcasts/' + show_id + '?include=episodes')
    episodes = details.get('data', [{}])[0].get('relationships', {}).get('episodes', {}).get('data', [])
    episode = next((x for x in episodes if str(x.get('id')) not in numeric_ids(saved)), None)
    save_result('test_catalog', {'status': status, 'episode_available': episode is not None})
    if args.write_tests and not args.position_only:
        roundtrip(client, LIBRARIES[0], show_id, shows)
        if episode:
            roundtrip(client, LIBRARIES[1], str(episode['id']), saved)
    # Validate position schema on a real existing continue-playing record.
    status, continuing = client.call('/v1/me/continue-playing/podcast-episodes?limit=5')
    ids = [str(x['id']) for x in continuing.get('data', []) if re.fullmatch(r'\d+', str(x.get('id', '')))]
    if ids:
        path = '/v1/me/playback/positions/podcast-episodes?' + urlencode({'ids': ','.join(ids)})
        code, positions = client.call(path)
        entries = positions.get('data', [])
        save_result('positions_read', {'status': code, 'count': len(entries),
                                     'attribute_keys': sorted(entries[0].get('attributes', {}).keys()) if entries else []})
        entry = next((x for x in entries if x.get('attributes', {}).get('positionInMilliseconds') is not None
                      and str(x.get('id', '')) in ids), None)
        if args.write_tests and entry:
            path = '/v1/me/playback/positions/podcast-episodes/' + str(entry['id'])
            attrs = {k: v for k, v in entry['attributes'].items() if k in ('positionInMilliseconds', 'completed', 'recordedAtTimestamp')}
            # Keep original timestamp as well. This proves acceptance/readback,
            # not conflict resolution or that the service applies newer progress.
            body = {'type': 'playback-positions', 'attributes': attrs}
            client.mutation_targets.add(('PUT', path))
            write_code, _ = client.call(path, 'PUT', body)
            # Read with the already verified collection schema, independently
            # from the singleton write endpoint's response shape.
            read_path = '/v1/me/playback/positions/podcast-episodes?' + urlencode({'ids': entry['id']})
            read_code, readback = client.call(read_path)
            after_entry = next((x for x in readback.get('data', []) if str(x.get('id')) == str(entry['id'])), {})
            after = after_entry.get('attributes', {})
            save_result('position_idempotent_write', {'write_status': write_code, 'read_status': read_code,
                        'readback_resource_found': bool(after_entry),
                        'position_unchanged': after.get('positionInMilliseconds') == attrs.get('positionInMilliseconds'),
                        'completed_unchanged': after.get('completed') == attrs.get('completed'),
                        'limitation': 'Does not prove actual progress changes or cross-device conflict handling'})
    save_result('finished', {'completed': True, 'session_refresh_tested': False,
                             'android_login_tested': False, 'paid_content_tested': False})

if __name__ == '__main__':
    try:
        main()
    except (Exception, KeyboardInterrupt) as exc:
        save_result('stopped', {'exception_type': type(exc).__name__, 'completed': False})
        raise SystemExit(1)
