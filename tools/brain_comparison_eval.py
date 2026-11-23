#!/usr/bin/env python3
"""Fair, conservative comparison of actual local LLM responses across A/B/C.

Never awards tactical accuracy for syntactic compliance alone. Human blinded
review needed for correctness; record missing responses and costs honestly.
"""
import argparse, json, math
from pathlib import Path

REQUIRED=("decision","reason","evidence","risk","alternative","contingency","uncertainty")
def validate(payload):
    if not isinstance(payload,dict): return ["not_json_object"]
    errors=[]
    for key in REQUIRED:
        if key not in payload or not isinstance(payload[key],str) or not payload[key].strip():
            errors.append("missing_or_invalid_"+key)
    if any(isinstance(payload.get(k),str) and len(payload[k])>1800 for k in REQUIRED):
        errors.append("oversized_response")
    return errors
def audit(records):
    counts={"questions":len(records),"attempted":0,"schema_valid":0,"failed":0,"missing":0}
    latencies=[]
    detail=[]
    for r in records:
        answer=r.get("answer")
        if answer is None:
            counts["missing"]+=1
            detail.append({"id":r.get("id"),"status":"NOT_RUN"})
            continue
        counts["attempted"]+=1
        errors=validate(answer)
        if errors:counts["failed"]+=1
        else:counts["schema_valid"]+=1
        latency=r.get("latency_ms")
        if isinstance(latency,(float,int)) and math.isfinite(latency) and latency>=0:
            latencies.append(latency)
        detail.append({"id":r.get("id"),"status":"SCHEMA_OK" if not errors else "SCHEMA_FAILED","errors":errors})
    return {"status":"NO_MODEL_INFERENCE" if not counts["attempted"] else "RESPONSES_AUDITED",
            "counts":counts,
            "schema_pass_percent":round(100*counts["schema_valid"]/counts["attempted"],2) if counts["attempted"] else None,
            "latency_mean_ms":round(sum(latencies)/len(latencies),2) if latencies else None,
            "strategic_correctness":"NOT_MEASURED",
            "hardware_memory_peak_mb":"NOT_MEASURED",
            "false_claims_rate":"NOT_MEASURED",
            "details":detail,"warning":"A valid JSON response is not evidence of correct tactical reasoning. Require blind human grading."}
def main():
    p=argparse.ArgumentParser();p.add_argument("--responses",type=Path,required=True);p.add_argument("--out",type=Path,required=True)
    a=p.parse_args();records=json.loads(a.responses.read_text(encoding="utf-8"))
    a.out.write_text(json.dumps(audit(records),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
if __name__=="__main__":main()
