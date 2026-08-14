package org.zstack.macvlan;

import org.zstack.kvm.KVMAgentCommands;

public class KvmAgentL2NetworkMacVlanCommands {
    public static class CheckVlanBridgeCmd extends KVMAgentCommands.CheckBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }


    public static class CheckVlanBridgeResponse extends KVMAgentCommands.CheckBridgeResponse {
    }

    public static class CreateVlanBridgeCmd extends KVMAgentCommands.CreateBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class CreateVlanBridgeResponse extends KVMAgentCommands.CreateBridgeResponse {
    }

    public static class DeleteVlanBridgeCmd extends KVMAgentCommands.DeleteBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class DeleteVlanBridgeResponse extends KVMAgentCommands.DeleteBridgeResponse {
    }
}
