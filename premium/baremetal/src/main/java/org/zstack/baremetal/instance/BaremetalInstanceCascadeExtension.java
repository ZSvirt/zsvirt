package org.zstack.baremetal.instance;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.baremetal.chassis.BaremetalChassisInventory;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.instance.*;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerVO;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountInventory;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.IpRangeVO;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.zone.ZoneInventory;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by GuoYi on 7/6/18.
 */
public class BaremetalInstanceCascadeExtension extends AbstractCascadeExtension {
    private static final CLogger logger = Utils.getLogger(BaremetalInstanceCascadeExtension.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    protected EventFacade evf;

    private static final String NAME = BaremetalInstanceVO.class.getSimpleName();

    private static final int OP_NOPE = 0;
    private static final int OP_DELETION = 1;
    private static final int OP_NOPXESERVER = 2;
    private static final int OP_DETACH_NIC = 3;

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
        int op = toDeletionOpCode(action);
        if (op == OP_NOPE) {
            completion.success();
            return;
        }

        final List<BaremetalInstanceDeletionStruct> bms = bmFromDeleteAction(action);
        if (bms == null || bms.isEmpty()) {
            completion.success();
            return;
        }

        if (op == OP_DELETION) {
            new While<>(bms).all((struct, noErrorCompletion) -> {
                DestroyBaremetalInstanceMsg msg = new DestroyBaremetalInstanceMsg();
                msg.setUuid(struct.getInventory().getUuid());
                msg.setDeletionPolicy(struct.getDeletionPolicy().toString());
                bus.makeTargetServiceIdByResourceUuid(msg, BaremetalInstanceConstant.SERVICE_ID, struct.getInventory().getUuid());
                bus.send(msg, new CloudBusCallBack(noErrorCompletion) {
                    @Override
                    public void run(MessageReply reply) {
                        if (!action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
                            if (!reply.isSuccess()) {
                                logger.warn(reply.getError().toString());
                            }
                        }
                        noErrorCompletion.done();
                    }
                });
            }).run(new WhileDoneCompletion(completion) {
                @Override
                public void done(ErrorCodeList errorCodeList) {
                    completion.success();
                }
            });
        } else if (op == OP_NOPXESERVER) {
            // all handled in BaremetalPxeServerDetachExtensionPoint
            completion.success();
        } else if (op == OP_DETACH_NIC) {
            // TODO: handle nic detaching in bm instance agent
            completion.success();
        }
    }

    private void handleDeletionCleanup(CascadeAction action, Completion completion) {
        completion.success();
    }

    private int toDeletionOpCode(CascadeAction action) {
        if (!CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            return OP_NOPE;
        }

        if (BaremetalChassisVO.class.getSimpleName().equals(action.getParentIssuer())) {
            if (BaremetalPxeServerVO.class.getSimpleName().equals(action.getRootIssuer())) {
                return OP_NOPXESERVER;
            }
            return OP_DELETION;
        }

        if (BaremetalInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
        }

        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH_NIC;
        }

        if (IpRangeVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH_NIC;
        }

        return OP_NOPE;
    }

    private List<BaremetalInstanceDeletionStruct> toBaremetalInstanceDeletionStruct(Collection<BaremetalInstanceVO> vos) {
        List<BaremetalInstanceDeletionStruct> structs = new ArrayList<>();
        for (BaremetalInstanceVO vo : vos) {
            BaremetalInstanceDeletionStruct s = new BaremetalInstanceDeletionStruct();
            s.setInventory(BaremetalInstanceInventory.valueOf(vo));
            // when bm chassis is deleted, then bm instance will be deleted directly.
            s.setDeletionPolicy(BaremetalInstanceDeletionPolicyManager.BaremetalInstanceDeletionPolicy.Direct);
            structs.add(s);
        }
        return structs;
    }

    private List<BaremetalInstanceDeletionStruct> bmFromDeleteAction(CascadeAction action) {
        List<BaremetalInstanceDeletionStruct> ret = null;
        if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        } else if (BaremetalChassisVO.class.getSimpleName().equals(action.getParentIssuer())) {
            Map<String, BaremetalInstanceVO> bmvos = new HashMap<>();

            List<BaremetalChassisInventory> chassis = action.getParentIssuerContext();
            List<String> chassisUuids = transformAndRemoveNull(chassis, BaremetalChassisInventory::getUuid);

            List<BaremetalInstanceVO> lst;
            if (chassisUuids != null && !chassisUuids.isEmpty()) {
                lst = Q.New(BaremetalInstanceVO.class)
                        .in(BaremetalInstanceVO_.chassisUuid, chassisUuids)
                        .list();
                for (BaremetalInstanceVO vo : lst) {
                    bmvos.put(vo.getUuid(), vo);
                }
            }

            if (ClusterVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ClusterInventory> clusters = action.getRootIssuerContext();
                List<String> clusterUuids = transformAndRemoveNull(clusters, ClusterInventory::getUuid);
                if (!clusterUuids.isEmpty()) {
                    lst = Q.New(BaremetalInstanceVO.class).in(BaremetalInstanceVO_.clusterUuid, clusterUuids).list();
                    for (BaremetalInstanceVO vo : lst) {
                        bmvos.put(vo.getUuid(), vo);
                    }
                }
            } else if (ZoneVO.class.getSimpleName().equals(action.getRootIssuer())) {
                List<ZoneInventory> zones = action.getRootIssuerContext();
                List<String> zoneUuids = transformAndRemoveNull(zones, ZoneInventory::getUuid);
                if (!zoneUuids.isEmpty()) {
                    lst = Q.New(BaremetalInstanceVO.class).in(BaremetalInstanceVO_.zoneUuid, zoneUuids).list();
                    for (BaremetalInstanceVO vo : lst) {
                        bmvos.put(vo.getUuid(), vo);
                    }
                }
            }

            if (!bmvos.isEmpty()) {
                ret = toBaremetalInstanceDeletionStruct(bmvos.values());
            }
        } else if (AccountVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<AccountInventory> accounts = action.getParentIssuerContext();
            List<String> auuids = CollectionUtils.transform(accounts, AccountInventory::getUuid);
            List<BaremetalInstanceVO> bmvos = Q.New(BaremetalInstanceVO.class, AccountResourceRefVO.class)
                    .table0()
                        .selectThisTable()
                        .eq(BaremetalInstanceVO_.uuid).table1(AccountResourceRefVO_.resourceUuid)
                    .table1()
                        .eq(AccountResourceRefVO_.resourceType, BaremetalInstanceVO.class.getSimpleName())
                        .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                        .in(AccountResourceRefVO_.accountUuid, auuids)
                    .list();

            if (!bmvos.isEmpty()) {
                ret = toBaremetalInstanceDeletionStruct(bmvos);
            }
        }
        return ret;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(
                BaremetalChassisVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            int op = toDeletionOpCode(action);
            if (op == OP_NOPE || op == OP_NOPXESERVER || op == OP_DETACH_NIC) {
                return null;
            }

            List<BaremetalInstanceDeletionStruct> bms = bmFromDeleteAction(action);
            if (bms == null) {
                return null;
            }

            return action.copy().setParentIssuer(NAME).setParentIssuerContext(bms);
        }

        return null;
    }
}
