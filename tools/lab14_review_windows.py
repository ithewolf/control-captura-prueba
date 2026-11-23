#!/usr/bin/env python3
"""Generate motion-only review windows from measured Lab12 video data. Not a tactic detector."""
import argparse,json,math
from pathlib import Path
def candidates(report, percentile=.85, min_gap=4.):
    if report.get('status')!='VISUAL_ACTIVITY_MEASURED' or report.get('game_events_status')!='NOT_MEASURED':
        raise ValueError('Expected raw motion-only Lab12 data')
    points=[]
    for row in report.get('records',[]):
        if row.get('mean_frame_change') is None:continue
        t,v=float(row['second']),float(row['mean_frame_change'])
        if not math.isfinite(t) or not math.isfinite(v) or t<0 or v<0:raise ValueError('Invalid measurement')
        points.append((t,v))
    points.sort()
    if len(points)<3 or not 0<percentile<1 or min_gap<0:raise ValueError('Invalid sample selection')
    if len(set(t for t,v in points))!=len(points):raise ValueError('Duplicate timestamp')
    values=sorted(v for t,v in points)
    cutoff=values[math.ceil(percentile*len(values))-1]
    selected=[(t,v) for t,v in points if v>=cutoff]
    groups=[]
    for t,v in selected:
        if not groups or t-groups[-1][-1][0]>min_gap:groups.append([])
        groups[-1].append((t,v))
    windows=[]
    for group in groups:
        peak=max(group,key=lambda x:x[1])
        windows.append({'start_s':max(0,group[0][0]-2),'end_s':min(float(report['duration_s']),group[-1][0]+2),
                        'peak_s':peak[0],'peak_visual_change':round(peak[1],4),
                        'reason':'HIGH_FRAME_DIFFERENCE_ONLY','game_event':'UNLABELED',
                        'champion_id':'UNVERIFIED','next_action':'HUMAN_REVIEW_CONTEXT'})
    return {'schema':1,'status':'REVIEW_CANDIDATES_FROM_REAL_MOTION','input_samples':report['samples'],
            'measured_pairs':len(points),'percentile':percentile,'threshold_visual_change':round(cutoff,4),
            'review_windows':windows,'game_events_status':'NOT_MEASURED',
            'strategy_recognition_status':'NOT_MEASURED',
            'warning':'Image changes may indicate cuts, effects, movement or combat. No event/champion detector evaluated.'}
def main():
    p=argparse.ArgumentParser();p.add_argument('--input',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args()
    a.out.parent.mkdir(parents=True,exist_ok=True)
    a.out.write_text(json.dumps(candidates(json.loads(a.input.read_text(encoding='utf-8'))),indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
if __name__=='__main__':main()
