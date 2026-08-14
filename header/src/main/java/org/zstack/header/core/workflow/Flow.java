package org.zstack.header.core.workflow;

import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface Flow {
    void run(FlowTrigger trigger, Map data);

    void rollback(FlowRollback trigger, Map data);

    default boolean skip(Map data) {
        return false;
    }

    /**
     * @since zsv 4.10.20
     */
    default String name() {
        String innerName = FieldUtils.getFieldValue("__name__", this);
        if (innerName != null && !innerName.trim().isEmpty()) {
            return innerName;
        }
        return String.format("%s", this.getClass().getSimpleName());
    }

    @SuppressWarnings("rawtypes")
    public static class FlowBuilder {
        public final String flowName;
        private Predicate<Map> skipPredicate;
        private BiConsumer<FlowTrigger, Map> triggerConsumer;
        private BiConsumer<FlowRollback, Map> rollbackConsumer;

        private FlowBuilder(String flowName) {
            DebugUtils.Assert(flowName != null, "flowName should not be null");
            this.flowName = flowName;
        }

        public FlowBuilder withSkipPredicate(Predicate<Map> predicate) {
            DebugUtils.Assert(predicate != null, "skipPredicate of FlowBuilder should not be null");
            this.skipPredicate = predicate;
            return this;
        }

        public FlowBuilder skipIf(Predicate<Map> predicate) {
            return withSkipPredicate(predicate);
        }

        public FlowBuilder runIf(Predicate<Map> predicate) {
            DebugUtils.Assert(predicate != null, "predicate of FlowBuilder.runIf() should not be null");
            return withSkipPredicate(predicate.negate());
        }

        public FlowBuilder handle(BiConsumer<FlowTrigger, Map> consumer) {
            DebugUtils.Assert(consumer != null, "consumer of FlowBuilder.handle() should not be null");
            this.triggerConsumer = consumer;
            return this;
        }

        public FlowBuilder handle(Consumer<FlowTrigger> consumer) {
            DebugUtils.Assert(consumer != null, "consumer of FlowBuilder.handle() should not be null");
            this.triggerConsumer = (trigger, data) -> consumer.accept(trigger);
            return this;
        }

        public FlowBuilder rollback(BiConsumer<FlowRollback, Map> consumer) {
            DebugUtils.Assert(consumer != null, "consumer of FlowBuilder.rollback() should not be null");
            this.rollbackConsumer = consumer;
            return this;
        }

        public FlowBuilder rollback(Consumer<FlowRollback> consumer) {
            DebugUtils.Assert(consumer != null, "consumer of FlowBuilder.rollback() should not be null");
            this.rollbackConsumer = (trigger, data) -> consumer.accept(trigger);
            return this;
        }

        public Flow build() {
            DebugUtils.Assert(triggerConsumer != null, "handle() must be called before build()");
            Predicate<Map> skipPredicateSnapshot = skipPredicate;
            BiConsumer<FlowTrigger, Map> triggerConsumerSnapshot = triggerConsumer;
            BiConsumer<FlowRollback, Map> rollbackConsumerSnapshot = rollbackConsumer;

            return new Flow() {
                @Override
                public boolean skip(Map data) {
                    if (skipPredicateSnapshot == null) {
                        return false;
                    }
                    return skipPredicateSnapshot.test(data);
                }

                @Override
                public void run(FlowTrigger trigger, Map data) {
                    triggerConsumerSnapshot.accept(trigger, data);
                }

                @Override
                public void rollback(FlowRollback trigger, Map data) {
                    if (rollbackConsumerSnapshot == null) {
                        trigger.rollback();
                    } else {
                        rollbackConsumerSnapshot.accept(trigger, data);
                    }
                }

                @Override
                public String name() {
                    return flowName;
                }

                @Override
                public String toString() {
                    return name();
                }
            };
        }
    }

    public static FlowBuilder of(String flowName) {
        return new FlowBuilder(flowName);
    }
}
