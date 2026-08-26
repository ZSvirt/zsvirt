package org.zstack.testlib.model.fsm

import org.zstack.testlib.FSMCase

import java.util.concurrent.ConcurrentHashMap

class FiniteStateMachineTesting {
    public final FiniteStateMachineTestingSpec spec
    public final DirectedGraphTraversal graph
    public final FSMCase baseCase

    public final List<Closure> afterEnvCreateHandlers = []
    public final List<Closure> eachStateCallbacks = []

    private final Map<String, Object> contextInCurrentRound = new ConcurrentHashMap<>()
    List<FSMEvent> events = []
    int round
    int currentIndex
    FSMState currentState

    FiniteStateMachineTesting(FiniteStateMachineTestingSpec spec, FSMCase baseCase) {
        this.spec = spec
        this.baseCase = baseCase
        this.graph = DirectedGraphTraversal.create(spec)
    }

    void run(int round) {
        this.round = round
        contextInCurrentRound.clear()
        events = graph.paths[round].event
        afterEnvCreateHandlers.each { it() }

        currentState = spec.states.find { it.name == events[0].fromState }
        checkCurrentState()

        for (currentIndex = 0; currentIndex < events.size(); currentIndex++) {
            def event = events[currentIndex]
            baseCase.logger.info(printCurrentPosition())

            def action = spec.actions.find { it.name == event.action }
            action.run.call()

            currentState = spec.states.find { it.name == event.toState }
            eachStateCallbacks.each { it() }
            checkCurrentState()
        }
    }

    String printCurrentPosition() {
        def builder = new StringBuilder()

        builder.append("Current Position:\n=========\n").append(" in Sequence #").append(round).append("\n\n")

        builder.append("Events in Sequence:\n=========\n")
        for (i in 0..<events.size()) {
            builder.append(" ").append(i).append(".\t").append(events[i])
            if (i == currentIndex) {
                builder.append("\t\t <-- CURRENT")
            }
            builder.append("\n")
        }

        builder.append("\n")

        builder.append("Contexts:\n=========\n")
        builder.append(" size = ").append(contextInCurrentRound.size()).append("\n")

        def items = [] as List<String>
        for (def entry in contextInCurrentRound) {
            def value = entry.value
            items.add(value == null ? "${entry.key} = null" : "${entry.key} = [${value.class.simpleName}] ${value}")
        }

        items.sort()
        items.each {
            builder.append(" * ").append(it).append('\n')
        }

        return builder.toString()
    }

    void checkCurrentState() {
        assert currentState != null

        try {
            currentState.checkers.each {
                it()
            }
        } catch (Throwable e) {
            baseCase.logger.warn(e.class.simpleName + " occur when check state: " + currentState)
            baseCase.logger.warn(printCurrentPosition())
            throw e
        }
    }

    void save(String key, Object value) {
        contextInCurrentRound.put(key, value)
    }

    Object get(String key) {
        return contextInCurrentRound.get(key)
    }

    Object getNotNull(String key) {
        def value = get(key)
        assert value != null : "failed to find value by key: " + key
        return value
    }
}
