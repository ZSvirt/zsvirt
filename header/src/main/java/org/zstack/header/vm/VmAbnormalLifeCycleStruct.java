package org.zstack.header.vm;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Created by frank on 11/1/2015.
 */
public class VmAbnormalLifeCycleStruct {
    public enum VmAbnormalLifeCycleOperation {
        VmStoppedOnTheSameHost {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return (struct.getOriginalState() == VmInstanceState.Running
                        || struct.getOriginalState() == VmInstanceState.Starting
                        || struct.getOriginalState() == VmInstanceState.NoState
                        || struct.getOriginalState() == VmInstanceState.Unknown)
                        && struct.getCurrentState() == VmInstanceState.Stopped
                        && Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        },
        VmRunningFromIntermediateState {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return VmInstanceState.intermediateStates.contains(struct.getOriginalState())
                        && struct.getCurrentState() == VmInstanceState.Running;
            }
        },
        VmStoppedFromIntermediateState {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return VmInstanceState.intermediateStates.contains(struct.getOriginalState())
                        && struct.getCurrentState() == VmInstanceState.Stopped;
            }
        },
        VmPausedFromUnknownStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Unknown
                        && struct.getCurrentState() == VmInstanceState.Paused
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmRunningFromUnknownStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Unknown
                        && struct.getCurrentState() == VmInstanceState.Running
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmRunningFromUnknownStateHostChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Unknown
                        && struct.getCurrentState() == VmInstanceState.Running
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmStoppedFromUnknownStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Unknown
                        && struct.getCurrentState() == VmInstanceState.Stopped
                        && struct.getOriginalHostUuid() == null
                        && struct.getCurrentHostUuid().equals(struct.getVmLastHostUuid());
            }
        },
        VmStoppedFromPausedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Paused
                        && struct.getCurrentState() == VmInstanceState.Stopped
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmMigrateToAnotherHost {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Running
                        && struct.getOriginalState() == struct.getCurrentState()
                        && !Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        },
        VmRunningOnTheHost {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Stopped
                        && (struct.getCurrentState() == VmInstanceState.Running
                        || struct.getCurrentState() == VmInstanceState.Paused);
            }
        },
        VmRunningFromDestroyed {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Destroyed &&
                        (struct.getCurrentState() == VmInstanceState.Running
                                || struct.getCurrentState() == VmInstanceState.Paused);
            }
        },
        VmPausedFromRunningStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Running
                        && struct.getCurrentState() == VmInstanceState.Paused
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmRunningFromPausedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Paused
                        && struct.getCurrentState() == VmInstanceState.Running
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmPausedFromMigratingStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Migrating
                        && struct.getCurrentState() == VmInstanceState.Paused
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmCrashedFromRunningStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Running
                        && struct.getCurrentState() == VmInstanceState.Crashed
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmRunningFromCrashedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Crashed
                        && struct.getCurrentState() == VmInstanceState.Running
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmStoppedFromCrashedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Crashed
                        && struct.getCurrentState() == VmInstanceState.Stopped
                        && struct.getCurrentHostUuid().equals(struct.getOriginalHostUuid());
            }
        },
        VmNoStateFromRunningStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Running
                        && struct.getCurrentState() == VmInstanceState.NoState
                        && Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        },
        VmNoStateFromCrashedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Crashed
                        && struct.getCurrentState() == VmInstanceState.NoState
                        && Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        },
        VmNoStateFromIntermediateState {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return VmInstanceState.intermediateStates.contains(struct.getOriginalState())
                        && struct.getCurrentState() == VmInstanceState.NoState
                        && Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        },
        VmNoStateFromStoppedStateHostNotChanged {
            @Override
            boolean match(VmAbnormalLifeCycleStruct struct) {
                return struct.getOriginalState() == VmInstanceState.Stopped
                        && struct.getCurrentState() == VmInstanceState.NoState
                        && Objects.equals(struct.getCurrentHostUuid(), struct.getOriginalHostUuid());
            }
        };

        abstract boolean match(VmAbnormalLifeCycleStruct struct);
    }

    public static VmAbnormalLifeCycleOperation getVmAbnormalLifeCycleOperationFromStruct(
            VmAbnormalLifeCycleStruct struct) {
        return Arrays.stream(VmAbnormalLifeCycleOperation.values())
                .filter(operation -> operation.match(struct))
                .findFirst()
                .orElse(null);
    }

    public static VmAbnormalLifeCycleOperation getVmAbnormalLifeCycleOperation(
            String lastHostUuid,
            VmInstanceState originalState,
            VmInstanceState currentState,
            String originalHostUuid,
            String currentHostUuid) {
        VmAbnormalLifeCycleStruct struct = new VmAbnormalLifeCycleStruct();
        struct.setCurrentHostUuid(currentHostUuid);
        struct.setCurrentState(currentState);
        struct.setOriginalHostUuid(originalHostUuid);
        struct.setOriginalState(originalState);
        struct.setVmLastHostUuid(lastHostUuid);
        return getVmAbnormalLifeCycleOperationFromStruct(struct);
    }

    private VmAbnormalLifeCycleOperation operation;
    private VmInstanceInventory vmInstance;
    private String originalHostUuid;
    private String currentHostUuid;
    private VmInstanceState originalState;
    private VmInstanceState currentState;

    private String vmLastHostUuid;

    public VmAbnormalLifeCycleOperation getOperation() {
        return operation;
    }

    public void setOperation(VmAbnormalLifeCycleOperation operation) {
        this.operation = operation;
    }

    public VmInstanceInventory getVmInstance() {
        return vmInstance;
    }

    public void setVmInstance(VmInstanceInventory vmInstance) {
        this.vmInstance = vmInstance;
    }

    public String getOriginalHostUuid() {
        return originalHostUuid;
    }

    public void setOriginalHostUuid(String originalHostUuid) {
        this.originalHostUuid = originalHostUuid;
    }

    public String getCurrentHostUuid() {
        return currentHostUuid;
    }

    public void setCurrentHostUuid(String currentHostUuid) {
        this.currentHostUuid = currentHostUuid;
    }

    public VmInstanceState getOriginalState() {
        return originalState;
    }

    public void setOriginalState(VmInstanceState originalState) {
        this.originalState = originalState;
    }

    public VmInstanceState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(VmInstanceState currentState) {
        this.currentState = currentState;
    }

    public String getVmLastHostUuid() {
        return vmLastHostUuid;
    }

    public void setVmLastHostUuid(String vmLastHostUuid) {
        this.vmLastHostUuid = vmLastHostUuid;
    }
}
