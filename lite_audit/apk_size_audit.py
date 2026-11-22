#!/usr/bin/env python3
"""Inspect actual APK contents and enforce a conservative size budget.

No claim of real Android speed or memory use follows from APK size.
"""
import argparse,json,zipfile
from pathlib import Path

def inspect(path,budget_mb=200):
    path=Path(path)
    if not path.is_file() or not zipfile.is_zipfile(path):raise ValueError("APK unavailable or invalid")
    with zipfile.ZipFile(path) as z:
        items=z.infolist()
        total=sum(i.file_size for i in items)
        by_group={}
        largest=[]
        for i in items:
            name=i.filename
            group=("native-libraries" if name.startswith("lib/") else
                   "assets" if name.startswith("assets/") else
                   "dex" if name.endswith(".dex") else
                   "other")
            by_group[group]=by_group.get(group,0)+i.compress_size
            largest.append((i.compress_size,name))
        forbidden=[i.filename for i in items if i.filename.startswith(
            ("lab_hp/","tools/","tests/","dataset/","training/","cvat/"))]
    mb=path.stat().st_size/(1024*1024)
    return {"apk_size_mb":round(mb,2),"budget_mb":budget_mb,
            "budget_pass":mb<=budget_mb,"archive_uncompressed_mb":round(total/(1024*1024),2),
            "compressed_groups_mb":{k:round(v/(1024*1024),2) for k,v in sorted(by_group.items())},
            "largest_entries":[{"path":name,"compressed_mb":round(size/(1024*1024),2)}
                               for size,name in sorted(largest,reverse=True)[:12]],
            "forbidden_research_files":forbidden,
            "status":"PASS" if mb<=budget_mb and not forbidden else "FAIL"}
if __name__=="__main__":
    a=argparse.ArgumentParser()
    a.add_argument("apk");a.add_argument("--budget-mb",type=float,default=200)
    a.add_argument("--output",default="apk-size-report.json");args=a.parse_args()
    if args.budget_mb<=0:raise SystemExit("budget must be positive")
    result=inspect(args.apk,args.budget_mb)
    Path(args.output).write_text(json.dumps(result,indent=2),encoding="utf8")
    print(json.dumps(result,indent=2))
    raise SystemExit(0 if result["status"]=="PASS" else 2)
