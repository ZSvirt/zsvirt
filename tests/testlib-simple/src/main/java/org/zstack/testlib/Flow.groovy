package org.zstack.testlib

/**
 * Created by xing5 on 2017/2/18.
 */
class Flow {
    static class Engine {
        private Closure doneHandler
        private Closure errorHandler
        private Closure unexpectedFailureHandler
        private Iterator<FlowEntry> iterator
        private List<FlowEntry> flowEntries = []
        private List<FlowEntry> executedFlows = []
        private String errorMessage

        class FlowEntry {
            FlowEntry(Closure run) {
                this.run = run
            }

            private Closure run
            private Closure rollback

            void onError(Closure cl) {
                rollback = cl
            }
        }

        class RunExtension {
            Engine subFlow(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Engine.class) Closure c) {
                def e = new Engine()
                def code = c.rehydrate(e, this, this)
                code.resolveStrategy = Closure.DELEGATE_FIRST
                code()
                return e
            }
        }

        FlowEntry step(Closure run) {
            def entry = new FlowEntry(run)
            flowEntries.add(entry)
            return entry
        }

        void done(Closure cl) {
            doneHandler = cl
        }

        void error(Closure cl) {
            errorHandler = cl
        }

        void run() {
            iterator  = flowEntries.iterator()
            next()
        }

        Engine subFlow(Closure cl) {
            return create(cl)
        }

        void onUnexpectedFailure(Closure cl) {
            unexpectedFailureHandler = cl
        }

        private void runFlow(FlowEntry entry) {
            try {
                executedFlows.add(entry)
                if (entry.run.maximumNumberOfParameters <= 1) {
                    entry.run({ next() })
                } else {
                    entry.run({ next() }, { fail(it as String) })
                }
            } catch (Throwable t) {
                fail(t.message)
            }
        }

        private void callErrorHandler() {
            if (errorHandler != null) {
                try {
                    errorHandler(errorMessage)
                } catch (Throwable t) {
                    if (unexpectedFailureHandler != null) {
                        unexpectedFailureHandler(t.message)
                    }
                }
            }
        }

        private void rollback() {
            if (executedFlows.isEmpty()) {
                callErrorHandler()
                return
            }

            FlowEntry e = executedFlows.pop()
            if (e.rollback != null) {
                try {
                    e.rollback({ rollback() })
                } catch (Throwable t) {
                    println("error happened when rollback a flow, continue, ${t.message}")
                    rollback()
                }
            } else {
                rollback()
            }
        }

        private void fail(String error) {
            errorMessage = error
            rollback()
        }

        private void next() {
            if (!iterator.hasNext()) {
                try {
                    if (doneHandler != null) {
                        doneHandler()
                    }
                } catch (Throwable t) {
                    errorMessage = t.message
                    callErrorHandler()
                }

                return
            }

            FlowEntry entry = iterator.next()
            runFlow(entry)
        }
    }

    static Engine create(@DelegatesTo(strategy = Closure.DELEGATE_FIRST, value = Engine.class) Closure c) {
        def e = new Engine()
        c.resolveStrategy = Closure.DELEGATE_FIRST
        c.delegate = e
        c()
        return e
    }
}
