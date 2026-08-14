package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.allocator.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.zstack.core.Platform.operr;

/**
 * Created by mingjian.deng on 2017/11/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostSortorChain implements HostSortorStrategy {
    private static final CLogger logger = Utils.getLogger(HostSortorChain.class);
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private HostCapacityReserveManager reserveMgr;

    private HostAllocatorSpec allocationSpec;

    private List<AbstractHostSortorFlow> flows;

    private boolean isDryRun = false;
    private boolean skipReserveCapacity = false;
    private ReturnValueCompletion<HostInventory> completion;
    private ReturnValueCompletion<List<HostInventory>> dryRunCompletion;

    public List<AbstractHostSortorFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<AbstractHostSortorFlow> flows) {
        this.flows = flows;
    }

    public void setSkipReserveCapacity(boolean skipReserveCapacity) {
        this.skipReserveCapacity = skipReserveCapacity;
    }

    @Override
    public void sort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<HostInventory> completion) {
        allocationSpec = spec;
        this.completion = completion;
        sort(hosts);
    }

    // adjust hosts
    private void reSortHosts(final List<HostInventory> sub, List<HostInventory> hosts) {
        DebugUtils.Assert(sub.size() <= hosts.size(), "subHosts' size cannot larger than hosts' size");
        hosts.removeAll(sub);
        hosts.addAll(0, sub);
    }

    private void sort(List<HostInventory> hosts) {
        DebugUtils.Assert(hosts.size() > 0, "must sort at least 1 host");
        List<HostInventory> subHosts = new ArrayList<>();
        subHosts.addAll(hosts);
        for (AbstractHostSortorFlow flow: flows) {
            if (subHosts.size() == 0) {
                break;
            }

            flow.setCandidates(subHosts);
            flow.setSpec(allocationSpec);
            logger.debug(String.format("sort by flow: %s", flow.getClass().getSimpleName()));
            flow.sort();
            reSortHosts(subHosts, hosts);
            subHosts = flow.getSubCandidates();

            if (flow.skipNext()) {
                break;
            }
        }
        done(hosts);
    }

    @Override
    public void dryRunSort(HostAllocatorSpec spec, List<HostInventory> hosts, ReturnValueCompletion<List<HostInventory>> completion) {
        allocationSpec = spec;
        this.dryRunCompletion = completion;
        isDryRun = true;
        sort(hosts);
    }

    private void reserveHost(HostInventory host, Completion cmpl){
        Map data = new HashMap();
        data.put(HostAllocatorConstant.Param.HOST, host);
        data.put(HostAllocatorConstant.Param.SPEC, allocationSpec);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setData(data);
        chain.setName("hostAllocation-reserve-flow");
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "hostAllocation-reserve-capacity";

                    boolean success = false;
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
                        try {
                            reserveCapacity(host);
                            success = true;
                            trigger.next();
                        } catch (UnableToReserveHostCapacityException e) {
                            logger.debug(String.format("[Host Allocation]: %s on host[uuid:%s]. try next one",
                                    e.getMessage(), host.getUuid()), e);
                            trigger.fail(operr("failed to reserve capacity in host[uuid=%s]", host.getUuid())
                                    .withException(e));
                        }
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            HostInventory host = (HostInventory) data.get(HostAllocatorConstant.Param.HOST);
                            rollbackCapacity(host);
                        }
                        trigger.rollback();
                    }
                });

                for (HostAllocatorReserveExtensionPoint exp: pluginRgty.getExtensionList(HostAllocatorReserveExtensionPoint.class)) {
                    flow(exp.getExtension());
                }

                done(new FlowDoneHandler(cmpl) {
                    @Override
                    public void handle(Map data) {
                        cmpl.success();
                    }
                });

                error(new FlowErrorHandler(cmpl) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        cmpl.fail(errCode);
                    }
                });
            }
        }).start();
    }

    private void done(List<HostInventory> hosts) {
        if (isDryRun) {
            dryRunCompletion.success(hosts);
            return;
        }
        if (skipReserveCapacity) {
            completion.success(hosts.iterator().next());
            return;
        }

        AtomicReference<HostInventory> selectedHost = new AtomicReference<>();
        new While<>(hosts).each((host, whileCompletion) -> reserveHost(host, new Completion(whileCompletion) {
            @Override
            public void success() {
                selectedHost.set(host);
                whileCompletion.allDone(); // break the new While loop: we only need one host
            }

            @Override
            public void fail(ErrorCode errorCode) {
                whileCompletion.addError(errorCode);
                whileCompletion.done();
            }
        })).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (selectedHost.get() == null) {
		    completion.fail(errorCodeList.getCauses().get(errorCodeList.getCauses().size()-1));
                    return;
                }
                completion.success(selectedHost.get());
            }
        });
    }

    private void reserveCapacity(final HostInventory host) {
        reserveMgr.reserveCapacity(host.getUuid(), allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity(), false);
        logger.debug(String.format("[Host Allocation]: successfully reserved cpu[%s], memory[%s bytes] on host[uuid:%s] for vm[uuid:%s]",
                allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity(), host.getUuid(),
                allocationSpec.getVmInstance().getUuid()));
    }

    private void rollbackCapacity(final HostInventory host) {
        reserveMgr.reserveCapacity(host.getUuid(), 0L - allocationSpec.getCpuCapacity(),0L - allocationSpec.getMemoryCapacity(), false);
        logger.debug(String.format("[Host Allocation]: successfully rollback cpu[%s], memory[%s bytes] on host[uuid:%s] for vm[uuid:%s]",
                allocationSpec.getCpuCapacity(), allocationSpec.getMemoryCapacity(), host.getUuid(),
                allocationSpec.getVmInstance().getUuid()));
    }
}
