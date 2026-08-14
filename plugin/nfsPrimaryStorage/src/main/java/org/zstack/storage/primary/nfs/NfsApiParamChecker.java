package org.zstack.storage.primary.nfs; /**
 * Created by xing5 on 2016/8/19.
 */

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.storage.primary.APIUpdatePrimaryStorageMsg;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.storage.primary.PrimaryStorageSystemTags;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.network.NetworkUtils;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.List;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NfsApiParamChecker {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    public void checkUrl(String zoneUuid, List<String> systemTags, String url) {
        DebugUtils.Assert(url != null, "URL cannot be null !!!");
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.add(PrimaryStorageVO_.type, Op.EQ, NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE);
        q.add(PrimaryStorageVO_.url, Op.EQ, url);
        q.add(PrimaryStorageVO_.zoneUuid, Op.EQ, zoneUuid);
        if (q.isExists()) {
            throw new ApiMessageInterceptionException(argerr("there has been a nfs primary storage having url as %s in zone[uuid:%s]", url, zoneUuid));
        }

        String[] results = url.split(":");
        if (results.length == 2 && (
                results[1].startsWith("/dev") || results[1].startsWith("/proc") || results[1].startsWith("/sys"))) {
            throw new ApiMessageInterceptionException(argerr(" the url contains an invalid folder[/dev or /proc or /sys]"));
        }

        validateUrl(systemTags, results[0]);
    }


    private void validateUrl(List<String> systemTags, String ipAddr) {
        if (systemTags != null) {
            boolean found = false;
            for (String sysTag: systemTags) {
                if (PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.isMatch(sysTag)) {
                    if (found) {
                        throw new ApiMessageInterceptionException(argerr("found multiple CIDR"));
                    }

                    validateCidrTag(sysTag, ipAddr);
                    found = true;
                }
            }
        }
    }
    private void validateCidrTag(String sysTag, String ipAddr) {
        String cidr = PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY.getTokenByTag(
                sysTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_GATEWAY_TOKEN);
        if (!NetworkUtils.isCidr(cidr)) {
            throw new ApiMessageInterceptionException(argerr("invalid CIDR: %s", cidr));
        }

        if (!NetworkUtils.isIpv4InCidr(ipAddr, cidr)) {
            throw new ApiMessageInterceptionException(argerr("IP address[%s] is not in CIDR[%s]", ipAddr, cidr));
        }
    }

    public void checkRunningVmForUpdateUrl(String psuuid) {
        String sql = "select vm.name, vm.uuid from VmInstanceVO vm, VolumeVO vol where vm.uuid = vol.vmInstanceUuid and" +
                " vol.primaryStorageUuid = :psUuid and vm.state = :vmState";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("psUuid", psuuid);
        q.setParameter("vmState", VmInstanceState.Running);
        List<Tuple> ts = q.getResultList();

        if (!ts.isEmpty()) {
            List<String> vms = ts.stream().map(v -> String.format("VM[name:%s, uuid:%s]", v.get(0, String.class), v.get(1, String.class))).collect(Collectors.toList());
            throw new ApiMessageInterceptionException(operr("there are %s running VMs on the NFS primary storage, please" +
                    " stop them and try again:\n%s\n", vms.size(), StringUtils.join(vms, "\n")));
        }
    }
}
