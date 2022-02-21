# Baked Tests Directory

A directory for test resources that are baked assembly tests. These tests
are basically actual circuits that have been converted to unconstrained MASM
through Yosys or anything else.

If your test depends on some input data files (very likely), ensure the paths
in the MASM annotations are correct (e.g., use relative addresses or preprocess
all `MEMINIT` annotations)