package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public abstract class VmTracer {
    private static final CLogger logger = Utils.getLogger(VmTracer.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private VmTracerHelper vmTracerHelper;

    private static final int strangeVmNumberInCache = 1500;

    private static Map<String, String> strangeVms = new LinkedHashMap<String, String>(strangeVmNumberInCache, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > strangeVmNumberInCache;
        }
    };

    private class Tracer {
        String hostUuid;
        Set<String> vmsToSkipHostSide;
        Map<String, VmInstanceState> hostSideStates;
        Map<String, VmInstanceState> mgmtSideStates;

        private void checkFromHostSide() {
            for (Map.Entry<String, VmInstanceState> e : hostSideStates.entrySet()) {
                String vmUuid = e.getKey();

                if (vmsToSkipHostSide != null && vmsToSkipHostSide.contains(vmUuid)) {
                    continue;
                }

                VmInstanceState actualState = e.getValue();

                VmInstanceState expectedState = mgmtSideStates.get(vmUuid);
                if (expectedState == null) {
                    // an anonymous vm showing on this host
                    handleAnonymousVm(vmUuid, actualState, expectedState);
                } else if (actualState != expectedState) {
                    // vm state changed on host side
                    handleStateChangeOnHostSide(vmUuid, actualState, expectedState);
                }
            }
        }

        private void handleStateChangeOnHostSide(final String vmUuid, final VmInstanceState actualState, VmInstanceState expected) {
            VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
            msg.setVmStateAtTracingMoment(expected);
            msg.setVmInstanceUuid(vmUuid);
            msg.setStateOnHost(actualState);
            msg.setHostUuid(hostUuid);
            msg.setFromSync(true);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg);
        }

        private void handleAnonymousVm(final String vmUuid, final VmInstanceState actualState, VmInstanceState expected) {
            final VmInstanceVO vm = dbf.findByUuid(vmUuid, VmInstanceVO.class);
            if (vm == null) {
                logger.debug(String.format("[Vm Tracer] detects stranger vm[identity:%s, state:%s]", vmUuid, actualState));
                String cachedHostUuid = strangeVms.get(vmUuid);

                // only report stranger vm once for each host
                // but if hostUuid changed, report it again
                if (cachedHostUuid != null && cachedHostUuid.equals(hostUuid)) {
                    logger.debug(String.format("[Vm Tracer] detects stranger vm[identity:%s, state:%s] but it's already in cache, skip firing event", vmUuid, actualState));
                    return;
                }

                strangeVms.put(vmUuid, hostUuid);

                VmTracerCanonicalEvents.StrangerVmFoundData data = new VmTracerCanonicalEvents.StrangerVmFoundData();
                data.setVmIdentity(vmUuid);
                data.setVmState(actualState);
                data.setHostUuid(hostUuid);
                evtf.fire(VmTracerCanonicalEvents.STRANGER_VM_FOUND_PATH, data);

                logger.warn(String.format("A strange vm[%s] was found on the host[%s], May cause problems, Please manually clean this vm", vmUuid, hostUuid));
                return;
            }

            handleStateChangeOnHostSide(vmUuid, actualState, expected);
        }

        private void checkFromManagementServerSide() {
            // from mgmt server we only check missing vm, vm state change has been updated by host side check
            for (Map.Entry<String, VmInstanceState> e : mgmtSideStates.entrySet()) {
                String vmUuid = e.getKey();
                VmInstanceState expectedState = e.getValue();
                if (expectedState != VmInstanceState.Stopped && expectedState != VmInstanceState.Created && !hostSideStates.containsKey(vmUuid)
                        && (vmsToSkipHostSide == null || !vmsToSkipHostSide.contains(vmUuid))) {
                    handleMissingVm(vmUuid, expectedState);
                }
            }
        }

        private void handleMissingVm(final String vmUuid, final VmInstanceState expectedState) {
            VmStateChangedOnHostMsg msg = new VmStateChangedOnHostMsg();
            msg.setVmStateAtTracingMoment(expectedState);
            msg.setHostUuid(hostUuid);
            msg.setVmInstanceUuid(vmUuid);
            msg.setStateOnHost(VmInstanceState.Stopped);
            msg.setFromSync(true);
            bus.makeTargetServiceIdByResourceUuid(msg, VmInstanceConstant.SERVICE_ID, vmUuid);
            bus.send(msg);
        }

        void trace() {
            if (mgmtSideStates == null) {
                mgmtSideStates = buildManagementServerSideVmStates(hostUuid);
            }

            checkFromHostSide();
            checkFromManagementServerSide();
        }
    }

    @Transactional(readOnly = true)
    protected Map<String, VmInstanceState> buildManagementServerSideVmStates(String hostUuid) {
        Map<String, VmInstanceState> mgmtSideStates = new HashMap<>();
        String sql = "select vm.uuid, vm.state from VmInstanceVO vm where vm.hostUuid = :huuid or (vm.hostUuid is null and vm.lastHostUuid = :huuid)" +
                " and vm.state not in (:vmstates)";

        if (!vmTracerHelper.getVmTracerUnsupportedVmInstanceTypeSet().isEmpty()) {
            sql += "and vm.type not in (:vmtypes)";
        }

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("huuid", hostUuid);
        q.setParameter("vmstates", list(VmInstanceState.Destroyed, VmInstanceState.Destroying));

        if (!vmTracerHelper.getVmTracerUnsupportedVmInstanceTypeSet().isEmpty()) {
            q.setParameter("vmtypes", vmTracerHelper.getVmTracerUnsupportedVmInstanceTypeSet());
        }

        List<Tuple> ts = q.getResultList();

        for (Tuple t : ts) {
            mgmtSideStates.put(t.get(0, String.class), t.get(1, VmInstanceState.class));
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("ManagementServerSideVmStates vm %s, state %s", t.get(0, String.class), t.get(1, VmInstanceState.class).toString()));
            }
        }

        return mgmtSideStates;
    }

    protected void reportVmState(final String hostUuid, final Map<String, VmInstanceState> vmStates, final Set<String> vmsToSkipHostSide, final Map<String, VmInstanceState> mgmtSideStates) {
        if (logger.isTraceEnabled()) {
            for (Map.Entry<String, VmInstanceState> e : vmStates.entrySet()) {
                logger.trace(String.format("reportVmState vm: %s, state: %s", e.getKey(), e.getValue().toString()));
            }
        }

        for (VmInstanceState state : vmStates.values()) {
            if (state != VmInstanceState.Running
                    && state != VmInstanceState.Stopped
                    && state != VmInstanceState.Paused
                    && state != VmInstanceState.Crashed
                    && state != VmInstanceState.NoState) {
                throw new CloudRuntimeException(String.format("host can only report vm state as Running, Stopped, Paused, Crashed, got %s", state));
            }
        }

        if (!CoreGlobalProperty.VM_TRACER_ON) {
            logger.debug(String.format("vm tracer is off, skip reporting vm state on host[uuid:%s]", hostUuid));
            return;
        }

        thdf.syncSubmit(new SyncTask<Object>() {
            @Override
            public String getSyncSignature() {
                return String.format("trace-vm-state-on-host-%s", hostUuid);
            }

            @Override
            public int getSyncLevel() {
                return 1;
            }

            @Override
            public String getName() {
                return String.format("trace-vm-state-on-host-%s", hostUuid);
            }

            @Override
            public Object call() {
                Tracer t = new Tracer();
                t.hostUuid = hostUuid;
                t.hostSideStates = vmStates;
                t.vmsToSkipHostSide = vmsToSkipHostSide;
                t.mgmtSideStates = mgmtSideStates;
                t.trace();
                return null;
            }
        });
    }

}
