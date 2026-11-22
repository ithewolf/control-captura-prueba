"""HP stage: semantic class and team labels cannot be conflated."""
import unittest
from entity_affiliation_contract import Observation,advisory
class EntityAffiliationTests(unittest.TestCase):
 def test_unknown_never_triggers_instruction(self):
  for entity,team in (("champion","unknown"),("unknown","enemy"),("turret","unknown")):
   r=advisory(Observation(entity,team,0.99))
   self.assertEqual(r["status"],"UNKNOWN")
   self.assertIsNone(r["advice"])
   self.assertFalse(r["ready_for_live"])
 def test_unverified_enemy_never_triggers_action(self):
  r=advisory(Observation("champion","enemy",1.0,False))
  self.assertEqual(r["status"],"UNKNOWN")
 def test_verified_object_is_observation_not_command(self):
  r=advisory(Observation("turret","enemy",0.99,True))
  self.assertEqual(r["status"],"OBSERVATION_ONLY")
  self.assertIsNone(r["advice"])
  self.assertFalse(r["ready_for_live"])
 def test_reject_invalid_classes(self):
  with self.assertRaises(ValueError):advisory(Observation("enemy_champion","enemy"))
  with self.assertRaises(ValueError):advisory(Observation("minion","opponent"))
  with self.assertRaises(ValueError):advisory(Observation("champion","enemy",1.5))
