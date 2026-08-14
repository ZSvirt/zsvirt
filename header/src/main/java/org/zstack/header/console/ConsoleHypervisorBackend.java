package org.zstack.header.console;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HypervisorType;
import org.zstack.header.vm.VmInstanceInventory;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 9:04 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleHypervisorBackend {
    HypervisorType getConsoleBackendHypervisorType();

    void generateConsoleUrl(VmInstanceInventory vm, ReturnValueCompletion<ConsoleUrl> complete);
}
