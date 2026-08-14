package org.zstack.core.workflow;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class ShareFlowChain extends SimpleFlowChain {
    private final List<ShareFlow> shareFlows = new ArrayList<>();

    @Override
    public ShareFlowChain then(Flow flow) {
        if (!(flow instanceof ShareFlow)) {
            throw new IllegalArgumentException(String.format("ShareFlowChain only receives ShareFlow in then(), but %s got", flow.getClass().getName()));
        }

        shareFlows.add((ShareFlow) flow);
        return this;
    }

    void install(Flow flow) {
        super.then(flow);
    }

    @Override
    public void start() {
        if (shareFlows.isEmpty()) {
            throw new CloudRuntimeException("you must call then() to install ShareFlow before start()");
        }

        for (ShareFlow shareFlow : shareFlows) {
            shareFlow.setChain(this);
            shareFlow.setup();
        }
        super.start();
    }
}
