import unittest
from unittest.mock import patch
import probe_extended as p
class Checks(unittest.TestCase):
 def test_external_pagination(self):
  with self.assertRaises(p.ProbeError): p.normalized_path('https://example.com/v1/me/library/podcasts')
 def test_existing_not_removed(self):
  class Fake:
   mutation_targets=set()
   def call(self,*a): raise AssertionError('must not request')
  self.assertEqual(p.roundtrip(Fake(),p.LIBRARIES[0],'123',[{'id':'123'}]),{'skipped':'already_present'})
 def test_failed_add_gets_cleanup(self):
  class Fake:
   mutation_targets=set()
   methods=[]
   def call(self,path,method):
    self.methods.append(method)
    if method=='POST': raise p.ProbeError('network')
    return 204,{}
  client=Fake()
  with patch.object(p,'verify_membership',return_value=True),patch.object(p,'save_result'):
   with self.assertRaises(p.ProbeError): p.roundtrip(client,p.LIBRARIES[0],'123',[])
  self.assertEqual(client.methods,['POST','DELETE'])
 def test_ambiguous_identity_aborts(self):
  with self.assertRaises(p.ProbeError): p.numeric_ids([{'id':'library.123'}])
if __name__ == '__main__': unittest.main()
