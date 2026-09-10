"""Scoop's bounded gallery-dl metadata bridge. Never downloads media or loads user config."""
import json
import logging
import sys
from urllib.parse import urlsplit

from gallery_dl import config, extractor, version
from gallery_dl.extractor.message import Message
import requests

IMAGE_EXTENSIONS = {"jpg", "jpeg", "png", "webp", "gif", "avif", "heic", "heif", "bmp", "tif", "tiff", "svg", "jxl"}
LIMIT = 200

COOKIE_NAMES = {"sessionid", "csrftoken", "ds_user_id", "rur", "mid", "ig_did", "ig_nrcb", "datr"}

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

def initialize_source(source):
    source.initialize()
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

def extract(url):
    config.set(("extractor",), "timeout", 12)
    config.set(("extractor",), "retries", 1)
    config.set(("extractor",), "cache", False)
    logging.basicConfig(level=logging.ERROR)
    pending = [(url, 0)]
    visited = set()
    images = []
    seen = set()
    supported = False
    while pending and len(images) < LIMIT and len(visited) < 20:
        page, depth = pending.pop(0)
        if page in visited or urlsplit(page).scheme not in ("http", "https"):
            continue
        visited.add(page)
        source = extractor.find(page)
        if source is None:
            continue
        supported = True
        initialize_source(source)
        for message in source:
            kind = message[0]
            if kind == Message.Queue and depth < 2:
                pending.append((message[1], depth + 1))
            if kind != Message.Url:
                continue
            image_url, metadata = message[1], message[2]
            if urlsplit(image_url).scheme not in ("http", "https") or image_url in seen:
                continue
            extension = str(metadata.get("extension", "")).lower()
            if extension not in IMAGE_EXTENSIONS:
                continue
            prepared = source.session.prepare_request(requests.Request("GET", image_url, headers=metadata.get("_http_headers")))
            headers = {k: v for k, v in prepared.headers.items()
                       if k.lower() in ("user-agent", "referer", "origin", "cookie", "accept", "authorization")}
            if source.referer and not any(key.lower() == "referer" for key in headers):
                headers["Referer"] = page
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
        else:
            payload = sys.stdin.read(16385)
            if len(payload) > 16384:
                raise ValueError("Session input too large")
            apply_instagram_session(sys.argv[1], json.loads(payload) if payload else {})
            result = extract(sys.argv[1])
        print(json.dumps(result, ensure_ascii=True))
    except Exception as error:
        # Do not expose session headers or full URLs in app-visible error messages.
        print(json.dumps({"error": error_code(error), "images": []}))
        sys.exit(1)
