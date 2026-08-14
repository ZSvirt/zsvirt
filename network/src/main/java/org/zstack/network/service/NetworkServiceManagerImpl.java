package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostAfterConnectedExtensionPoint;
import org.zstack.header.host.HostDeleteExtensionPoint;
import org.zstack.header.host.HostException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.network.NetworkException;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l3.*;
import org.zstack.header.network.service.*;
import org.zstack.header.network.service.NetworkServiceExtensionPoint.NetworkServiceExtensionPosition;
import org.zstack.header.vm.*;
import org.zstack.query.QueryFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;

public class NetworkServiceManagerImpl extends AbstractService implements NetworkServiceManager, PreVmInstantiateResourceExtensionPoint,
        VmReleaseResourceExtensionPoint, PostVmInstantiateResourceExtensionPoint, ReleaseNetworkServiceOnDetachingNicExtensionPoint,
        InstantiateResourceOnAttachingNicExtensionPoint, HostAfterConnectedExtensionPoint, HostDeleteExtensionPoint {
	private static final CLogger logger = Utils.getLogger(NetworkServiceManagerImpl.class);

	@Autowired
	private CloudBus bus;
	@Autowired
	private PluginRegistry pluginRgty;
	@Autowired
	private DatabaseFacade dbf;
    @Autowired
    private QueryFacade qf;

	private final Map<String, NetworkServiceProviderFactory> providerFactories = new HashMap<String, NetworkServiceProviderFactory>();
	private final Map<String, ApplyNetworkServiceExtensionPoint> providerExts = new HashMap<String, ApplyNetworkServiceExtensionPoint>();
	private Set<String> supportedVmTypes = new HashSet<>();
    private List<NetworkServiceExtensionPoint> nsExts = new ArrayList<NetworkServiceExtensionPoint>();
    private List<NetworkServiceHostExtensionPoint> nsHostExts = new ArrayList<NetworkServiceHostExtensionPoint>();


	private void populateExtensions() {
        for (NetworkServiceProviderFactory extp : pluginRgty.getExtensionList(NetworkServiceProviderFactory.class)) {
            NetworkServiceProviderFactory old = providerFactories.get(extp.getType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate NetworkServiceProviderFactory[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getType()));
            }
            providerFactories.put(extp.getType().toString(), extp);
        }

        for (ApplyNetworkServiceExtensionPoint extp : pluginRgty.getExtensionList(ApplyNetworkServiceExtensionPoint.class)) {
            ApplyNetworkServiceExtensionPoint old = providerExts.get(extp.getProviderType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate ApplyNetworkServiceExtensionPoint[%s, %s] for type[%s]",
                        extp.getClass().getName(), old.getClass().getName(), extp.getProviderType()));
            }
            providerExts.put(extp.getProviderType().toString(), extp);
        }

        nsExts = pluginRgty.getExtensionList(NetworkServiceExtensionPoint.class);
        nsHostExts = pluginRgty.getExtensionList(NetworkServiceHostExtensionPoint.class);
	}

	private NetworkServiceProviderFactory getProviderFactory(String type) {
		NetworkServiceProviderFactory factory = providerFactories.get(type);
		if (factory == null) {
			throw new IllegalArgumentException(String.format("unable to find NetworkServiceProviderFactory for type[%s]", type));
		}
		return factory;
	}
	
	@Override
    @MessageSafe
	public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
	}

	private void handleLocalMessage(Message msg) {
	    bus.dealWithUnknownMessage(msg);
	}

	private void handleApiMessage(APIMessage msg) {
		if (msg instanceof APIAttachNetworkServiceProviderToL2NetworkMsg) {
			handle((APIAttachNetworkServiceProviderToL2NetworkMsg) msg);
		} else if (msg instanceof APIDetachNetworkServiceProviderFromL2NetworkMsg) {
			handle((APIDetachNetworkServiceProviderFromL2NetworkMsg)msg);
		} else if (msg instanceof APIQueryNetworkServiceProviderMsg) {
		    handle((APIQueryNetworkServiceProviderMsg) msg);
        } else if (msg instanceof APIGetNetworkServiceTypesMsg) {
            handle((APIGetNetworkServiceTypesMsg) msg);
		} else {
			bus.dealWithUnknownMessage(msg);
		}
	}

    private void handle(APIGetNetworkServiceTypesMsg msg) {
        Map<String, List<String>> ret = new HashMap<String, List<String>>();

        List<NetworkServiceTypeVO> types = dbf.listAll(NetworkServiceTypeVO.class);
        for (NetworkServiceTypeVO vo : types) {
            List<String> providers = ret.computeIfAbsent(vo.getType(), k -> new ArrayList<String>());
            providers.add(vo.getNetworkServiceProviderUuid());
        }

        APIGetNetworkServiceTypesReply reply = new APIGetNetworkServiceTypesReply();
        reply.setServiceAndProviderTypes(ret);

        bus.reply(msg, reply);
    }

    private void handle(APIQueryNetworkServiceProviderMsg msg) {
	    List<NetworkServiceProviderInventory> invs = qf.query(msg, NetworkServiceProviderInventory.class);
	    APIQueryNetworkServiceProviderReply reply = new APIQueryNetworkServiceProviderReply();
	    reply.setInventories(invs);
	    bus.reply(msg, reply);
    }

	private void handle(APIDetachNetworkServiceProviderFromL2NetworkMsg msg) {
		NetworkServiceProviderVO vo = dbf.findByUuid(msg.getNetworkServiceProviderUuid(), NetworkServiceProviderVO.class);
		NetworkServiceProviderFactory factory = getProviderFactory(vo.getType());
		NetworkServiceProvider provider = factory.getNetworkServiceProvider(vo);
		L2NetworkVO l2vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
		
		APIDetachNetworkServiceProviderFromL2NetworkEvent evt = new APIDetachNetworkServiceProviderFromL2NetworkEvent(msg.getId());
		try {
			provider.detachFromL2Network(L2NetworkInventory.valueOf(l2vo), msg);
		} catch (NetworkException e) {
            evt.setError(err(NetworkServiceErrors.DETACH_NETWORK_SERVICE_PROVIDER_ERROR, "unable to detach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s], %s",
                    vo.getUuid(), vo.getName(), vo.getType(), l2vo.getUuid(), l2vo.getName(), l2vo.getType(), e.getMessage()));
            logger.warn(evt.getError().getDetails(), e);
			bus.publish(evt);
			return;
		}
		
		SimpleQuery<NetworkServiceProviderL2NetworkRefVO> query = dbf.createQuery(NetworkServiceProviderL2NetworkRefVO.class);
		query.select(NetworkServiceProviderL2NetworkRefVO_.id);
		query.add(NetworkServiceProviderL2NetworkRefVO_.l2NetworkUuid, Op.EQ, l2vo.getUuid());
		query.add(NetworkServiceProviderL2NetworkRefVO_.networkServiceProviderUuid, Op.EQ, vo.getUuid());
		Long id = query.findValue();
		if (id != null) {
			dbf.removeByPrimaryKey(id, NetworkServiceProviderL2NetworkRefVO.class);
		}
		
		vo = dbf.findByUuid(vo.getUuid(), NetworkServiceProviderVO.class);
		evt.setInventory(NetworkServiceProviderInventory.valueOf(vo));
		String info = String.format("successfully detach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
				vo.getUuid(), vo.getName(), vo.getType(), l2vo.getUuid(), l2vo.getName(), l2vo.getType());
		logger.debug(info);
		bus.publish(evt);
	}

	private void handle(APIAttachNetworkServiceProviderToL2NetworkMsg msg) {
		NetworkServiceProviderVO vo = dbf.findByUuid(msg.getNetworkServiceProviderUuid(), NetworkServiceProviderVO.class);
		NetworkServiceProviderFactory factory = getProviderFactory(vo.getType());
		NetworkServiceProvider provider = factory.getNetworkServiceProvider(vo);
		L2NetworkVO l2vo = dbf.findByUuid(msg.getL2NetworkUuid(), L2NetworkVO.class);
		
		APIAttachNetworkServiceProviderToL2NetworkEvent evt = new APIAttachNetworkServiceProviderToL2NetworkEvent(msg.getId());
		try {
			provider.attachToL2Network(L2NetworkInventory.valueOf(l2vo), msg);
		} catch (NetworkException e) {
            evt.setError(err(NetworkServiceErrors.ATTACH_NETWORK_SERVICE_PROVIDER_ERROR, "unable to attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s], %s",
                    vo.getUuid(), vo.getName(), vo.getType(), l2vo.getUuid(), l2vo.getName(), l2vo.getType(), e.getMessage()));
            logger.warn(evt.getError().getDetails(), e);
			bus.publish(evt);
			return;
		}
		
		NetworkServiceProviderL2NetworkRefVO ref = new NetworkServiceProviderL2NetworkRefVO();
		ref.setL2NetworkUuid(l2vo.getUuid());
		ref.setNetworkServiceProviderUuid(vo.getUuid());
		dbf.persist(ref);
		
		vo = dbf.findByUuid(vo.getUuid(), NetworkServiceProviderVO.class);
		evt.setInventory(NetworkServiceProviderInventory.valueOf(vo));
		String info = String.format("successfully attach network service provider[uuid:%s, name:%s, type:%s] to l2network[uuid:%s, name:%s, type:%s]",
				vo.getUuid(), vo.getName(), vo.getType(), l2vo.getUuid(), l2vo.getName(), l2vo.getType());
		logger.debug(info);
		bus.publish(evt);
	}

	@Override
	public String getId() {
		return bus.makeLocalServiceId(NetworkServiceConstants.SERVICE_ID);
	}

	@Override
	public boolean start() {
		populateExtensions();
		collectSupportedVmTypes();
		return true;
	}

	private void collectSupportedVmTypes() {
	    for (CollectVmTypeNeedToApplyNetworkServiceExtensionPoint ext : pluginRgty.getExtensionList(CollectVmTypeNeedToApplyNetworkServiceExtensionPoint.class)) {
	        supportedVmTypes.add(ext.getVmTypeNeedApplyNetworkService());
        }

	    supportedVmTypes.add(VmInstanceConstant.USER_VM_TYPE);
    }

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public void preBeforeInstantiateVmResource(VmInstanceSpec spec) {
	}

    /* AFTER_VM_CREATED will apply security group, where BEFORE_VM_CREATED will apply other network service */
    private void applyNetworkServices(final VmInstanceSpec spec, NetworkServiceExtensionPosition position, final Completion completion) {
        if (!supportedVmTypes.contains(spec.getVmInventory().getType())) {
            completion.success();
            return;
        }

        if (nsExts.isEmpty()) {
            completion.success();
            return;
        }

        if (spec.getL3Networks() == null || spec.getL3Networks().isEmpty()) {
            completion.success();
            return;
        }

        // we run into this situation when VM nics are all detached and the
        // VM is being rebooted
        if (spec.getDestNics().isEmpty()) {
            completion.success();
            return;
        }

        List<String> nsTypes = spec.getRequiredNetworkServiceTypes();

        FlowChain schain = FlowChainBuilder.newSimpleFlowChain().setName(String.format("apply-network-service-to-vm-%s", spec.getVmInventory().getUuid()));
        schain.allowEmptyFlow();
        for (final NetworkServiceExtensionPoint ns : nsExts) {
            if (position != null && ns.getNetworkServiceExtensionPosition() != position) {
                continue;
            }

            if (!nsTypes.contains(ns.getNetworkServiceType().toString())) {
                continue;
            }

            Flow flow = new Flow() {
                String __name__ = String.format("apply-network-service-%s", ns.getNetworkServiceType());

                @Override
                public void run(final FlowTrigger chain, Map data) {
                    logger.debug(String.format("NetworkServiceExtensionPoint[%s] is asking back ends to apply network service[%s] if needed", ns.getClass().getName(), ns.getNetworkServiceType()));
                    ns.applyNetworkService(spec, data, new Completion(chain) {
                        @Override
                        public void success() {
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            chain.fail(operr("Failed to apply network service[%s] to vm[uuid: %s]",
                                    ns.getNetworkServiceType(),
                                    spec.getVmInventory().getUuid())
                                    .causedBy(errorCode));
                        }
                    });
                }

                @Override
                public void rollback(final FlowRollback chain, Map data) {
                    logger.debug(String.format("NetworkServiceExtensionPoint[%s] is asking back ends to release network service[%s] if needed", ns.getClass().getName(), ns.getNetworkServiceType()));
                    ns.releaseNetworkService(spec, data, new NoErrorCompletion(chain) {
                        @Override
                        public void done() {
                            chain.rollback();
                        }
                    });
                }
            };

            schain.then(flow);
        }

        schain.error(new FlowErrorHandler(completion) {
            @Override
            public void handle(ErrorCode err, Map data) {
                completion.fail(err);
            }
        }).done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                completion.success();
            }
        }).start();
    }

	@Override
	public void preInstantiateVmResource(final VmInstanceSpec spec, final Completion completion) {
        applyNetworkServices(spec, NetworkServiceExtensionPosition.BEFORE_VM_CREATED, completion);
	}

    @Override
    public NetworkServiceProviderType getTypeOfNetworkServiceProviderForService(String l3NetworkUuid, NetworkServiceType serviceType) {
        L3NetworkVO l3vo = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3NetworkUuid).find();
        L3NetworkInventory l3inv = L3NetworkInventory.valueOf(l3vo);
        NetworkServiceL3NetworkRefInventory targetRef = null;
        for (NetworkServiceL3NetworkRefInventory ref : l3inv.getNetworkServices()) {
            if (ref.getNetworkServiceType().equals(serviceType.toString())) {
                targetRef = ref;
                break;
            }
        }
        
        if (targetRef == null) {
            throw new OperationFailureException(operr("L3Network[uuid:%s] doesn't have network service[type:%s] enabled or no provider provides this network service",
                    l3NetworkUuid, serviceType));
        }

        String providerType = Q.New(NetworkServiceProviderVO.class)
                .select(NetworkServiceProviderVO_.type)
                .eq(NetworkServiceProviderVO_.uuid, targetRef.getNetworkServiceProviderUuid())
                .findValue();
        return NetworkServiceProviderType.valueOf(providerType);
    }

    @Override
    public void preReleaseVmResource(final VmInstanceSpec spec, final Completion completion) {
        releaseNetworkServices(spec, NetworkServiceExtensionPosition.BEFORE_VM_CREATED, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void postBeforeInstantiateVmResource(VmInstanceSpec spec) {
    }

    @Override
    public void postInstantiateVmResource(final VmInstanceSpec spec, final Completion completion) {
        applyNetworkServices(spec, NetworkServiceExtensionPosition.AFTER_VM_CREATED, completion);
    }

    private void releaseNetworkServices(final VmInstanceSpec spec, NetworkServiceExtensionPosition position, final NoErrorCompletion completion) {
        if (!supportedVmTypes.contains(spec.getVmInventory().getType())) {
            completion.done();
            return;
        }

        if (nsExts.isEmpty()) {
            completion.done();
            return;
        }

        // we run into this situation when VM nics are all detached and the
        // VM is being rebooted
        if (spec.getDestNics().isEmpty()) {
            completion.done();
            return;
        }

        List<String> nsTypes = spec.getRequiredNetworkServiceTypes();

        FlowChain schain = FlowChainBuilder.newSimpleFlowChain().setName(String.format("release-network-services-from-vm-%s", spec.getVmInventory().getUuid()));
        schain.allowEmptyFlow();

        for (final NetworkServiceExtensionPoint ns : nsExts) {
            if (position != null && ns.getNetworkServiceExtensionPosition() != position) {
                continue;
            }

            if (!nsTypes.contains(ns.getNetworkServiceType().toString())) {
                continue;
            }

            NoRollbackFlow flow = new NoRollbackFlow() {
                String __name__ = String.format("release-network-service-%s", ns.getNetworkServiceType());

                @Override
                public void run(final FlowTrigger chain, Map data) {
                    logger.debug(String.format("NetworkServiceExtensionPoint[%s] is asking back ends to release network service[%s] if needed", ns.getClass().getName(), ns.getNetworkServiceType()));
                    ns.releaseNetworkService(spec, data, new NoErrorCompletion() {
                        @Override
                        public void done() {
                            chain.next();
                        }
                    });
                }
            };

            schain.then(flow);
        }

        schain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully released network services for vm[uuid:%s,  name:%s]", spec.getVmInventory().getUuid(), spec.getVmInventory().getName()));
                completion.done();
            }
        }).start();
    }

    @Override
    public void postReleaseVmResource(final VmInstanceSpec spec, final Completion completion) {
        releaseNetworkServices(spec, NetworkServiceExtensionPosition.AFTER_VM_CREATED, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void releaseVmResource(VmInstanceSpec spec, final Completion completion) {
        releaseNetworkServices(spec, null, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void releaseResourceOnDetachingNic(VmInstanceSpec spec, VmNicInventory nic, NoErrorCompletion completion) {
        releaseNetworkServices(spec, null, completion);
    }

    @Override
    public void releaseNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPosition position, Completion completion) {
        releaseNetworkServices(spec, position, new NoErrorCompletion(completion) {
            @Override
            public void done() {
                completion.success();
            }
        });
    }

    @Override
    public void applyNetworkServiceOnChangeIP(VmInstanceSpec spec, NetworkServiceExtensionPosition position, Completion completion) {
        applyNetworkServices(spec, position, completion);
    }

    @Override
    public List<String> getL3NetworkDns(String l3NetworkUuid){
        List<String> dns = Q.New(L3NetworkDnsVO.class).eq(L3NetworkDnsVO_.l3NetworkUuid, l3NetworkUuid)
                .select(L3NetworkDnsVO_.dns).orderBy(L3NetworkDnsVO_.id, SimpleQuery.Od.ASC).listValues();
        if (dns == null) {
            dns = new ArrayList<String>();
        }

        L3NetworkVO l3VO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, l3NetworkUuid).find();
        L3NetworkInventory l3Inv = L3NetworkInventory.valueOf(l3VO);

        for (DnsServiceExtensionPoint exp : pluginRgty.getExtensionList(DnsServiceExtensionPoint.class)) {
            List<String> dnsExt = exp.getDnsAddress(l3Inv);
            if (!dnsExt.isEmpty()) {
                dns.addAll(dnsExt);
                break;
            }
        }

        return dns;
    }

    @Override
    public List<String> getVmDns(String vmUuid, String l3NetworkUuid) {
        List<String> dnsList = DnsUtils.getVmDnsList(vmUuid);
        if (!dnsList.isEmpty() || l3NetworkUuid == null) {
            return dnsList;
        }

        dnsList.addAll(getL3NetworkDns(l3NetworkUuid));
        return dnsList;
    }

    @Override
    public List<String> getVmNicDns(String vmNicUuid, Integer ipVersion, String l3NetworkUuid) {
        List<String> dnsList = DnsUtils.getVmNicDnsList(vmNicUuid, ipVersion);

        if (!dnsList.isEmpty() || l3NetworkUuid == null) {
            return dnsList;
        }

        dnsList.addAll(getL3NetworkDns(l3NetworkUuid));
        return dnsList.stream().filter(dns -> {
            try {
                return Objects.equals(NetworkUtils.getIpversion(dns), ipVersion);
            } catch (Exception ignored) {
                return false;
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void instantiateResourceOnAttachingNic(VmInstanceSpec spec, L3NetworkInventory l3, Completion completion) {
        preInstantiateVmResource(spec, completion);
    }

    @Override
    public void releaseResourceOnAttachingNic(final VmInstanceSpec spec, final L3NetworkInventory l3, final NoErrorCompletion completion) {
        releaseVmResource(spec, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.warn(String.format("unable to release a network service of the VM[uuid:%s] when rolling back an attached" +
                        " L3 network[uuid: %s], %s. You may need to reboot the VM to fix the issue", spec.getVmInventory().getUuid(), l3.getUuid(), errorCode));
                completion.done();
            }
        });
    }

    @Override
    public boolean isVmNeedNetworkService(String vmType, NetworkServiceType serviceType) {
	    /* serviceType is not used now, maybe used in future */
        return supportedVmTypes.contains(vmType);
    }

    @Override
    public void afterHostConnected(HostInventory host) {
        FlowChain schain = FlowChainBuilder.newSimpleFlowChain()
                .setName(String.format("host-%s-connected-network-service-action", host.getUuid()));
        schain.allowEmptyFlow();

        for (final NetworkServiceHostExtensionPoint ns : nsHostExts) {

            NoRollbackFlow flow = new NoRollbackFlow() {
                String __name__ = String.format("host-%s-connected-%s-action", host.getUuid(), ns.getNetworkServiceName());

                @Override
                public void run(final FlowTrigger chain, Map data) {
                    ns.afterHostConnected(host, new Completion(chain) {
                        @Override
                        public void success() {
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            chain.fail(errorCode);
                        }
                    });
                }
            };

            schain.then(flow);
        }

        schain.done(new FlowDoneHandler(new NopeCompletion()) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully finished network services after host[uuid:%s] connected", host.getUuid()));
            }
        }).start();

    }

    @Override
    public void preDeleteHost(HostInventory inventory) throws HostException {

    }

    @Override
    public void beforeDeleteHost(HostInventory host) {
        FlowChain schain = FlowChainBuilder.newSimpleFlowChain()
                .setName(String.format("host-%s-deletion-network-service-action", host.getUuid()));
        schain.allowEmptyFlow();

        final FutureCompletion completion = new FutureCompletion(null);
        for (final NetworkServiceHostExtensionPoint ns : nsHostExts) {

            NoRollbackFlow flow = new NoRollbackFlow() {
                String __name__ = String.format("host-%s-deletion-%s-action", host.getUuid(), ns.getNetworkServiceName());

                @Override
                public void run(final FlowTrigger chain, Map data) {
                    ns.beforeDeleteHost(host, new Completion(chain) {
                        @Override
                        public void success() {
                            chain.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            chain.fail(errorCode);
                        }
                    });
                }
            };

            schain.then(flow);
        }

        schain.done(new FlowDoneHandler(completion) {
            @Override
            public void handle(Map data) {
                logger.debug(String.format("successfully finished network services before host[uuid:%s] deletion", host.getUuid()));
                completion.success();
            }
        }).start();

        completion.await(TimeUnit.SECONDS.toMillis(60));
    }

    @Override
    public void afterDeleteHost(HostInventory inventory) {

    }


    @Override
    public void enableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType,
                                     NetworkServiceType nsType, List<String> systemTags, Completion completion) {
        for (final NetworkServiceExtensionPoint ns : nsExts) {
            if (ns.getNetworkServiceType() == nsType) {
                ns.enableNetworkService(l3VO, providerType, systemTags, completion);
                return;
            }
        }
        logger.debug(String.format("there is no backend[provideType:%s, serviceType:%S] to enable service", providerType.toString(), nsType.toString()));
        completion.success();
    }

    @Override
    public void disableNetworkService(L3NetworkVO l3VO, NetworkServiceProviderType providerType, NetworkServiceType nsType, Completion completion) {
        for (final NetworkServiceExtensionPoint ns : nsExts) {
            if (ns.getNetworkServiceType() == nsType) {
                ns.disableNetworkService(l3VO, providerType, completion);
                return;
            }
        }
        logger.debug(String.format("there is no backend[provideType:%s, serviceType:%S] to disable service", providerType.toString(), nsType.toString()));
        completion.success();
    }
}
