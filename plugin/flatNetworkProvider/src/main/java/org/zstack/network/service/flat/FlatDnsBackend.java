package org.zstack.network.service.flat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.*;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.MessageReply;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.service.*;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by frank on 9/15/2015.
 */
public class FlatDnsBackend implements NetworkServiceDnsBackend {
    private static final CLogger logger = Utils.getLogger(FlatDnsBackend.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public NetworkServiceProviderType getProviderType() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE;
    }

    @Override
    public void addDns(final L3NetworkInventory l3, final List<String> dns, final Completion completion) {
        /** dns will not install to vm immediately, but change the dnsmasq
         * once vm reboot or use restart dhcp process, host route will be installed to vm */
        L3NetworkUpdateDhcpMsg msg = new L3NetworkUpdateDhcpMsg();
        msg.setL3NetworkUuid(l3.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void removeDns(final L3NetworkInventory l3, List<String> dns, Completion completion) {
        /** dns will not install to vm immediately, but change the dnsmasq
         * once vm reboot or use restart dhcp process, host route will be installed to vm */
        L3NetworkUpdateDhcpMsg msg = new L3NetworkUpdateDhcpMsg();
        msg.setL3NetworkUuid(l3.getUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, FlatNetworkServiceConstant.SERVICE_ID, l3.getUuid());
        bus.send(msg, new CloudBusCallBack(completion) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    completion.success();
                } else {
                    completion.fail(reply.getError());
                }
            }
        });
    }

    @Override
    public void applyDnsService(List<DnsStruct> dnsStructList, VmInstanceSpec spec, Completion completion) {
        completion.success();
    }

    @Override
    public void releaseDnsService(List<DnsStruct> dnsStructsList, VmInstanceSpec spec, NoErrorCompletion completion) {
        completion.done();
    }
}
