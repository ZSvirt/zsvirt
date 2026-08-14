package org.zstack.storage.device;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storageDevice.*;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.*;
import org.zstack.storage.device.fibreChannel.FiberChannelStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by weiwang on 21/06/2017.
 */
public class ScsiLunHostRefCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(ScsiLunHostRefCascadeExtension.class);

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

    private static final String NAME = ScsiLunVO.class.getSimpleName();

    protected static final int OP_NOPE = 0;
    protected static final int OP_DETACH = 1;
    protected static final int OP_DELETION = 2;

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(HostVO.class.getSimpleName());
    }

    protected int toDeleteOpCode(CascadeAction action) {
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH;
        }

        return OP_NOPE;
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ScsiLunHostRefVO> ctx = ScsiLunHostRefFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(PciDeviceVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        int op = toDeleteOpCode(action);

        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<ScsiLunHostRefVO> vos = ScsiLunHostRefFromAction(action);
        if (vos == null || vos.isEmpty()) {
            completion.success();
            return;
        }

        if (op == OP_DETACH) {
            dbf.removeCollection(vos, ScsiLunHostRefVO.class);
            for (ScsiLunHostRefVO refVO : vos) {
                ScsiLunVO scsiLunVO = Q.New(ScsiLunVO.class).eq(ScsiLunVO_.uuid, refVO.getScsiLunUuid()).find();
                if (scsiLunVO == null) {
                    continue;
                }
                if (scsiLunVO.getScsiLunHostRefs() != null && !scsiLunVO.getScsiLunHostRefs().isEmpty()) {
                    continue;
                }

                if (scsiLunVO.getScsiLunVmInstanceRefs() != null && !scsiLunVO.getScsiLunVmInstanceRefs().isEmpty()) {
                    dbf.removeCollection(scsiLunVO.getScsiLunVmInstanceRefs(), ScsiLunVmInstanceRefVO.class);
                }
                dbf.removeByPrimaryKey(refVO.getScsiLunUuid(), ScsiLunVO.class);
            }
            processFiberChannel();
            completion.success();
        }
    }

    //TODO(weiw): refactor this
    private void processFiberChannel() {
        List<FiberChannelStorageVO> storageVOS = Q.New(FiberChannelStorageVO.class).list();
        if (storageVOS != null && !storageVOS.isEmpty()) {
            for (FiberChannelStorageVO vo : storageVOS) {
                if (vo.getFiberChannelLuns() == null || vo.getFiberChannelLuns().isEmpty()) {
                    dbf.remove(vo);
                    logger.debug(String.format("cleared staled fiber channel storage %s", vo.getUuid()));
                }
            }
        }
    }

    private List<ScsiLunHostRefVO> ScsiLunHostRefFromAction(CascadeAction action) {
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<ScsiLunHostRefVO> scsiLunHostRefVOS = new ArrayList<>();
            for (HostInventory host : hosts) {
                scsiLunHostRefVOS.addAll(Q.New(ScsiLunHostRefVO.class).eq(ScsiLunHostRefVO_.hostUuid, host.getUuid()).list());
            }

            if (!scsiLunHostRefVOS.isEmpty()) {
                return scsiLunHostRefVOS;
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
