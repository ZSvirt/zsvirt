package org.zstack.compute.vm;

import org.zstack.header.vm.hooks.*;

import java.util.function.Supplier;

public interface VmInstanceHookManager {
    void hookStartEvent(Supplier<VmInstanceStartHook> supplier);

    void hookStopEvent(Supplier<VmInstanceStopHook> supplier);

    void hookRebootEvent(Supplier<VmInstanceRebootHook> supplier);

    void hookDestroyEvent(Supplier<VmInstanceDestroyHook> supplier);

    void hookCreateEvent(Supplier<VmInstanceCreateHook> supplier);
}
