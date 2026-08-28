#!/usr/bin/env python
"""Verify actual Java recording expressions with synthetic Prometheus vectors.

Run with PROMETHEUS_URL=http://host:9090 python .../test_vm_performance_promql.py.
Only read-only /api/v1/query requests are issued; no series or rules are written.
Without PROMETHEUS_URL, source contracts run but expression tests are skipped.
"""
from __future__ import division

import json
import os
import re
import unittest
try:
    from urllib.parse import urlencode
    from urllib.request import urlopen
except ImportError:
    from urllib import urlencode
    from urllib2 import urlopen


SOURCE = os.environ.get("VM_NAMESPACE_SOURCE", os.path.join(
    os.path.dirname(__file__), "..", "..", "main", "java", "org", "zstack",
    "zwatch", "prometheus", "VmPrometheusNamespace.java"))
with open(SOURCE) as stream:
    EXPRESSIONS = dict(re.findall(
        r'metric == VmNamespace\.(\w+)\)\s*\{\s*(?://[^\n]*\n\s*)*'
        r'rule\.setExpression\("([^"\n]+)"\)', stream.read()))


def vector(samples):
    if not samples:
        return "(vector(0) unless vector(0))"
    parts = []
    for labels, value in samples:
        part = "vector(%s)" % value
        for key, label in sorted(labels.items()):
            part = 'label_replace(%s, %s, %s, "", "")' % (
                part, json.dumps(key), json.dumps(label))
        parts.append(part)
    return "(" + " or ".join(parts) + ")"


class VmPerformancePromQLTest(unittest.TestCase):
    def test_source_contract(self):
        self.assertEqual("sum by (vm_uuid) (zstack_vm_vcpu_ready_time_milliseconds)",
                         EXPRESSIONS["CPUReadyTimeMilliseconds"])
        self.assertEqual("avg by (vm_uuid) (zstack_vm_vcpu_readiness_percent)",
                         EXPRESSIONS["CPUReadinessPercent"])
        for direction in ("Read", "Write"):
            for prefix in ("Disk", "DiskAverage"):
                expr = EXPRESSIONS[prefix + direction + "LatencyMilliseconds"]
                self.assertIn("requests > 0", expr)
                self.assertNotIn("rate(", expr)
                self.assertNotIn("avg(", expr)

    def query(self, metric, inputs):
        url = os.environ.get("PROMETHEUS_URL")
        if not url:
            self.skipTest("set PROMETHEUS_URL to evaluate real PromQL")
        expression = re.sub(r"zstack_vm_[a-z_]+", lambda match:
                            vector(inputs.get(match.group(0), [])), EXPRESSIONS[metric])
        response = json.loads(urlopen(url.rstrip("/") + "/api/v1/query?" +
                                     urlencode({"query": expression}), timeout=20).read())
        self.assertEqual("success", response["status"], response)
        return response["data"]["result"]

    def test_vcpu_sum_average_and_vm_isolation(self):
        samples = [({"vm_uuid": "vm-a", "vcpu_id": "0"}, 0),
                   ({"vm_uuid": "vm-a", "vcpu_id": "1"}, 4),
                   ({"vm_uuid": "vm-b", "vcpu_id": "0"}, 10)]
        for metric, raw, expected in (
                ("CPUReadyTimeMilliseconds", "zstack_vm_vcpu_ready_time_milliseconds", 4),
                ("CPUReadinessPercent", "zstack_vm_vcpu_readiness_percent", 2)):
            result = self.query(metric, {raw: samples})
            values = dict((v["metric"]["vm_uuid"], float(v["value"][1])) for v in result)
            self.assertEqual({"vm-a": expected, "vm-b": 10}, values)
            self.assertTrue(all(set(v["metric"]) == set(["vm_uuid"]) for v in result))

    def test_disk_weighted_average_single_disk_and_idle(self):
        for direction in ("read", "write"):
            time_metric = "zstack_vm_disk_%s_time_milliseconds" % direction
            request_metric = "zstack_vm_disk_%s_requests" % direction
            durations, requests = [], []
            for vm, disk, duration, count in (("vm-a", "vda", 100, 1),
                                             ("vm-a", "vdb", 100, 100),
                                             ("vm-a", "vdc", 50, 0),
                                             ("vm-b", "vda", 20, 2),
                                             ("vm-idle", "vda", 0, 0)):
                labels = {"vm_uuid": vm, "disk_name": disk}
                durations.append((labels, duration))
                requests.append((labels, count))
            inputs = {time_metric: durations, request_metric: requests}
            result = self.query("DiskAverage" + direction.title() + "LatencyMilliseconds", inputs)
            values = dict((v["metric"]["vm_uuid"], float(v["value"][1])) for v in result)
            self.assertEqual(set(["vm-a", "vm-b"]), set(values))
            self.assertAlmostEqual(200.0 / 101, values["vm-a"])
            self.assertEqual(10, values["vm-b"])
            detail = self.query("Disk" + direction.title() + "LatencyMilliseconds", inputs)
            values = dict(((v["metric"]["vm_uuid"], v["metric"]["disk_name"]),
                           float(v["value"][1])) for v in detail)
            self.assertEqual({("vm-a", "vda"): 100, ("vm-a", "vdb"): 1,
                              ("vm-b", "vda"): 10}, values)

    def test_no_samples_produces_no_aggregate(self):
        for name in ("CPUReadyTimeMilliseconds", "CPUReadinessPercent",
                     "DiskAverageReadLatencyMilliseconds", "DiskAverageWriteLatencyMilliseconds"):
            self.assertEqual([], self.query(name, {}))


if __name__ == "__main__":
    unittest.main()
