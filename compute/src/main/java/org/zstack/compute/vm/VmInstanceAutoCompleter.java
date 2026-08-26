package org.zstack.compute.vm;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.findOneOrNull;
import static org.zstack.utils.CollectionUtils.isEmpty;

public class VmInstanceAutoCompleter implements GlobalApiMessageInterceptor {
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APICreateVmInstanceMsg) {
            validate((APICreateVmInstanceMsg) msg);
        }
        return msg;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return list(
            APICreateVmInstanceMsg.class
        );
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.FRONT;
    }

    @Override
    public int getPriority() {
        return -1;
    }

    private void validate(APICreateVmInstanceMsg msg) throws ApiMessageInterceptionException {
        if (!isEmpty(msg.getDiskAOs())) {
            long bootCount = msg.getDiskAOs().stream().filter(DiskAO::isBoot).count();
            if (bootCount == 0) {
                throw new ApiMessageInterceptionException(argerr("missing root disk"));
            }
            if (bootCount > 1) {
                throw new ApiMessageInterceptionException(argerr("multiple root disks are not allowed"));
            }
        } else {
            msg.setDiskAOs(new ArrayList<>());
            msg.getDiskAOs().add(DiskAO.rootDisk());
        }

        if (!CollectionUtils.isEmpty(msg.getDataDiskSizes())) {
            throw new ApiMessageInterceptionException(argerr("APICreateVmInstanceMsg.dataDiskSizes is not supported. use diskAOs instead"));
        }

        DiskAO rootDisk = findOneOrNull(msg.getDiskAOs(), DiskAO::isBoot);
        if (rootDisk == null) {
            throw new ApiMessageInterceptionException(argerr("missing root disk"));
        }

        reconcileDuplicatedString(msg.getPlatform(), rootDisk.getPlatform(), msg::setPlatform, rootDisk::setPlatform, "platform");
        reconcileDuplicatedString(msg.getGuestOsType(), rootDisk.getGuestOsType(), msg::setGuestOsType, rootDisk::setGuestOsType, "guestOsType");
        reconcileDuplicatedString(msg.getArchitecture(), rootDisk.getArchitecture(), msg::setArchitecture, rootDisk::setArchitecture, "architecture");
        reconcileDuplicatedString(msg.getPrimaryStorageUuidForRootVolume(), rootDisk.getPrimaryStorageUuid(),
                msg::setPrimaryStorageUuidForRootVolume, rootDisk::setPrimaryStorageUuid, "primaryStorageUuidForRootVolume");
        reconcileDuplicatedString(msg.getImageUuid(), rootDisk.getTemplateUuid(),
                msg::setImageUuid, rootDisk::setTemplateUuid, "imageUuid");
        reconcileDuplicatedString(msg.getRootDiskOfferingUuid(), rootDisk.getDiskOfferingUuid(),
                msg::setRootDiskOfferingUuid, rootDisk::setDiskOfferingUuid, "rootDiskOfferingUuid");
        reconcileRootDiskSize(msg, rootDisk);

        if (msg.getDataDiskOfferingUuids() != null) {
            int plannedNewDataDiskCount = msg.getDataDiskOfferingUuids().size();
            assertTotalDiskCountWithinLimit(msg.getDiskAOs(), plannedNewDataDiskCount);
            appendDeprecatedDataDisks(msg, plannedNewDataDiskCount);
            msg.setDataDiskOfferingUuids(null);
        }

        moveRootDiskToHead(msg, rootDisk);
    }

    /**
     * Total {@link DiskAO} entries must not exceed {@link VolumeConstant#DEFAULT_MAX_DATA_VOLUME_NUMBER}
     * (1 root volume + up to {@code DEFAULT_MAX_DATA_VOLUME_NUMBER - 1} data volumes).
     */
    private static void assertTotalDiskCountWithinLimit(List<DiskAO> diskAOs, int additionalDataDisks) throws ApiMessageInterceptionException {
        if (isEmpty(diskAOs)) {
            return;
        }
        int maxTotal = VolumeConstant.DEFAULT_MAX_DATA_VOLUME_NUMBER;
        if (diskAOs.size() + additionalDataDisks > maxTotal) {
            throw new ApiMessageInterceptionException(argerr(
                    "a VM can have at most %s disks in total (1 root volume and up to %s data volumes); current diskAOs: %s, additional data disks requested: %s",
                    maxTotal, maxTotal - 1, diskAOs.size(), additionalDataDisks));
        }
    }

    private static void reconcileDuplicatedString(String msgVal, String diskVal,
                Consumer<String> setMsg, Consumer<String> setDisk, String fieldLabel) throws ApiMessageInterceptionException {
        boolean m = StringUtils.isNotBlank(msgVal);
        boolean d = StringUtils.isNotBlank(diskVal);
        if (m && d && !StringUtils.equals(msgVal, diskVal)) {
            throw new ApiMessageInterceptionException(argerr("inconsistent %s between vm message and root disk", fieldLabel));
        }
        if (m && !d) {
            setDisk.accept(msgVal);
        } else if (!m && d) {
            setMsg.accept(diskVal);
        }
    }

    private static void reconcileRootDiskSize(APICreateVmInstanceMsg msg, DiskAO rootDisk) throws ApiMessageInterceptionException {
        Long m = msg.getRootDiskSize();
        long d = rootDisk.getSize();
        boolean mSet = m != null;
        boolean dSet = d != 0L;
        if (mSet && dSet && !m.equals(d)) {
            throw new ApiMessageInterceptionException(argerr("inconsistent rootDiskSize between vm message and root disk"));
        }
        if (mSet && !dSet) {
            rootDisk.setSize(m);
        } else if (!mSet && dSet) {
            msg.setRootDiskSize(d);
        }
    }

    private static void appendDeprecatedDataDisks(APICreateVmInstanceMsg msg, int newDataDiskCount) {
        List<String> offeringUuids = msg.getDataDiskOfferingUuids();

        if (newDataDiskCount <= 0) {
            return;
        }

        for (int i = 0; i < newDataDiskCount; i++) {
            DiskAO d = DiskAO.nonRootDisk();
            if (offeringUuids != null && i < offeringUuids.size() && StringUtils.isNotBlank(offeringUuids.get(i))) {
                d.setDiskOfferingUuid(offeringUuids.get(i));
            }
            msg.getDiskAOs().add(d);
        }
    }

    private static void moveRootDiskToHead(APICreateVmInstanceMsg msg, DiskAO rootDisk) {
        if (rootDisk == null || isEmpty(msg.getDiskAOs())) {
            return;
        }
        msg.getDiskAOs().remove(rootDisk);
        msg.getDiskAOs().add(0, rootDisk);
    }
}
