package org.zstack.core.statemachine;

public interface StateMachineListener<T extends Enum<T>, K extends Enum<K>> {
    void before(T old, K evt, T next, Object...args);
    
    void after (T prev, K evt, T curr, Object...args);
}
