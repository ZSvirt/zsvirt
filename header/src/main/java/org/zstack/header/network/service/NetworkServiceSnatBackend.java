package org.zstack.header.network.service;

import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 10:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface NetworkServiceSnatBackend {
    NetworkServiceProviderType getProviderType();

    void applySnatService(List<SnatStruct> snatStructList, VmInstanceSpec spec, Completion completion);

    void releaseSnatService(List<SnatStruct> snatStructsList, VmInstanceSpec spec, NoErrorCompletion completion);
}
