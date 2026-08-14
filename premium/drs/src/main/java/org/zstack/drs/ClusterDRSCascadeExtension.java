package org.zstack.drs;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.Q;
import org.zstack.drs.entity.ClusterDRSVO;
import org.zstack.drs.entity.ClusterDRSVO_;
import org.zstack.drs.message.DeleteClusterDRSMsg;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ClusterDRSCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final String NAME = ClusterDRSVO.class.getSimpleName();

    @Autowired
    private CloudBus bus;

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(ClusterVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE, CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<ClusterDRSVO> ctx = ClusterDRSVOFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }

    private List<ClusterDRSVO> ClusterDRSVOFromAction(CascadeAction action) {
        if (ClusterVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<ClusterInventory> clusters = action.getParentIssuerContext();
            if (!clusters.isEmpty()) {
                List<String> clusterUuids = clusters.stream()
                        .map(ClusterInventory::getUuid)
                        .collect(Collectors.toList());
                return Q.New(ClusterDRSVO.class)
                        .in(ClusterDRSVO_.clusterUuid, clusterUuids)
                        .list();
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<ClusterDRSVO> vos = ClusterDRSVOFromAction(action);
        if (vos == null || vos.isEmpty()) {
            completion.success();
            return;
        }

        final List<DeleteClusterDRSMsg> msgs = vos.stream()
                .map(vo -> {
                    DeleteClusterDRSMsg msg = new DeleteClusterDRSMsg();
                    msg.setUuid(vo.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, DRSConstants.SERVICE_ID, vo.getUuid());
                    return msg;
                }).collect(Collectors.toList());


        new While<>(msgs).step((msg, compl) -> bus.send(msg, new CloudBusCallBack(compl) {
            @Override
            public void run(MessageReply reply) {
                compl.done();
            }
        }), 3).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }
}
