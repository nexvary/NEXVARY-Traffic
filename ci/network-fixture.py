from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import socket
import threading
import ssl
import subprocess
import tempfile
from pathlib import Path

keys=Path(tempfile.mkdtemp(prefix='nexvary-fixture-'))
subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-keyout',str(keys/'key.pem'),'-out',str(keys/'cert.pem'),'-days','1','-subj','/CN=10.0.2.2','-addext','subjectAltName=IP:10.0.2.2'],check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
subprocess.run(['openssl','x509','-in',str(keys/'cert.pem'),'-outform','DER','-out','fixture-ca.der'],check=True)

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        data = b'NEXVARY_TCP_OK'
        self.send_response(200)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def do_POST(self):
        body=self.rfile.read(int(self.headers.get('Content-Length','0')))
        data=b'accepted:'+body
        self.send_response(200)
        self.send_header('Content-Type','text/plain; charset=utf-8')
        self.send_header('Content-Length',str(len(data)))
        self.end_headers()
        self.wfile.write(data)

def udp():
    with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as server:
        server.bind(('0.0.0.0', 18778))
        while True:
            data, peer = server.recvfrom(65507)
            server.sendto(data, peer)

threading.Thread(target=udp, daemon=True).start()
secure=ThreadingHTTPServer(('0.0.0.0',18779),Handler)
tls=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
tls.load_cert_chain(str(keys/'cert.pem'),str(keys/'key.pem'))
secure.socket=tls.wrap_socket(secure.socket,server_side=True)
threading.Thread(target=secure.serve_forever,daemon=True).start()
ThreadingHTTPServer(('0.0.0.0', 18777), Handler).serve_forever()
