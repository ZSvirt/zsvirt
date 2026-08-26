package org.zstack.testlib.model.fsm

import org.zstack.utils.data.Pair

class FSMAction {
    public final String name
    public Closure run = {}
    public List<Pair<String, String>> events = []

    FSMAction(String name) {
        this.name = name
    }

    void registerEvent(String fromState, String toState) {
        events << new Pair<>(fromState, toState)
    }
}
