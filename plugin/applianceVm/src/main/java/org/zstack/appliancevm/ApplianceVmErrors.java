package org.zstack.appliancevm;

/**
 */
public enum ApplianceVmErrors {
    MANAGEMENT_IP_OCCUPIED(1000),
    UNABLE_TO_START(1001);

    private String code;

    ApplianceVmErrors(int id) {
        code = String.format("APPLIANCE_VM.%s", id);
    }

    public boolean isEqual(Object t) {
        return t.toString().equals(this.toString());
    }

    @Override
    public String toString() {
        return code;
    }
}
