#!/usr/bin/env python3
"""Serve the skyWash frontend and proxy /api/* to the Spring Boot backend."""
from __future__ import annotations

import http.server
import socketserver
import urllib.error
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent
API = "http://127.0.0.1:8080"
PORT = 3000


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def log_message(self, fmt, *args):
        print("[%s] %s" % (self.log_date_time_string(), fmt % args))

    def _proxy(self):
        url = API + self.path
        length = int(self.headers.get("Content-Length", "0") or 0)
        body = self.rfile.read(length) if length > 0 else None
        headers = {}
        for key in ("Content-Type", "Authorization", "Accept", "X-Paystack-Signature"):
            val = self.headers.get(key)
            if val:
                headers[key] = val
        req = urllib.request.Request(url, data=body, headers=headers, method=self.command)
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                data = resp.read()
                self.send_response(resp.status)
                for hk, hv in resp.headers.items():
                    lk = hk.lower()
                    if lk in ("transfer-encoding", "connection", "content-encoding"):
                        continue
                    self.send_header(hk, hv)
                self.send_header("Cache-Control", "no-store")
                self.end_headers()
                if self.command != "HEAD":
                    self.wfile.write(data)
        except urllib.error.HTTPError as err:
            data = err.read()
            self.send_response(err.code)
            ctype = err.headers.get("Content-Type", "application/json")
            self.send_header("Content-Type", ctype)
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            if self.command != "HEAD":
                self.wfile.write(data)
        except Exception as exc:
            msg = ('{"error":"API unreachable at %s — start the backend on :8080","detail":"%s"}'
                   % (API, str(exc).replace('"', "'")))
            raw = msg.encode()
            self.send_response(502)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(raw)))
            self.end_headers()
            self.wfile.write(raw)

    def do_GET(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        return super().do_GET()

    def do_HEAD(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        return super().do_HEAD()

    def do_POST(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        self.send_error(404)

    def do_PUT(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        self.send_error(404)

    def do_PATCH(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        self.send_error(404)

    def do_DELETE(self):
        if self.path.startswith("/api/") or self.path == "/api":
            return self._proxy()
        self.send_error(404)

    def do_OPTIONS(self):
        # Same-origin proxy — browser won't need CORS, but answer anyway
        self.send_response(204)
        self.send_header("Access-Control-Allow-Origin", self.headers.get("Origin", "*"))
        self.send_header("Access-Control-Allow-Methods", "GET,POST,PUT,PATCH,DELETE,OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization")
        self.send_header("Access-Control-Max-Age", "86400")
        self.end_headers()


class ReusableTCPServer(socketserver.TCPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    print(f"skyWash FE+proxy http://127.0.0.1:{PORT}  →  API {API}")
    with ReusableTCPServer(("0.0.0.0", PORT), Handler) as httpd:
        httpd.serve_forever()
