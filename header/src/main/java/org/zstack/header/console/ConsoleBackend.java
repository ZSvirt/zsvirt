package org.zstack.header.console;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.Message;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 12:04 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleBackend {
    String getConsoleBackendType();

    void deleteConsoleSession(ConsoleProxyInventory consoleProxy, Completion completion);

    void grantConsoleAccess(SessionInventory session, VmInstanceInventory vm, ReturnValueCompletion<ConsoleInventory> complete);

    void deleteConsoleSession(VmInstanceInventory vm, Completion completion);

    void deleteConsoleSession(SessionInventory session, NoErrorCompletion completion);

    String returnServiceIdForConsoleAgentMsg(ConsoleProxyAgentMessage msg, String agentUuid);

    void handleMessage(Message msg);

    void deactivateConsoleProxy(VmInstanceInventory vmInventory, Completion completion);

    void updateConsoleProxy(VmInstanceInventory vm, ConsoleProxyVO consoleProxy, ReturnValueCompletion<ConsoleInventory> consoleInventoryReturnValueCompletion);
}
