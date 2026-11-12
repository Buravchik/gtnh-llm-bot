#!/usr/bin/env python3
# usage: mcpcall.py tool '{"json":"args"}' [tool2 'args2' ...]
import subprocess, json, sys, os
p = subprocess.Popen([os.path.expanduser('~/gtnh-bot/bin/headlessmc-mcp')], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True)
def send(m): p.stdin.write(json.dumps(m)+"\n"); p.stdin.flush()
def recv(i):
    while True:
        l = p.stdout.readline()
        if not l: return None
        try:
            m=json.loads(l)
            if m.get("id")==i: return m
        except: pass
send({"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2024-11-05","capabilities":{},"clientInfo":{"name":"t","version":"1"}}}); recv(1)
send({"jsonrpc":"2.0","method":"notifications/initialized"})
a=sys.argv[1:]
for k in range(0,len(a),2):
    i=100+k
    send({"jsonrpc":"2.0","id":i,"method":"tools/call","params":{"name":a[k],"arguments":json.loads(a[k+1]) if k+1<len(a) else {}}})
    r=recv(i)
    res=r.get("result",r.get("error"))
    txt="\n".join(c.get("text","") for c in res.get("content",[])) if isinstance(res,dict) and "content" in res else json.dumps(res,ensure_ascii=False)
    print(f"== {a[k]}:\n{txt}\n")
p.kill()
