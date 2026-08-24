package org.zstack.testlib

import org.zstack.core.db.Q
import org.zstack.testlib.model.fsm.FSMAction
import org.zstack.testlib.model.fsm.FSMEvent
import org.zstack.testlib.model.fsm.FSMState
import org.zstack.testlib.model.fsm.FiniteStateMachineTesting
import org.zstack.testlib.model.fsm.FiniteStateMachineTestingSpec
import org.zstack.utils.gson.JSONObjectUtil

import java.util.function.Predicate

abstract class FSMCase extends Test implements Case {
    protected FiniteStateMachineTestingSpec spec = new FiniteStateMachineTestingSpec()
    protected FiniteStateMachineTesting testing
    protected EnvSpec env
    protected List<Closure> afterEnvCreateHandlers = []
    protected List<Closure> beforeEnvDeleteHandlers = []
    protected List<Closure> eachStateCallbacks = []
    protected boolean needClean

    @Override
    protected void configProperty() {
        configSkipMNExit()
    }

    @Override
    final void run() {
        envCreateTime = 0
        testRunTime = 0
        cleanEnvTime = 0
        needClean = true

        try {
            environment()
        } catch (Throwable t) {
            logger.warn("a sub case [${this.class}] fails, (in environment stage)\n${t.message}", t)
            collectErrorLog()
            throw t
        }

        long startTestTime = System.currentTimeMillis()
        test()
        testRunTime = System.currentTimeMillis() - startTestTime
    }

    final void test() {
        testing = spec.build(this)
        testing.eachStateCallbacks.addAll(this.eachStateCallbacks)
        testing.afterEnvCreateHandlers.addAll(this.afterEnvCreateHandlers)

        logger.info(testing.graph.toString())
        def paths = testing.graph.paths

        for (i in 0..<paths.length) {
            logger.info("run sequence #" + i)
            everyRound(i)
        }
    }

    @Override
    final void clean() {
        Throwable thrown = null
        try {
            beforeEnvDeleteHandlers.each { it() }
        } catch (Throwable t) {
            logger.warn("failed to run beforeEnvDeleteHandlers", t)
            thrown = t
        }
        try {
            env.delete()
        } catch (Throwable t) {
            logger.warn("failed to delete env", t)
            if (thrown != null) {
                thrown.addSuppressed(t)
            } else {
                thrown = t
            }
        }
        try {
            methodsOnClean.each { it() }
        } catch (Throwable t) {
            logger.warn("failed to run methodsOnClean", t)
            if (thrown != null) {
                thrown.addSuppressed(t)
            } else {
                thrown = t
            }
        } finally {
            methodsOnClean.clear()
        }
        if (thrown != null) {
            throw thrown
        }
    }

    void everyRound(int round) {
        Throwable thrown = null
        try {
            env.create {
                testing.run(round)
            }
        } catch (Throwable t) {
            thrown = t
        } finally {
            if (needClean) {
                String context = null
                try {
                    context = testing.printCurrentPosition()
                } catch (Throwable t) {
                    logger.warn("failed to print current position", t)
                    if (thrown != null) {
                        thrown.addSuppressed(t)
                    } else {
                        thrown = t
                    }
                }

                logger.info("start cleanup for case ${this.class}")
                try {
                    long startCleanTime = System.currentTimeMillis()
                    clean()
                    cleanEnvTime = System.currentTimeMillis() - startCleanTime
                    logger.info("create env spend: ${envCreateTime}, test run spend: ${testRunTime}, clean env spend: ${cleanEnvTime}")
                    if (context != null) {
                        logger.info("context details: " + context)
                    }
                } catch (Throwable t) {
                    collectErrorLog()
                    if (thrown != null) {
                        thrown.addSuppressed(t)
                    } else {
                        thrown = t
                    }
                }
            }
        }

        if (thrown != null) {
            throw thrown
        }
    }

    void state(String name, @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = FSMState.class) Closure c) {
        def state = new FSMState(name)
        c.delegate = state
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        spec.states << state
    }

    void action(String name, @DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = FSMAction.class) Closure c) {
        def action = new FSMAction(name)
        c.delegate = action
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        spec.actions << action
    }

    void event(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = FSMEvent.class) Closure c) {
        def event = new FSMEvent()
        c.delegate = event
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c()
        spec.events << event
    }

    /**
     * calling when current state is null (before set state = init state)
     */
    void afterEnvCreate(Closure c) {
        this.afterEnvCreateHandlers << c
    }

    void beforeEnvDelete(Closure c) {
        this.beforeEnvDeleteHandlers << c
    }

    void beforeAnyState(Closure c) {
        this.eachStateCallbacks << c
    }

    public <T> void printTable(Class<T> table) {
        printTable(table, { any -> true })
    }

    public <T> void printTable(Class<T> table, Predicate<T> filterRecordsShouldInclude) {
        def list = Q.New(table).list() as List<T>
        list.removeIf { !filterRecordsShouldInclude.test(it) }
        def texts = list.collect { JSONObjectUtil.toJsonString(it) }

        logger.info("""Print Table: ${table.simpleName}
=========================
    size = ${list.size()}

    ${texts.join("\n    ")}
""".toString())
    }

    void save(String key, Object value) {
        testing.save(key, value)
    }

    Object get(String key) {
        return testing.get(key)
    }

    Object getNotNull(String key) {
        return testing.getNotNull(key)
    }

    @Override
    protected void runSubCases() {
        throw new Exception("runSubCases() cannot be called in a FSMCase")
    }
}
