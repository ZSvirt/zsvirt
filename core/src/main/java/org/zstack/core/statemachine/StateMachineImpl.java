package org.zstack.core.statemachine;

import org.zstack.header.exception.CloudStateMachineException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StateMachineImpl<T extends Enum<T>, K extends Enum<K>> implements StateMachine<T, K> {
    private Map<T, HashMap<K, T>> _chart = new HashMap<T, HashMap<K, T>>();
    private List<StateMachineListener<T, K>> _listeners = new ArrayList<StateMachineListener<T, K>>();
    private List<StateMachineListener<T, K>> _listenersTmp = new ArrayList<StateMachineListener<T, K>>();
    private static final CLogger _logger = Utils.getLogger(StateMachineImpl.class);

    @Override
    public void addTranscation(T old, K evt, T next) {
        HashMap<K, T> entry = _chart.get(old);
        if (entry == null) {
           entry = new HashMap<K, T>(1); 
           _chart.put(old, entry);
        }
        entry.put(evt, next);
    }

    @Override
    public T getNextState(T old, K evt) {
        HashMap<K, T> entry = _chart.get(old);
        if (entry == null) {
            StringBuilder err = new StringBuilder("Cannot find next state:");
            err.append("[old state: ").append(old).append(",");
            err.append(" state event: ").append(evt).append("]");
            throw new CloudStateMachineException(err.toString());
        }
       
        T next = entry.get(evt);
        if (next == null) {
            StringBuilder err = new StringBuilder("Cannot find next state:");
            err.append("[old state: ").append(old).append(",");
            err.append(" state event: ").append(evt).append("]");
            throw new CloudStateMachineException(err.toString());
        }
        
        return next;
    }

    @Override
    public void addListener(StateMachineListener<T, K> l) {
        synchronized (_listeners) {
            _listeners.add(l);
        }
    }

    @Override
    public void removeListener(StateMachineListener<T, K> l) {
        synchronized (_listeners) {
            _listeners.remove(l);
        }
    }

    @Override
    public void fireBeforeListener(T old, K evt, T next, Object... args) {
        _listenersTmp.clear();
        synchronized (_listeners) {
            _listenersTmp.addAll(_listeners);
        }
        
        for (StateMachineListener<T, K> l : _listenersTmp) {
           try {
              l.before(old, evt, next, args); 
           } catch (Exception e) {
               StringBuilder err = new StringBuilder("Unhandled exception while calling listener: " + l.getClass().getCanonicalName());
               err.append( " before state changing.").append("[").append("current state:" + old).append(" event: " + evt);
               err.append(" next state: " + next).append("]");
               _logger.warn(err.toString(), e);
           }
        }
    }

    @Override
    public void fireAfterListener(T prev, K evt, T curr, Object... args) {
        _listenersTmp.clear();
        synchronized (_listeners) {
            _listenersTmp.addAll(_listeners);
        }
        
        for (StateMachineListener<T, K> l : _listenersTmp) {
           try {
              l.after(prev, evt, curr, args);
           } catch (Exception e) {
               StringBuilder err = new StringBuilder("Unhandled exception while calling listener: " + l.getClass().getCanonicalName());
               err.append( " after state changing.").append("[").append("previous state:" + prev).append(" event: " + evt);
               err.append(" current state: " + curr).append("]");
               _logger.warn(err.toString(), e);
           }
        }
    }
}
