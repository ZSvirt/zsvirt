package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.allocator.*;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.allocator.HostCandidateProducer.HostCandidateProducerContext;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionUtils.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostAllocatorChain implements HostAllocatorTrigger, HostAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(HostAllocatorChain.class);

    private String name;
    private HostAllocatorSpec allocationSpec;
    private HostAllocationPaginationInfo paginationInfo;
    private int pageSize;

    private List<HostCandidateProducer> producers;
    private HostCandidateProducer producerInUse;

    private List<AbstractHostAllocatorFlow> flows;
    private Iterator<AbstractHostAllocatorFlow> it;
    private AbstractHostAllocatorFlow lastFlow;

    private List<HostCandidate> result = null;
    private final List<HostCandidate.RejectedCandidate> rejectedList = new ArrayList<>();

    private boolean isDryRun;
    private ErrorCode errorCode;
    private Set<ErrorCode> seriesErrorWhenPagination = new HashSet<>();
    private ReturnValueCompletion<List<HostInventory>> completion;

    public HostAllocatorSpec getAllocationSpec() {
        return allocationSpec;
    }

    public void setAllocationSpec(HostAllocatorSpec allocationSpec) {
        this.allocationSpec = allocationSpec;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AbstractHostAllocatorFlow> getFlows() {
        return flows;
    }

    public void setFlows(List<AbstractHostAllocatorFlow> flows) {
        this.flows = flows;
    }

    public List<HostCandidateProducer> getProducers() {
        return producers;
    }

    public void setProducers(List<HostCandidateProducer> producers) {
        this.producers = producers;
    }

    private void done() {
        if (result == null) {
            if (isDryRun && HostAllocatorError.NO_AVAILABLE_HOST.toString().equals(errorCode.getCode())) {
                completion.success(new ArrayList<>());
                return;
            }
            completion.fail(errorCode);
            return;
        }

        // in case a wrong flow returns an empty result set
        if (result.isEmpty()) {
            completion.fail(err(HostAllocatorError.NO_AVAILABLE_HOST,
                    "host allocation flow doesn't indicate any details"));
            return;
        }

        completion.success(HostInventory.valueOf(transform(result, candidate -> candidate.host)));
    }

    private void runFlow(AbstractHostAllocatorFlow flow) {
        try {
            lastFlow = flow;
            flow.setCandidates(result);
            flow.setSpec(allocationSpec);
            flow.setTrigger(this);
            flow.allocate();
        } catch (OperationFailureException ofe) {
            if (ofe.getErrorCode().getCode().equals(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getCode())) {
                logger.debug(String.format("[Host Allocation]: intermediate failure; " +
                                "because of pagination, will start over allocation again; " +
                                "current pagination info %s; failure details: %s",
                        JSONObjectUtil.toJsonString(paginationInfo), ofe.getErrorCode().getDetails()));
                seriesErrorWhenPagination.add(ofe.getErrorCode().getCause());
                startNextPage();
            } else {
                fail(ofe.getErrorCode());
            }
        } catch (Throwable t) {
            logger.warn("unhandled throwable", t);
            completion.fail(inerr(t.getMessage()));
        }
    }

    /**
     * {@link #producers} used to produce {@link HostCandidate}.
     * If any producer produces hosts, these host will be put into {@link #flows} and do allocation.
     *
     * <blockquote><pre>
     *     HostCandidateProducer -&gt; (HostCandidate) -&gt; AbstractHostAllocatorFlow
     * </pre></blockquote>
     */
    private void start() {
        if (HostAllocatorGlobalConfig.USE_PAGINATION.value(Boolean.class)) {
            pageSize = HostAllocatorGlobalConfig.PAGINATION_LIMIT.value(Integer.class);
            startNextPage();
            return;
        }

        it = flows.iterator();
        DebugUtils.Assert(it.hasNext(), "can not run an empty host allocation chain");
        runFlow(it.next());
    }

    private void startNextPage() {
        if (paginationInfo == null) {
            paginationInfo = new HostAllocationPaginationInfo();
            paginationInfo.setLimit(pageSize);
        } else {
            paginationInfo.setOffset(paginationInfo.getOffset() + pageSize);
        }

        List<HostVO> hosts = new ArrayList<>();

        HostCandidateProducerContext context = new HostCandidateProducerContext();
        context.spec = allocationSpec;
        context.paginationInfo = paginationInfo;
        context.hostConsumer = hosts::addAll;
        context.errorReporter = errorCode -> fail(operr("failed to allocate hosts").withCause(errorCode));

        if (producerInUse == null) {
            for (HostCandidateProducer producer : producers) {
                producer.produce(context);

                if (!hosts.isEmpty()) {
                    producerInUse = producer;
                    break;
                }
            }

            if (producerInUse == null) { // that means hosts.isEmpty()
                fail(err(HostAllocatorError.NO_AVAILABLE_HOST, "no available hosts found"));
                return;
            }
        } else {
            producerInUse.produce(context);

            if (hosts.isEmpty()) {
                fail(err(HostAllocatorError.NO_AVAILABLE_HOST, "no available hosts found"));
                return;
            }
        }

        result = transform(hosts, HostCandidate::new);
        it = flows.iterator();
        runFlow(it.next());
    }

    @Override
    public void next() {
        boolean anyAllowed = false;

        if (result != null) {
            for (Iterator<HostCandidate> iterator = result.iterator(); iterator.hasNext(); ) {
                HostCandidate candidate = iterator.next();
                if (candidate.reject != null) {
                    logger.debug(String.format(
                            "[Host Allocation]: flow[%s] rejected candidate host[uuid:%s, name:%s]: %s",
                            candidate.rejectBy, candidate.getUuid(), candidate.host.getName(), candidate.reject));
                    iterator.remove();
                    rejectedList.add(candidate.toRejectedCandidate());
                    continue;
                }
                anyAllowed = true;
            }
        }

        if (!anyAllowed) {
            ErrorCode errorCode = new ErrorCode();
            errorCode.setCode(HostAllocatorError.NO_AVAILABLE_HOST.toString());
            errorCode.setDetails("no host meet the requirements");

            if (paginationInfo != null) {
                // in pagination, and a middle flow fails, we can continue
                ErrorCode upperError = new ErrorCode();
                upperError.setCode(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getCode());
                upperError.setDetails("no host meet the requirements (in pagination, a middle flow fails)");
                upperError.setDescription(HostAllocatorConstant.PAGINATION_INTERMEDIATE_ERROR.getDescription());
                upperError.setCause(errorCode);
                errorCode = upperError;
            }
            // else: no host found, stop allocating

            throw new OperationFailureException(errorCode);
        }

        VmInstanceInventory vm = allocationSpec.getVmInstance();
        logger.debug(String.format("[Host Allocation]: flow[%s] successfully found %s candidate hosts for vm[uuid:%s, name:%s]",
                lastFlow.getClass().getName(), result.size(), vm.getUuid(), vm.getName()));
        if (logger.isTraceEnabled()) {
            StringBuilder sb = new StringBuilder("[Host Allocation Details]:");
            for (HostCandidate candidate : result) {
                HostVO vo = candidate.host;
                sb.append(String.format("\ncandidate host[name:%s, uuid:%s, zoneUuid:%s, clusterUuid:%s, hypervisorType:%s]",
                        vo.getName(), vo.getUuid(), vo.getZoneUuid(), vo.getClusterUuid(), vo.getHypervisorType()));
            }
            logger.trace(sb.toString());
        }

        if (it.hasNext()) {
            runFlow(it.next());
            return;
        }

        done();
    }

    @Override
    public void fail(ErrorCode errorCode) {
        result = null;

        for (HostCandidate.RejectedCandidate candidate : rejectedList) {
            if (candidate.rejectI18n == null)
                candidate.rejectI18n = i18n(candidate.reject);
        }

        LinkedHashMap<String, Object> opaque = new LinkedHashMap<>();
        opaque.put("rejectedCandidates", rejectedList);

        if (rejectedList.size() == 1) {
            HostCandidate.RejectedCandidate candidate = rejectedList.get(0);
            this.errorCode = err(HostAllocatorError.NO_AVAILABLE_HOST,
            "failed to allocate host[%s]: %s", candidate.hostUuid, candidate);
        } else {
            this.errorCode = err(HostAllocatorError.NO_AVAILABLE_HOST,
            "[Host Allocation] no host meet the requirements");
        }

        this.errorCode.setOpaque(opaque);
        this.errorCode.withCause(errorCode);
        if (!seriesErrorWhenPagination.isEmpty()) {
            this.errorCode.withCause(seriesErrorWhenPagination);
        }

        done();
    }

    @Override
    public void allocate(HostAllocatorSpec spec, ReturnValueCompletion<List<HostInventory>> completion) {
        this.allocationSpec = spec;
        this.isDryRun = false;
        this.completion = completion;
        start();
    }

    @Override
    public void dryRun(HostAllocatorSpec spec, ReturnValueCompletion<List<HostInventory>> completion) {
        this.allocationSpec = spec;
        this.isDryRun = true;
        this.completion = completion;
        start();
    }
}
