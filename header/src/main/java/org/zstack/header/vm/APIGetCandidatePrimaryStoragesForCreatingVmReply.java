package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.utils.data.SizeUnit;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.asList;
/**
 * Created by MaJin on 2017-08-16.
 */

@RestResponse(fieldsTo = "all")
public class APIGetCandidatePrimaryStoragesForCreatingVmReply extends APIReply {
    private List<PrimaryStorageInventory> rootVolumePrimaryStorages = new ArrayList<>();
    private Map<String, List<PrimaryStorageInventory>> dataVolumePrimaryStorages= new ConcurrentHashMap<>();

    public void setDataVolumePrimaryStorages(Map<String, List<PrimaryStorageInventory>> dataVolumePrimaryStorages) {
        this.dataVolumePrimaryStorages = dataVolumePrimaryStorages;
    }

    public Map<String, List<PrimaryStorageInventory>> getDataVolumePrimaryStorages() {
        return dataVolumePrimaryStorages;
    }

    public void setRootVolumePrimaryStorages(List<PrimaryStorageInventory> rootVolumePrimaryStorages) {
        this.rootVolumePrimaryStorages = rootVolumePrimaryStorages;
    }

    public List<PrimaryStorageInventory> getRootVolumePrimaryStorages() {
        return rootVolumePrimaryStorages;
    }

    public static APIGetCandidatePrimaryStoragesForCreatingVmReply __example__() {
        APIGetCandidatePrimaryStoragesForCreatingVmReply reply = new APIGetCandidatePrimaryStoragesForCreatingVmReply();

        PrimaryStorageInventory lsInv = new PrimaryStorageInventory();
        lsInv.setName("example");
        lsInv.setDescription("example");
        lsInv.setUuid(uuid());
        lsInv.setAttachedClusterUuids(asList(uuid()));
        lsInv.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(200L));
        lsInv.setAvailablePhysicalCapacity(SizeUnit.GIGABYTE.toByte(200L));
        lsInv.setTotalCapacity(SizeUnit.GIGABYTE.toByte(300L));
        lsInv.setTotalPhysicalCapacity(SizeUnit.GIGABYTE.toByte(300L));
        lsInv.setState(PrimaryStorageState.Enabled.toString());
        lsInv.setStatus(PrimaryStorageStatus.Connected.toString());
        lsInv.setType("LocalStorage");
        lsInv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        lsInv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        lsInv.setUrl("/zstack_ps");

        PrimaryStorageInventory nfsInv = new PrimaryStorageInventory();
        String uuid = uuid();
        nfsInv.setName("example");
        nfsInv.setDescription("example");
        nfsInv.setUuid(uuid);
        nfsInv.setAttachedClusterUuids(asList(uuid()));
        nfsInv.setAvailableCapacity(SizeUnit.GIGABYTE.toByte(200L));
        nfsInv.setAvailablePhysicalCapacity(SizeUnit.GIGABYTE.toByte(200L));
        nfsInv.setTotalCapacity(SizeUnit.GIGABYTE.toByte(300L));
        nfsInv.setTotalPhysicalCapacity(SizeUnit.GIGABYTE.toByte(300L));
        nfsInv.setState(PrimaryStorageState.Enabled.toString());
        nfsInv.setStatus(PrimaryStorageStatus.Connected.toString());
        nfsInv.setType("NFS");
        nfsInv.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        nfsInv.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        nfsInv.setUrl("/opt/zstack/nfsprimarystorage/prim-" + uuid);

        reply.getDataVolumePrimaryStorages().put(uuid(), asList(nfsInv));
        reply.setRootVolumePrimaryStorages(asList(lsInv));
        return reply;
    }
}
