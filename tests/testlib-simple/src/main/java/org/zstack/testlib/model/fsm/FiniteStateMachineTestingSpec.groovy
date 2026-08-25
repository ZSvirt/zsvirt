package org.zstack.testlib.model.fsm

import org.zstack.testlib.FSMCase

class FiniteStateMachineTestingSpec {
    final List<FSMState> states = []
    final List<FSMAction> actions = []
    final List<FSMEvent> events = []

    FiniteStateMachineTesting build(FSMCase baseCase) {
        Set<String> validStateName = new HashSet<>(states.name)
        Set<String> validActionName = new HashSet<>(actions.name)

        actions.each { action ->
            action.events.each {
                def event = new FSMEvent()
                event.action = action.name
                event.fromState = it.first()
                event.toState = it.second()
                this.events << event
            }
        }

        for (def event in events) {
            assert validStateName.contains(event.fromState) : "state ${event.fromState} not define"
            assert validStateName.contains(event.toState) : "state ${event.toState} not define"
            assert validActionName.contains(event.action) : "action ${event.action} not define"
        }

        return new FiniteStateMachineTesting(this, baseCase)
    }
}
