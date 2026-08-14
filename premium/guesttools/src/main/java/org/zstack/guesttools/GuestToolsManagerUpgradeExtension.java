package org.zstack.guesttools;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.vm.VmGuestNetworkInfoVO;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.network.l3.VmNicSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GuestToolsManagerUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(GuestToolsManagerUpgradeExtension.class);

    @Autowired
    DatabaseFacade dbf;

    private void upgradeInternalIpSystemTags() {
        List<Tuple> tuples = Q.New(VmNicVO.class).select(VmNicVO_.uuid, VmNicVO_.vmInstanceUuid).listTuple();
        if (CollectionUtils.isEmpty(tuples)) {
            logger.debug("no need to upgrade internal ip system tag because there is no vm nic");
            return;
        }

        Map<String, String> nicVmMap = tuples.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, String.class),
                        tuple -> tuple.get(1, String.class)
                ));

        Map<String, VmGuestNetworkInfoVO> infoMap = new HashMap<>();
        List<SystemTagInventory> tagInventories = VmNicSystemTags.VM_NIC_INTERNAL_IP.getTagInventories(new ArrayList<>(nicVmMap.keySet()));
        tagInventories.addAll(VmNicSystemTags.VM_NIC_INTERNAL_IPV6.getTagInventories(new ArrayList<>(nicVmMap.keySet())));
        for (SystemTagInventory tagInv : tagInventories) {
            String nicUuid = tagInv.getResourceUuid();
            String vmUuid = nicVmMap.get(nicUuid);
            if (vmUuid == null) {
                continue;
            }
            VmGuestNetworkInfoVO info = infoMap.computeIfAbsent(nicUuid, k -> new VmGuestNetworkInfoVO(vmUuid, nicUuid));
            if (VmNicSystemTags.VM_NIC_INTERNAL_IP.isMatch(tagInv.getTag())) {
                info.setIpAddress(VmNicSystemTags.VM_NIC_INTERNAL_IP.getTokenByTag(tagInv.getTag(), VmNicSystemTags.VM_NIC_INTERNAL_IP_TOKEN));
            } else if (VmNicSystemTags.VM_NIC_INTERNAL_IPV6.isMatch(tagInv.getTag())) {
                info.setIpv6Address(IPv6NetworkUtils.ipv6TagValueToAddress(
                        VmNicSystemTags.VM_NIC_INTERNAL_IPV6.getTokenByTag(tagInv.getTag(), VmNicSystemTags.VM_NIC_INTERNAL_IPV6_TOKEN)));
            }
        }

        dbf.persistCollection(new ArrayList<>(infoMap.values()));
    }

    @Override
    public boolean start() {
        upgradeInternalIpSystemTags();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
