package org.zstack.crypto.keyprovider;

import org.zstack.core.cloudbus.MarshalReplyMessageExtensionPoint;
import org.zstack.core.db.Q;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO;
import org.zstack.header.keyprovider.EncryptedResourceKeyRefVO_;
import org.zstack.header.message.Message;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotReply;
import org.zstack.header.storage.snapshot.APIQueryVolumeSnapshotTreeReply;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotTree;
import org.zstack.header.storage.snapshot.VolumeSnapshotTreeInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.APIQueryVmInstanceReply;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.APIQueryVolumeReply;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.query.APIZQLQueryReply;
import org.zstack.zql.ZQLQueryReturn;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EncryptionInventoryReplyMarshaller implements MarshalReplyMessageExtensionPoint {
    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        return Arrays.asList(
                APIQueryVmInstanceReply.class,
                APIQueryVolumeReply.class,
                APIQueryVolumeSnapshotReply.class,
                APIQueryVolumeSnapshotTreeReply.class,
                APIZQLQueryReply.class
        );
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof APIQueryVmInstanceReply) {
            fillVmEncryptionInfo(((APIQueryVmInstanceReply) replyOrEvent).getInventories());
        } else if (replyOrEvent instanceof APIQueryVolumeReply) {
            fillVolumeEncryptionInfo(((APIQueryVolumeReply) replyOrEvent).getInventories());
        } else if (replyOrEvent instanceof APIQueryVolumeSnapshotReply) {
            fillVolumeSnapshotEncryptionInfo(((APIQueryVolumeSnapshotReply) replyOrEvent).getInventories());
        } else if (replyOrEvent instanceof APIQueryVolumeSnapshotTreeReply) {
            fillVolumeSnapshotTreeEncryptionInfo(((APIQueryVolumeSnapshotTreeReply) replyOrEvent).getInventories());
        } else if (replyOrEvent instanceof APIZQLQueryReply) {
            fillZqlResultEncryptionInfo((APIZQLQueryReply) replyOrEvent);
        }
    }

    private void fillZqlResultEncryptionInfo(APIZQLQueryReply reply) {
        if (reply.getResults() == null) {
            return;
        }

        for (ZQLQueryReturn result : reply.getResults()) {
            if (result == null || result.inventories == null || result.inventories.isEmpty()) {
                continue;
            }

            Object first = result.inventories.get(0);
            if (first instanceof VmInstanceInventory) {
                fillVmEncryptionInfo(castInventoryList(result.inventories, VmInstanceInventory.class));
            } else if (first instanceof VolumeInventory) {
                fillVolumeEncryptionInfo(castInventoryList(result.inventories, VolumeInventory.class));
            } else if (first instanceof VolumeSnapshotInventory) {
                fillVolumeSnapshotEncryptionInfo(castInventoryList(result.inventories, VolumeSnapshotInventory.class));
            }
        }
    }

    private <T> List<T> castInventoryList(List inventories, Class<T> clz) {
        List<T> ret = new ArrayList<>();
        for (Object inv : inventories) {
            if (clz.isInstance(inv)) {
                ret.add(clz.cast(inv));
            }
        }
        return ret;
    }

    private void fillVolumeEncryptionInfo(List<VolumeInventory> volumes) {
        if (volumes == null || volumes.isEmpty()) {
            return;
        }

        Map<String, VolumeInventory> volumeByUuid = volumes.stream()
                .filter(v -> v.getUuid() != null)
                .collect(Collectors.toMap(VolumeInventory::getUuid, v -> v, (a, b) -> a));
        if (volumeByUuid.isEmpty()) {
            return;
        }

        List<Tuple> tuples = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid, VolumeVO_.encrypted)
                .in(VolumeVO_.uuid, volumeByUuid.keySet())
                .listTuple();
        for (Tuple t : tuples) {
            VolumeInventory inv = volumeByUuid.get(t.get(0, String.class));
            if (inv != null) {
                inv.setEncrypted(t.get(1, Boolean.class));
            }
        }
    }

    private void fillVolumeSnapshotTreeEncryptionInfo(List<VolumeSnapshotTreeInventory> trees) {
        if (trees == null || trees.isEmpty()) {
            return;
        }

        // Snapshot tree replies embed VolumeSnapshotInventory nodes that also need keyLastOpDate.
        List<VolumeSnapshotInventory> snapshots = new ArrayList<>();
        for (VolumeSnapshotTreeInventory tree : trees) {
            collectSnapshotsFromTree(tree.getTree(), snapshots);
        }
        fillVolumeSnapshotEncryptionInfo(snapshots);
    }

    private void collectSnapshotsFromTree(VolumeSnapshotTree.SnapshotLeafInventory leaf,
                                          List<VolumeSnapshotInventory> snapshots) {
        if (leaf == null) {
            return;
        }

        if (leaf.getInventory() != null) {
            snapshots.add(leaf.getInventory());
        }
        if (leaf.getChildren() != null) {
            for (VolumeSnapshotTree.SnapshotLeafInventory child : leaf.getChildren()) {
                collectSnapshotsFromTree(child, snapshots);
            }
        }
    }

    private void fillVolumeSnapshotEncryptionInfo(List<VolumeSnapshotInventory> snapshots) {
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        Map<String, VolumeSnapshotInventory> snapshotByUuid = snapshots.stream()
                .filter(s -> s.getUuid() != null)
                .collect(Collectors.toMap(VolumeSnapshotInventory::getUuid, s -> s, (a, b) -> a));
        if (snapshotByUuid.isEmpty()) {
            return;
        }

        Set<String> encryptedSnapshotUuids = new HashSet<>();
        Map<String, Timestamp> createDateByUuid = new HashMap<>();
        List<Tuple> tuples = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.uuid, VolumeSnapshotVO_.encrypted, VolumeSnapshotVO_.createDate)
                .in(VolumeSnapshotVO_.uuid, snapshotByUuid.keySet())
                .listTuple();
        for (Tuple t : tuples) {
            VolumeSnapshotInventory inv = snapshotByUuid.get(t.get(0, String.class));
            if (inv == null) {
                continue;
            }

            String snapshotUuid = t.get(0, String.class);
            boolean encrypted = Boolean.TRUE.equals(t.get(1, Boolean.class));
            inv.setEncrypted(encrypted);
            createDateByUuid.put(snapshotUuid, t.get(2, Timestamp.class));
            if (encrypted) {
                encryptedSnapshotUuids.add(snapshotUuid);
            } else {
                inv.setKeyLastOpDate(null);
            }
        }

        Map<String, Timestamp> keyDateBySnapshotUuid = queryLatestKeyDates(
                VolumeSnapshotVO.class.getSimpleName(), encryptedSnapshotUuids);
        for (String snapshotUuid : encryptedSnapshotUuids) {
            VolumeSnapshotInventory inv = snapshotByUuid.get(snapshotUuid);
            if (inv == null) {
                continue;
            }

            Timestamp keyDate = keyDateBySnapshotUuid.get(snapshotUuid);
            inv.setKeyLastOpDate(keyDate != null ? keyDate : createDateByUuid.get(snapshotUuid));
        }
    }

    private Map<String, Timestamp> queryLatestKeyDates(String resourceType, Set<String> resourceUuids) {
        if (resourceUuids == null || resourceUuids.isEmpty()) {
            return Collections.emptyMap();
        }

        List<EncryptedResourceKeyRefVO> refs = Q.New(EncryptedResourceKeyRefVO.class)
                .eq(EncryptedResourceKeyRefVO_.resourceType, resourceType)
                .in(EncryptedResourceKeyRefVO_.resourceUuid, resourceUuids)
                .list();
        Map<String, Timestamp> ret = new HashMap<>();
        for (EncryptedResourceKeyRefVO ref : refs) {
            Timestamp keyDate = ref.getLastOpDate() == null ? ref.getCreateDate() : ref.getLastOpDate();
            if (keyDate != null) {
                ret.merge(ref.getResourceUuid(), keyDate, (existing, incoming) -> incoming.after(existing) ? incoming : existing);
            }
        }
        return ret;
    }

    private void fillVmEncryptionInfo(List<VmInstanceInventory> vms) {
        if (vms == null || vms.isEmpty()) {
            return;
        }

        Map<String, VmInstanceInventory> vmByUuid = vms.stream()
                .filter(v -> v.getUuid() != null)
                .collect(Collectors.toMap(VmInstanceInventory::getUuid, v -> v, (a, b) -> a));
        if (vmByUuid.isEmpty()) {
            return;
        }

        Set<String> vmUuids = vmByUuid.keySet();
        Map<String, Timestamp> vmCreateDateByUuid = queryVmCreateDates(vmUuids);
        Map<String, Boolean> vmEncryptionByUuid = queryVmEncryptionStates(vmUuids);
        Map<String, List<String>> encryptedVolumeUuidsByVmUuid = new HashMap<>();
        Set<String> encryptedVolumeUuids = queryEncryptedVolumeUuids(vmUuids, encryptedVolumeUuidsByVmUuid);

        Map<String, List<Timestamp>> keyDatesByVmUuid = new HashMap<>();
        addRefDates(keyDatesByVmUuid, Collections.singletonMap(VolumeVO.class.getSimpleName(), encryptedVolumeUuids),
                resourceUuidToVmUuid(encryptedVolumeUuidsByVmUuid));

        for (VmInstanceInventory vm : vms) {
            if (vm.getUuid() == null) {
                continue;
            }

            boolean hasEncryptedVolume = encryptedVolumeUuidsByVmUuid.containsKey(vm.getUuid());
            boolean hasEncryptedVm = Boolean.TRUE.equals(vmEncryptionByUuid.get(vm.getUuid()));
            boolean hasEncryptionResource = hasEncryptedVm || hasEncryptedVolume;
            vm.setVmEncryption(hasEncryptedVm);
            vm.setHasEncryptionResource(hasEncryptionResource);
            vm.setKeyLastOpDate(hasEncryptionResource ?
                    latestOrDefault(keyDatesByVmUuid.get(vm.getUuid()), hasEncryptedVm ? vmCreateDateByUuid.get(vm.getUuid()) : null) : null);
        }
    }

    private Map<String, Timestamp> queryVmCreateDates(Set<String> vmUuids) {
        List<Tuple> tuples = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.createDate)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple();
        return tuples.stream().collect(Collectors.toMap(
                t -> t.get(0, String.class),
                t -> t.get(1, Timestamp.class),
                (a, b) -> a
        ));
    }

    private Map<String, Boolean> queryVmEncryptionStates(Set<String> vmUuids) {
        List<Tuple> tuples = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.uuid, VmInstanceVO_.vmEncryption)
                .in(VmInstanceVO_.uuid, vmUuids)
                .listTuple();
        return tuples.stream().collect(Collectors.toMap(
                t -> t.get(0, String.class),
                t -> t.get(1, Boolean.class),
                (a, b) -> a
        ));
    }

    private Set<String> queryEncryptedVolumeUuids(Set<String> vmUuids, Map<String, List<String>> encryptedVolumeUuidsByVmUuid) {
        List<Tuple> tuples = Q.New(VolumeVO.class)
                .select(VolumeVO_.uuid, VolumeVO_.vmInstanceUuid, VolumeVO_.encrypted)
                .in(VolumeVO_.vmInstanceUuid, vmUuids)
                .listTuple();

        Set<String> ret = new HashSet<>();
        for (Tuple t : tuples) {
            if (!Boolean.TRUE.equals(t.get(2, Boolean.class))) {
                continue;
            }

            String volumeUuid = t.get(0, String.class);
            String vmUuid = t.get(1, String.class);
            ret.add(volumeUuid);
            encryptedVolumeUuidsByVmUuid.computeIfAbsent(vmUuid, k -> new ArrayList<>()).add(volumeUuid);
        }
        return ret;
    }

    private Map<String, String> resourceUuidToVmUuid(Map<String, List<String>> volumeUuidsByVmUuid) {
        Map<String, String> ret = new HashMap<>();
        for (Map.Entry<String, List<String>> e : volumeUuidsByVmUuid.entrySet()) {
            for (String volumeUuid : e.getValue()) {
                ret.put(volumeUuid, e.getKey());
            }
        }
        return ret;
    }

    private void addRefDates(Map<String, List<Timestamp>> keyDatesByVmUuid,
                             Map<String, Set<String>> resourceUuidsByType,
                             Map<String, String> resourceUuidToVmUuid) {
        for (Map.Entry<String, Set<String>> e : resourceUuidsByType.entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) {
                continue;
            }

            List<EncryptedResourceKeyRefVO> refs = Q.New(EncryptedResourceKeyRefVO.class)
                    .eq(EncryptedResourceKeyRefVO_.resourceType, e.getKey())
                    .in(EncryptedResourceKeyRefVO_.resourceUuid, e.getValue())
                    .list();
            for (EncryptedResourceKeyRefVO ref : refs) {
                String vmUuid = resourceUuidToVmUuid.get(ref.getResourceUuid());
                if (vmUuid == null) {
                    continue;
                }

                Timestamp keyDate = ref.getLastOpDate() == null ? ref.getCreateDate() : ref.getLastOpDate();
                if (keyDate != null) {
                    keyDatesByVmUuid.computeIfAbsent(vmUuid, k -> new ArrayList<>()).add(keyDate);
                }
            }
        }
    }

    private Timestamp latestOrDefault(List<Timestamp> timestamps, Timestamp defaultValue) {
        if (timestamps == null || timestamps.isEmpty()) {
            return defaultValue;
        }

        Timestamp latest = null;
        for (Timestamp ts : timestamps) {
            if (latest == null || ts.after(latest)) {
                latest = ts;
            }
        }
        return latest == null ? defaultValue : latest;
    }
}
