package org.zstack.vpc;

import org.zstack.header.vm.VmNicInventory;

public class SetSnatStateTaskData {
    private VmNicInventory publicNic;
    private boolean state;

    public VmNicInventory getPublicNic() {
        return publicNic;
    }

    public void setPublicNic(VmNicInventory publicNic) {
        this.publicNic = publicNic;
    }

    public boolean isState() {
        return state;
    }

    public void setState(boolean state) {
        this.state = state;
    }
}
