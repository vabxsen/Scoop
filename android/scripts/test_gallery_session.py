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
        bridge.initialize_source(source, 'https://www.instagram.com/p/test/', 'http://127.0.0.1:12345')
        self.assertFalse(source.session.trust_env)
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
    def test_referer_is_sanitized_regardless_of_input_case(self):
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

                with patch.object(bridge.extractor, 'find', return_value=Source()), \
                        patch.object(bridge.socket, 'getaddrinfo', return_value=[(2, 1, 6, '', ('93.184.216.34', 443))]):
                    headers = bridge.extract(page, 'http://127.0.0.1:12345')['images'][0]['headers']
                referers = [value for name, value in headers.items() if name.lower() == 'referer']
                self.assertEqual(referers, [page])

    def test_private_and_cleartext_urls_are_rejected(self):
        with patch.object(bridge.socket, 'getaddrinfo', return_value=[(2, 1, 6, '', ('127.0.0.1', 443))]):
            self.assertFalse(bridge.public_https('https://example.com/image.jpg'))
        self.assertFalse(bridge.public_https('http://example.com/image.jpg'))


class GalleryPartialResultTest(unittest.TestCase):
    PUBLIC = [(2, 1, 6, '', ('93.184.216.34', 443))]

    @staticmethod
    def source(messages, failure=None):
        class Source:
            category = 'test'
            referer = False
            session = bridge.requests.Session()

            def initialize(self):
                pass

            def __iter__(self):
                yield from messages
                if failure:
                    raise failure
        return Source()

    def test_failing_sub_page_keeps_images_from_other_pages(self):
        root = 'https://example.com/gallery'
        child = 'https://example.com/gallery/page2'
        sources = {
            root: self.source([(bridge.Message.Url, 'https://example.com/a.jpg', {'extension': 'jpg', 'width': '1920px'}),
                               (bridge.Message.Queue, child, {})]),
            child: self.source([], RuntimeError('HTTP 500')),
        }
        with patch.object(bridge.extractor, 'find', side_effect=lambda page: sources.get(page)), \
                patch.object(bridge.socket, 'getaddrinfo', return_value=self.PUBLIC):
            result = bridge.extract(root, 'http://127.0.0.1:12345')
        self.assertEqual([image['url'] for image in result['images']], ['https://example.com/a.jpg'])
        self.assertEqual(result['images'][0]['width'], 1920)

    def test_failing_first_page_without_images_still_reports_its_error(self):
        failure = RuntimeError('HTTP redirect to login page')
        with patch.object(bridge.extractor, 'find', return_value=self.source([], failure)), \
                patch.object(bridge.socket, 'getaddrinfo', return_value=self.PUBLIC):
            with self.assertRaises(RuntimeError) as raised:
                bridge.extract('https://example.com/private', 'http://127.0.0.1:12345')
        self.assertEqual(bridge.error_code(raised.exception), 'authentication_required')

    def test_dimensions_never_raise(self):
        for value, expected in ((640, 640), (480.7, 480), ('1920px', 1920), (' 300 ', 300), (None, 0),
                                ('wide', 0), ('nan', 0), ('inf', 0), (-5, 0)):
            with self.subTest(value=value):
                self.assertEqual(bridge.dimension(value), expected)


class GalleryInputLimitTest(unittest.TestCase):
    def test_input_at_limit_is_accepted_and_larger_input_is_rejected(self):
        for size in (16383, 16384, 16385):
            with self.subTest(size=size):
                base = json.dumps({'url': 'https://example.com/', 'cookies': {}, 'proxy': 'http://127.0.0.1:12345'})
                payload = base + ' ' * (size - len(base))
                output = io.StringIO()
                with patch.object(sys, 'argv', [str(spec.origin), '--stdin']), \
                        patch.object(sys, 'stdin', io.StringIO(payload)), \
                        patch.object(bridge.extractor, 'find', return_value=None) as find, \
                        patch.object(bridge.socket, 'getaddrinfo', return_value=[(2, 1, 6, '', ('93.184.216.34', 443))]), \
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
