package org.zstack.network.securitygroup;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.identity.ResourceHelper;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class SecurityGroupCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(SecurityGroupCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static String NAME = VmNicSecurityGroupRefVO.class.getSimpleName();

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

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        dbf.eoCleanup(SecurityGroupVO.class);
        completion.success();
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    private void handleSecurityGroupRefDeletion(CascadeAction action, final Completion completion) {
        List<VmNicSecurityGroupRefInventory> refs = refFromAction(action);
        if (refs.isEmpty()) {
            completion.success();
            return;
        }

        Map<String, List<String>> map = new HashMap<String, List<String>>();
        for (VmNicSecurityGroupRefInventory ref : refs) {
            List<String> nicUuids = map.get(ref.getSecurityGroupUuid());
            if (nicUuids == null) {
                nicUuids = new ArrayList<String>();
                map.put(ref.getSecurityGroupUuid(), nicUuids);
            }
            nicUuids.add(ref.getVmNicUuid());
        }

        List<RemoveVmNicFromSecurityGroupMsg> msgs = new ArrayList<RemoveVmNicFromSecurityGroupMsg>();
        for (Map.Entry<String, List<String>> e : map.entrySet()) {
            RemoveVmNicFromSecurityGroupMsg msg = new RemoveVmNicFromSecurityGroupMsg();
            msg.setSecurityGroupUuid(e.getKey());
            msg.setVmNicUuids(e.getValue());
            bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, e.getKey());
            msgs.add(msg);
        }

        bus.send(msgs, new CloudBusListCallBack(completion) {
            @Override
            public void run(List<MessageReply> replies) {
                for (MessageReply reply : replies) {
                    if (!reply.isSuccess()) {
                        logger.warn(String.format("failed to remove vm nic from some security group for some destroyed vm," +
                                "no worry, security group will catch it up later. %s", reply.getError()));
                    }
                }

                completion.success();
            }
        });
    }

    private void handleDeletion(CascadeAction action, final Completion completion) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            handleSecurityGroupRefDeletion(action, completion);
        } else {
            handleSecurityGroupDeletion(action, completion);
        }

    }

    private void handleSecurityGroupDeletion(CascadeAction action, final Completion completion) {
        List<AccountInventory> accounts = action.getParentIssuerContext();
        List<String> accountUuids = accounts.stream().map(AccountInventory::getUuid).collect(Collectors.toList());

        List<SecurityGroupDeletionMsg> msgs = new SQLBatchWithReturn<List<SecurityGroupDeletionMsg>>() {
            @Override
            protected List<SecurityGroupDeletionMsg> scripts() {
                List<String> uuids = ResourceHelper.findOwnResourceUuidList(SecurityGroupVO.class, accountUuids);
                if (uuids.isEmpty()) {
                    return null;
                }

                return uuids.stream().map(auuid -> {
                    SecurityGroupDeletionMsg msg = new SecurityGroupDeletionMsg();
                    msg.setUuid(auuid);
                    bus.makeTargetServiceIdByResourceUuid(msg, SecurityGroupConstant.SERVICE_ID, auuid);
                    return msg;
                }).collect(Collectors.toList());
            }
        }.execute();


        if (msgs == null) {
            completion.success();
            return;
        }

        new While<>(msgs).all((msg, com) -> bus.send(msg, new CloudBusCallBack(com) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    logger.warn(String.format("failed to delete scheduler[uuid:%s], %s", msg.getUuid(), reply.getError()));
                }

                com.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.success();
            }
        });
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<VmNicSecurityGroupRefInventory> refFromAction(CascadeAction action) {
        List<VmDeletionStruct> vms = action.getParentIssuerContext();
        List<String> nicUuids = new ArrayList<>();
        for (VmDeletionStruct vm : vms) {
            nicUuids.addAll(transformAndRemoveNull(vm.getInventory().getVmNics(), VmNicInventory::getUuid));
        }


        if (nicUuids.isEmpty()) {
            return new ArrayList<>();
        }

        SimpleQuery<VmNicSecurityGroupRefVO> q = dbf.createQuery(VmNicSecurityGroupRefVO.class);
        q.add(VmNicSecurityGroupRefVO_.vmNicUuid, SimpleQuery.Op.IN, nicUuids);
        List<VmNicSecurityGroupRefVO> vos = q.list();
        return VmNicSecurityGroupRefInventory.valueOf(vos);
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CLEANUP_CODE.equals(action.getActionCode())) {
            return null;
        }

        if (!action.getParentIssuer().equals(VmInstanceVO.class.getSimpleName())) {
            return null;
        }

        List<VmNicSecurityGroupRefInventory> refs = refFromAction(action);
        if (refs.isEmpty()) {
            return null;
        }

        return action.copy().setParentIssuer(NAME).setParentIssuerContext(refs);
    }
}
