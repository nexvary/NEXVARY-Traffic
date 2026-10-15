from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import socket
import threading

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        data = b'NEXVARY_TCP_OK'
        self.send_response(200)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

def udp():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as server:
        server.bind(('0.0.0.0', 18778))
        while True:
            data, peer = server.recvfrom(65507)
            server.sendto(data, peer)

threading.Thread(target=udp, daemon=True).start()
ThreadingHTTPServer(('0.0.0.0', 18777), Handler).serve_forever()
