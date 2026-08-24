package org.zstack.testlib.model.fsm

class FSMState {
    public final String name
    public boolean initial
    public List<Closure> checkers = []

    FSMState(String name) {
        this.name = name
    }

    FSMState markAsInitialState() {
        initial = true
        return this
    }

    FSMState checker(Closure c) {
        checkers << c
        return this
    }

    @Override
    String toString() {
        return name
    }
}
