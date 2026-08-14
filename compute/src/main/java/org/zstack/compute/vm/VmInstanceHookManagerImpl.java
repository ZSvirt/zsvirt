package org.zstack.compute.vm;

import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.*;
import org.zstack.header.vm.hooks.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class VmInstanceHookManagerImpl implements VmInstanceHookManager, VmInstanceStartExtensionPoint,
        VmInstanceStopExtensionPoint, VmInstanceRebootExtensionPoint, VmInstanceDestroyExtensionPoint, VmInstanceStartNewCreatedVmExtensionPoint {

    class StartVmState {
        VmInstanceStartHook hook;
        StartVmState(VmInstanceStartHook hook) {
            this.hook = hook;
        }

        void pre(VmInstanceInventory vm) {
            if (hook instanceof VmInstancePreStartHook) {
                ((VmInstancePreStartHook) hook).preStart(vm);
            }
        }

        @ExceptionSafe
        void before(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceBeforeStartHook) {
                ((VmInstanceBeforeStartHook) hook).beforeStart(vm);
            }
        }

        @ExceptionSafe
        void after(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceAfterStartHook) {
                ((VmInstanceAfterStartHook)hook).afterStart(vm);
            }
        }

        @ExceptionSafe
        void fail(VmInstanceInventory vm, ErrorCode error) {
            if (hook instanceof VmInstanceFailToStartHook) {
                ((VmInstanceFailToStartHook) hook).failToStart(vm, error);
            }
        }
    }

    class StopVmState {
        VmInstanceStopHook hook;

        StopVmState(VmInstanceStopHook hook) {
            this.hook = hook;
        }

        void pre(VmInstanceInventory vm) {
            if (hook instanceof VmInstancePreStopHook) {
                ((VmInstancePreStopHook) hook).preStop(vm);
            }
        }

        @ExceptionSafe
        void before(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceBeforeStopHook) {
                ((VmInstanceBeforeStopHook) hook).beforeStop(vm);
            }
        }

        @ExceptionSafe
        void after(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceAfterStopHook) {
                ((VmInstanceAfterStopHook)hook).afterStop(vm);
            }
        }

        @ExceptionSafe
        void fail(VmInstanceInventory vm, ErrorCode error) {
            if (hook instanceof VmInstanceFailToStopHook) {
                ((VmInstanceFailToStopHook) hook).failToStop(vm, error);
            }
        }
    }

    class RebootVmState {
        VmInstanceRebootHook hook;

        RebootVmState(VmInstanceRebootHook hook) {
            this.hook = hook;
        }

        void pre(VmInstanceInventory vm) {
            if (hook instanceof VmInstancePreRebootHook) {
                ((VmInstancePreRebootHook) hook).preReboot(vm);
            }
        }

        @ExceptionSafe
        void before(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceBeforeRebootHook) {
                ((VmInstanceBeforeRebootHook) hook).beforeReboot(vm);
            }
        }

        @ExceptionSafe
        void after(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceAfterRebootHook) {
                ((VmInstanceAfterRebootHook) hook).afterReboot(vm);
            }
        }

        @ExceptionSafe
        void fail(VmInstanceInventory vm, ErrorCode error) {
            if (hook instanceof VmInstanceFailToRebootHook) {
                ((VmInstanceFailToRebootHook) hook).failToReboot(vm, error);
            }
        }
    }

    class CreateVmState {
        VmInstanceCreateHook hook;

        CreateVmState(VmInstanceCreateHook hook) {
            this.hook = hook;
        }

        void pre(VmInstanceInventory vm) {
            if (hook instanceof VmInstancePreCreateHook) {
                ((VmInstancePreCreateHook) hook).preCreate(vm);
            }
        }

        @ExceptionSafe
        void before(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceBeforeCreateHook) {
                ((VmInstanceBeforeCreateHook) hook).beforeCreate(vm);
            }
        }

        @ExceptionSafe
        void after(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceAfterCreateHook) {
                ((VmInstanceAfterCreateHook)hook).afterCreate(vm);
            }
        }

        @ExceptionSafe
        void fail(VmInstanceInventory vm, ErrorCode error) {
            if (hook instanceof VmInstanceFailToCreateHook) {
                ((VmInstanceFailToCreateHook) hook).failToCreate(vm, error);
            }
        }
    }

    class DestroyVmState {
        VmInstanceDestroyHook hook;

        DestroyVmState(VmInstanceDestroyHook hook) {
            this.hook = hook;
        }

        void pre(VmInstanceInventory vm) {
            if (hook instanceof VmInstancePreDestroyHook) {
                ((VmInstancePreDestroyHook) hook).preDestroy(vm);
            }
        }

        @ExceptionSafe
        void before(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceBeforeDestroyHook) {
                ((VmInstanceBeforeDestroyHook) hook).beforeDestroy(vm);
            }
        }

        @ExceptionSafe
        void after(VmInstanceInventory vm) {
            if (hook instanceof VmInstanceAfterDestroyHook) {
                ((VmInstanceAfterDestroyHook)hook).afterDestroy(vm);
            }
        }

        @ExceptionSafe
        void fail(VmInstanceInventory vm, ErrorCode error) {
            if (hook instanceof VmInstanceFailToDestroyHook) {
                ((VmInstanceFailToDestroyHook) hook).failToDestroy(vm, error);
            }
        }
    }

    private Map<String, List<StartVmState>> vmStartHooks = new ConcurrentHashMap<>();
    private final List<Supplier<VmInstanceStartHook>> vmStartHookSuppliers = Collections.synchronizedList(new ArrayList());
    private Map<String, List<StopVmState>> vmStopHooks = new ConcurrentHashMap<>();
    private final List<Supplier<VmInstanceStopHook>> vmStopHookSuppliers = Collections.synchronizedList(new ArrayList());
    private Map<String, List<RebootVmState>> vmRebootHooks = new ConcurrentHashMap<>();
    private final List<Supplier<VmInstanceRebootHook>> vmRebootHookSuppliers = Collections.synchronizedList(new ArrayList());
    private Map<String, List<CreateVmState>> vmCreateHooks = new ConcurrentHashMap<>();
    private final List<Supplier<VmInstanceCreateHook>> vmCreateHookSuppliers = Collections.synchronizedList(new ArrayList());
    private Map<String, List<DestroyVmState>> vmDestroyHooks = new ConcurrentHashMap<>();
    private final List<Supplier<VmInstanceDestroyHook>> vmDestroyHookSuppliers = Collections.synchronizedList(new ArrayList());

    @Override
    @Deferred
    public String preDestroyVm(VmInstanceInventory inv) {
        List<DestroyVmState> states;
        synchronized (vmDestroyHookSuppliers) {
            if (vmDestroyHookSuppliers.isEmpty()) {
                return null;
            }

            states = vmDestroyHookSuppliers.stream().map(s -> new DestroyVmState(s.get())).collect(Collectors.toList());
        }

        vmDestroyHooks.put(inv.getUuid(), states);
        Defer.guard(() -> vmDestroyHooks.remove(inv.getUuid()));

        states.forEach(s -> s.pre(inv));

        return null;
    }

    @Override
    public void beforeDestroyVm(VmInstanceInventory inv) {
        List<DestroyVmState> states = vmDestroyHooks.get(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.before(inv));
        }
    }

    @Override
    public void afterDestroyVm(VmInstanceInventory inv) {
        List<DestroyVmState> states = vmDestroyHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.after(inv));
        }
    }

    @Override
    public void failedToDestroyVm(VmInstanceInventory inv, ErrorCode reason) {
        List<DestroyVmState> states = vmDestroyHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.fail(inv, reason));
        }
    }

    @Override
    @Deferred
    public String preRebootVm(VmInstanceInventory inv) {
        List<RebootVmState> states;
        synchronized (vmRebootHookSuppliers) {
            if (vmRebootHookSuppliers.isEmpty()) {
                return null;
            }

            states = vmRebootHookSuppliers.stream().map(s -> new RebootVmState(s.get())).collect(Collectors.toList());
        }

        vmRebootHooks.put(inv.getUuid(), states);
        Defer.guard(() -> vmRebootHooks.remove(inv.getUuid()));

        states.forEach(s -> s.pre(inv));

        return null;
    }

    @Override
    public void beforeRebootVm(VmInstanceInventory inv) {
        List<RebootVmState> states = vmRebootHooks.get(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.before(inv));
        }
    }

    @Override
    public void afterRebootVm(VmInstanceInventory inv) {
        List<RebootVmState> states = vmRebootHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.after(inv));
        }
    }

    @Override
    public void failedToRebootVm(VmInstanceInventory inv, ErrorCode reason) {
        List<RebootVmState> states = vmRebootHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.fail(inv, reason));
        }
    }

    @Override
    @Deferred
    public String preStartVm(VmInstanceInventory inv) {
        List<StartVmState> states;
        synchronized (vmStartHookSuppliers) {
            if (vmStartHookSuppliers.isEmpty()) {
                return null;
            }

            states = vmStartHookSuppliers.stream().map(s -> new StartVmState(s.get())).collect(Collectors.toList());
        }

        vmStartHooks.put(inv.getUuid(), states);
        Defer.guard(()-> vmStartHooks.remove(inv.getUuid()));

        states.forEach(s -> s.pre(inv));

        return null;
    }

    @Override
    public void beforeStartVm(VmInstanceInventory inv) {
        List<StartVmState> states = vmStartHooks.get(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.before(inv));
        }
    }

    @Override
    public void afterStartVm(VmInstanceInventory inv) {
        List<StartVmState> states = vmStartHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.after(inv));
        }
    }

    @Override
    public void failedToStartVm(VmInstanceInventory inv, ErrorCode reason) {
        List<StartVmState> states = vmStartHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.fail(inv, reason));
        }
    }

    @Override
    @Deferred
    public String preStartNewCreatedVm(VmInstanceInventory inv) {
        List<CreateVmState> states;
        synchronized (vmCreateHookSuppliers) {
            if (vmCreateHookSuppliers.isEmpty()) {
                return null;
            }

            states = vmCreateHookSuppliers.stream().map(s -> new CreateVmState(s.get())).collect(Collectors.toList());
        }

        vmCreateHooks.put(inv.getUuid(), states);
        Defer.guard(() -> vmCreateHooks.remove(inv.getUuid()));

        states.forEach(s -> s.pre(inv));

        return null;
    }

    @Override
    public void beforeStartNewCreatedVm(VmInstanceInventory inv) {
        List<CreateVmState> states = vmCreateHooks.get(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.before(inv));
        }
    }

    @Override
    public void afterStartNewCreatedVm(VmInstanceInventory inv) {
        List<CreateVmState> states = vmCreateHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.after(inv));
        }
    }

    @Override
    public void failedToStartNewCreatedVm(VmInstanceInventory inv, ErrorCode reason) {
        List<CreateVmState> states = vmCreateHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.fail(inv, reason));
        }
    }

    @Override
    @Deferred
    public String preStopVm(VmInstanceInventory inv) {
        List<StopVmState> states;
        synchronized (vmStopHookSuppliers) {
            if (vmStopHookSuppliers.isEmpty()) {
                return null;
            }

            states = vmStopHookSuppliers.stream().map(s -> new StopVmState(s.get())).collect(Collectors.toList());
        }

        vmStopHooks.put(inv.getUuid(), states);
        Defer.guard(() -> vmStopHooks.remove(inv.getUuid()));

        states.forEach(s -> s.pre(inv));

        return null;
    }

    @Override
    public void beforeStopVm(VmInstanceInventory inv) {
        List<StopVmState> states = vmStopHooks.get(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.before(inv));
        }
    }

    @Override
    public void afterStopVm(VmInstanceInventory inv) {
        List<StopVmState> states = vmStopHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.after(inv));
        }
    }

    @Override
    public void failedToStopVm(VmInstanceInventory inv, ErrorCode reason) {
        List<StopVmState> states = vmStopHooks.remove(inv.getUuid());
        if (states != null) {
            states.forEach(s -> s.fail(inv, reason));
        }
    }

    @Override
    public void hookStartEvent(Supplier<VmInstanceStartHook> supplier) {
        vmStartHookSuppliers.add(supplier);
    }

    @Override
    public void hookStopEvent(Supplier<VmInstanceStopHook> supplier) {
        vmStopHookSuppliers.add(supplier);
    }

    @Override
    public void hookRebootEvent(Supplier<VmInstanceRebootHook> supplier) {
        vmRebootHookSuppliers.add(supplier);
    }

    @Override
    public void hookDestroyEvent(Supplier<VmInstanceDestroyHook> supplier) {
        vmDestroyHookSuppliers.add(supplier);
    }

    @Override
    public void hookCreateEvent(Supplier<VmInstanceCreateHook> supplier) {
        vmCreateHookSuppliers.add(supplier);
    }
}
