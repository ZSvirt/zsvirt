package org.zstack.allocator.domain.api;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.util.ArrayList;
import java.util.List;

@RestResponse(fieldsTo = {"all"})
public class APIGetAllocatorDomainCandidatesReply extends APIReply {
    private List<HostInventory> hosts;
    private List<ClusterInventory> clusters;
    private List<PrimaryStorageInventory> primaryStorages;
    private List<BackupStorageInventory> backupStorages;
    private List<L3NetworkInventory> l3Networks;

    public List<HostInventory> getHosts() {
        return hosts;
    }

    public void setHosts(List<HostInventory> hosts) {
        this.hosts = hosts;
    }

    public List<ClusterInventory> getClusters() {
        return clusters;
    }

    public void setClusters(List<ClusterInventory> clusters) {
        this.clusters = clusters;
    }

    public List<PrimaryStorageInventory> getPrimaryStorages() {
        return primaryStorages;
    }

    public void setPrimaryStorages(List<PrimaryStorageInventory> primaryStorages) {
        this.primaryStorages = primaryStorages;
    }

    public List<BackupStorageInventory> getBackupStorages() {
        return backupStorages;
    }

    public void setBackupStorages(List<BackupStorageInventory> backupStorages) {
        this.backupStorages = backupStorages;
    }

    public List<L3NetworkInventory> getL3Networks() {
        return l3Networks;
    }

    public void setL3Networks(List<L3NetworkInventory> l3Networks) {
        this.l3Networks = l3Networks;
    }

    public static APIGetAllocatorDomainCandidatesReply __example__() {
        APIGetAllocatorDomainCandidatesReply reply = new APIGetAllocatorDomainCandidatesReply();
        reply.setHosts(new ArrayList<>());
        reply.setClusters(new ArrayList<>());
        reply.setPrimaryStorages(new ArrayList<>());
        reply.setBackupStorages(new ArrayList<>());
        reply.setL3Networks(new ArrayList<>());
        return reply;
    }
}
