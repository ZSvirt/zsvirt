package org.zstack.ovf.api;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.ovf.datatype.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Qi Le on 2022/3/3
 */
@RestResponse(allTo = "ovfInfo")
public class APIParseOvfReply extends APIReply {
    private OvfInfo ovfInfo;

    public OvfInfo getOvfInfo() {
        return ovfInfo;
    }

    public void setOvfInfo(OvfInfo ovfInfo) {
        this.ovfInfo = ovfInfo;
    }

    public static APIParseOvfReply __example__() {
        APIParseOvfReply reply = new APIParseOvfReply();
        OvfInfo info = new OvfInfo();
        reply.setOvfInfo(info);
        info.setVmName("VM-1");
        OvfCpuInfo cpu = new OvfCpuInfo();
        info.setCpu(cpu);
        cpu.setQuantity(4);
        cpu.setCoresPerSocket(4);
        cpu.setInstanceId("2");
        OvfMemoryInfo memory = new OvfMemoryInfo();
        info.setMemory(memory);
        memory.setQuantity(4294967296L);
        memory.setInstanceId("3");
        List<OvfDiskInfo> disks = new ArrayList<>();
        info.setDisks(disks);
        OvfDiskInfo disk = new OvfDiskInfo();
        disks.add(disk);
        disk.setFormat("vmdk");
        disk.setDiskId("system");
        disk.setCapacity(64424509440L);
        disk.setFileName("system.vmdk");
        disk.setFileRef("file1");
        disk.setPopulatedSize(1688600576L);
        List<OvfNetworkInfo> networks = new ArrayList<>();
        info.setNetworks(networks);
        OvfNetworkInfo network = new OvfNetworkInfo();
        networks.add(network);
        network.setName("red-net");
        OvfOSInfo os = new OvfOSInfo();
        info.setOs(os);
        os.setDescription("CentOS 7");
        os.setOsType("centos");
        os.setId(107);
        os.setVersion("7");
        OvfSystemInfo system = new OvfSystemInfo();
        info.setSystemInfo(system);
        system.setVirtualSystemType("vmx-18");
        List<OvfEthernetAdapterInfo> nics = new ArrayList<>();
        info.setNics(nics);
        OvfEthernetAdapterInfo nic = new OvfEthernetAdapterInfo();
        nics.add(nic);
        nic.setNetworkName("red-net");
        nic.setNicName("nic-1");
        nic.setNicModel("E1000");
        nic.setAutoAllocation(true);
        List<OvfCdDriverInfo> cds = new ArrayList<>();
        info.setCdDrivers(cds);
        OvfCdDriverInfo cd = new OvfCdDriverInfo();
        cds.add(cd);
        cd.setName("CD");
        cd.setSubType("vmware.cdrom.remotepassthrough");
        cd.setAutoAllocation(true);
        cd.setDriverType("SATA");
        List<OvfVolumeInfo> volumes = new ArrayList<>();
        info.setVolumes(volumes);
        OvfVolumeInfo volume = new OvfVolumeInfo();
        volumes.add(volume);
        volume.setName("System Volume");
        volume.setDriverType("IDE");
        volume.setDiskId("system");
        return reply;
    }
}
