import unittest
from localize import valid, directory

class LocalizationTests(unittest.TestCase):
    def test_placeholder_loss_rejected(self): self.assertIn("placeholders", valid("%1$d of %2$d", "%1$d", []))
    def test_placeholder_reordering_allowed(self): self.assertEqual([], valid("%1$d of %2$d", "%2$d sur %1$d", []))
    def test_product_name_protected(self): self.assertIn("glossary:RoutineNest", valid("RoutineNest", "Nid", ["RoutineNest"]))
    def test_empty_translation_rejected(self): self.assertIn("empty", valid("hello", "", []))
    def test_resource_qualifier(self): self.assertEqual("values-ar-rSA", directory("ar-SA"))

if __name__ == "__main__":
    import sys
    from test_support import record
    program = unittest.main(exit=False)
    record("localization guards", program.result)
    sys.exit(0 if program.result.wasSuccessful() else 1)
