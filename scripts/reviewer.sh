#!/usr/bin/env bash
echo "Running local reviewer against coding_guidelines.md"
exitCode=0
if grep -R "System.out.println" -n src || true; then
  echo "WARNING: Found System.out.println usages. Use logger instead."
  exitCode=1
fi
if grep -R "TODO: Implement" -n src || true; then
  echo "NOTE: Found TODOs that may need attention."
fi
exit $exitCode
