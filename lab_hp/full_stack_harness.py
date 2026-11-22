"""Lab 2: integration contracts for 5 components, not 5 trained models."""
from time import perf_counter
from resource_budget import choose_profile
class FiveStageLab:
 def __init__(self,camera,tracker=None,cvat_trained_detector=None,action_model=None,mobile_onnx=None):
  self.components=(("camera",camera),("tracking",tracker),("champion_detector",cvat_trained_detector),("action_recognition",action_model),("onnx_mobile",mobile_onnx))
 def process(self,previous_frame,frame,thermal="normal",frame_drop_pct=0,requested="balanced"):
  profile=choose_profile(thermal,frame_drop_pct,user_limit=requested)
  if profile.name=="pause":return {"profile":"pause","stages":{},"recommendation":None,"ready_for_live":False}
  reports={}
  for name,stage in self.components:
   if stage is None:
    reports[name]={"status":"UNAVAILABLE","reason":"no_trained_model_or_adapter"}
    continue
   start=perf_counter()
   try:
    result=stage(previous_frame,frame)
    if not isinstance(result,dict) or result.get("status") not in ("MEASURED","UNKNOWN","VALIDATED"):result={"status":"UNKNOWN","reason":"bad_adapter_contract"}
   except Exception as exc:result={"status":"UNKNOWN","reason":"adapter_exception:"+type(exc).__name__}
   reports[name]={**result,"elapsed_ms":round((perf_counter()-start)*1000,3)}
  return {"profile":profile.name,"stages":reports,"recommendation":None,"ready_for_live":False,"all_five_executed":all(v["status"]!="UNAVAILABLE" for v in reports.values())}
