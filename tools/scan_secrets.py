"""Scan staged text without printing matched secrets. No network or credentials needed."""
import argparse
import pathlib
import re
import subprocess
import sys

RULES = {
    'JWT value': re.compile(rb'eyJ[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}\.[A-Za-z0-9_-]{12,}'),
    'GitHub credential': re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{30,}|github_pat_[A-Za-z0-9_]{40,})'),
    'private key': re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    'credential assignment': re.compile(rb'(?i)(?:media-user-token|password|cookie|authorization|access_token)\s*[=:]\s*[\x22\x27][A-Za-z0-9+/=_-]{24,}[\x22\x27]'),
}
BLOCKED_PARTS = {'research', 'reference', '.tooling', 'outputs', '.gradle', '.kotlin', 'build', 'auth-local'}
BLOCKED_SUFFIXES = {'.jks', '.keystore', '.har', '.apk', '.aab', '.db', '.sqlite', '.pem', '.key', '.log'}

def main():
    parser = argparse.ArgumentParser(); mode = parser.add_mutually_exclusive_group(required=True); mode.add_argument('--staged',action='store_true'); mode.add_argument('--all',action='store_true'); args = parser.parse_args()
    names = subprocess.check_output(['git','diff','--cached','--name-only','--diff-filter=ACMR','-z'] if args.staged else ['git','ls-files','-z']).split(b'\0')
    problems = []
    for raw in filter(None,names):
        name = raw.decode('utf-8'); path = pathlib.PurePosixPath(name)
        if any(part in BLOCKED_PARTS for part in path.parts) or path.suffix.lower() in BLOCKED_SUFFIXES or path.name in {'local.properties','.env'}:
            problems.append((name,'excluded local file')); continue
        data = subprocess.check_output(['git','show',':'+name])
        if b'\0' in data: continue
        for rule, regex in RULES.items():
            for match in regex.finditer(data):
                line = data.count(b'\n',0,match.start())+1
                problems.append((name+':'+str(line),rule))
    for name,rule in problems: print(name+' — '+rule)
    print('Files scanned:',len(list(filter(None,names))),'; findings:',len(problems))
    return int(bool(problems))
if __name__ == '__main__': sys.exit(main())
