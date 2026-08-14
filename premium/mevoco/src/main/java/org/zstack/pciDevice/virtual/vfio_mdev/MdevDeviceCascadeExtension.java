package org.zstack.pciDevice.virtual.vfio_mdev;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.AccountManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019/4/29.
 */
public class MdevDeviceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(MdevDeviceCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    protected EventFacade evtf;

    private static final String NAME = MdevDeviceVO.class.getSimpleName();

    private static final int OP_NOPE = 0;
    private static final int OP_DETACH = 1;
    private static final int OP_DELETION = 2;

    private int toDeleteOpCode(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        return OP_NOPE;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CHECK_CODE)) {
            handleDeletionCheck(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        int op = toDeleteOpCode(action);

        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<MdevDeviceInventory> invs = MdevDeviceFromAction(action);
        if (invs == null) {
            completion.success();
            return;
        }

        if (op == OP_DELETION) {
            List<String> hostUuids = invs.stream().map(MdevDeviceInventory::getHostUuid).distinct().collect(Collectors.toList());
            List<String> mdevDeviceUuids = invs.stream().map(MdevDeviceInventory::getUuid).collect(Collectors.toList());

            if (mdevDeviceUuids.isEmpty()) {
                completion.success();
                return;
            }

            // delete all mdev devices in the host, even they are attached
            logger.debug(String.format("remove mdev devices[uuid:%s] because hosts[uuid:%s] are deleted", mdevDeviceUuids, hostUuids));
            SQL.New(MdevDeviceVO.class).in(MdevDeviceVO_.uuid, mdevDeviceUuids).hardDelete();

            completion.success();
            return;
        }

        if (op == OP_DETACH) {
            List<String> mdevUuids = invs.stream().map(MdevDeviceInventory::getUuid).collect(Collectors.toList());
            MdevDeviceUtils.detachMdevDeviceFromVmInDB(mdevUuids);

            logger.debug(String.format("auto detached mdev devices[uuid:%s] because vm instances are expunged", mdevUuids));
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(MdevDeviceVO.class);
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName(), HostVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<MdevDeviceInventory> ctx = MdevDeviceFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }

    private List<MdevDeviceInventory> MdevDeviceFromAction(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> structs = action.getParentIssuerContext();
            List<MdevDeviceVO> mdevs = new ArrayList<>();
            for (VmDeletionStruct struct : structs) {
                VmInstanceInventory inv = struct.getInventory();
                mdevs.addAll(Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.vmInstanceUuid, inv.getUuid()).list());
            }

            if (!mdevs.isEmpty()) {
                return MdevDeviceInventory.valueOf(mdevs);
            }
        } else if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<MdevDeviceVO> mdevs = new ArrayList<>();
            for (HostInventory host : hosts) {
                mdevs.addAll(Q.New(MdevDeviceVO.class).eq(MdevDeviceVO_.hostUuid, host.getUuid()).list());
            }

            if (!mdevs.isEmpty()) {
                return MdevDeviceInventory.valueOf(mdevs);
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
