package org.zstack.header.host;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface HypervisorFactory {
    HostVO createHost(HostVO vo, AddHostMessage msg);

    Host getHost(HostVO vo);

    HypervisorType getHypervisorType();

    HostInventory getHostInventory(HostVO vo);

    HostInventory getHostInventory(String uuid);

    /**
     * <p>New added hosts need to check properties
     * during the process of adding to the cluster.
     * 
     * <p>The items that need to be checked are as follows:
     * 
     * <li><b>Host operating system</b>
     *    <br/>to ensure that the operating system of this host
     *    matches that of other hosts in the cluster.
     * </li>
     * </p>
     */
    default ErrorCode checkNewAddedHost(HostVO vo) {
        return null;
    }

    default HostOperationSystem getHostOS(String uuid) {
        throw new CloudRuntimeException(
                String.format("Obtaining operation system of %s host is not supported", getHypervisorType()));
    }

    default Map<String, HostOperationSystem> getHostOsMap(Collection<String> hostUuidList) {
        return hostUuidList.stream().collect(Collectors.toMap(Function.identity(), this::getHostOS));
    }

    default boolean supportGetHostOs() {
        return false;
    }
}
