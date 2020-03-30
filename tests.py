#! /usr/bin/env python3
import subprocess

def doTest(method, url, exp_status, req_data=None, exp_data=None):
    proc = subprocess.Popen(
            ['curl', '-X', method] + (['-d', req_data] if req_data else []) + ['http://localhost:8080/logmein-1.0' + url, '-D', '/dev/stderr', '--silent'],
            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = proc.communicate('')
    err = err.decode('utf-8')

    assert(err.startswith('HTTP/1.1 ' + str(exp_status)))
    if exp_data is not None:
        assert(exp_data == out)
    if out != b'':
        return out

doTest('POST', '/storage/documents/NONEXISTENT', 405)
doTest('GET', '/storage/documents', 405)
doTest('PUT', '/storage/documents', 405)
doTest('DELETE', '/storage/documents', 405)

doTest('GET', '/storage/documents/NONEXISTENT', 404)
doTest('DELETE', '/storage/documents/NONEXISTENT', 404)
doTest('PUT', '/storage/documents/NONEXISTENT', 404)

idx = doTest('POST', '/storage/documents', 201, req_data=b'LOL\r')
assert(idx is not None and len(idx) == 20)
idx = idx.decode('utf-8')

doTest('GET', '/storage/documents/'+idx, 200, exp_data=b'LOL\r')
doTest('PUT', '/storage/documents/'+idx, 204, req_data=b'Update')
doTest('GET', '/storage/documents/'+idx, 200, exp_data=b'Update')
doTest('DELETE', '/storage/documents/'+idx, 204)
doTest('GET', '/storage/documents/'+idx, 404)
doTest('PUT', '/storage/documents/'+idx, 404, req_data=b'Update')
doTest('DELETE', '/storage/documents/'+idx, 404)
