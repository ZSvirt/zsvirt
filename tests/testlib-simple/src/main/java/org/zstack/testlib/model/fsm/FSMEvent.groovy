package org.zstack.testlib.model.fsm

class FSMEvent {
    public String fromState
    public String toState
    public String action

    @Override
    String toString() {
        return "[${fromState}] -> <${action}> -> [${toState}]"
    }
}
