package org.zstack.simulator.consoleproxy;

import org.zstack.header.console.ConsoleProxyCommands;
import org.zstack.header.console.ConsoleProxyCommands.DeleteProxyCmd;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConsoleProxySimulatorConfig {
    public volatile boolean availableSuccess = true;
    public volatile boolean isAvailable = true;
    public volatile Integer proxyPort = 5900;
    public volatile boolean proxySuccess = true;
    public List<DeleteProxyCmd> deleteProxyCmdList = new ArrayList<DeleteProxyCmd>();
    public List<ConsoleProxyCommands.PingCmd> pingCmdList = new ArrayList<ConsoleProxyCommands.PingCmd>();
    public volatile boolean pingSuccess = true;
}
