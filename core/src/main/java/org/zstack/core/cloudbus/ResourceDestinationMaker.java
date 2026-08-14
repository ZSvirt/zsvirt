package org.zstack.core.cloudbus;

import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.managementnode.ManagementNodeVO;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:35 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ResourceDestinationMaker {
    class NodeInfo {
        private String nodeUuid;
        private String nodeIP;

        NodeInfo(ManagementNodeVO vo) {
            nodeUuid = vo.getUuid();
            nodeIP = vo.getHostName();
        }

        NodeInfo(ManagementNodeInventory inv) {
            nodeUuid = inv.getUuid();
            nodeIP = inv.getHostName();
        }

        public String getNodeUuid() {
            return nodeUuid;
        }

        public void setNodeUuid(String nodeUuid) {
            this.nodeUuid = nodeUuid;
        }

        public String getNodeIP() {
            return nodeIP;
        }

        public void setNodeIP(String nodeIP) {
            this.nodeIP = nodeIP;
        }
    }

    String makeDestination(String resourceUuid);

    boolean isManagedByUs(String resourceUuid);

    Collection<String> getManagementNodesInHashRing();

    NodeInfo getNodeInfo(String nodeUuid);

    Collection<NodeInfo> getAllNodeInfo();

    int getManagementNodeCount();

    boolean isNodeInCircle(String nodeId);
}
