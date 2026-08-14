package org.zstack.guesttools;

import org.zstack.header.vm.ArchiveBundle;

public class ArchiveVmGuestToolsBundle extends ArchiveBundle {
    private GuestToolsStateInventory guestToolsState;

    ArchiveVmGuestToolsBundle() {

    }

    ArchiveVmGuestToolsBundle(GuestToolsStateInventory guestToolsState) {
        this.guestToolsState = guestToolsState;
    }

    public GuestToolsStateInventory getGuestToolsState() {
        return guestToolsState;
    }

    public void setGuestToolsState(GuestToolsStateInventory guestToolsState) {
        this.guestToolsState = guestToolsState;
    }
}
