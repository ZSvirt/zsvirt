package org.zstack.kvm.hypervisor.message;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.kvm.hypervisor.datatype.HostOsCategoryInventory;
import org.zstack.kvm.hypervisor.datatype.KvmHostHypervisorMetadataInventory;

import java.sql.Timestamp;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by Wenhao.Zhang on 23/02/23
 */
@RestResponse(allTo = "inventories")
public class APIQueryHostOsCategoryReply extends APIQueryReply {
    private List<HostOsCategoryInventory> inventories;

    public List<HostOsCategoryInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<HostOsCategoryInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryHostOsCategoryReply __example__() {
        APIQueryHostOsCategoryReply reply = new APIQueryHostOsCategoryReply();
        HostOsCategoryInventory inv = new HostOsCategoryInventory();
        inv.setArchitecture("x86_64");
        inv.setOsReleaseVersion("centos Core 7.6.1810");
        inv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));

        KvmHostHypervisorMetadataInventory metadata = new KvmHostHypervisorMetadataInventory();
        metadata.setManagementNodeUuid(uuid());
        metadata.setHypervisor("qemu-kvm");
        metadata.setVersion("4.2.0-632.g6a6222b.el7");
        metadata.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        metadata.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inv.setMetadataList(asList(metadata));

        reply.setSuccess(true);
        reply.setInventories(asList(inv));
        return reply;
    }
}
