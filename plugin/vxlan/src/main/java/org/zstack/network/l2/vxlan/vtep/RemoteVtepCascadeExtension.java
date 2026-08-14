package org.zstack.network.l2.vxlan.vtep;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.network.l2.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class RemoteVtepCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(RemoteVtepCascadeExtension.class);

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE)) {
            handleDeletionCleanup(action, completion);
        } else if (action.isActionCode(L2NetworkConstant.DETACH_L2NETWORK_CODE)) {
            handleDetach(action, completion);
        } else {
            completion.success();
        }
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private static final String NAME = RemoteVtepVO.class.getSimpleName();

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(L2NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private void handleDetach(CascadeAction action, final Completion completion) {
        List<L2NetworkDetachStruct> structs = action.getParentIssuerContext();
        List<RemoteVtepVO> vteps = new ArrayList<>();
        for (L2NetworkDetachStruct s : structs) {
            vteps.addAll(Q.New(RemoteVtepVO.class).eq(RemoteVtepVO_.poolUuid, s.getL2NetworkUuid()).eq(RemoteVtepVO_.clusterUuid, s.getClusterUuid()).list());
        }

        if (vteps.isEmpty()) {
            completion.success();
            return;
        }
        for (RemoteVtepVO vtep : vteps) {
            RemoteVtepVO vo = dbf.findByUuid(vtep.getUuid(), RemoteVtepVO.class);
            dbf.remove(vo);
        }
        completion.success();
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(RemoteVtepVO.class);
        completion.success();
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        List<RemoteVtepInventory> vteps = vtepFromAction(action);

        if (vteps == null) {
            completion.success();
            return;
        }

        for (RemoteVtepInventory vtep : vteps) {
            RemoteVtepVO vo = dbf.findByUuid(vtep.getUuid(), RemoteVtepVO.class);
            dbf.remove(vo);
        }
        completion.success();
    }

    private List<RemoteVtepInventory> vtepFromAction(CascadeAction action) {
        List<RemoteVtepInventory> ret = null;
        if (L2NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> l2uuids = transformAndRemoveNull(action.getParentIssuerContext(), L2NetworkInventory::getUuid);

            List<RemoteVtepVO> vos = Q.New(RemoteVtepVO.class).in(RemoteVtepVO_.poolUuid, l2uuids).list();
            if (!vos.isEmpty()) {
                ret = RemoteVtepInventory.valueOf(vos);
            }
        }  else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<RemoteVtepInventory> ctx = vtepFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
