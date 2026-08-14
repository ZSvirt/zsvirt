package org.zstack.header.console;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.vm.VmInstanceInventory;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 8:44 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ConsoleProxy {
    void establishProxy(VmInstanceInventory vm, ReturnValueCompletion<ConsoleProxyInventory> completion);

    void checkAvailability(ReturnValueCompletion<Boolean> completion);

    void deleteProxy(VmInstanceInventory vm, Completion completion);

    void deleteProxy(ConsoleProxyInventory proxy, Completion completion);
}
