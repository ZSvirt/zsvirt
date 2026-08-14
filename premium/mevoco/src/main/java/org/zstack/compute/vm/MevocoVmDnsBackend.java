package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.*;
import org.zstack.network.service.DnsUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MevocoVmDnsBackend implements VmDnsBackend, VmPlatformChangedExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MevocoVmDnsBackend.class);
    @Autowired
    protected DatabaseFacade dbf;

    @Override
    public String getVmInstanceType() {
        return VmInstanceConstant.USER_VM_TYPE;
    }

    @Override
    public void setNicDns(String vmUuid, String vmNicUuid, List<String> dnsList, Integer ipVersion) {
        if (dnsList == null) {
            return;
        }

        List<Tuple> oldDnsTuple = DnsUtils.getTupleOfIdAndDns(vmNicUuid, ipVersion);
        List<String> oldDnsList = oldDnsTuple.stream().map(t -> t.get(1, String.class)).collect(Collectors.toList());
        if (Objects.equals(oldDnsList, dnsList)) {
            if (!dnsList.isEmpty()) {
                logger.info(String.format("dns list %s of vm nic[uuid:%s] has not changed, no need to update", dnsList, vmNicUuid));
            }
            return;
        }

        List<Long> oldDnsIds = oldDnsTuple.stream().map(t -> t.get(0, Long.class)).collect(Collectors.toList());
        for (String dns: dnsList) {
            logger.debug(String.format("set dns[%s] for vm nic[uuid:%s]", dns, vmNicUuid));
            VmDnsVO vo = new VmDnsVO();
            vo.setVmInstanceUuid(vmUuid);
            vo.setVmNicUuid(vmNicUuid);
            vo.setDns(dns);
            vo.setIpVersion(ipVersion);
            dbf.persist(vo);
        }

        if (!oldDnsIds.isEmpty()) {
            logger.debug(String.format("delete old dns list %s of vm nic[uuid:%s]", oldDnsList, vmNicUuid));
            dbf.removeByPrimaryKeys(oldDnsIds, VmDnsVO.class);
        }
    }

    @Override
    public void setVmDns(String vmUuid, List<String> dnsList) {
        if (dnsList == null) {
            return;
        }

        List<Tuple> oldDnsTuple = Q.New(VmDnsVO.class).select(VmDnsVO_.id, VmDnsVO_.dns)
                .eq(VmDnsVO_.vmInstanceUuid, vmUuid)
                .listTuple();
        List<String> oldDnsList = oldDnsTuple.stream().map(t -> t.get(1, String.class)).collect(Collectors.toList());
        if (Objects.equals(oldDnsList, dnsList)) {
            if (!dnsList.isEmpty()) {
                logger.info(String.format("dns list %s of vm[uuid:%s] has not changed, no need to update", dnsList, vmUuid));
            }
            return;
        }

        List<Long> oldDnsIds = oldDnsTuple.stream().map(t -> t.get(0, Long.class)).collect(Collectors.toList());
        for (String dns: dnsList) {
            logger.debug(String.format("set dns[%s] for vm[uuid:%s]", dns, vmUuid));
            VmDnsVO vo = new VmDnsVO();
            vo.setVmInstanceUuid(vmUuid);
            vo.setDns(dns);
            vo.setIpVersion(NetworkUtils.getIpversion(dns));
            dbf.persist(vo);
        }

        if (!oldDnsIds.isEmpty()) {
            logger.debug(String.format("delete old dns list %s of vm[uuid:%s]", oldDnsList, vmUuid));
            dbf.removeByPrimaryKeys(oldDnsIds, VmDnsVO.class);
        }
    }

    @Override
    public boolean skipPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        return false;
    }

    private boolean isWindows(String platform) {
        return ImagePlatform.Windows.toString().equals(platform) || ImagePlatform.WindowsVirtio.toString().equals(platform);
    }

    @Override
    public void vmPlatformChange(VmInstanceInventory vm, String previousPlatform, String nowPlatform) {
        boolean fromWindows = isWindows(previousPlatform) && !isWindows(nowPlatform);
        boolean toWindows = !isWindows(previousPlatform) && isWindows(nowPlatform);
        if (fromWindows || toWindows) {
            List<Tuple> oldDnsTuple = Q.New(VmDnsVO.class).select(VmDnsVO_.id, VmDnsVO_.dns)
                    .eq(VmDnsVO_.vmInstanceUuid, vm.getUuid())
                    .listTuple();
            if (oldDnsTuple.isEmpty()) {
                return;
            }

            List<String> oldDnsList = oldDnsTuple.stream().map(t -> t.get(1, String.class)).collect(Collectors.toList());
            List<Long> oldDnsIds = oldDnsTuple.stream().map(t -> t.get(0, Long.class)).collect(Collectors.toList());
            String msg;
            if (fromWindows) {
                msg = "from Windows to others";
            } else {
                msg = "from others to Windows";
            }

            dbf.removeByPrimaryKeys(oldDnsIds, VmDnsVO.class);
            logger.debug(String.format("delete old dns list %s of vm[uuid:%s] when changing platform %s", oldDnsList, vm.getUuid(), msg));
        }
    }
}
