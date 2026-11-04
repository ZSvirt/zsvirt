package org.zstack.test.integration.network.l3network.ipv6

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmNicExtensionPoint
import org.zstack.compute.vm.VmNicManager
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.componentloader.PluginRegistry
import org.zstack.core.db.Q
import org.zstack.header.message.MessageReply
import org.zstack.header.network.l3.AllocateIpMsg
import org.zstack.header.network.l3.AllocateIpReply
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.network.l3.L3NetworkGlobalConfig
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.network.IPv6Constants

import static java.util.Arrays.asList

/**
 * Created by shixin on 2018/09/10.
 */
class IPv6DhcpCase extends SubCase {
    EnvSpec env
    CloudBus bus
    PluginRegistry pluginRgty

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)
    }
    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        bus = bean(CloudBus.class)
        pluginRgty = bean(PluginRegistry.class)
        env.create {
            testAttachDetachL3ToVmNic()
        }
    }

    void testAttachDetachL3ToVmNic() {
        L3NetworkInventory l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        InstanceOfferingInventory offering = env.inventoryByName("instanceOffering")
        ImageInventory image = env.inventoryByName("image1")

        updateGlobalConfig {
            name = L3NetworkGlobalConfig.BASIC_NETWORK_ENABLE_RA.name
            category = L3NetworkGlobalConfig.CATEGORY
            value = true
        }

        List<FlatDhcpBackend.PrepareDhcpCmd> pcmds = new ArrayList<>()
        env.afterSimulator(FlatDhcpBackend.BATCH_PREPARE_DHCP_PATH) { rsp, HttpEntity<String> e ->
            FlatDhcpBackend.BatchPrepareDhcpCmd pcmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchPrepareDhcpCmd.class)
            pcmds.addAll(pcmd.dhcpInfos)
            return rsp
        }

        List<FlatDhcpBackend.ApplyDhcpCmd> cmds = new ArrayList<>()
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            FlatDhcpBackend.BatchApplyDhcpCmd cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            cmds.addAll(cmd.dhcpInfos)
            return rsp
        }

        VmInstanceInventory vm = createVmInstance {
            name = "vm"
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = asList(l3_statefull.uuid)
        }

        GetL3NetworkDhcpIpAddressResult ret = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3_statefull.uuid
        }
        assert ret.ip != null
        assert ret.ip6 != null
        assert ret.ip6 == ret.ip
        VmNicInventory nic = vm.getVmNics()[0]
        IpRangeInventory ipr = l3_statefull.getIpRanges().get(0)
        assert cmds.size() == 1
        FlatDhcpBackend.ApplyDhcpCmd cmd = cmds.get(0)
        assert cmd.dhcp.size() == 1
        FlatDhcpBackend.DhcpInfo dhcpInfo = cmd.dhcp.get(0)
        assert dhcpInfo.l3NetworkUuid == l3_statefull.uuid
        assert dhcpInfo.ip6 == nic.ip
        assert dhcpInfo.gateway6 == ipr.getGateway()
        assert dhcpInfo.mac == nic.mac
        assert dhcpInfo.firstIp == ipr.getStartIp()
        assert dhcpInfo.endIp == ipr.getEndIp()
        assert dhcpInfo.ipVersion == IPv6Constants.IPv6
        assert dhcpInfo.enableRa
        assert pcmds.size() == 1
        FlatDhcpBackend.PrepareDhcpCmd pcmd = pcmds.get(0)
        assert pcmd.ipVersion == IPv6Constants.IPv6
        assert pcmd.dhcp6ServerIp == ret.ip6
        assert pcmd.prefixLen == 64
        assert pcmd.addressMode == IPv6Constants.Stateful_DHCP
        assert pcmd.dhcpServerIp == null
        assert pcmd.dhcpNetmask == null
        assert pcmd.dhcpNetmask == null
        assert pcmd.dhcpNetmask == null
        assert pcmd.dhcpNetmask == null
        assert pcmd.dhcpNetmask == null

        /* simulate an old dual stack nic */
        AllocateIpMsg msg = new AllocateIpMsg()
        msg.setL3NetworkUuid(l3.uuid)
        bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, l3.uuid);
        MessageReply reply = bus.call(msg);
        AllocateIpReply r = reply.castReply();
        org.zstack.header.network.l3.UsedIpInventory ip = r.getIpInventory();
        for (VmNicExtensionPoint ext : pluginRgty.getExtensionList(VmNicExtensionPoint.class)) {
            ext.afterAddIpAddress(nic.uuid, ip.getUuid())
        }

        vm = queryVmInstance {
            conditions=["uuid=${vm.uuid}".toString()]
        } [0]
        nic = vm.getVmNics()[0]
        assert nic.getUsedIps().size() == 2
        UsedIpInventory ip4 = null
        UsedIpInventory ip6 = null
        for (UsedIpInventory ipAddress : nic.getUsedIps()) {
            if (ipAddress.l3NetworkUuid == l3.uuid) {
                ip4 = ipAddress
            } else if (ipAddress.l3NetworkUuid == l3_statefull.uuid){
                ip6 = ipAddress
            }
        }

        pcmds = new ArrayList<>()
        cmds = new ArrayList<>()
        rebootVmInstance {
            uuid = vm.uuid
        }
        GetL3NetworkDhcpIpAddressResult ret4 = getL3NetworkDhcpIpAddress {
            l3NetworkUuid = l3.uuid
        }
        assert ret4.ip6 == null
        assert ret4.ip != null
        assert pcmds.size() == 2
        for (FlatDhcpBackend.PrepareDhcpCmd pc : pcmds) {
            if (pc.ipVersion == IPv6Constants.IPv6) {
                assert pc.dhcp6ServerIp == ret.ip6
                assert pc.prefixLen == 64
                assert pc.addressMode == IPv6Constants.Stateful_DHCP
                assert pc.dhcpServerIp == null
                assert pc.dhcpNetmask == null
            } else {
                assert pc.dhcp6ServerIp == null
                assert pc.prefixLen == null
                assert pc.addressMode == null
                assert pc.dhcpServerIp == ret4.ip
                assert pc.dhcpNetmask == "255.255.255.0"
            }
        }
        assert cmds.size() == 2
        for (FlatDhcpBackend.ApplyDhcpCmd c : cmds) {
            assert c.dhcp.size() == 1
            dhcpInfo = c.dhcp.get(0)
            if (dhcpInfo.ipVersion == IPv6Constants.IPv6) {
                assert dhcpInfo.l3NetworkUuid == l3_statefull.uuid
                assert dhcpInfo.ip6 == ip6.ip
                assert dhcpInfo.gateway6 == ip6.getGateway()
                assert dhcpInfo.mac == nic.mac
                assert dhcpInfo.firstIp == ipr.getStartIp()
                assert dhcpInfo.endIp == ipr.getEndIp()
                assert dhcpInfo.ip == null
                assert dhcpInfo.gateway == null
            } else {
                assert dhcpInfo.l3NetworkUuid == l3.uuid
                assert dhcpInfo.ip6 == null
                assert dhcpInfo.gateway6 == null
                assert dhcpInfo.mac == nic.mac
                assert dhcpInfo.ip == ip4.ip
                assert dhcpInfo.gateway == ip4.gateway
            }
        }

        pcmds = Collections.synchronizedList(new ArrayList())
        cmds =  Collections.synchronizedList(new ArrayList())
        reconnectHost {
            uuid = vm.hostUuid
        }
        assert pcmds.size() == 2
        for (FlatDhcpBackend.PrepareDhcpCmd pc : pcmds) {
            if (pc.ipVersion == IPv6Constants.IPv6) {
                assert pc.dhcp6ServerIp == ret.ip6
                assert pc.prefixLen == 64
                assert pc.addressMode == IPv6Constants.Stateful_DHCP
                assert pc.dhcpServerIp == null
                assert pc.dhcpNetmask == null
            } else {
                assert pc.dhcp6ServerIp == null
                assert pc.prefixLen == null
                assert pc.addressMode == null
                assert pc.dhcpServerIp == ret4.ip
                assert pc.dhcpNetmask == "255.255.255.0"
            }
        }
        assert cmds.size() == 2
        for (FlatDhcpBackend.ApplyDhcpCmd c : cmds) {
            assert c.dhcp.size() == 1
            dhcpInfo = c.dhcp.get(0)
            if (dhcpInfo.ipVersion == IPv6Constants.IPv6) {
                assert dhcpInfo.l3NetworkUuid == l3_statefull.uuid
                assert dhcpInfo.ip6 == ip6.ip
                assert dhcpInfo.gateway6 == ip6.getGateway()
                assert dhcpInfo.mac == nic.mac
                assert dhcpInfo.firstIp == ipr.getStartIp()
                assert dhcpInfo.endIp == ipr.getEndIp()
                assert dhcpInfo.ip == null
                assert dhcpInfo.gateway == null
            } else {
                assert dhcpInfo.l3NetworkUuid == l3.uuid
                assert dhcpInfo.ip6 == null
                assert dhcpInfo.gateway6 == null
                assert dhcpInfo.mac == nic.mac
                assert dhcpInfo.ip == ip4.ip
                assert dhcpInfo.gateway == ip4.gateway
            }
        }
    }


}

