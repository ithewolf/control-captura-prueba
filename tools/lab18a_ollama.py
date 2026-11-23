#!/usr/bin/env python3
"""Lab A: actual free-form local Ollama inference. Never invent responses."""
import argparse,json,time,urllib.request,urllib.error
from pathlib import Path
from tools.brain_comparison_eval import audit
FIELDS=("decision","reason","evidence","risk","alternative","contingency","uncertainty")
SCHEMA={"type":"object","properties":{f:{"type":"string"} for f in FIELDS},"required":list(FIELDS),"additionalProperties":False}
SYSTEM=("Sos CONTROL, asesor táctico de Wild Rift. Responde en español rioplatense. "
        "No inventes posiciones, habilidades, campeones ni datos que el usuario no observó. "
        "Detalla incertidumbre. No tomes acciones dentro del juego.")
def call(prompt,model,host,timeout):
    data=json.dumps({"model":model,"messages":[{"role":"system","content":SYSTEM},
          {"role":"user","content":prompt}],"stream":False,"format":SCHEMA,
          "options":{"temperature":0.2,"num_predict":450}}).encode()
    req=urllib.request.Request(host.rstrip("/")+"/api/chat",data=data,headers={"Content-Type":"application/json"})
    start=time.monotonic()
    with urllib.request.urlopen(req,timeout=timeout) as resp:
        result=json.load(resp)
    ms=(time.monotonic()-start)*1000
    raw=result.get("message",{}).get("content","")
    try:answer=json.loads(raw)
    except (json.JSONDecodeError,TypeError):answer={"raw_unparsed_text":raw}
    return answer,ms
def run(questions,model,host,timeout):
    results=[]
    for q in questions:
        try:
            answer,ms=call(q["prompt"],model,host,timeout)
            results.append({"id":q["id"],"answer":answer,"latency_ms":round(ms,2)})
        except (urllib.error.URLError,TimeoutError,ValueError,OSError) as exc:
            results.append({"id":q["id"],"answer":None,"error":type(exc).__name__})
    return results
def main():
    p=argparse.ArgumentParser();p.add_argument("--model",required=True)
    p.add_argument("--host",default="http://127.0.0.1:11434");p.add_argument("--timeout",type=int,default=90)
    p.add_argument("--questions",type=Path,default=Path("lab17/open_response_20_questions.json"))
    p.add_argument("--out",type=Path,default=Path("lab18a_results.json"));a=p.parse_args()
    if not a.host.startswith("http://127.0.0.1:") and not a.host.startswith("http://localhost:"):
        p.error("Only localhost Ollama is allowed for free/private experiment")
    if not 1<=a.timeout<=300:p.error("Invalid timeout")
    prompts=json.loads(a.questions.read_text())["questions"]
    records=run(prompts,a.model,a.host,a.timeout)
    result={"architecture":"A_OLLAMA_PC","model":a.model,"host":"loopback","responses":records,"evaluation":audit(records)}
    a.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n")
    print(json.dumps({"status":result["evaluation"]["status"],"attempted":result["evaluation"]["counts"]["attempted"]}))
if __name__=="__main__":main()
