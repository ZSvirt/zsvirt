# VM performance aggregation contract

Utility normalizes libvirt UUIDs to lowercase 32-character hexadecimal ZStack
resource UUIDs before using them as snapshot keys or `vm_uuid` labels.

VM CPU exports only per-vCPU interval gauges:

- `zstack_vm_vcpu_ready_time_milliseconds{vm_uuid,vcpu_id}`
- `zstack_vm_vcpu_readiness_percent{vm_uuid,vcpu_id}`

`CPUReadyTimeMilliseconds` sums per-vCPU ready time; `CPUReadinessPercent`
averages per-vCPU percentages. A zero-valued vCPU participates in the average.
Utility suppresses the CPU batch if any expected vCPU lacks a valid delta,
so incomplete batches cannot silently produce a partial-VM average.

VM disks export four interval gauges per `{vm_uuid,disk_name}`:

- `zstack_vm_disk_read_time_milliseconds`
- `zstack_vm_disk_read_requests`
- `zstack_vm_disk_write_time_milliseconds`
- `zstack_vm_disk_write_requests`

These are deltas from the SAME two scrapes, not cumulative counters. Do not
apply `rate()` or `increase()`. Both components of a direction are omitted on
missing fields or counter reset. Valid zero-request components can be zero,
but latency rules filter them out instead of producing NaN/Infinity or fake 0.

Public API namespace remains `ZStack/VM`:

| metricName | labels | expression semantics |
| --- | --- | --- |
| CPUReadyTimeMilliseconds | VMUuid | sum of vCPU ready milliseconds |
| CPUReadinessPercent | VMUuid | average of vCPU readiness percentages |
| VCPUReadyTimeMilliseconds | VMUuid, VCPUId | individual vCPU ready milliseconds |
| VCPUReadinessPercent | VMUuid, VCPUId | individual vCPU readiness percentage |
| DiskReadLatencyMilliseconds | VMUuid, DiskName | individual disk read time / requests |
| DiskWriteLatencyMilliseconds | VMUuid, DiskName | individual disk write time / requests |
| DiskAverageReadLatencyMilliseconds | VMUuid | sum of read time / sum of read requests |
| DiskAverageWriteLatencyMilliseconds | VMUuid | sum of write time / sum of write requests |

The two disk average names are new. Existing API names are retained. Omitting
DiskName on a detail metric is not a request for a weighted aggregate; use the
corresponding DiskAverage metric. Host metrics and their data sources are not
changed by this VM aggregation update.

## Tests

From premium repository root:

```
bash zwatch/src/test/python/run_vm_performance_promql.sh
```

This starts the bundled Prometheus 1.8 executable on loopback port 19093 with
no scrape targets and a fresh temporary data directory, tests the expressions
extracted from the Java source using synthetic vectors, and stops the process.
The temporary directory is retained for logs. No production data or rules are
modified. The port must be free. For a different Prometheus version:

```
PROMETHEUS_URL=http://test-host:9090 python zwatch/src/test/python/test_vm_performance_promql.py -v
```

Utility regression suite, from the Utility repository root:

```
python kvmagent/kvmagent/test/test_vm_performance_metrics.py -v
```

## Coordinated rollout

Utility no longer exports VM aggregate CPU or precomputed disk latency series.
Update Utility and ZWatch together; mixed old/new versions can have missing VM
aggregate/disk data. Allow two valid scrapes to establish new baselines.
Public metric names alone do not make the raw-series migration compatible.

Existing hyphenated UUID history is not rewritten. New samples use compact
UUIDs; an explicit historical migration or compatibility plan would be a
separate change. This patch does not deploy or restart production services.
