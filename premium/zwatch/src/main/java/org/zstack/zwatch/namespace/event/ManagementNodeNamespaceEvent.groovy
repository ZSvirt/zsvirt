package org.zstack.zwatch.namespace.event

import org.zstack.header.managementnode.ManagementNodeCanonicalEvent
import org.zstack.zwatch.ZWatchConstants
import org.zstack.zwatch.datatype.EventFamily
import org.zstack.zwatch.namespace.ManagementNodeNamespace

/**
 * Created by kayo on 2018/8/3.
 */
class ManagementNodeNamespaceEvent {
    ManagementNodeNamespaceEvent() {
        ManagementNodeNamespace.ManagementNodeLeft.onCanonicalEvent(ZWatchConstants.NODE_LIFECYCLE_LEFT_CANONICAL_EVENT_PATH) { ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData data ->
            if (data.getLifeCycle().equals(ManagementNodeCanonicalEvent.LifeCycle.NodeLeft.toString())) {
                return new EventFamily.Event(data.getNodeUuid(), data.getInventory().getHostName())
            }
        }

        ManagementNodeNamespace.ManagementNodeJoin.onCanonicalEvent(ManagementNodeCanonicalEvent.NODE_LIFECYCLE_PATH) { ManagementNodeCanonicalEvent.ManagementNodeLifeCycleData data ->
            if (data.getLifeCycle().equals(ManagementNodeCanonicalEvent.LifeCycle.NodeJoin.toString())) {
                return new EventFamily.Event(data.getNodeUuid(), data.getInventory().getHostName())
            }
        }

        ManagementNodeNamespace.ManagementNodeTemporalRegression.onCanonicalEvent(ManagementNodeCanonicalEvent.NODE_TEMPORAL_REGRESSION_PATH) { ManagementNodeCanonicalEvent.ManagementNodeTemporalRegressionData data ->
            return new EventFamily.Event(data.getNodeUuid(), data.getHostname())
        }
    }
}
