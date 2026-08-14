package org.zstack.console;

import org.zstack.header.console.ConsoleBackend;
import org.zstack.header.console.ConsoleHypervisorBackend;
import org.zstack.header.host.HypervisorType;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 11:50 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleManager {
    ConsoleHypervisorBackend getHypervisorConsoleBackend(HypervisorType type);

    ConsoleBackend getConsoleBackend();
}
