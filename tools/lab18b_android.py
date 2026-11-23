#!/usr/bin/env python3
"""Lab B: test a genuine on-device llama.cpp binary through ADB without an APK.
Never use a PC/cloud model as a substitute for Android inference.
Requires preinstalled Android arm64 llama-cli binary and GGUF in device.
"""
import argparse,json,subprocess,time,shlex
from pathlib import Path
from tools.brain_comparison_eval import audit
def run_cmd(command,timeout=15):
    try:\n        return subprocess.run(command,capture_output=True,text=True,timeout=timeout,check=False)\n    except (FileNotFoundError,subprocess.TimeoutExpired):\n        return subprocess.CompletedProcess(command,127,stdout='',stderr='ADB_MISSING_OR_TIMEOUT')
def check_device(binary,model):
    state=run_cmd(["adb","get-state"])
    if state.returncode!=0 or state.stdout.strip()!="device":
        return False,"ADB_DEVICE_UNAVAILABLE"
    abi=run_cmd(["adb","shell","getprop","ro.product.cpu.abi"])
    if "arm64-v8a" not in abi.stdout:return False,"ARM64_REQUIRED"
    for name,path in (("BINARY",binary),("MODEL",model)):
        chk=run_cmd(["adb","shell","test","-f",path])
        if chk.returncode!=0:return False,name+"_MISSING_ON_DEVICE"
    return True,"READY"
def safe_path(path):
    return path.startswith("/data/local/tmp/") and all(x not in path for x in ("'",";","\n","\r","$","`"))
def main():
    p=argparse.ArgumentParser()
    p.add_argument("--binary",default="/data/local/tmp/wolf/llama-cli")
    p.add_argument("--model",default="/data/local/tmp/wolf/model.gguf")
    p.add_argument("--out",type=Path,default=Path("lab18b_android_results.json"))
    p.add_argument("--questions",type=Path,default=Path("lab17/open_response_20_questions.json"))
    p.add_argument("--timeout",type=int,default=180)
    a=p.parse_args()
    if not all(safe_path(x) for x in (a.binary,a.model)):p.error("Use paths under /data/local/tmp, no shell escapes")
    if not 1<=a.timeout<=600:p.error("Invalid timeout")
    questions=json.loads(a.questions.read_text())["questions"]
    results=[{"id":q["id"],"answer":None} for q in questions]
    status,reason=check_device(a.binary,a.model)
    if status:
        # llama-cli output is plain text; we DO NOT fabricate structured fields.
        # Real responses are stored as raw data requiring human/manual rubric review.
        for i,q in enumerate(questions):
            payload=("Sos CONTROL, asesor de Wild Rift. Responde sin opciones: "+
                     q["prompt"]+" Incluí decisión, motivo, evidencia, riesgo, alternativa, contingencia e incertidumbre.")
            # Use shell-safe quoting and explicit flag; available CLI versions vary,
            # failures are recorded, never counted as successful inference.
            argv=f"{shlex.quote(a.binary)} -m {shlex.quote(a.model)} -p {shlex.quote(payload)} -n 300"
            t=time.monotonic()
            try:
                x=run_cmd(["adb","shell",argv],timeout=a.timeout)
                if x.returncode==0 and x.stdout.strip():
                    results[i]["answer"]={"raw_unparsed_text":x.stdout.strip()}
                    results[i]["latency_ms"]=round((time.monotonic()-t)*1000,2)
                else:results[i]["error"]="DEVICE_INFERENCE_FAILED"
            except (subprocess.TimeoutExpired,FileNotFoundError):results[i]["error"]="DEVICE_INFERENCE_TIMEOUT_OR_ADB_MISSING"
    data={"architecture":"B_ON_DEVICE_ANDROID","probe":reason,
          "device_execution_attempted":status,"responses":results,"evaluation":audit(results),
          "warning":"Raw text response requires manual evaluation; the shared JSON schema is not enforced by this CLI."}
    a.out.write_text(json.dumps(data,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
    print(json.dumps({"probe":reason,"attempted":data["evaluation"]["counts"]["attempted"]}))
if __name__=="__main__":main()
