package org.zstack.simulator2;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.rest.RESTFacade;

/**
 * Created by xing5 on 2017/9/16.
 */
public class SimulatorManager {
    private Simulator simulator;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private RESTFacade restf;

    void init() {
        Simulator.setVersion(dbf.getDbVersion());
        Simulator.setSendCommandUrl(restf.getSendCommandUrl());
        if (!SimulatorGlobalProperty.SIMULATOR_CONFIGURATION_FILE.equals("")) {
            simulator = new Simulator();
        } else {
            simulator = new Simulator(SimulatorGlobalProperty.SIMULATOR_CONFIGURATION_FILE);
        }
    }

    public SimulatorManager() {
    }
}
