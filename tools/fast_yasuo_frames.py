#!/usr/bin/env python3
"""Fast offline frame extraction with FFmpeg for Wild Rift footage.

Examples:
  python tools/fast_yasuo_frames.py yasuo.mp4 --out samples --start 60 --duration 20 --fps 2
  python tools/fast_yasuo_frames.py yasuo.mp4 --out samples --fps 0.2
Timestamps are approximate positions on the requested uniform sampling grid.
No vision labels are auto-asserted. Does not upload anything.
"""
import argparse
import json
import pathlib
import subprocess
import time

FIELDS=("hud_visible","player_health_visible","minimap_visible","death_screen")

def prepare(video,out,start=0,duration=None,fps=2,width=640):
 if start<0 or fps<=0 or width<64 or (duration is not None and duration<=0):
  raise ValueError("invalid selection")
 video=pathlib.Path(video)
 out=pathlib.Path(out)
 out.mkdir(parents=True,exist_ok=True)
 target=out/"frame_%05d.jpg"
 cmd=["ffmpeg","-hide_banner","-nostats","-loglevel","error","-y",
      "-ss",str(start),"-i",str(video)]
 if duration is not None:cmd+=["-t",str(duration)]
 cmd+=["-vf",f"fps={fps},scale={width}:-2","-q:v","4",str(target)]
 t=time.perf_counter()
 subprocess.run(cmd,check=True)
 files=sorted(out.glob("frame_*.jpg"))
 # For multi-pass sampling use separate output directories; avoid stale files.
 rows=[]
 for i,p in enumerate(files):
  row={"video":video.name,"second":round(start+i/fps,3),"frame_file":p.name,
       "perspective":None,"occluded":None}
  row.update({name:None for name in FIELDS})
  rows.append(row)
 (out/"labels.template.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")
 result={"video":video.name,"start":start,"fps_sampling":fps,"samples":len(rows),
         "elapsed_s":round(time.perf_counter()-t,3),
         "note":"Timestamps approximate; all labels must be human verified"}
 (out/"extraction.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
 return result

if __name__=="__main__":
 parser=argparse.ArgumentParser(description=__doc__)
 parser.add_argument("video")
 parser.add_argument("--out",required=True)
 parser.add_argument("--start",type=float,default=0)
 parser.add_argument("--duration",type=float,default=None)
 parser.add_argument("--fps",type=float,default=2)
 parser.add_argument("--width",type=int,default=640)
 args=parser.parse_args()
 print(json.dumps(prepare(args.video,args.out,args.start,args.duration,args.fps,args.width),
                  ensure_ascii=False,indent=2))
