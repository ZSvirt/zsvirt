package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.compute.evaluator.AllocatorDomainRuleEvaluator
import org.zstack.header.allocator.domain.AllocatorHostRule
import org.zstack.header.host.HostVO
import org.zstack.testlib.SubCase

class AllocatorDomainRuleEvaluatorCase extends SubCase {
    @Override
    void setup() {
        useSpring(AllocatorDomainTest.springSpec)
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        testHardUnionInclude()
        testHardExcludeWinsOverInclude()
        testDomainZoneBoundsSoftOnlyCandidates()
        testIncludeKeepsRecommend()
        testUuidHardMembership()
        testPrimaryStorageQualifier()
    }

    void testHardUnionInclude() {
        logger.info("Test 001: Include cluster2 + Include host-1 is union, order does not matter")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-12", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-13", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision clusterThenHost = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert clusterThenHost.allowedHostUuids.size() == 4
        assert clusterThenHost.allowedHostUuids.contains("host-1")
        assert !clusterThenHost.allowedHostUuids.contains("host-2")
        assert !clusterThenHost.allowedHostUuids.contains("host-3")
        assert clusterThenHost.allowedHostUuids.contains("host-11")
        assert clusterThenHost.allowedHostUuids.contains("host-12")
        assert clusterThenHost.allowedHostUuids.contains("host-13")

        AllocatorDomainRuleEvaluator.HostDecision hostThenCluster = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"},
            {"priority": 1, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert hostThenCluster.allowedHostUuids.size() == 4
        assert hostThenCluster.allowedHostUuids.contains("host-1")
        assert !hostThenCluster.allowedHostUuids.contains("host-2")
        assert !hostThenCluster.allowedHostUuids.contains("host-3")
        assert hostThenCluster.allowedHostUuids.contains("host-11")
        assert hostThenCluster.allowedHostUuids.contains("host-12")
        assert hostThenCluster.allowedHostUuids.contains("host-13")
    }

    void testHardExcludeWinsOverInclude() {
        logger.info("Test 011: Exclude host-13 still drops host-13 even when Include cluster2 is first")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-12", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-13", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-21", "clusterUuid": "cluster-3", "zoneUuid": "zone-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"},
            {"priority": 2, "resourceUuid": "host-13",   "resourceType": "HostVO",    "effect": "Exclude"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert !decision.allowedHostUuids.contains("host-2")
        assert !decision.allowedHostUuids.contains("host-3")
        assert decision.allowedHostUuids.contains("host-11")
        assert decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-13")
        assert !decision.allowedHostUuids.contains("host-21")

        logger.info("""Test 012: Rule:

Exclude cluster
Include host1 (in cluster)

expect: host1 pass, other hosts in cluster deny, other cluster deny
""")

        def decision012 = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-1", "resourceType": "ClusterVO", "effect": "Exclude"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision012.allowedHostUuids.size() == 1
        assert decision012.allowedHostUuids[0] == "host-1"
        assert decision012.hasInclude
        assert decision012.recommendedHostUuids.empty
        assert decision012.notRecommendedHostUuids.empty

        logger.info("""Test 013: Rule:

<empty>

expect: all host pass. if zoneUuid specified, all host in this zone pass.
""")

        def decision013a = evaluator.evaluateHosts(json("[]", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision013a.allowedHostUuids.size() == 7
        assert decision013a.allowedHostUuids.contains("host-1")
        assert decision013a.allowedHostUuids.contains("host-2")
        assert decision013a.allowedHostUuids.contains("host-3")
        assert decision013a.allowedHostUuids.contains("host-11")
        assert decision013a.allowedHostUuids.contains("host-12")
        assert decision013a.allowedHostUuids.contains("host-13")
        assert decision013a.allowedHostUuids.contains("host-21")
        assert !decision013a.hasInclude
        assert decision013a.recommendedHostUuids.empty
        assert decision013a.notRecommendedHostUuids.empty

        def decision013b = evaluator.evaluateHosts(json("[]", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts, "zone-1")
        assert decision013b.allowedHostUuids.size() == 6
        assert decision013b.allowedHostUuids.contains("host-1")
        assert decision013b.allowedHostUuids.contains("host-2")
        assert decision013b.allowedHostUuids.contains("host-3")
        assert decision013b.allowedHostUuids.contains("host-11")
        assert decision013b.allowedHostUuids.contains("host-12")
        assert decision013b.allowedHostUuids.contains("host-13")
        assert !decision013b.allowedHostUuids.contains("host-21")
        assert !decision013b.hasInclude
        assert decision013b.recommendedHostUuids.empty
        assert decision013b.notRecommendedHostUuids.empty
    }

    void testDomainZoneBoundsSoftOnlyCandidates() {
        logger.info("Test 021: Soft-only rules still cannot leave the domain zone")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"},
            {"uuid": "host-12", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"},
            {"uuid": "host-13", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "host-1", "resourceType": "HostVO", "effect": "Recommend"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts, "zone-1")
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert decision.allowedHostUuids.contains("host-2")
        assert decision.allowedHostUuids.contains("host-3")
        assert !decision.allowedHostUuids.contains("host-11")
        assert !decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-13")
        assert decision.recommendedHostUuids.size() == 1
        assert decision.recommendedHostUuids.contains("host-1")
        assert !decision.hasInclude
    }

    void testIncludeKeepsRecommend() {
        logger.info("Test 031: Include cluster1 + Recommend host-1, host-1 stays recommended")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-12", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"},
            {"uuid": "host-13", "clusterUuid": "cluster-2", "zoneUuid": "zone-1"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-1", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Recommend"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision.hasInclude
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert decision.allowedHostUuids.contains("host-2")
        assert decision.allowedHostUuids.contains("host-3")
        assert !decision.allowedHostUuids.contains("host-11")
        assert !decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-13")
        assert decision.recommendedHostUuids.size() == 1
        assert decision.recommendedHostUuids.contains("host-1")
        assert !decision.recommendedHostUuids.contains("host-2")
        assert !decision.recommendedHostUuids.contains("host-3")

        logger.info("Test 032")
        AllocatorDomainRuleEvaluator.HostDecision decision32 = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-1", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Recommend"},
            {"priority": 2, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "NotRecommend"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision32.hasInclude
        assert decision32.allowedHostUuids.size() == 3
        assert decision32.allowedHostUuids.contains("host-1")
        assert decision32.allowedHostUuids.contains("host-2")
        assert decision32.allowedHostUuids.contains("host-3")
        assert decision32.recommendedHostUuids.isEmpty()
        assert decision32.notRecommendedHostUuids.size() == 1
        assert decision32.notRecommendedHostUuids.contains("host-1")

        logger.info("Test 033")
        AllocatorDomainRuleEvaluator.HostDecision decision33 = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-1", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Recommend"},
            {"priority": 2, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Exclude"},
            {"priority": 3, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, hosts)
        assert decision33.hasInclude
        assert decision33.allowedHostUuids.size() == 3
        assert decision33.allowedHostUuids.contains("host-1")
        assert decision33.allowedHostUuids.contains("host-2")
        assert decision33.allowedHostUuids.contains("host-3")
        assert decision33.recommendedHostUuids.isEmpty()
        assert decision33.notRecommendedHostUuids.isEmpty()
    }

    void testUuidHardMembership() {
        logger.info("Test 041: no rules of that type means no chain")
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()
        List<String> candidates = ["ps-1", "ps-2", "ps-3"]

        AllocatorDomainRuleEvaluator.UuidDecision noChain = evaluator.evaluatePrimaryStorages(json("""[
            {"priority": 0, "resourceUuid": "host-1", "resourceType": "HostVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates)
        assert !noChain.hasChain
        assert noChain.allowedUuids.isEmpty()

        logger.info("Test 041b: Include ps-1 + Include ps-2 is union, order does not matter")
        AllocatorDomainRuleEvaluator.UuidDecision union = evaluator.evaluatePrimaryStorages(json("""[
            {"priority": 0, "resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "ps-2", "resourceType": "PrimaryStorageVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates)
        assert union.hasChain
        assert union.hasInclude
        assert union.allowedUuids.size() == 2
        assert union.allowedUuids.contains("ps-1")
        assert union.allowedUuids.contains("ps-2")
        assert !union.allowedUuids.contains("ps-3")

        logger.info("Test 042: Include then Exclude same uuid, latter Exclude wins")
        AllocatorDomainRuleEvaluator.UuidDecision excludeWins = evaluator.evaluateL3Networks(json("""[
            {"priority": 0, "resourceUuid": "l3-1", "resourceType": "L3NetworkVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "l3-2", "resourceType": "L3NetworkVO", "effect": "Include"},
            {"priority": 2, "resourceUuid": "l3-2", "resourceType": "L3NetworkVO", "effect": "Exclude"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, ["l3-1", "l3-2", "l3-3"])
        assert excludeWins.hasChain
        assert excludeWins.hasInclude
        assert excludeWins.allowedUuids.size() == 1
        assert excludeWins.allowedUuids.contains("l3-1")
        assert !excludeWins.allowedUuids.contains("l3-2")
        assert !excludeWins.allowedUuids.contains("l3-3")

        logger.info("Test 043: Exclude then Include same uuid, latter Include wins")
        AllocatorDomainRuleEvaluator.UuidDecision latterInclude = evaluator.evaluatePrimaryStorages(json("""[
            {"priority": 0, "resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Exclude"},
            {"priority": 1, "resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates)
        assert latterInclude.hasChain
        assert latterInclude.hasInclude
        assert latterInclude.allowedUuids.size() == 1
        assert latterInclude.allowedUuids.contains("ps-1")
        assert !latterInclude.allowedUuids.contains("ps-2")
        assert !latterInclude.allowedUuids.contains("ps-3")

        logger.info("Test 044: Exclude only is blacklist")
        AllocatorDomainRuleEvaluator.UuidDecision blacklist = evaluator.evaluatePrimaryStorages(json("""[
            {"priority": 0, "resourceUuid": "ps-2", "resourceType": "PrimaryStorageVO", "effect": "Exclude"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates)
        assert blacklist.hasChain
        assert !blacklist.hasInclude
        assert blacklist.allowedUuids.size() == 2
        assert blacklist.allowedUuids.contains("ps-1")
        assert !blacklist.allowedUuids.contains("ps-2")
        assert blacklist.allowedUuids.contains("ps-3")

        logger.info("Test 045: Recommend does not grant membership when Include exists")
        AllocatorDomainRuleEvaluator.UuidDecision softNoGrant = evaluator.evaluateL3Networks(json("""[
            {"priority": 0, "resourceUuid": "l3-1", "resourceType": "L3NetworkVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "l3-2", "resourceType": "L3NetworkVO", "effect": "Recommend"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, ["l3-1", "l3-2", "l3-3"])
        assert softNoGrant.hasChain
        assert softNoGrant.hasInclude
        assert softNoGrant.allowedUuids.size() == 1
        assert softNoGrant.allowedUuids.contains("l3-1")
        assert !softNoGrant.allowedUuids.contains("l3-2")
        assert !softNoGrant.allowedUuids.contains("l3-3")
    }

    void testPrimaryStorageQualifier() {
        logger.info("Test 051: Root qualifier ignores Data-only Include; empty qualifier applies to both")
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()
        List<String> candidates = ["ps-1", "ps-2", "ps-3"]
        List<AllocatorHostRule> rules = json("""[
            {"resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Include", "resourceQualifier": "Root"},
            {"resourceUuid": "ps-2", "resourceType": "PrimaryStorageVO", "effect": "Include", "resourceQualifier": "Data"},
            {"resourceUuid": "ps-3", "resourceType": "PrimaryStorageVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>

        AllocatorDomainRuleEvaluator.UuidDecision all = evaluator.evaluatePrimaryStorages(rules, candidates)
        assert all.hasChain
        assert all.allowedUuids.size() == 3
        assert all.allowedUuids.contains("ps-1")
        assert all.allowedUuids.contains("ps-2")
        assert all.allowedUuids.contains("ps-3")

        AllocatorDomainRuleEvaluator.UuidDecision root = evaluator.evaluatePrimaryStorages(rules, candidates, "Root")
        assert root.hasChain
        assert root.allowedUuids.size() == 2
        assert root.allowedUuids.contains("ps-1")
        assert !root.allowedUuids.contains("ps-2")
        assert root.allowedUuids.contains("ps-3")

        AllocatorDomainRuleEvaluator.UuidDecision data = evaluator.evaluatePrimaryStorages(rules, candidates, "Data")
        assert data.hasChain
        assert data.allowedUuids.size() == 2
        assert !data.allowedUuids.contains("ps-1")
        assert data.allowedUuids.contains("ps-2")
        assert data.allowedUuids.contains("ps-3")

        AllocatorDomainRuleEvaluator.UuidDecision dataOnlyForRoot = evaluator.evaluatePrimaryStorages(json("""[
            {"resourceUuid": "ps-2", "resourceType": "PrimaryStorageVO", "effect": "Include", "resourceQualifier": "Data"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates, "Root")
        assert !dataOnlyForRoot.hasChain
        assert dataOnlyForRoot.allowedUuids.isEmpty()

        logger.info("Test 052: empty Include then Root Exclude, latter Exclude wins only for Root")
        List<AllocatorHostRule> latterExcludeRoot = json("""[
            {"resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Include"},
            {"resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Exclude", "resourceQualifier": "Root"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>
        AllocatorDomainRuleEvaluator.UuidDecision rootExclude = evaluator.evaluatePrimaryStorages(latterExcludeRoot, candidates, "Root")
        assert rootExclude.hasChain
        assert rootExclude.hasInclude
        assert rootExclude.allowedUuids.isEmpty()

        AllocatorDomainRuleEvaluator.UuidDecision dataKeepsInclude = evaluator.evaluatePrimaryStorages(latterExcludeRoot, candidates, "Data")
        assert dataKeepsInclude.hasChain
        assert dataKeepsInclude.hasInclude
        assert dataKeepsInclude.allowedUuids.size() == 1
        assert dataKeepsInclude.allowedUuids.contains("ps-1")

        logger.info("Test 053: Root Exclude then empty Include, latter Include wins for Root")
        AllocatorDomainRuleEvaluator.UuidDecision latterIncludeRoot = evaluator.evaluatePrimaryStorages(json("""[
            {"resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Exclude", "resourceQualifier": "Root"},
            {"resourceUuid": "ps-1", "resourceType": "PrimaryStorageVO", "effect": "Include"}
        ]""", AllocatorHostRule[].class) as List<AllocatorHostRule>, candidates, "Root")
        assert latterIncludeRoot.hasChain
        assert latterIncludeRoot.hasInclude
        assert latterIncludeRoot.allowedUuids.size() == 1
        assert latterIncludeRoot.allowedUuids.contains("ps-1")
    }

    @Override
    void clean() {
    }
}
