package org.zstack.network.service.vipQos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.header.vipQos.*;
import org.zstack.network.service.vip.VipCanonicalEvents;
import org.zstack.network.service.vip.VipInventory;
import org.zstack.network.service.vip.VipVO;
import org.zstack.network.service.vip.VipVO_;
import org.zstack.network.service.virtualrouter.vip.VipConfigProxy;
import org.zstack.network.service.virtualrouter.vyos.VyosConstants;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static java.util.Arrays.asList;
import static org.zstack.core.Platform.operr;

/**
 * Created by liangbo.zhou on 17-6-12.
 */
public class VipQosManagerImpl extends AbstractService implements VipQosManager {
    private static final CLogger logger = Utils.getLogger(VipQosManagerImpl.class);

    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    private VipConfigProxy proxy;

    private Map<String, VipQosBackend> backends = new HashMap<>();

    @MessageSafe
    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APISetVipQosMsg) {
            handle((APISetVipQosMsg) msg);
        } else if (msg instanceof APIDeleteVipQosMsg) {
            handle((APIDeleteVipQosMsg) msg);
        } else if (msg instanceof APIGetVipQosMsg) {
            handle((APIGetVipQosMsg) msg);
        } else if (msg instanceof SyncVipQosMsg) {
            handle((SyncVipQosMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VipQosConstants.SERVICE_ID);
    }

    private String getThreadSyncSignature(String vipUuid) {return String.format("vipqos-%s", vipUuid);}

    public VipQosStruct getVipQosStruct(VipQosInventory inv) {
        VipQosStruct struct = new VipQosStruct();
        VipVO vo = dbf.findByUuid(inv.getVipUuid(), VipVO.class);
        struct.setVipUuid(vo.getUuid());
        struct.setVip(vo.getIp());
        struct.setPort(inv.getPort());
        struct.setInboundBandwidth(inv.getInboundBandwidth());
        struct.setOutboundBandwidth(inv.getOutboundBandwidth());
        struct.setL3NetworkUuid(vo.getL3NetworkUuid());
        return struct;
    }

    private void deleteVipQos(String vipUuid, int port, Completion completion) {
        VipQosBackend bkd = getBackend(vipUuid);
        if (bkd == null) {
            completion.success();
            return;
        }

        VipQosVO vo = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, vipUuid).eq(VipQosVO_.port, port).find();
        VipQosStruct struct = getVipQosStruct(VipQosInventory.valueOf(vo));
        VipQosStruct struct1 = getVipQosStruct(VipQosInventory.valueOf(vo));
        bkd.deleteVipQos(asList(struct), new Completion(completion) {
            @Override
            public void success() {
                List<VipQosVO> vos = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, vipUuid).notEq(VipQosVO_.port, port).list();
                if (vos == null || vos.isEmpty()){
                    /* execept this port, no other port, then notify backend to delete all
                     * This is api is used by flat vip */
                    bkd.deleteVipAllQos(asList(struct1), completion);
                } else {
                    completion.success();
                }
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });

        return;
    }

    private void syncVipQos(String vipUuid, String vrUuid, Completion completion) {
        VipQosBackend bkd = getBackend(vipUuid);
        if (bkd == null) {
            completion.fail(operr("Can not find VipQos backend for Vip [uuid:%s]", vipUuid));
            return;
        }

        List<VipQosStruct> structs = new ArrayList<>();
        List<VipQosVO> vos = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, vipUuid).list();
        for (VipQosVO vo: vos) {
            VipQosStruct struct = getVipQosStruct(VipQosInventory.valueOf(vo));
            structs.add(struct);
        }
        if (structs.isEmpty()) {
            completion.success();
            return;
        }

        bkd.setVipQos(structs, vrUuid, completion);
    }

    private void handle(SyncVipQosMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(msg.getVipUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                SyncVipQosReply reply = new SyncVipQosReply();
                syncVipQos(msg.getVipUuid(), msg.getVrUuid(), new Completion(chain) {

                    @Override
                    public void success() {
                        bus.reply(msg, reply);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        reply.setError(errorCode);
                        bus.reply(msg, reply);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void handle(APISetVipQosMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(msg.getVipUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                setVipQos(msg, new NoErrorCompletion(chain) {
                    @Override
                    public void done() {
                        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, msg.getVipUuid()).find();
                        VipInventory vipInventory = VipInventory.valueOf(vipVO);

                        VipCanonicalEvents.VipEventData vipEventData = new VipCanonicalEvents.VipEventData();
                        vipEventData.setVipUuid(msg.getVipUuid());
                        vipEventData.setCurrentStatus(VipCanonicalEvents.VIP_STATUS_CREATED);
                        vipEventData.setInventory(vipInventory);
                        vipEventData.setDate(new Date());
                        evtf.fire(VipQosCanonicalEvents.VIP_QOS_CHANGE_PATH, vipEventData);

                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void setVipQos(final APISetVipQosMsg msg, final NoErrorCompletion completion) {
        APISetVipQosEvent evt = new APISetVipQosEvent(msg.getId());

        VipQosVO vo = new VipQosVO();
        vo.setUuid(Platform.getUuid());
        vo.setVipUuid(msg.getVipUuid());
        if (msg.getPort() != null) {
            vo.setPort(msg.getPort());
        }
        if (msg.getInboundBandwidth() != null) {
            vo.setInboundBandwidth(msg.getInboundBandwidth());
        }
        if (msg.getOutboundBandwidth() != null) {
            vo.setOutboundBandwidth(msg.getOutboundBandwidth());
        }
        vo.setType(VipQosLimitType.ibit.toString());
        dbf.persistAndRefresh(vo);

        VipQosInventory inv = VipQosInventory.valueOf(vo);
        VipQosStruct struct = getVipQosStruct(inv);

        VipQosBackend bkd = getBackend(msg.getVipUuid());
        if (bkd == null) {
            evt.setInventory(inv);
            bus.publish(evt);
            completion.done();
            return;
        }

        bkd.setVipQos(asList(struct), null, new Completion(completion) {
            @Override
            public void success() {
                bus.publish(evt);
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                dbf.remove(vo);
                evt.setError(errorCode);
                bus.publish(evt);
                completion.done();
            }
        });
    }

    private void handle(APIDeleteVipQosMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getThreadSyncSignature(msg.getUuid());
            }

            @Override
            public void run(final SyncTaskChain chain) {
                APIDeleteVipQosEvent evt = new APIDeleteVipQosEvent(msg.getId());
                deleteVipQos(msg.getUuid(), msg.getPort() == null ? 0 : msg.getPort(), new Completion(chain) {
                    @Override
                    public void success() {
                        VipQosVO vo;
                        if (msg.getPort() != null) {
                            vo = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getUuid()).eq(VipQosVO_.port, msg.getPort()).find();
                        } else {
                            vo = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getUuid()).eq(VipQosVO_.port, 0).find();
                        }
                        if (vo != null) {
                            dbf.remove(vo);
                        }

                        VipVO vipVO = Q.New(VipVO.class).eq(VipVO_.uuid, msg.getUuid()).find();
                        VipInventory vipInventory = VipInventory.valueOf(vipVO);
                        VipCanonicalEvents.VipEventData vipEventData = new VipCanonicalEvents.VipEventData();
                        vipEventData.setVipUuid(msg.getUuid());
                        vipEventData.setCurrentStatus(VipCanonicalEvents.VIP_STATUS_CREATED);
                        vipEventData.setInventory(vipInventory);
                        vipEventData.setDate(new Date());
                        evtf.fire(VipQosCanonicalEvents.VIP_QOS_CHANGE_PATH, vipEventData);

                        bus.publish(evt);
                        chain.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        evt.setError(errorCode);
                        bus.publish(evt);
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    protected void handle(final APIGetVipQosMsg msg) {
        APIGetVipQosReply reply = new APIGetVipQosReply();
        List<VipQosInventory> invs = new ArrayList<>();

        if (msg.getUuid() != null) {
            List<VipQosVO> vos = Q.New(VipQosVO.class).eq(VipQosVO_.vipUuid, msg.getUuid()).list();
            invs.addAll(VipQosInventory.valueOf(vos));
        } else {
            List<VipQosVO> vos = Q.New(VipQosVO.class).orderBy(VipQosVO_.vipUuid, SimpleQuery.Od.DESC).list();
            invs.addAll(VipQosInventory.valueOf(vos));
        }

        reply.setInventories(invs);

        bus.reply(msg, reply);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }
    
    private void populateExtensions() {
        for (VipQosBackend bkd : pluginRgty.getExtensionList(VipQosBackend.class)) {
            VipQosBackend old = backends.get(bkd.getNetworkServiceProviderType());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate VipQosBackend[%s, %s] for the network service provider" +
                        " type[%s]", old.getClass(), bkd.getClass(), bkd.getNetworkServiceProviderType()));
            }

            backends.put(bkd.getNetworkServiceProviderType(), bkd);
        }
    }

    @Transactional
    private VipQosBackend getBackend(String vipUuid) {
        String providerType;
        /*peerL3netowrk may support both flat/vrouter provides network services*/

        /* this is a work around method for some case:
               VirtualRouterVipVO still exists, but VipPeerL3NetworkRefVO is deleted */
        List<String> vrUuids = proxy.getVrUuidsByNetworkService(VipVO.class.getSimpleName(), vipUuid);
        if (vrUuids != null && !vrUuids.isEmpty()) {
            providerType = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE;
        } else {
            providerType = SQL.New("select distinct pro.type" +
                    " from NetworkServiceL3NetworkRefVO ref, NetworkServiceProviderVO pro, VipPeerL3NetworkRefVO peer" +
                    " where peer.vipUuid =:vipUuid" +
                    " and peer.l3NetworkUuid = ref.l3NetworkUuid" +
                    " and ref.networkServiceType =:vipQosType" +
                    " and ref.networkServiceProviderUuid = pro.uuid", String.class)
                                     .param("vipUuid", vipUuid)
                                     .param("vipQosType", VipQosConstants.VIPQOS_NETWORK_SERVICE_TYPE.toString())
                                     .limit(1).find();

            if (providerType == null) {
                logger.warn(String.format("can not find vipQos provider for Vip [uuid: %s]", vipUuid));
            }
        }
        return backends.get(providerType);
    }

    @Override
    public boolean stop() {
        return true;
    }
}
