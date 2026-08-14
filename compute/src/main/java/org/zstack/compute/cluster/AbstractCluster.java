package org.zstack.compute.cluster;

import org.zstack.core.Platform;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.cluster.Cluster;
import org.zstack.header.cluster.ClusterState;
import org.zstack.header.cluster.ClusterStateEvent;

public abstract class AbstractCluster implements Cluster {
    private final static StateMachine<ClusterState, ClusterStateEvent> stateMachine;

    static {
        stateMachine = Platform.createStateMachine();
        stateMachine.addTranscation(ClusterState.Enabled, ClusterStateEvent.disable, ClusterState.Disabled);
        stateMachine.addTranscation(ClusterState.Enabled, ClusterStateEvent.enable, ClusterState.Enabled);
        stateMachine.addTranscation(ClusterState.Disabled, ClusterStateEvent.disable, ClusterState.Disabled);
        stateMachine.addTranscation(ClusterState.Disabled, ClusterStateEvent.enable, ClusterState.Enabled);
    }

    static ClusterState getNextState(ClusterState curr, ClusterStateEvent event) {
        return stateMachine.getNextState(curr, event);
    }

    protected abstract void deleteHook();
}
