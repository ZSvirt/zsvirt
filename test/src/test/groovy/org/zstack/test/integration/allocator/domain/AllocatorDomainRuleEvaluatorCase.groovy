package org.zstack.test.integration.allocator.domain

import org.zstack.allocator.domain.compute.AllocatorDomainRuleEvaluator
import org.zstack.allocator.domain.entity.AllocatorDomainRuleVO
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
    }

    void testHardUnionInclude() {
        logger.info("Test 000a: Include cluster2 + Include host-1 is union, order does not matter")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2"},
            {"uuid": "host-12", "clusterUuid": "cluster-2"},
            {"uuid": "host-20", "clusterUuid": "cluster-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision clusterThenHost = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"}
        ]""", AllocatorDomainRuleVO[].class) as List<AllocatorDomainRuleVO>, hosts)
        assert clusterThenHost.allowedHostUuids.size() == 4
        assert clusterThenHost.allowedHostUuids.contains("host-1")
        assert !clusterThenHost.allowedHostUuids.contains("host-2")
        assert !clusterThenHost.allowedHostUuids.contains("host-3")
        assert clusterThenHost.allowedHostUuids.contains("host-11")
        assert clusterThenHost.allowedHostUuids.contains("host-12")
        assert clusterThenHost.allowedHostUuids.contains("host-20")

        AllocatorDomainRuleEvaluator.HostDecision hostThenCluster = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"},
            {"priority": 1, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"}
        ]""", AllocatorDomainRuleVO[].class) as List<AllocatorDomainRuleVO>, hosts)
        assert hostThenCluster.allowedHostUuids.size() == 4
        assert hostThenCluster.allowedHostUuids.contains("host-1")
        assert !hostThenCluster.allowedHostUuids.contains("host-2")
        assert !hostThenCluster.allowedHostUuids.contains("host-3")
        assert hostThenCluster.allowedHostUuids.contains("host-11")
        assert hostThenCluster.allowedHostUuids.contains("host-12")
        assert hostThenCluster.allowedHostUuids.contains("host-20")
    }

    void testHardExcludeWinsOverInclude() {
        logger.info("Test 000b: Exclude host-20 still drops host-20 even when Include cluster2 is first")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2"},
            {"uuid": "host-12", "clusterUuid": "cluster-2"},
            {"uuid": "host-20", "clusterUuid": "cluster-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-2", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Include"},
            {"priority": 2, "resourceUuid": "host-20",   "resourceType": "HostVO",    "effect": "Exclude"}
        ]""", AllocatorDomainRuleVO[].class) as List<AllocatorDomainRuleVO>, hosts)
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert !decision.allowedHostUuids.contains("host-2")
        assert !decision.allowedHostUuids.contains("host-3")
        assert decision.allowedHostUuids.contains("host-11")
        assert decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-20")
    }

    void testDomainZoneBoundsSoftOnlyCandidates() {
        logger.info("Test 000c: Soft-only rules still cannot leave the domain zone")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1", "zoneUuid": "zone-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"},
            {"uuid": "host-12", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"},
            {"uuid": "host-20", "clusterUuid": "cluster-2", "zoneUuid": "zone-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "host-1", "resourceType": "HostVO", "effect": "Recommend"}
        ]""", AllocatorDomainRuleVO[].class) as List<AllocatorDomainRuleVO>, hosts, "zone-1")
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert decision.allowedHostUuids.contains("host-2")
        assert decision.allowedHostUuids.contains("host-3")
        assert !decision.allowedHostUuids.contains("host-11")
        assert !decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-20")
        assert decision.recommendedHostUuids.size() == 1
        assert decision.recommendedHostUuids.contains("host-1")
        assert !decision.hasInclude
    }

    void testIncludeKeepsRecommend() {
        logger.info("Test 000d: Include cluster1 + Recommend host-1, host-1 stays recommended")
        List<HostVO> hosts = json("""[
            {"uuid": "host-1",  "clusterUuid": "cluster-1"},
            {"uuid": "host-2",  "clusterUuid": "cluster-1"},
            {"uuid": "host-3",  "clusterUuid": "cluster-1"},
            {"uuid": "host-11", "clusterUuid": "cluster-2"},
            {"uuid": "host-12", "clusterUuid": "cluster-2"},
            {"uuid": "host-20", "clusterUuid": "cluster-2"}
        ]""", HostVO[].class) as List<HostVO>
        AllocatorDomainRuleEvaluator evaluator = new AllocatorDomainRuleEvaluator()

        AllocatorDomainRuleEvaluator.HostDecision decision = evaluator.evaluateHosts(json("""[
            {"priority": 0, "resourceUuid": "cluster-1", "resourceType": "ClusterVO", "effect": "Include"},
            {"priority": 1, "resourceUuid": "host-1",    "resourceType": "HostVO",    "effect": "Recommend"}
        ]""", AllocatorDomainRuleVO[].class) as List<AllocatorDomainRuleVO>, hosts)
        assert decision.hasInclude
        assert decision.allowedHostUuids.size() == 3
        assert decision.allowedHostUuids.contains("host-1")
        assert decision.allowedHostUuids.contains("host-2")
        assert decision.allowedHostUuids.contains("host-3")
        assert !decision.allowedHostUuids.contains("host-11")
        assert !decision.allowedHostUuids.contains("host-12")
        assert !decision.allowedHostUuids.contains("host-20")
        assert decision.recommendedHostUuids.size() == 1
        assert decision.recommendedHostUuids.contains("host-1")
        assert !decision.recommendedHostUuids.contains("host-2")
        assert !decision.recommendedHostUuids.contains("host-3")
    }

    @Override
    void clean() {
    }
}
