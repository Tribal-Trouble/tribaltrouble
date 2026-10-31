"""Send commands to the game's scripted input socket. Usage: ttcmd.py "move 100 200" "click" "shot C:/x.bmp" ...
A command may be prefixed with "sleep:N" (seconds) to pause before sending it, e.g. "sleep:1.5 shot".
"""
import os
import socket
import sys
import time

HOST, PORT = "127.0.0.1", int(os.environ.get("TT_PORT", "7777"))
TIMEOUT = float(os.environ.get("TT_TIMEOUT", "300"))

def main(args):
    with socket.create_connection((HOST, PORT), timeout=TIMEOUT) as s:
        f = s.makefile("rw", encoding="utf-8", newline="\n")
        for arg in args:
            if arg.startswith("sleep:"):
                head, _, rest = arg.partition(" ")
                time.sleep(float(head[6:]))
                if not rest:
                    continue
                arg = rest
            f.write(arg + "\n")
            f.flush()
            reply = f.readline().rstrip("\r\n")
            print(f"> {arg}\n{reply}")
            if reply == "ok" and arg.split()[0] in ("tree", "state", "sites"):
                while True:
                    line = f.readline().rstrip("\r\n")
                    if line == "END" or line == "":
                        break
                    print(line)

if __name__ == "__main__":
    main(sys.argv[1:])
