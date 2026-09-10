"""Offline regression tests for the bundled bridge's authentication boundary."""
import importlib.util
import contextlib
import io
import json
import runpy
from pathlib import Path
import sys
sys.dont_write_bytecode = True
import tempfile
import unittest
from unittest.mock import patch
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

class GalleryHeadersTest(unittest.TestCase):
    def test_existing_referer_is_preserved_regardless_of_case(self):
        page = 'https://example.com/gallery'
        image_url = 'https://example.com/image.jpg'
        for key in ('Referer', 'referer', 'REFERER', 'rEfErEr', None):
            with self.subTest(key=key):
                class Source:
                    category = 'test'
                    referer = True
                    session = bridge.requests.Session()

                    def initialize(self):
                        pass

                    def __iter__(self):
                        yield (bridge.Message.Url, image_url, {
                            'extension': 'jpg',
                            '_http_headers': {key: 'https://example.com/original'} if key else {},
                        })

                with patch.object(bridge.extractor, 'find', return_value=Source()):
                    headers = bridge.extract(page)['images'][0]['headers']
                referers = [value for name, value in headers.items() if name.lower() == 'referer']
                self.assertEqual(referers, ['https://example.com/original' if key else page])


class GalleryInputLimitTest(unittest.TestCase):
    def test_input_at_limit_is_accepted_and_larger_input_is_rejected(self):
        for size in (16383, 16384, 16385):
            with self.subTest(size=size):
                payload = '{}' + ' ' * (size - 2)
                output = io.StringIO()
                with patch.object(sys, 'argv', [str(spec.origin), 'https://example.com/']), \
                        patch.object(sys, 'stdin', io.StringIO(payload)), \
                        patch.object(bridge.extractor, 'find', return_value=None) as find, \
                        contextlib.redirect_stdout(output):
                    if size > 16384:
                        with self.assertRaises(SystemExit) as failure:
                            runpy.run_path(str(spec.origin), run_name='__main__')
                        self.assertEqual(failure.exception.code, 1)
                        find.assert_not_called()
                    else:
                        runpy.run_path(str(spec.origin), run_name='__main__')
                        find.assert_called_once()
                result = json.loads(output.getvalue())
                if size > 16384:
                    self.assertEqual(result['error'], 'ValueError')
                else:
                    self.assertNotIn('error', result)


if __name__ == '__main__':
    try:
        unittest.main()
    finally:
        TEMP.cleanup()
