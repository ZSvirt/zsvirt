package org.zstack.network.l3;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 */
public class IpRangeCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(IpRangeCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;

    private static final String NAME = IpRangeVO.class.getSimpleName();

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
        dbf.eoCleanup(IpRangeVO.class);
        completion.success();
    }

    private void deleteIpRanges(final CascadeAction action,List<IpRangeInventory> iprs, NoErrorCompletion completion) {
        List<IpRangeDeletionMsg> msgs = new ArrayList<IpRangeDeletionMsg>();
        for (IpRangeInventory iprinv : iprs) {
            IpRangeDeletionMsg msg = new IpRangeDeletionMsg();
            msg.setForceDelete(action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE));
            msg.setL3NetworkUuid(iprinv.getL3NetworkUuid());
            msg.setIpRangeUuid(iprinv.getUuid());
            bus.makeTargetServiceIdByResourceUuid(msg, L3NetworkConstant.SERVICE_ID, iprinv.getL3NetworkUuid());
            msgs.add(msg);
        }

        new While<>(msgs).all((msg, compl) -> {
            bus.send(msg, new CloudBusCallBack(compl) {
                @Override
                public void run(MessageReply reply) {
                    compl.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                completion.done();
            }
        });
    }

    private void handleDeletion(final CascadeAction action, final Completion completion) {
        final List<IpRangeInventory> iprinvs = ipRangeFromAction(action);
        if (iprinvs == null) {
            completion.success();
            return;
        }

        List<IpRangeInventory> addressPools = new ArrayList<>();
        List<IpRangeInventory> normalIpRanges = new ArrayList<>();
        for (IpRangeInventory inv : iprinvs) {
            if (Q.New(AddressPoolVO.class).eq(AddressPoolVO_.uuid, inv.getUuid()).isExists()) {
                addressPools.add(inv);
            } else {
                normalIpRanges.add(inv);
            }
        }

        /* delete address pool first */
        if (!addressPools.isEmpty()) {
            deleteIpRanges(action, addressPools, new NoErrorCompletion(completion) {
                @Override
                public void done() {
                    if (normalIpRanges.isEmpty()) {
                        completion.success();
                    } else {
                        deleteIpRanges(action, normalIpRanges, new NoErrorCompletion() {
                            @Override
                            public void done() {
                                completion.success();
                            }
                        });
                    }
                }
            });
        } else {
            if (normalIpRanges.isEmpty()) {
                completion.success();
            } else {
                deleteIpRanges(action, normalIpRanges, new NoErrorCompletion() {
                    @Override
                    public void done() {
                        completion.success();
                    }
                });
            }
        }
    }

    private void handleDeletionCheck(CascadeAction action, Completion completion) {
        completion.success();
    }

    @Override
    public List<String> getEdgeNames() {
        return Collections.singletonList(L3NetworkVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return NAME;
    }

    private List<IpRangeInventory> ipRangeFromAction(CascadeAction action) {
        List<IpRangeInventory> ret = null;
        if (L3NetworkVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<String> l3uuids = transformAndRemoveNull(action.getParentIssuerContext(), L3NetworkInventory::getUuid);

            SimpleQuery<IpRangeVO> q = dbf.createQuery(IpRangeVO.class);
            q.add(IpRangeVO_.l3NetworkUuid, SimpleQuery.Op.IN, l3uuids);
            List<IpRangeVO> iprvos = q.list();
            if (!iprvos.isEmpty()) {
                ret = IpRangeInventory.valueOf(iprvos);
            }
        } else if (NAME.equals(action.getParentIssuer())) {
            ret = action.getParentIssuerContext();
        }

        return ret;
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        if (CascadeConstant.DELETION_CODES.contains(action.getActionCode())) {
            List<IpRangeInventory> ctx = ipRangeFromAction(action);
            if (ctx != null) {
                return action.copy().setParentIssuer(NAME).setParentIssuerContext(ctx);
            }
        }

        return null;
    }
}
