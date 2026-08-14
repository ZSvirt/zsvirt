package org.zstack.portal.managementnode;

import sun.misc.Signal;
import sun.misc.SignalHandler;

public class DebugSignalHandler implements SignalHandler {
    private ManagementNodeManagerImpl impl;

    public static void listenTo(String name, ManagementNodeManagerImpl impl) {
        Signal signal = new Signal(name);
        Signal.handle(signal, new DebugSignalHandler(impl));
    }

    private DebugSignalHandler(ManagementNodeManagerImpl impl) {
        this.impl = impl;
    }

    @Override
    public void handle(Signal signal) {
        if (signal.toString().trim().equals("SIGUSR2")) {
            impl.setSigUsr2();
        }
    }
}
