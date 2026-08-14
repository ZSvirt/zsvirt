package org.zstack.pciDevice;

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
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowErrorHandler;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.identity.AccountManager;
import org.zstack.pciDevice.virtual.PciDeviceVirtStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by weiwang on 21/06/2017.
 */
public class PciDeviceCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(PciDeviceCascadeExtension.class);

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
    @Autowired
    protected PciDeviceManager pciMgr;

    private static final String NAME = PciDeviceVO.class.getSimpleName();

    protected static final int OP_NOPE = 0;
    protected static final int OP_DETACH = 1;
    protected static final int OP_DELETION = 2;

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName(), HostVO.class.getSimpleName());
    }

    protected int toDeleteOpCode(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DETACH;
        }

        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            return OP_DELETION;
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
            List<PciDeviceInventory> ctx = PciDeviceFromAction(action);
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

        final List<PciDeviceInventory> invs = PciDeviceFromAction(action);
        if (invs == null) {
            completion.success();
            return;
        }

        if (op == OP_DELETION) {
            List<String> hostUuids = invs.stream().map(PciDeviceInventory::getHostUuid).distinct().collect(Collectors.toList());
            List<String> pciDeviceUuids = invs.stream().map(PciDeviceInventory::getUuid).collect(Collectors.toList());

            if (pciDeviceUuids.isEmpty()) {
                completion.success();
                return;
            }

            // delete all pci devices in the host, even they are attached
            logger.debug(String.format("remove pci devices[uuid:%s] because hosts[uuid:%s] are deleted", pciDeviceUuids, hostUuids));
            SQL.New(PciDeviceVO.class).in(PciDeviceVO_.uuid, pciDeviceUuids).hardDelete();

            // public event
            for (PciDeviceInventory inv : invs) {
                if (inv.getVmInstanceUuid() == null) {
                    continue;
                }

                PciDeviceCanonicalEvents.PciDeviceStateChangedData cdata = new PciDeviceCanonicalEvents.PciDeviceStateChangedData();
                cdata.setVmUuid(inv.getVmInstanceUuid());
                cdata.setPciDeviceUuid(inv.getUuid());
                cdata.setHostUuid(inv.getHostUuid());
                cdata.setStatus("Deleted");
                cdata.setInventory(inv);
                cdata.setDescription(inv.getDescription());
                evtf.fire(PciDeviceCanonicalEvents.PCIDEVICE_FULL_STATE_CHANGED_PATH, cdata);
            }
            completion.success();
            return;
        }

        if (op == OP_DETACH) {
            List<String> pciUuids = invs.stream().map(PciDeviceInventory::getUuid).collect(Collectors.toList());
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName("detach-pci-devices");
            chain.then(new ShareFlow() {
                @Override
                public void setup() {
                    flow(pciMgr.createDetachPciDeviceFromVmFlow(pciUuids));
                    flow(pciMgr.createAttachPciDeviceToHostFlow(pciUuids));
                    //flow(pciMgr.createSyncPciDeviceInfoFlow(pciUuids));

                    done(new FlowDoneHandler(completion) {
                        @Override
                        public void handle(Map data) {
                            pciMgr.detachPciDeviceFromVmInDb(pciUuids);
                            completion.success();
                        }
                    });

                    error(new FlowErrorHandler(completion) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            completion.fail(errCode);
                        }
                    });
                }
            }).start();
        }
    }

    private List<PciDeviceInventory> PciDeviceFromAction(CascadeAction action) {
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> structs = action.getParentIssuerContext();
            List<PciDeviceVO> pciDeviceVOS = new ArrayList<>();
            for (VmDeletionStruct struct : structs) {
                VmInstanceInventory inv = struct.getInventory();
                pciDeviceVOS.addAll(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.vmInstanceUuid, inv.getUuid()).list());
            }

            // pci devices associated to vf nics are not handled here
            pciDeviceVOS = pciDeviceVOS.stream().filter(v -> v.getType() != PciDeviceType.Ethernet_Controller ||
                    v.getVirtStatus() != PciDeviceVirtStatus.SRIOV_VIRTUAL).collect(Collectors.toList());

            if (!pciDeviceVOS.isEmpty()) {
                return PciDeviceInventory.valueOf(pciDeviceVOS);
            }
        } else if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            List<PciDeviceVO> pciDeviceVOS = new ArrayList<>();
            for (HostInventory host : hosts) {
                pciDeviceVOS.addAll(Q.New(PciDeviceVO.class).eq(PciDeviceVO_.hostUuid, host.getUuid()).list());
            }

            if (!pciDeviceVOS.isEmpty()) {
                return PciDeviceInventory.valueOf(pciDeviceVOS);
            }

        } else if (NAME.equals(action.getParentIssuer())) {
            return action.getParentIssuerContext();
        }

        return null;
    }
}
