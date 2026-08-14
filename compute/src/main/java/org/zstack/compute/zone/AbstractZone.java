package org.zstack.compute.zone;

import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.header.zone.Zone;
import org.zstack.header.zone.ZoneState;
import org.zstack.header.zone.ZoneStateEvent;

abstract class AbstractZone implements Zone {
	private static DatabaseFacade dbf = Platform.getComponentLoader().getComponent(DatabaseFacade.class);
	private final static StateMachine<ZoneState, ZoneStateEvent> stateMachine;
	
    static {
        stateMachine = Platform.<ZoneState, ZoneStateEvent>createStateMachine();
        stateMachine.addTranscation(ZoneState.Enabled, ZoneStateEvent.disable, ZoneState.Disabled);
        stateMachine.addTranscation(ZoneState.Enabled, ZoneStateEvent.enable, ZoneState.Enabled);
        stateMachine.addTranscation(ZoneState.Disabled, ZoneStateEvent.disable, ZoneState.Disabled);
        stateMachine.addTranscation(ZoneState.Disabled, ZoneStateEvent.enable, ZoneState.Enabled); 
    }
    
	static ZoneState getNextState(ZoneState curr, ZoneStateEvent event) {
		return stateMachine.getNextState(curr, event);
	}

    protected abstract void deleteHook();
}
