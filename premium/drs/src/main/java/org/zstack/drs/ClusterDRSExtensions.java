package org.zstack.drs;

import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.drs.entity.DRSAdviceVO;
import org.zstack.drs.entity.DRSAdviceVO_;
import org.zstack.drs.entity.DRSVmMigrationActivityVO;
import org.zstack.drs.entity.DRSVmMigrationActivityVO_;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceDestroyExtensionPoint;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/09/26
 */
public class ClusterDRSExtensions implements VmInstanceDestroyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(ClusterDRSExtensions.class);

    @Override
    public String preDestroyVm(VmInstanceInventory inv) {
        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {
        // do-nothing
    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {
        final List<String> adviceUuidList = Q.New(DRSAdviceVO.class)
                .select(DRSAdviceVO_.uuid)
                .eq(DRSAdviceVO_.vmUuid, inv.getUuid())
                .listValues();

        // When a VM is destroyed, related DRS migration activities will fail.
        // This method cleans up any DRS advice associated with the destroyed VM.
        if (adviceUuidList.isEmpty()) {
            return;
        }

        logger.debug(String.format("Cleaning up DRS advice[uuid=%s] for VM[uuid=%s]", adviceUuidList, inv.getUuid()));
        SQL.New(DRSVmMigrationActivityVO.class)
                .in(DRSVmMigrationActivityVO_.adviceUuid, adviceUuidList)
                .set(DRSVmMigrationActivityVO_.adviceUuid, null)
                .update();

        SQL.New(DRSAdviceVO.class)
                .in(DRSAdviceVO_.uuid, adviceUuidList)
                .delete();
    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {
        // do-nothing
    }
}
