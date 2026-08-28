#!/usr/bin/env bash
# Isolated smoke test using the repository's bundled Prometheus 1.8 binary.
set -euo pipefail
script_dir=$(cd "$(dirname "$0")" && pwd)
repo_root=$(cd "$script_dir/../../../.." && pwd)
test_dir=$(mktemp -d /tmp/zwatch-promql-test.XXXXXX)
echo "Isolated Prometheus test logs: $test_dir"
"$repo_root/conf/tools/prometheus" \
  -config.file="$script_dir/prometheus-empty.yml" \
  -storage.local.path="$test_dir/data" \
  -web.listen-address=127.0.0.1:19093 >"$test_dir/prometheus.log" 2>&1 &
test_pid=$!
trap 'kill "$test_pid" 2>/dev/null || true; wait "$test_pid" 2>/dev/null || true' EXIT
for attempt in $(seq 1 50); do
  kill -0 "$test_pid" 2>/dev/null || { cat "$test_dir/prometheus.log"; exit 1; }
  if curl -sf 'http://127.0.0.1:19093/api/v1/query?query=vector(1)' >/dev/null; then
    PROMETHEUS_URL=http://127.0.0.1:19093 python "$script_dir/test_vm_performance_promql.py" -v
    exit $?
  fi
  sleep 0.2
done
cat "$test_dir/prometheus.log"
exit 1
