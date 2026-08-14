package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;

import java.util.List;
import java.util.function.Consumer;

public interface HostCandidateProducer {
    public static class HostCandidateProducerContext {
        public HostAllocatorSpec spec;
        public HostAllocationPaginationInfo paginationInfo;
        public Consumer<List<HostVO>> hostConsumer;
        public Consumer<ErrorCode> errorReporter;

        public void accept(List<HostVO> hosts) {
            hostConsumer.accept(hosts);
        }

        public boolean usePagination() {
            return paginationInfo != null;
        }

        public void reportError(ErrorCode error) {
            errorReporter.accept(error);
        }
    }

    void produce(HostCandidateProducerContext context);
}
