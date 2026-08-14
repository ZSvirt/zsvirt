package org.zstack.network.service;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.vm.VmDnsVO;
import org.zstack.header.vm.VmDnsVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import javax.persistence.Tuple;
import java.util.ArrayList;
import java.util.List;

public class DnsUtils {
    public static List<Tuple> getTupleOfIdAndDns(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.id, VmDnsVO_.dns)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listTuple();
    }

    public static List<String> getVmNicDnsList(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.dns)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listValues();
    }

    public static List<String> getVmDnsList(String vmUuid) {
        return Q.New(VmDnsVO.class).select(VmDnsVO_.dns)
                .eq(VmDnsVO_.vmInstanceUuid, vmUuid)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .listValues();
    }

    public static List<VmDnsVO> getVmDnsVOList(String vmNicUuid, Integer ipVersion) {
        return Q.New(VmDnsVO.class)
                .eq(VmDnsVO_.vmNicUuid, vmNicUuid)
                .eq(VmDnsVO_.ipVersion, ipVersion)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .list();
    }

    public static List<VmDnsVO> getVmDnsVOList(String vmUuid) {
        return Q.New(VmDnsVO.class)
                .eq(VmDnsVO_.vmInstanceUuid, vmUuid)
                .orderBy(VmDnsVO_.id, SimpleQuery.Od.ASC)
                .list();
    }

    public static List<VmDnsVO> getVmDnsListOnL3(List<String> vmUuids, Integer ipVersion, String l3Uuid) {
        if (CollectionUtils.isEmpty(vmUuids)) {
            return new ArrayList<>();
        }

        List<VmDnsVO> vos = SQL.New("select dns from VmDnsVO dns, VmNicVO nic" +
                        " where dns.ipVersion = :ipVersion" +
                        " and nic.uuid = dns.vmNicUuid" +
                        " and nic.l3NetworkUuid = :l3Uuid" +
                        " and nic.vmInstanceUuid in (:vmUuids)")
                .param("ipVersion", ipVersion)
                .param("l3Uuid", l3Uuid)
                .param("vmUuids", vmUuids)
                .list();

        vos.addAll(SQL.New("select dns from VmDnsVO dns, VmInstanceVO vm" +
                        " where dns.vmInstanceUuid = vm.uuid" +
                        " and dns.vmNicUuid is null" +
                        " and vm.defaultL3NetworkUuid = :l3Uuid" +
                        " and vm.uuid in (:vmUuids)")
                .param("l3Uuid", l3Uuid)
                .param("vmUuids", vmUuids)
                .list());

        return vos;
    }

    @SuppressWarnings({"unchecked"})
    public static List<String> getDnsListFromString(String dnsListStr) {
        if (StringUtils.isEmpty(dnsListStr)) {
            return new ArrayList<>();
        }

        try {
            return JSONObjectUtil.toCollection(dnsListStr, ArrayList.class, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
