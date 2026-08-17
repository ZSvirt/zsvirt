package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.*;
import org.zstack.network.service.DnsUtils;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.network.IPv6Constants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNicParamBuilder {
    @Autowired
    private ResourceConfigFacade rcf;

    public List<VmNicParam> buildByVmUuid(String vmInstanceUuid) {
        List<VmNicParam> vmNicParams = new ArrayList<>();
        VmInstanceVO vm = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmInstanceUuid).find();
        boolean isWindowsVm = ImagePlatform.Windows.toString().equals(vm.getPlatform());

        List<VmNicVO> sortedVmNics = vm.getVmNics().stream()
                .sorted(Comparator.comparingInt(VmNicVO::getDeviceId))
                .collect(Collectors.toList());
        if (isWindowsVm) {
            for (VmNicVO nicVO: sortedVmNics) {
                VmNicParam nicParam = new VmNicParam();
                nicParam.setL3NetworkUuid(nicVO.getL3NetworkUuid());

                boolean isSet = false;

                List<String> dnsList = DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv4);
                if (!CollectionUtils.isEmpty(dnsList)) {
                    nicParam.setDnsList(dnsList);
                    isSet = true;
                }

                List<String> dns6List = DnsUtils.getVmNicDnsList(nicVO.getUuid(), IPv6Constants.IPv6);
                if (!CollectionUtils.isEmpty(dns6List)) {
                    nicParam.setDns6List(dns6List);
                    isSet = true;
                }

                if (isSet) {
                    vmNicParams.add(nicParam);
                }
            }
        } else {
            String defaultL3Uuid = vm.getDefaultL3NetworkUuid();
            VmNicParam nicParam = new VmNicParam();
            nicParam.setL3NetworkUuid(defaultL3Uuid);
            boolean isSet = false;

            List<String> dnsList = DnsUtils.getVmDnsList(vmInstanceUuid);
            if (!CollectionUtils.isEmpty(dnsList)) {
                nicParam.setDnsList(dnsList);
                isSet = true;
            }

            if (isSet) {
                vmNicParams.add(nicParam);
            }
        }

        List<VmNicParam> remainingParams = new ArrayList<>(vmNicParams);
        List<VmNicParam> ret = new ArrayList<>();
        for (VmNicVO nic: sortedVmNics) {
            VmNicParam nicParam = remainingParams.stream()
                    .filter(param -> Objects.equals(param.getL3NetworkUuid(), nic.getL3NetworkUuid()))
                    .findFirst()
                    .orElse(new VmNicParam());

            nicParam.setL3NetworkUuid(nic.getL3NetworkUuid());
            nicParam.setState(nic.getState().toString());
            nicParam.setDriverType(nic.getDriverType());

            Integer nicMultiQueueNum = rcf.getResourceConfigValueByResourceType(
                    VmGlobalConfig.VM_NIC_MULTIQUEUE_NUM, nic.getUuid(), VmNicVO.class.getSimpleName(), Integer.class);
            if (nicMultiQueueNum != null) {
                nicParam.setMultiQueueNum(nicMultiQueueNum);
            }

            remainingParams.remove(nicParam);
            ret.add(nicParam);
        }

        return ret;
    }
}
