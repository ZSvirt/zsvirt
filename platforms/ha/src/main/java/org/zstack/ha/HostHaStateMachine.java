package org.zstack.ha;

import org.zstack.core.Platform;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.ha.header.HostHaState;
import org.zstack.ha.header.HostHaStateEvent;

public class HostHaStateMachine {
    protected final static StateMachine<HostHaState, HostHaStateEvent> states;

    static {
        states = Platform.<HostHaState, HostHaStateEvent>createStateMachine();
        states.addTranscation(HostHaState.None, HostHaStateEvent.syncNotExist, HostHaState.None);
        states.addTranscation(HostHaState.None, HostHaStateEvent.syncExist, HostHaState.Done);
        states.addTranscation(HostHaState.None, HostHaStateEvent.setup, HostHaState.Setuping);
        states.addTranscation(HostHaState.None, HostHaStateEvent.cancel, HostHaState.Cancelling);
        states.addTranscation(HostHaState.Done, HostHaStateEvent.cancel, HostHaState.Cancelling);
        states.addTranscation(HostHaState.Done, HostHaStateEvent.syncExist, HostHaState.Done);
        states.addTranscation(HostHaState.Done, HostHaStateEvent.syncNotExist, HostHaState.SetupFail);
        states.addTranscation(HostHaState.Done, HostHaStateEvent.setup, HostHaState.Setuping);
        states.addTranscation(HostHaState.Setuping, HostHaStateEvent.setupDone, HostHaState.Done);
        states.addTranscation(HostHaState.Setuping, HostHaStateEvent.setupFail, HostHaState.SetupFail);
        states.addTranscation(HostHaState.Setuping, HostHaStateEvent.cancel, HostHaState.CancelFail);
        states.addTranscation(HostHaState.Setuping, HostHaStateEvent.setup, HostHaState.Setuping);
        states.addTranscation(HostHaState.SetupFail, HostHaStateEvent.syncExist, HostHaState.Done);
        states.addTranscation(HostHaState.SetupFail, HostHaStateEvent.setup, HostHaState.Setuping);
        states.addTranscation(HostHaState.SetupFail, HostHaStateEvent.cancel, HostHaState.Cancelling);
        states.addTranscation(HostHaState.SetupFail, HostHaStateEvent.syncNotExist, HostHaState.SetupFail);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.cancelDone, HostHaState.None);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.cancelFail, HostHaState.CancelFail);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.setup, HostHaState.SetupFail);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.cancel, HostHaState.Cancelling);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.syncNotExist, HostHaState.None);
        states.addTranscation(HostHaState.Cancelling, HostHaStateEvent.syncExist, HostHaState.CancelFail);
        states.addTranscation(HostHaState.CancelFail, HostHaStateEvent.cancel, HostHaState.Cancelling);
        states.addTranscation(HostHaState.CancelFail, HostHaStateEvent.setup, HostHaState.Setuping);
        states.addTranscation(HostHaState.CancelFail, HostHaStateEvent.syncNotExist, HostHaState.None);
        states.addTranscation(HostHaState.CancelFail, HostHaStateEvent.syncExist, HostHaState.CancelFail);
    }

    public static HostHaState getState(HostHaState curr, HostHaStateEvent evt) {
        return states.getNextState(curr, evt);
    }
}
