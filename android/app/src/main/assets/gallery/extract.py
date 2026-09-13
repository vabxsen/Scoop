"""Scoop's bounded gallery-dl metadata bridge. Never downloads media or loads user config."""
import json
import ipaddress
import logging
import socket
import sys
from urllib.parse import urlsplit

from gallery_dl import config, extractor, version
from gallery_dl.extractor.message import Message
import requests
from requests.adapters import HTTPAdapter

IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "webp", "gif", "avif", "heic", "heif", "bmp", "tif", "tiff", "svg", "jxl"}
LIMIT = 200
MAX_PENDING = 200
MAX_MESSAGES = 2000

COOKIE_NAMES = {"sessionid", "csrftoken", "ds_user_id", "rur", "mid", "ig_did", "ig_nrcb", "datr"}

def public_https(url):
    parsed = urlsplit(url)
    host = (parsed.hostname or "").rstrip(".").lower()
    if parsed.scheme != "https" or not host or parsed.username or parsed.password:
        return False
    if host == "localhost" or host.endswith((".localhost", ".local")):
        return False
    try:
        addresses = {item[4][0] for item in socket.getaddrinfo(host, parsed.port or 443, type=socket.SOCK_STREAM)}
        return bool(addresses) and all(ipaddress.ip_address(address).is_global for address in addresses)
    except (OSError, ValueError):
        return False

def origin(url):
    parsed = urlsplit(url)
    port = "" if parsed.port in (None, 443) else ":" + str(parsed.port)
    return "https://" + (parsed.hostname or "") + port + "/"

def safe_referrer(source, target):
    return source.split("?", 1)[0].split("#", 1)[0] if origin(source) == origin(target) else origin(source)

class PublicHttpsAdapter(HTTPAdapter):
    def __init__(self, source_url):
        self.source_origin = origin(source_url)
        super().__init__()

    def send(self, request, **kwargs):
        if not public_https(request.url):
            raise requests.RequestException("Blocked non-public destination")
        if origin(request.url) != self.source_origin:
            request.headers.pop("Cookie", None)
            request.headers.pop("Authorization", None)
        return super().send(request, **kwargs)

def apply_instagram_session(url, cookies):
    parsed = urlsplit(url)
    host = parsed.hostname or ""
    if parsed.scheme != "https" or not (host == "instagram.com" or host.endswith(".instagram.com")):
        return
    if not isinstance(cookies, dict):
        return
    safe = {key: value for key, value in cookies.items()
            if key in COOKIE_NAMES and isinstance(value, str) and len(value) <= 4096
            and value and not any(char in value for char in "\r\n;")}
    if safe:
        config.set(("extractor", "instagram"), "cookies", safe)

def initialize_source(source, page, proxy):
    def enforce_transport():
        source.session.trust_env = False
        source.session.proxies = {"http": proxy, "https": proxy}
        source.session.mount("https://", PublicHttpsAdapter(page))
    # gallery-dl normally creates the Requests session inside initialize(). Create it first so
    # site-specific _init hooks cannot perform a request before the enforcing proxy is attached.
    if source.session is None:
        source._init_options()
        source._init_session()
        source.cookies = source.session.cookies
        if source.cookies_domain is not None:
            source._init_cookies()
    enforce_transport()
    source.initialize()
    enforce_transport()
    if source.category == "instagram":
        # Dictionary cookie configuration loses the browser's Secure attribute.
        for cookie in source.cookies:
            if cookie.name in COOKIE_NAMES:
                cookie.secure = True

def error_code(error):
    # Classify only; never expose upstream exception text, which can contain URLs or cookies.
    if type(error).__name__ in ("AuthenticationError", "AuthRequired") or getattr(error, "status", 0) == 401:
        return "authentication_required"
    message = str(error).lower()
    if any(text in message for text in ("redirect to login", "login_required", "login required", "checkpoint", "challenge_required")):
        return "authentication_required"
    return type(error).__name__

def extract(url, proxy):
    config.set(("extractor",), "timeout", 12)
    config.set(("extractor",), "retries", 1)
    config.set(("extractor",), "cache", False)
    logging.disable(logging.CRITICAL)
    if not public_https(url):
        raise ValueError("Blocked URL")
    pending = [(url, 0)]
    visited = set()
    images = []
    seen = set()
    supported = False
    message_count = 0
    while pending and len(images) < LIMIT and len(visited) < 20 and message_count < MAX_MESSAGES:
        page, depth = pending.pop(0)
        if page in visited or not public_https(page):
            continue
        visited.add(page)
        source = extractor.find(page)
        if source is None:
            continue
        supported = True
        initialize_source(source, page, proxy)
        for message in source:
            message_count += 1
            if message_count > MAX_MESSAGES:
                break
            kind = message[0]
            if kind == Message.Queue and depth < 2 and len(pending) < MAX_PENDING and public_https(message[1]):
                pending.append((message[1], depth + 1))
            if kind != Message.Url:
                continue
            image_url, metadata = message[1], message[2]
            if not public_https(image_url) or image_url in seen:
                continue
            extension = str(metadata.get("extension", "")).lower()
            if extension not in IMAGE_EXTENSIONS:
                continue
            prepared = source.session.prepare_request(requests.Request("GET", image_url, headers=metadata.get("_http_headers")))
            headers = {k: v for k, v in prepared.headers.items()
                       if k.lower() in ("user-agent", "referer", "origin", "cookie", "accept", "authorization")}
            had_referrer = source.referer or any(key.lower() == "referer" for key in headers)
            headers = {key: value for key, value in headers.items()
                       if key.lower() not in ("referer", "origin") and "\r" not in value and "\n" not in value}
            if had_referrer:
                headers["Referer"] = safe_referrer(page, image_url)
            if origin(page) != origin(image_url):
                headers = {key: value for key, value in headers.items()
                           if key.lower() not in ("cookie", "authorization")}
            filename = str(metadata.get("filename") or metadata.get("id") or "Image")
            images.append({"url": image_url, "title": filename,
                           "mimeType": "image/" + {"jpg": "jpeg", "svg": "svg+xml"}.get(extension, extension),
                           "headers": headers,
                           "width": int(metadata.get("width") or 0),
                           "height": int(metadata.get("height") or 0)})
            seen.add(image_url)
            if len(images) >= LIMIT:
                break
    return {"supported": supported, "images": images, "limited": len(images) >= LIMIT}

if __name__ == "__main__":
    try:
        if sys.argv[1] == "--version":
            result = {"version": version.__version__}
        elif sys.argv[1] == "--stdin":
            payload = sys.stdin.read(16385)
            if len(payload) > 16384:
                raise ValueError("Session input too large")
            request = json.loads(payload)
            url = request.get("url", "")
            cookies = request.get("cookies", {})
            proxy = request.get("proxy", "")
            if not proxy.startswith("http://127.0.0.1:"):
                raise ValueError("Invalid proxy")
            apply_instagram_session(url, cookies)
            result = extract(url, proxy)
        else:
            raise ValueError("Unsupported invocation")
        print(json.dumps(result, ensure_ascii=True))
    except Exception as error:
        # Do not expose session headers or full URLs in app-visible error messages.
        print(json.dumps({"error": error_code(error), "images": []}))
        sys.exit(1)
