package org.zstack.compute.vm;

import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.cdrom.VmCdRomVO;
import org.zstack.header.vm.cdrom.VmCdRomVO_;
import org.zstack.tag.SystemTagCreator;
import java.util.List;
import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by xing5 on 2016/5/26.
 */
public class IsoOperator {
    public static List<String> getIsoUuidByVmUuid(String vmUuid) {
       List<String> isoUuids = Q.New(VmCdRomVO.class)
               .select(VmCdRomVO_.isoUuid)
               .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
               .notNull(VmCdRomVO_.isoUuid)
               .listValues();
       return isoUuids;
    }

    public static List<String> getVmUuidByIsoUuid(String isoUuid) {
        List<String> result = Q.New(VmCdRomVO.class)
                .select(VmCdRomVO_.vmInstanceUuid)
                .eq(VmCdRomVO_.isoUuid, isoUuid)
                .listValues();
        return result;
    }

    static void checkAttachIsoToVm(String vmUuid, String isoUuid) {
        List<String> isoList = getIsoUuidByVmUuid(vmUuid);

        if (isoList.contains(isoUuid)) {
            throw new OperationFailureException(operr("VM[uuid:%s] has attached ISO[uuid:%s]", vmUuid, isoUuid));
        }

        long emptyCdRomNum = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .isNull(VmCdRomVO_.isoUuid)
                .count();
        if (emptyCdRomNum == 0) {
            throw new OperationFailureException(operr("All vm[uuid:%s] CD-ROMs have mounted ISO", vmUuid));
        }
    }

    public static Integer getIsoDeviceId(String vmUuid, String isoUuid) {
        VmCdRomVO vmCdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .eq(VmCdRomVO_.isoUuid, isoUuid)
                .find();

        if (vmCdRomVO == null) {
            return null;
        }

        return vmCdRomVO.getDeviceId();
    }

    public static boolean isIsoAttachedToVm(String vmUuid) {
        boolean exsit = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .notNull(VmCdRomVO_.isoUuid)
                .isExists();

        return exsit;
    }

    static VmCdRomVO getEmptyCdRom(String vmUuid) {
        VmCdRomVO cdRomVO = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .isNull(VmCdRomVO_.occupant)
                .orderBy(VmCdRomVO_.deviceId, SimpleQuery.Od.ASC)
                .limit(1)
                .find();

        return cdRomVO;
    }

    @AsyncThread
    @Deprecated
    public void syncVmIsoSystemTag(String vmUuid) {
        if (!VmCdRomGlobalProperty.syncVmIsoSystemTag) {
            return;
        }

        VmSystemTags.ISO.delete(vmUuid);

        List<VmCdRomVO> cdRomVOS = Q.New(VmCdRomVO.class)
                .eq(VmCdRomVO_.vmInstanceUuid, vmUuid)
                .notNull(VmCdRomVO_.isoUuid)
                .list();
        if (cdRomVOS.isEmpty()) {
            return;
        }

        for (VmCdRomVO cdRomVO : cdRomVOS){
            SystemTagCreator creator = VmSystemTags.ISO.newSystemTagCreator(vmUuid);
            creator.setTagByTokens(map(
                    e(VmSystemTags.ISO_TOKEN, cdRomVO.getIsoUuid()),
                    e(VmSystemTags.ISO_DEVICEID_TOKEN, cdRomVO.getDeviceId())
            ));
            creator.create();
        }
    }
}
