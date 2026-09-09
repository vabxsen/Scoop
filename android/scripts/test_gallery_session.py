"""Offline regression tests for the bundled bridge's authentication boundary."""
import importlib.util
from pathlib import Path
import sys
sys.dont_write_bytecode = True
import tempfile
import unittest
import zipfile

ANDROID = Path(__file__).resolve().parents[1]
TEMP = tempfile.TemporaryDirectory()
with zipfile.ZipFile(ANDROID / 'app/src/main/assets/gallery/runtime.zip') as archive:
    archive.extractall(TEMP.name)
sys.path.insert(0, TEMP.name)
spec = importlib.util.spec_from_file_location('scoop_gallery', ANDROID / 'app/src/main/assets/gallery/extract.py')
bridge = importlib.util.module_from_spec(spec)
spec.loader.exec_module(bridge)

class GallerySessionTest(unittest.TestCase):
    def setUp(self):
        bridge.config.clear()

    def test_cookie_config_is_instagram_only(self):
        bridge.apply_instagram_session('https://www.instagram.com/p/test/', {'sessionid': 'local-test', 'unknown': 'discard'})
        self.assertEqual(bridge.config.get(('extractor', 'instagram'), 'cookies'), {'sessionid': 'local-test'})
        self.assertIsNone(bridge.config.get(('extractor',), 'cookies'))

    def test_other_origins_and_malformed_cookies_are_rejected(self):
        for url in ['https://example.com/', 'https://instagram.com.evil.test/', 'http://instagram.com/']:
            bridge.apply_instagram_session(url, {'sessionid': 'local-test'})
            self.assertIsNone(bridge.config.get(('extractor', 'instagram'), 'cookies'))
        bridge.apply_instagram_session('https://www.instagram.com/p/test/', {'sessionid': 'a\r\nInjected: b'})
        self.assertIsNone(bridge.config.get(('extractor', 'instagram'), 'cookies'))

    def test_real_extractor_cookie_jar_only_sends_session_to_secure_instagram(self):
        bridge.apply_instagram_session('https://www.instagram.com/p/test/', {'sessionid': 'local-test'})
        source = bridge.extractor.find('https://www.instagram.com/p/test/')
        bridge.initialize_source(source)
        def cookie(url):
            return source.session.prepare_request(bridge.requests.Request('GET', url)).headers.get('Cookie', '')
        self.assertIn('sessionid=local-test', cookie('https://www.instagram.com/api/v1/media/test/info/'))
        self.assertNotIn('sessionid', cookie('http://www.instagram.com/'))
        self.assertNotIn('sessionid', cookie('https://cdninstagram.com/image.jpg'))
        self.assertNotIn('sessionid', cookie('https://example.com/'))

    def test_login_errors_are_actionable_without_leaking_exception_text(self):
        failure = RuntimeError('HTTP redirect to login page (https://instagram.com/?secret=sensitive)')
        self.assertEqual(bridge.error_code(failure), 'authentication_required')
        self.assertEqual(bridge.error_code(ValueError('sensitive')), 'ValueError')

if __name__ == '__main__':
    try:
        unittest.main()
    finally:
        TEMP.cleanup()
