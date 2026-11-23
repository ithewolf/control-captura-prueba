#!/usr/bin/env python3
"""Lab C: local Ollama reasoning + conservative deterministic tactical gate.
No raw game frames, cross-game memories, paid endpoints, or fabricated inference.
"""
import argparse,json,time,urllib.request,urllib.error
from pathlib import Path
from tools.brain_comparison_eval import audit,validate
FIELDS=("decision","reason","evidence","risk","alternative","contingency","uncertainty")
SCHEMA={"type":"object","properties":{f:{"type":"string"} for f in FIELDS},"required":list(FIELDS),"additionalProperties":False}
OFFENSE=("attack","atacar","iniciar pelea","forzar pelea","divear","entrar a pelear",
         "pelear ahora","asegurar kill","matar al rival")
def gate(answer,observations):
    """Only structured, trusted observations can justify offensive advice.
    This simple safety layer is NOT a strategic understanding benchmark.
    """
    errors=validate(answer)
    if errors:return {"safe":False,"reason":"INVALID_RESPONSE_SCHEMA","errors":errors}
    if not isinstance(observations,dict):return {"safe":False,"reason":"UNVERIFIED_OBSERVATIONS"}
    verified=observations.get("reliable") is True and observations.get("fresh") is True
    enemy=observations.get("enemy_champion_confirmed") is True
    escape=observations.get("escape_route_verified") is True
    decision=answer["decision"].lower()
    offense=any(p in decision for p in OFFENSE)
    if offense and not (verified and enemy and escape):
        return {"safe":False,"reason":"UNSUPPORTED_OFFENSIVE_DECISION"}
    if not verified:return {"safe":False,"reason":"UNVERIFIED_OBSERVATIONS"}
    return {"safe":True,"reason":"MINIMUM_STRUCTURAL_AND_EVIDENCE_GATE_PASSED"}
def ask(model,host,prompt,timeout=90):
    base=("Sos CONTROL para Wild Rift. Analizá solo hechos validados. "
          "No inventes habilidades, visión, posición o cooldowns. "
          "Si no hay evidencia, recomendá observar y decí qué falta. "
          "Respondé en español claro con decisión, motivo, evidencia, riesgo, alternativa, contingencia e incertidumbre.")
    req=urllib.request.Request(host.rstrip("/")+"/api/chat",
          data=json.dumps({"model":model,"stream":False,"format":SCHEMA,
             "messages":[{"role":"system","content":base},{"role":"user","content":prompt}],
             "options":{"temperature":0.2,"num_predict":450}}).encode(),
          headers={"Content-Type":"application/json"})
    start=time.monotonic()
    with urllib.request.urlopen(req,timeout=timeout) as stream:r=json.load(stream)
    raw=r.get("message",{}).get("content","")
    try:return json.loads(raw),round((time.monotonic()-start)*1000,2)
    except (TypeError,json.JSONDecodeError):return {"raw_unparsed_text":raw},round((time.monotonic()-start)*1000,2)
def bench(questions,model,host,timeout):
    out=[]
    for q in questions:
        # For blind text questions no verified structured observations exist.
        obs=q.get("observations",{})
        context=json.dumps(obs,ensure_ascii=False)
        prompt=q["prompt"]+"\nOBSERVACIONES CONFIRMADAS (JSON): "+context
        try:
            ans,ms=ask(model,host,prompt,timeout)
            safety=gate(ans,obs)
            out.append({"id":q["id"],"answer":ans,"latency_ms":ms,"guard":safety})
        except (urllib.error.URLError,OSError,TimeoutError,ValueError) as ex:
            out.append({"id":q["id"],"answer":None,"error":type(ex).__name__,"guard":{"safe":False,"reason":"MODEL_UNAVAILABLE"}})
    return out
def main():
    p=argparse.ArgumentParser();p.add_argument("--model",required=True)
    p.add_argument("--host",default="http://127.0.0.1:11434")
    p.add_argument("--questions",type=Path,default=Path("lab17/open_response_20_questions.json"))
    p.add_argument("--out",type=Path,default=Path("lab18c_results.json"));p.add_argument("--timeout",type=int,default=90)
    a=p.parse_args()
    if not (a.host.startswith("http://127.0.0.1:") or a.host.startswith("http://localhost:")):p.error("Only local Ollama allowed")
    if not 1<=a.timeout<=300:p.error("Invalid timeout")
    questions=json.loads(a.questions.read_text())["questions"]
    responses=bench(questions,a.model,a.host,a.timeout)
    result={"architecture":"C_HYBRID","model":a.model,"responses":responses,"evaluation":audit(responses),
            "guard_passed":sum(1 for x in responses if x["guard"]["safe"]),
            "note":"Guard passes only minimum evidence checks; human blind strategic grading is still missing."}
    a.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+"\n")
    print(json.dumps({"attempted":result["evaluation"]["counts"]["attempted"],"guard_passed":result["guard_passed"]}))
if __name__=="__main__":main()
