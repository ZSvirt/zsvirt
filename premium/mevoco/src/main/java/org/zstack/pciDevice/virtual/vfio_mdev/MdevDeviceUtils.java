package org.zstack.pciDevice.virtual.vfio_mdev;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.core.db.UpdateQuery;
import org.zstack.pciDevice.PciDeviceSystemTags;
import org.zstack.pciDevice.specification.mdev.VmInstanceMdevDeviceSpecRefVO;
import org.zstack.pciDevice.specification.mdev.VmInstanceMdevDeviceSpecRefVO_;

import javax.persistence.Tuple;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by GuoYi on 2019-05-07.
 */
public class MdevDeviceUtils {
    public static Map<String, Integer> getVmMdevSpecUuids(String vmUuid) {
        Map<String, Integer> ret = new HashMap<>();

        // transform MDEV_DEVICE_SPEC tags into RefVO records
        List<Map<String, String>> tokenList = MdevDeviceSystemTags.MDEV_DEVICE_SPEC.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            String specUuid = tokens.get(MdevDeviceSystemTags.MDEV_DEVICE_SPEC_UUID_TOKEN);
            String devNumber = tokens.get(MdevDeviceSystemTags.MDEV_DEVICE_NUMBER_TOKEN);
            ret.put(specUuid, Integer.valueOf(devNumber));
        }

        // add records in VmInstanceMdevDeviceSpecRefVO
        new SQLBatch() {
            @Override
            protected void scripts() {
                for (Map.Entry<String, Integer> entry : ret.entrySet()) {
                    String specUuid = entry.getKey();
                    int deviceNum = entry.getValue();
                    boolean exists = q(VmInstanceMdevDeviceSpecRefVO.class)
                            .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                            .eq(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, specUuid)
                            .eq(VmInstanceMdevDeviceSpecRefVO_.mdevDeviceNumber, deviceNum)
                            .isExists();
                    if (exists) continue;

                    VmInstanceMdevDeviceSpecRefVO ref = new VmInstanceMdevDeviceSpecRefVO();
                    ref.setVmInstanceUuid(vmUuid);
                    ref.setMdevSpecUuid(entry.getKey());
                    ref.setMdevDeviceNumber(entry.getValue());
                    persist(ref);
                }
                flush();
            }
        }.execute();

        // delete MDEV_DEVICE_SPEC tags
        if (MdevDeviceSystemTags.MDEV_DEVICE_SPEC.hasTag(vmUuid)) {
            MdevDeviceSystemTags.MDEV_DEVICE_SPEC.delete(vmUuid);
        }

        List<Tuple> tuples = Q.New(VmInstanceMdevDeviceSpecRefVO.class)
                .eq(VmInstanceMdevDeviceSpecRefVO_.vmInstanceUuid, vmUuid)
                .groupBy(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid)
                .select(VmInstanceMdevDeviceSpecRefVO_.mdevSpecUuid, VmInstanceMdevDeviceSpecRefVO_.mdevDeviceNumber)
                .listTuple();
        if (!tuples.isEmpty()) {
            for (Tuple tuple : tuples) {
                String specUuid = (String) tuple.get(0);
                int deviceNum = (int) tuple.get(1);

                if (!ret.containsKey(specUuid)) {
                    ret.put(specUuid, deviceNum);
                }
            }
        }

        return ret;
    }

    public static boolean releaseSpecReleatedVirtualMdevDevicesWhenStop(String vmUuid) {
        return PciDeviceSystemTags.AUTO_RELEASE_SPEC_RELEATED_VIRTUAL_PCI_DEVICE.hasTag(vmUuid);
    }

    /**
     * status: Active && state: Enable => return true
     */
    public static boolean checkMdevDeviceAvailable(String mdevUuid) {
        return Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuid)
                .eq(MdevDeviceVO_.state, MdevDeviceState.Enabled)
                .in(MdevDeviceVO_.status, MdevDeviceStatus.attachableMdevDeviceStatus)
                .isExists();
    }

    /**
     * status: Active => Reserve
     */
    public static void reserveMdevDeviceInDB(String mdevUuid, String vmUuid, MdevDeviceChooser chooser) {
        UpdateQuery.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuid)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Reserved)
                .set(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .set(MdevDeviceVO_.chooser, chooser)
                .update();
    }

    /**
     * status: Active => Reserve
     */
    public static void reserveMdevDeviceInDB(Collection<String> mdevUuids, String vmUuid, MdevDeviceChooser chooser) {
        if (mdevUuids == null || mdevUuids.isEmpty()) {
            return;
        }
        UpdateQuery.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, mdevUuids)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Reserved)
                .set(MdevDeviceVO_.vmInstanceUuid, vmUuid)
                .set(MdevDeviceVO_.chooser, chooser)
                .update();
    }

    /**
     * status: Reserve => Active
     */
    public static void unreserveMdevDeviceInDB(String mdevUuid) {
        UpdateQuery.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuid)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Active)
                .set(MdevDeviceVO_.vmInstanceUuid, null)
                .set(MdevDeviceVO_.chooser, MdevDeviceChooser.None)
                .update();
    }

    /**
     * status: Reserve => Active
     */
    public static void unreserveMdevDeviceInDB(Collection<String> mdevUuids) {
        if (mdevUuids == null || mdevUuids.isEmpty()) {
            return;
        }
        UpdateQuery.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, mdevUuids)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Active)
                .set(MdevDeviceVO_.vmInstanceUuid, null)
                .set(MdevDeviceVO_.chooser, MdevDeviceChooser.None)
                .update();
    }

    /**
     * status: Reserve => Attached, chooser no change
     */
    public static void attachMdevDeviceToVmInDB(String mdevUuid) {
        UpdateQuery.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuid)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .update();
    }

    /**
     * status: Reserve => Attached, chooser no change
     */
    public static void attachMdevDeviceToVmInDB(Collection<String> mdevUuids) {
        if (mdevUuids == null || mdevUuids.isEmpty()) {
            return;
        }
        UpdateQuery.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, mdevUuids)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .update();
    }

    /**
     * status: Attached / Reserved => Active
     */
    public static void detachMdevDeviceFromVmInDB(String mdevUuid) {
        UpdateQuery.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuid)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Active)
                .set(MdevDeviceVO_.vmInstanceUuid, null)
                .set(MdevDeviceVO_.chooser, MdevDeviceChooser.None)
                .update();
    }

    /**
     * status: Attached / Reserved => Active
     */
    public static void detachMdevDeviceFromVmInDB(Collection<String> mdevUuids) {
        if (mdevUuids == null || mdevUuids.isEmpty()) {
            return;
        }
        UpdateQuery.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.uuid, mdevUuids)
                .set(MdevDeviceVO_.status, MdevDeviceStatus.Active)
                .set(MdevDeviceVO_.vmInstanceUuid, null)
                .set(MdevDeviceVO_.chooser, MdevDeviceChooser.None)
                .update();
    }
    
    /**
     * @param specUuid nullable
     * @param status nullable
     * @param chooser nullable
     * status: Attached / Reserved => Active
     */
    public static void detachMdevDeviceForVmInDB(String vmUuid, String specUuid, MdevDeviceStatus status, MdevDeviceChooser chooser) {
        UpdateQuery q = UpdateQuery.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, vmUuid);
        if (specUuid != null) q = q.eq(MdevDeviceVO_.mdevSpecUuid, specUuid);
        if (status != null) q = q.eq(MdevDeviceVO_.status, status);
        if (chooser != null) q = q.eq(MdevDeviceVO_.chooser, chooser);
        q.set(MdevDeviceVO_.status, MdevDeviceStatus.Active)
                .set(MdevDeviceVO_.vmInstanceUuid, null)
                .set(MdevDeviceVO_.chooser, MdevDeviceChooser.None)
                .update();
    }
}
