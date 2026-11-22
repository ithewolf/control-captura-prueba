"""Wild Rift observation contract, no detector trained or inferencing here."""
from dataclasses import dataclass
ENTITY_TYPES=frozenset(("champion","minion","jungle_monster","turret","other","unknown"))
AFFILIATIONS=frozenset(("ally","enemy","neutral","unknown"))
@dataclass(frozen=True)
class Observation:
 entity_type:str
 affiliation:str
 confidence:float|None=None
 detector_verified:bool=False

def advisory(observation:Observation)->dict:
 if not isinstance(observation,Observation):raise TypeError("observation required")
 if observation.entity_type not in ENTITY_TYPES or observation.affiliation not in AFFILIATIONS:
  raise ValueError("unrecognized class")
 if observation.confidence is not None and (not isinstance(observation.confidence,(float,int)) or not 0<=observation.confidence<=1):
  raise ValueError("invalid confidence")
 # A known class from a fake adapter must never become a live instruction.
 if not observation.detector_verified or observation.entity_type=="unknown" or observation.affiliation=="unknown":
  return {"status":"UNKNOWN","advice":None,"ready_for_live":False}
 # Even a detector-verified observation requires unseen context, so no tactical advice yet.
 return {"status":"OBSERVATION_ONLY","advice":None,"ready_for_live":False}
