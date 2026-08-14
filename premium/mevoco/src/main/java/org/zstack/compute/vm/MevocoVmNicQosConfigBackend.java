package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.configuration.InstanceOfferingVO;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.*;
import org.zstack.mevoco.MevocoSystemTags;
import org.zstack.tag.SystemTag;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

public class MevocoVmNicQosConfigBackend implements VmNicQosConfigBackend {

    @Autowired
    private TagManager tagMgr;

    @Override
    public String getVmInstanceType() {
        return VmInstanceConstant.USER_VM_TYPE;
    }

    @Override
    public void addNicQos(String vmUuid, String vmNicUuid, Long outboundBandwidth, Long inboundBandwidth) {
        if (outboundBandwidth != null) {
            if (outboundBandwidth != -1) {
                SystemTagCreator creator = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.newSystemTagCreator(vmNicUuid);
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN, outboundBandwidth)));
                creator.create();
            } else {
                MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.delete(vmNicUuid);
            }
        }

        if (inboundBandwidth != null) {
            if (inboundBandwidth != -1) {
                SystemTagCreator creator = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.newSystemTagCreator(vmNicUuid);
                creator.inherent = false;
                creator.recreate = true;
                creator.setTagByTokens(map(e(MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN, inboundBandwidth)));
                creator.create();
            } else {
                MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.delete(vmNicUuid);
            }
        }
    }

    @Override
    public void deleteNicQos(String vmUuid, String vmNicUuid,String direction) {
        if (direction.equals("out")) {
            MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.delete(vmNicUuid);
            if (vmUuid != null) {
                MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.delete(vmUuid, VmInstanceVO.class);
            }
        } else if (direction.equals("in")) {
            MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.delete(vmNicUuid);
            if (vmUuid != null) {
                MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.delete(vmUuid, VmInstanceVO.class);
            }
        }
    }

    @Override
    public VmNicQosStruct getNicQos(String vmUuid, String vmNicUuid) {
        VmNicQosStruct struct = new VmNicQosStruct();
        struct.vmUuid = vmUuid;
        struct.vmNicUuid = vmNicUuid;

        String inThreshold = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid,
                VmInstanceVO.class, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
        if (inThreshold != null && !inThreshold.equals("0")) {
            struct.inboundBandwidthUpthreshold = Long.parseLong(inThreshold);
        }

        String outThreashold = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid,
                VmInstanceVO.class, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
        if (outThreashold != null && !outThreashold.equals("0")) {
            struct.outboundBandwidthUpthreshold = Long.parseLong(outThreashold);
        }

        String inbound = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByResourceUuid(vmNicUuid,
                InstanceOfferingVO.class, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
        if (inbound == null) {
            inbound = inThreshold;
        }
        if (inbound != null && !inbound.equals("0")) {
            struct.inboundBandwidth = Long.parseLong(inbound);
        } else {
            struct.inboundBandwidth = -1L;
        }

        String outbound = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(vmNicUuid,
                InstanceOfferingVO.class, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
        if (outbound == null) {
            outbound = outThreashold;
        }
        if (outbound != null && !outbound.equals("0")) {
            struct.outboundBandwidth = Long.parseLong(outbound);
        } else {
            struct.outboundBandwidth = -1L;
        }

        return struct;
    }

    @Override
    public void addVmQos(String vmUuid, Long outboundBandwidth, Long inboundBandwidth) {
        if (outboundBandwidth != null && outboundBandwidth != -1L) {
            SystemTagCreator creator = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.newSystemTagCreator(vmUuid);
            creator.inherent = false;
            creator.recreate = true;
            creator.resourceClass = VmInstanceVO.class;
            creator.setTagByTokens(map(e(MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN, outboundBandwidth)));
            creator.create();
        }

        if (inboundBandwidth != null && inboundBandwidth != -1L) {
            SystemTagCreator icreator = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.newSystemTagCreator(vmUuid);
            icreator.inherent = false;
            icreator.recreate = true;
            icreator.resourceClass = VmInstanceVO.class;
            icreator.setTagByTokens(map(e(MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN, inboundBandwidth)));
            icreator.create();
        }
    }

    @Override
    public void deleteVmQos(String vmUuid, String direction) {
        if (direction.equals("in")) {
            MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.delete(vmUuid, VmInstanceVO.class);
        } else if (direction.equals("out")) {
            MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.delete(vmUuid, VmInstanceVO.class);
        }
    }

    @Override
    public VmNicQosStruct getVmQos(String vmUuid) {
        VmNicQosStruct struct = new VmNicQosStruct();
        struct.vmUuid = vmUuid;
        String in = MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid,
                VmInstanceVO.class, MevocoSystemTags.NETWORK_INBOUND_BANDWIDTH_TOKEN);
        if (in != null) {
            struct.inboundBandwidthUpthreshold = Long.parseLong(in);
        }

        String out = MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH.getTokenByResourceUuid(vmUuid,
                VmInstanceVO.class, MevocoSystemTags.NETWORK_OUTBOUND_BANDWIDTH_TOKEN);
        if (out != null) {
            struct.outboundBandwidthUpthreshold = Long.parseLong(out);
        }
        return struct;
    }
}
