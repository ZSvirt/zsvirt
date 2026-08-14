package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class ChangeVmMetaDataMsg extends NeedReplyMessage implements VmInstanceMessage {
    public static class AtomicVmState {
        private String expected;
        private String value;

        public VmInstanceState getExpected() {
            if (expected != null) {
                return VmInstanceState.valueOf(expected);
            }
            return null;
        }

        public void setExpected(VmInstanceState expected) {
            if (expected != null) {
                this.expected = expected.toString();
            }
        }

        public VmInstanceState getValue() {
            if (value != null) {
                return VmInstanceState.valueOf(value);
            }
            return null;
        }

        public void setValue(VmInstanceState value) {
            if (value != null) {
                this.value = value.toString();
            }
        }
    }

    public static class AtomicHostUuid {
        private String expected;
        private String value;

        public String getExpected() {
            return expected;
        }

        public void setExpected(String expected) {
            this.expected = expected;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private String vmInstanceUuid;
    private AtomicHostUuid hostUuid;
    private AtomicVmState state;
    private boolean needHostAndStateBothMatch;

    public boolean isNeedHostAndStateBothMatch() {
        return needHostAndStateBothMatch;
    }

    public void setNeedHostAndStateBothMatch(boolean needHostAndStateBothMatch) {
        this.needHostAndStateBothMatch = needHostAndStateBothMatch;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public AtomicHostUuid getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(AtomicHostUuid hostUuid) {
        this.hostUuid = hostUuid;
    }

    public AtomicVmState getState() {
        return state;
    }

    public void setState(AtomicVmState state) {
        this.state = state;
    }
}
