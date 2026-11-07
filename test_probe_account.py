import json
import subprocess
import unittest
from unittest.mock import patch
import probe_account as probe


class AccountProbeSafetyTests(unittest.TestCase):
    def test_secret_stays_off_command_line_and_out_of_report(self):
        payload = {'data': [{'id': 'private-id', 'attributes': {'name': 'private-title'}}]}
        response = subprocess.CompletedProcess([], 0, json.dumps(payload).encode() + b'\n200', b'')
        with patch.object(probe.subprocess, 'run', return_value=response) as run:
            result = probe.request(probe.PATHS['library_shows'], 'test-public', 'test-private')
        args, kwargs = run.call_args
        self.assertNotIn('test-private', repr(args))
        self.assertIn(b'media-user-token: test-private', kwargs['input'])
        self.assertNotIn('private', json.dumps(result))
        self.assertEqual(result['returned_count'], 1)
        self.assertNotIn('-L', args[0])

    def test_control_character_injection_rejected(self):
        with self.assertRaises(ValueError):
            probe.config_quote('token\nurl = "https://example.com"')

    def test_nonallowlisted_path_rejected(self):
        with self.assertRaises(ValueError):
            probe.request('/v1/me/library/podcasts/123', 'test-public', 'test-private')


if __name__ == '__main__':
    unittest.main()
