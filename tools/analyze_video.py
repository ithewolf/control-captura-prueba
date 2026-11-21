#!/usr/bin/env python3
"""Diagnóstico reproducible de video usando FFmpeg; no identifica campeones."""
import argparse
import csv
import shutil
import subprocess
import sys
from pathlib import Path

W,H=8,6

def extract(path, fps):
    cmd=["ffmpeg","-hide_banner","-loglevel","error","-nostdin","-i",str(path),
         "-vf",f"fps={fps},scale={W}:{H}:flags=area",
         "-pix_fmt","rgb24","-f","rawvideo","pipe:1"]
    proc=subprocess.Popen(cmd,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
    block=W*H*3
    try:
        index=0
        while True:
            data=proc.stdout.read(block)
            if not data: break
            if len(data)!=block: raise RuntimeError("Fotograma incompleto")
            index+=1
            yield index,data
    finally:
        proc.stdout.close()
        err=proc.stderr.read().decode(errors="replace")
        result=proc.wait()
        proc.stderr.close()
        if result!=0: raise RuntimeError("FFmpeg falló: "+err[:1500])

def analyze(source, csv_path, fps):
    if not shutil.which("ffmpeg"): raise RuntimeError("Necesitás ffmpeg disponible en PATH")
    if not source.is_file(): raise FileNotFoundError(source)
    total=changes=dark=0
    last=None
    with csv_path.open("w",newline="",encoding="utf-8") as out:
        writer=csv.writer(out)
        writer.writerow(["frame","time_s","dark","changed","mean_abs_rgb_delta"])
        for index,data in extract(source,fps):
            total+=1
            luminance=sum(data)/len(data)
            is_dark=luminance<=12
            changed=last is not None and data!=last
            delta=sum(abs(a-b) for a,b in zip(data,last))/len(data) if last is not None else 0
            dark+=int(is_dark)
            changes+=int(changed)
            writer.writerow([index,round((index-1)/fps,3),int(is_dark),int(changed),round(delta,3)])
            last=data
    print(f"fotogramas={total} cambios={changes} oscuros={dark} fps_muestreo={fps} informe={csv_path}")
    if total==0: raise RuntimeError("El archivo no produjo fotogramas")
    return total,changes,dark

def main():
    p=argparse.ArgumentParser(description="Prueba de cambios visuales de CONTROL (sin reconocimiento táctico)")
    p.add_argument("video",type=Path)
    p.add_argument("--fps",type=float,default=1.0)
    p.add_argument("--csv",type=Path,default=Path("reporte_visual.csv"))
    a=p.parse_args()
    if not 0<a.fps<=30: p.error("--fps debe estar entre 0 y 30")
    try: analyze(a.video,a.csv,a.fps)
    except Exception as e:
        print("ERROR:",e,file=sys.stderr);return 1
    return 0

if __name__=="__main__":sys.exit(main())
