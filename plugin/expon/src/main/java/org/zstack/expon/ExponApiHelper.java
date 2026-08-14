package org.zstack.expon;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.singleflight.MultiNodeSingleFlightImpl;
import org.zstack.expon.sdk.*;
import org.zstack.expon.sdk.cluster.QueryTianshuClusterRequest;
import org.zstack.expon.sdk.cluster.QueryTianshuClusterResponse;
import org.zstack.expon.sdk.cluster.TianshuClusterModule;
import org.zstack.expon.sdk.config.SetTrashExpireTimeRequest;
import org.zstack.expon.sdk.config.SetTrashExpireTimeResponse;
import org.zstack.expon.sdk.iscsi.*;
import org.zstack.expon.sdk.nvmf.*;
import org.zstack.expon.sdk.pool.*;
import org.zstack.expon.sdk.uss.QueryUssGatewayRequest;
import org.zstack.expon.sdk.uss.QueryUssGatewayResponse;
import org.zstack.expon.sdk.uss.UssGatewayModule;
import org.zstack.expon.sdk.vhost.*;
import org.zstack.expon.sdk.volume.*;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.expon.ExponError;
import org.zstack.header.storage.addon.SingleFlightExecutor;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExponApiHelper implements SingleFlightExecutor {
    AccountInfo accountInfo;
    ExponClient client;
    String sessionId;
    String refreshToken;
    private String storageUuid;

    @Autowired
    private MultiNodeSingleFlightImpl singleFlight;

    private static final Cache<String, String> snapshotClientCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    ExponApiHelper(AccountInfo accountInfo, ExponClient client) {
        this.accountInfo = accountInfo;
        this.client = client;
        this.client.setSessionRefresher(this::refreshSessionInQueue);
        this.client.setSessionGetter(() -> sessionId);
    }

    void setStorageUuid(String storageUuid) {
        this.storageUuid = storageUuid;
    }

    private <T extends ExponResponse> T callWithExpiredSessionRetry(ExponRequest req, Class<T> clz) {
        req.setSessionId(sessionId);
        T rsp = client.call(req, clz);

        if (!rsp.isSuccess() && rsp.sessionExpired()) {
            refreshSessionInQueue();
            req.setSessionId(sessionId);
            rsp = client.call(req, clz);
        }
        return rsp;
    }

    private synchronized String refreshSessionInQueue() {
        FutureCompletion fcompl = new FutureCompletion(null);
        ReturnValueCompletion<Object> completion = new ReturnValueCompletion<Object>(fcompl) {
            @Override
            public void success(Object returnValue) {
                sessionId = (String) returnValue;
                fcompl.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                fcompl.fail(errorCode);
            }
        };

        singleFlight.run(this, "refreshSession", completion);

        fcompl.await(TimeUnit.MILLISECONDS.toSeconds(30));
        if (!fcompl.isSuccess()) {
            throw new OperationFailureException(fcompl.getErrorCode());
        }
        return sessionId;
    }

    @SingleFlightExecutor.SingleFlight
    public void refreshSession(ReturnValueCompletion<Object> completion) {
        if (sessionExpired()) {
            login();
        }
        completion.success(sessionId);
    }

    private boolean sessionExpired() {
        if (sessionId == null) {
            return true;
        }

        QueryTianshuClusterRequest req = new QueryTianshuClusterRequest();

        req.setSessionId(sessionId);
        QueryTianshuClusterResponse rsp = client.call(req, QueryTianshuClusterResponse.class);
        return rsp.sessionExpired();
    }

    public <T extends ExponResponse> T call(ExponRequest req, Class<T> clz) {
        return callWithExpiredSessionRetry(req, clz);
    }

    public <T extends ExponResponse> T callErrorOut(ExponRequest req, Class<T> clz) {
        T rsp = callWithExpiredSessionRetry(req, clz);
        errorOut(rsp);
        return rsp;
    }

    public <T extends ExponResponse> void call(ExponRequest req, Completion completion) {
        req.setSessionId(sessionId);
        client.call(req, result -> {
            if (result.error == null) {
                completion.success();
                return;
            }

            if (!result.error.sessionExpired()) {
                completion.fail(operr("expon request failed, code %s, message: %s.", result.error.getRetCode(), result.error.getMessage()));
                return;
            }

            refreshSessionInQueue();
            req.setSessionId(sessionId);
            client.call(req, retryRes -> {
                if (retryRes.error != null) {
                    completion.fail(operr("expon request failed, code %s, message: %s.", retryRes.error.getRetCode(), retryRes.error.getMessage()));
                    return;
                }

                completion.success();
            });
        });
    }

    public void errorOut(ExponResponse rsp) {
        if (!rsp.isSuccess()) {
            throw new OperationFailureException(operr("expon request failed, code %s, message: %s.", rsp.getRetCode(), rsp.getMessage()));
        }
    }

    public <T extends ExponQueryResponse> T query(ExponQueryRequest req, Class<T> clz) {
        return call(req, clz);
    }

    public <T extends ExponQueryResponse> T queryErrorOut(ExponQueryRequest req, Class<T> clz) {
        return callErrorOut(req, clz);
    }

    public void login() {
        LoginExponRequest req = new LoginExponRequest();
        req.setName(accountInfo.username);
        req.setPassword(accountInfo.password);
        LoginExponResponse rsp = callErrorOut(req, LoginExponResponse.class);
        sessionId = rsp.getAccessToken();
    }

    public List<FailureDomainModule> queryPools() {
        QueryFailureDomainRequest req = new QueryFailureDomainRequest();
        return queryErrorOut(req, QueryFailureDomainResponse.class).getFailureDomains();
    }

    public FailureDomainModule getPool(String id) {
        GetFailureDomainRequest req = new GetFailureDomainRequest();
        req.setId(id);
        return callErrorOut(req, GetFailureDomainResponse.class).getMembers();
    }

    public List<TianshuClusterModule> queryClusters() {
        QueryTianshuClusterRequest req = new QueryTianshuClusterRequest();
        return queryErrorOut(req, QueryTianshuClusterResponse.class).getResult();
    }

    public UssGatewayModule queryUssGateway(String name) {
        QueryUssGatewayRequest q = new QueryUssGatewayRequest();
        q.addCond("name", name);
        QueryUssGatewayResponse rsp = queryErrorOut(q, QueryUssGatewayResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getUssGateways().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public List<UssGatewayModule> listUssGateway() {
        QueryUssGatewayRequest q = new QueryUssGatewayRequest();
        QueryUssGatewayResponse rsp = queryErrorOut(q, QueryUssGatewayResponse.class);

        return rsp.getUssGateways();
    }

    public VhostControllerModule queryVhostController(String name) {
        QueryVhostControllerRequest q = new QueryVhostControllerRequest();
        q.addCond("name", name);
        QueryVhostControllerResponse rsp = queryErrorOut(q, QueryVhostControllerResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getVhosts().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public VhostControllerModule createVhostController(String name) {
        CreateVhostControllerRequest req = new CreateVhostControllerRequest();
        req.setName(name);
        CreateVhostControllerResponse rsp = callErrorOut(req, CreateVhostControllerResponse.class);

        VhostControllerModule inv = new VhostControllerModule();
        inv.setId(rsp.getId());
        inv.setName(name);
        inv.setPath("/var/run/wds/" + name);
        return inv;
    }

    public void deleteVhostController(String id) {
        DeleteVhostControllerRequest req = new DeleteVhostControllerRequest();
        req.setId(id);
        callErrorOut(req, DeleteVhostControllerResponse.class);
    }

    public VolumeModule queryVolume(String name) {
        QueryVolumeRequest req = new QueryVolumeRequest();
        req.addCond("name", name);
        QueryVolumeResponse rsp = queryErrorOut(req, QueryVolumeResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getVolumes().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public VolumeModule getVolume(String id) {
        GetVolumeRequest req = new GetVolumeRequest();
        req.setVolId(id);
        GetVolumeResponse rsp = callErrorOut(req, GetVolumeResponse.class);

        return rsp.getVolumeDetail();
    }

    public boolean addVhostVolumeToUss(String volumeId, String vhostId, String ussGwId) {
        AddVhostControllerToUssRequest req = new AddVhostControllerToUssRequest();
        req.setLunId(volumeId);
        req.setVhostId(vhostId);
        req.setUssGwId(ussGwId);
        AddVhostControllerToUssResponse rsp = call(req, AddVhostControllerToUssResponse.class);
        if (rsp.isError(ExponError.VHOST_BIND_USS_FAILED) && rsp.getMessage().contains("already bind")) {
            return true;
        }

        errorOut(rsp);
        return true;
    }

    public boolean removeVhostVolumeFromUss(String volumeId, String vhostId, String ussGwId) {
        RemoveVhostControllerFromUssRequest req = new RemoveVhostControllerFromUssRequest();
        req.setLunId(volumeId);
        req.setVhostId(vhostId);
        req.setUssGwId(ussGwId);
        RemoveVhostControllerFromUssResponse rsp = call(req, RemoveVhostControllerFromUssResponse.class);
        if (rsp.isError(ExponError.VHOST_ALREADY_UNBIND_USS)) {
            return true;
        }

        errorOut(rsp);
        return true;
    }

    public List<UssGatewayModule> getVhostControllerBoundUss(String vhostId) {
        GetVhostControllerBoundUssRequest req = new GetVhostControllerBoundUssRequest();
        req.setVhostId(vhostId);
        GetVhostControllerBoundUssResponse rsp = call(req, GetVhostControllerBoundUssResponse.class);
        return rsp.getUss();
    }

    public VolumeModule createVolume(String name, String poolId, long size) {
        CreateVolumeRequest req = new CreateVolumeRequest();
        req.setName(name);
        req.setPhyPoolId(poolId);
        req.setVolumeSize(size);
        CreateVolumeResponse rsp = callErrorOut(req, CreateVolumeResponse.class);

        return getVolume(rsp.getId());
    }

    public void deleteVolume(String volId, boolean force) {
        DeleteVolumeRequest req = new DeleteVolumeRequest();
        req.setVolId(volId);
        req.setForce(force);
        callErrorOut(req, DeleteVolumeResponse.class);
    }
    
    public VolumeModule cloneVolume(String snapId, String name, ExponVolumeQos qos) {
        CloneVolumeRequest req = new CloneVolumeRequest();
        req.setSnapshotId(snapId);
        if (qos != null) {
            req.setQos(qos);
        }
        req.setName(name);
        CloneVolumeResponse rsp = callErrorOut(req, CloneVolumeResponse.class);

        return getVolume(rsp.getId());
    }

    public void copySnapshot(String snapId, String poolId, String name, ExponVolumeQos qos, ReturnValueCompletion<VolumeModule> completion) {
        CopyVolumeSnapshotRequest req = new CopyVolumeSnapshotRequest();
        req.setSnapshotId(snapId);
        req.setPhyPoolId(poolId);
        if (qos != null) {
            req.setQos(qos);
        }
        req.setName(name);

        call(req, new Completion(completion) {
            @Override
            public void success() {
                VolumeModule vol = queryVolume(name);
                if (vol == null) {
                    completion.fail(operr("cannot find volume[name:%s] after copy snapshot", name));
                    return;
                }

                completion.success(vol);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    public VolumeModule expandVolume(String volId, long size) {
        ExpandVolumeRequest req = new ExpandVolumeRequest();
        req.setId(volId);
        req.setSize(size);
        ExpandVolumeResponse rsp = callErrorOut(req, ExpandVolumeResponse.class);

        return getVolume(volId);
    }

    public VolumeModule setVolumeQos(String volId, ExponVolumeQos qos) {
        SetVolumeQosRequest req = new SetVolumeQosRequest();
        req.setQos(qos);
        req.setVolId(volId);
        callErrorOut(req, SetVolumeQosResponse.class);
        return getVolume(volId);
    }

    public void deleteVolumeQos(String volId) {
        SetVolumeQosRequest req = new SetVolumeQosRequest();
        req.setVolId(volId);
        req.setQos(new ExponVolumeQos());
        callErrorOut(req, SetVolumeQosResponse.class);
    }

    public VolumeModule recoverySnapshot(String volId, String snapId) {
        RecoveryVolumeSnapshotRequest req = new RecoveryVolumeSnapshotRequest();
        req.setSnapId(snapId);
        req.setVolumeId(volId);
        RecoveryVolumeSnapshotResponse rsp = callErrorOut(req, RecoveryVolumeSnapshotResponse.class);

        return getVolume(volId);
    }

    public VolumeSnapshotModule createVolumeSnapshot(String volId, String name, String description) {
        CreateVolumeSnapshotRequest req = new CreateVolumeSnapshotRequest();
        req.setName(name);
        req.setDescription(description);
        req.setVolumeId(volId);
        CreateVolumeSnapshotResponse rsp = callErrorOut(req, CreateVolumeSnapshotResponse.class);

        return queryVolumeSnapshot(name);
    }

    // TODO change to async
    public void deleteVolumeSnapshot(String snapId, boolean force) {
        DeleteVolumeSnapshotRequest req = new DeleteVolumeSnapshotRequest();
        req.setSnapshotId(snapId);
        req.setForce(force);
        callErrorOut(req, DeleteVolumeSnapshotResponse.class);
    }

    public VolumeSnapshotModule queryVolumeSnapshot(String name) {
        QueryVolumeSnapshotRequest req = new QueryVolumeSnapshotRequest();
        req.addCond("name", name);
        QueryVolumeSnapshotResponse rsp = queryErrorOut(req, QueryVolumeSnapshotResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getSnaps().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public VolumeSnapshotModule getVolumeSnapshot(String snapshotId) {
        GetVolumeSnapshotRequest req = new GetVolumeSnapshotRequest();
        req.setId(snapshotId);
        GetVolumeSnapshotResponse rsp = callErrorOut(req, GetVolumeSnapshotResponse.class);

        return JSONObjectUtil.rehashObject(rsp, VolumeSnapshotModule.class);
    }

    public NvmfModule queryNvmfController(String name) {
        QueryNvmfTargetRequest req = new QueryNvmfTargetRequest();
        req.addCond("name", name);
        QueryNvmfTargetResponse rsp = queryErrorOut(req, QueryNvmfTargetResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getNvmfs().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public NvmfModule createNvmfController(String name, String tianshuId, String nqnSuffix) {
        CreateNvmfTargetRequest req = new CreateNvmfTargetRequest();
        req.setName(name);
        LocalDate d = LocalDate.now();
        String nqn = String.format("nqn.%d-%d.com.sds.wds:%s", d.getYear(), d.getMonthValue(), nqnSuffix);
        req.setNqn(nqn);
        req.setTianshuId(tianshuId);
        CreateNvmfTargetResponse rsp = callErrorOut(req, CreateNvmfTargetResponse.class);

        NvmfModule nvmf = new NvmfModule();
        nvmf.setId(rsp.getId());
        nvmf.setName(name);
        nvmf.setNqn(nqn);
        return nvmf;
    }

    public void deleteNvmfController(String id) {
        DeleteNvmfTargetRequest req = new DeleteNvmfTargetRequest();
        req.setNvmfId(id);
        callErrorOut(req, DeleteNvmfTargetResponse.class);
    }

    public NvmfClientGroupModule queryNvmfClient(String name) {
        QueryNvmfClientGroupRequest req = new QueryNvmfClientGroupRequest();
        req.addCond("name", name);
        QueryNvmfClientGroupResponse rsp = queryErrorOut(req, QueryNvmfClientGroupResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getClients().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public NvmfClientGroupModule createNvmfClient(String name, String tianshuId, List<String> hostNqns) {
        CreateNvmfClientGroupRequest req = new CreateNvmfClientGroupRequest();
        req.setName(name);
        req.setDescription("description");
        req.setTianshuId(tianshuId);
        List<NvmfClient> hosts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(hostNqns)) {
            for (String hostNqn : hostNqns) {
                NvmfClient host = new NvmfClient();
                host.setHostType("nqn");
                host.setHost(hostNqn);
                hosts.add(host);
            }
            req.setHosts(hosts);
        }
        CreateNvmfClientGroupResponse rsp = callErrorOut(req, CreateNvmfClientGroupResponse.class);

        NvmfClientGroupModule client = new NvmfClientGroupModule();
        client.setId(rsp.getId());
        client.setName(name);
        return client;
    }

    public NvmfClientGroupModule addNvmfClientHost(String clientId, String nqn) {
        ChangeNvmeClientGroupRequest req = new ChangeNvmeClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.add.name());
        NvmfClient host = new NvmfClient();
        host.setHostType("nqn");
        host.setHost(nqn);
        req.setHosts(Collections.singletonList(host));
        ChangeNvmeClientGroupResponse rsp = callErrorOut(req, ChangeNvmeClientGroupResponse.class);

        return queryNvmfClient(clientId);
    }

    public NvmfClientGroupModule removeNvmfClientHost(String clientId, String nqn) {
        ChangeNvmeClientGroupRequest req = new ChangeNvmeClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.remove.name());
        NvmfClient host = new NvmfClient();
        host.setHostType("nqn");
        host.setHost(nqn);
        req.setHosts(Collections.singletonList(host));
        ChangeNvmeClientGroupResponse rsp = callErrorOut(req, ChangeNvmeClientGroupResponse.class);

        return queryNvmfClient(clientId);
    }

    public void deleteNvmfClient(String clientId) {
        DeleteNvmfClientGroupRequest req = new DeleteNvmfClientGroupRequest();
        req.setClientId(clientId);
        callErrorOut(req, DeleteNvmfClientGroupResponse.class);
    }

    public void addNvmfClientToNvmfTarget(String clientId, String targetId) {
        AddNvmeClientGroupToNvmfTargetRequest req = new AddNvmeClientGroupToNvmfTargetRequest();
        req.setClients(Collections.singletonList(clientId));
        req.setGatewayId(targetId);
        callErrorOut(req, AddNvmeClientGroupToNvmfTargetResponse.class);
    }

    public void removeNvmfClientFromNvmfTarget(String clientId, String targetId) {
        RemoveNvmeClientGroupFromNvmfTargetRequest req = new RemoveNvmeClientGroupFromNvmfTargetRequest();
        req.setClients(Collections.singletonList(clientId));
        req.setGatewayId(targetId);
        callErrorOut(req, RemoveNvmeClientGroupFromNvmfTargetResponse.class);
    }

    public void addVolumeToNvmfClientGroup(String volId, String clientId, String targetId) {
        ChangeVolumeInNvmfClientGroupRequest req = new ChangeVolumeInNvmfClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.add.name());
        req.setLuns(Collections.singletonList(new LunResource(volId, "volume")));
        req.setGateways(Collections.singletonList(targetId));
        callErrorOut(req, ChangeVolumeInNvmfClientGroupResponse.class);
    }

    public void addSnapshotToNvmfClientGroup(String snapId, String clientId, String targetId) {
        ChangeVolumeInNvmfClientGroupRequest req = new ChangeVolumeInNvmfClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.add.name());
        req.setLuns(Collections.singletonList(new LunResource(snapId, "snapshot")));
        req.setGateways(Collections.singletonList(targetId));
        callErrorOut(req, ChangeVolumeInNvmfClientGroupResponse.class);
    }

    public void removeVolumeFromNvmfClientGroup(String volId, String clientId) {
        ChangeVolumeInNvmfClientGroupRequest req = new ChangeVolumeInNvmfClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.remove.name());
        req.setLuns(Collections.singletonList(new LunResource(volId, "volume")));
        callErrorOut(req, ChangeVolumeInNvmfClientGroupResponse.class);
    }

    public void removeSnapshotFromNvmfClientGroup(String snapId, String clientId) {
        ChangeVolumeInNvmfClientGroupRequest req = new ChangeVolumeInNvmfClientGroupRequest();
        req.setClientId(clientId);
        req.setAction(ExponAction.remove.name());
        req.setLuns(Collections.singletonList(new LunResource(snapId, "snapshot")));
        callErrorOut(req, ChangeVolumeInNvmfClientGroupResponse.class);
    }

    public NvmfBoundUssGatewayRefModule bindNvmfTargetToUss(String nvmfId, String ussGwId, int port) {
        BindNvmfTargetToUssRequest req = new BindNvmfTargetToUssRequest();
        req.setNvmfId(nvmfId);
        req.setUssGwId(Collections.singletonList(ussGwId));
        req.setPort(port);
        callErrorOut(req, BindNvmfTargetToUssResponse.class);
        NvmfBoundUssGatewayRefModule ref = getNvmfBoundUssGateway(nvmfId, ussGwId);
        if (ref == null) {
            throw new ExponApiException(String.format("cannot find nvmf[id:%s] bound uss gateway[id:%s] ref after bind success", nvmfId, ussGwId));
        }
        return ref;
    }

    public void unbindNvmfTargetToUss(String nvmfId, String ussGwId) {
        UnbindNvmfTargetFromUssRequest req = new UnbindNvmfTargetFromUssRequest();
        req.setNvmfId(nvmfId);
        req.setUssGwId(Collections.singletonList(ussGwId));
        callErrorOut(req, UnbindNvmfTargetFromUssResponse.class);
    }

    public List<NvmfBoundUssGatewayRefModule> getNvmfBoundUssGateway(String nvmfId) {
        GetNvmfTargetBoundUssRequest req = new GetNvmfTargetBoundUssRequest();
        req.setNvmfId(nvmfId);
        GetNvmfTargetBoundUssResponse rsp = callErrorOut(req, GetNvmfTargetBoundUssResponse.class);
        return rsp.getResult();
    }

    public NvmfBoundUssGatewayRefModule getNvmfBoundUssGateway(String nvmfId, String ussGwId) {
        GetNvmfTargetBoundUssRequest req = new GetNvmfTargetBoundUssRequest();
        req.setNvmfId(nvmfId);
        GetNvmfTargetBoundUssResponse rsp = callErrorOut(req, GetNvmfTargetBoundUssResponse.class);
        return rsp.getResult().stream().filter(it -> it.getUssGwId().equals(ussGwId)).findFirst().orElse(null);
    }

    public List<IscsiSeverNode> getIscsiTargetServer(String tianshuId) {
        GetIscsiTargetServerRequest req = new GetIscsiTargetServerRequest();
        req.setTianshuId(tianshuId);
        GetIscsiTargetServerResponse rsp = callErrorOut(req, GetIscsiTargetServerResponse.class);
        return rsp.getNodes();
    }


    public IscsiModule createIscsiController(String name, String tianshuId, int port, List<IscsiUssResource> uss) {
        CreateIscsiTargetRequest req = new CreateIscsiTargetRequest();
        req.setName(name);
        req.setTianshuId(tianshuId);
        req.setPort(port);
        req.setNodes(uss);
        CreateIscsiTargetResponse rsp = callErrorOut(req, CreateIscsiTargetResponse.class);
        return queryIscsiController(name);
    }

    public IscsiModule queryIscsiController(String name) {
        QueryIscsiTargetRequest req = new QueryIscsiTargetRequest();
        req.addCond("name", name);
        QueryIscsiTargetResponse rsp = queryErrorOut(req, QueryIscsiTargetResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getGateways().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public List<IscsiModule> listIscsiController() {
        QueryIscsiTargetRequest req = new QueryIscsiTargetRequest();
        QueryIscsiTargetResponse rsp = queryErrorOut(req, QueryIscsiTargetResponse.class);
        if (rsp.getTotal() == 0) {
            return Collections.emptyList();
        }

        return rsp.getGateways();
    }

    public IscsiModule getIscsiController(String id) {
        GetIscsiTargetRequest req = new GetIscsiTargetRequest();
        req.setId(id);

        GetIscsiTargetResponse rsp = callErrorOut(req, GetIscsiTargetResponse.class);
        return JSONObjectUtil.rehashObject(rsp, IscsiModule.class);
    }

    public void deleteIscsiController(String id) {
        DeleteIscsiTargetRequest req = new DeleteIscsiTargetRequest();
        req.setId(id);
        callErrorOut(req, DeleteIscsiTargetResponse.class);
    }

    public IscsiClientGroupModule queryIscsiClient(String name) {
        QueryIscsiClientGroupRequest req = new QueryIscsiClientGroupRequest();
        req.addCond("name", name);
        QueryIscsiClientGroupResponse rsp = queryErrorOut(req, QueryIscsiClientGroupResponse.class);
        if (rsp.getTotal() == 0) {
            return null;
        }

        return rsp.getClients().stream().filter(it -> it.getName().equals(name)).findFirst().orElse(null);
    }

    public IscsiClientGroupModule getIscsiClient(String id) {
        GetIscsiClientGroupRequest req = new GetIscsiClientGroupRequest();
        req.setId(id);
        GetIscsiClientGroupResponse rsp = callErrorOut(req, GetIscsiClientGroupResponse.class);

        return JSONObjectUtil.rehashObject(rsp, IscsiClientGroupModule.class);
    }

    public IscsiClientGroupModule createIscsiClient(String name, String tianshuId, List<String> clients) {
        CreateIscsiClientGroupRequest req = new CreateIscsiClientGroupRequest();
        req.setName(name);
        req.setTianshuId(tianshuId);
        List<IscsiClient> hosts = new ArrayList<>();
        if (!CollectionUtils.isEmpty(clients)) {
            for (String client : clients) {
                IscsiClient host = new IscsiClient();
                host.setHostType(client.contains("iqn") ? "iqn" : "ip");
                host.setHost(client);
                hosts.add(host);
            }
            req.setHosts(hosts);
        }
        CreateIscsiClientGroupResponse rsp = callErrorOut(req, CreateIscsiClientGroupResponse.class);
        return queryIscsiClient(name);
    }

    public void deleteIscsiClient(String id) {
        DeleteIscsiClientGroupRequest req = new DeleteIscsiClientGroupRequest();
        req.setId(id);
        req.setForce(true);
        callErrorOut(req, DeleteIscsiClientGroupResponse.class);
    }

    public void addIscsiClientToIscsiTarget(String clientId, String targetId) {
        AddIscsiClientGroupToIscsiTargetRequest req = new AddIscsiClientGroupToIscsiTargetRequest();
        req.setClients(Collections.singletonList(clientId));
        req.setId(targetId);
        callErrorOut(req, AddIscsiClientGroupToIscsiTargetResponse.class);
    }

    public void removeIscsiClientFromIscsiTarget(String clientId, String targetId) {
        RemoveIscsiClientGroupFromIscsiTargetRequest req = new RemoveIscsiClientGroupFromIscsiTargetRequest();
        req.setClients(Collections.singletonList(clientId));
        req.setId(targetId);
        callErrorOut(req, RemoveIscsiClientGroupFromIscsiTargetResponse.class);
    }

    public void addHostToIscsiClient(String host, String clientId) {
        ChangeIscsiClientGroupRequest req = new ChangeIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.add.name());
        IscsiClient iscsiClient = new IscsiClient();
        iscsiClient.setHostType(host.contains("iqn") ? "iqn" : "ip");
        iscsiClient.setHost(host);
        req.setHosts(Collections.singletonList(iscsiClient));
        callErrorOut(req, ChangeIscsiClientGroupResponse.class);
    }

    public void removeHostFromIscsiClient(String host, String clientId) {
        ChangeIscsiClientGroupRequest req = new ChangeIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.remove.name());
        IscsiClient iscsiClient = new IscsiClient();
        iscsiClient.setHostType(host.contains("iqn") ? "iqn" : "ip");
        iscsiClient.setHost(host);
        req.setHosts(Collections.singletonList(iscsiClient));
        callErrorOut(req, ChangeIscsiClientGroupResponse.class);
    }

    public List<IscsiModule> getIscsiClientAttachedTargets(String clientId) {
        GetIscsiClientGroupAttachedTargetRequest req = new GetIscsiClientGroupAttachedTargetRequest();
        req.setId(clientId);
        GetIscsiClientGroupAttachedTargetResponse rsp = callErrorOut(req, GetIscsiClientGroupAttachedTargetResponse.class);
        return rsp.getGateways();
    }

    public void addVolumeToIscsiClientGroup(String volId, String clientId, String targetId, boolean readonly) {
        ChangeVolumeInIscsiClientGroupRequest req = new ChangeVolumeInIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.add.name());
        req.setLuns(Collections.singletonList(new LunResource(volId, "volume", readonly)));
        req.setGateways(Collections.singletonList(targetId));
        ChangeVolumeInIscsiClientGroupResponse rsp = call(req, ChangeVolumeInIscsiClientGroupResponse.class);
        if (!rsp.isSuccess() && rsp.isError(ExponError.LUN_ALREADY_MAPPED_SOME_ISCSI_CLIENT) &&
                getIscsiClientGroupAttachedVolumes(clientId).contains(volId)) {
            return;
        }

        errorOut(rsp);
    }

    public void addSnapshotToIscsiClientGroup(String snapId, String clientId, String targetId) {
        ChangeSnapshotInIscsiClientGroupRequest req = new ChangeSnapshotInIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.add.name());
        req.setLuns(Collections.singletonList(new LunResource(snapId, "snapshot", true)));
        req.setGateways(Collections.singletonList(targetId));
        ChangeVolumeInIscsiClientGroupResponse rsp = call(req, ChangeVolumeInIscsiClientGroupResponse.class);
        if (!rsp.isSuccess() && rsp.isError(ExponError.LUN_ALREADY_MAPPED_SOME_ISCSI_CLIENT) &&
                getIscsiClientGroupAttachedSnapshots(clientId).contains(snapId)) {
            return;
        }

        errorOut(rsp);
    }

    public Set<String> getIscsiClientGroupAttachedVolumes(String clientId) {
        GetVolumesInIscsiClientGroupRequest req = new GetVolumesInIscsiClientGroupRequest();
        req.setId(clientId);
        GetVolumesInIscsiClientGroupResponse rsp = callErrorOut(req, GetVolumesInIscsiClientGroupResponse.class);
        return rsp.getLuns().stream().map(IscsiClientMappedLunModule::getId).collect(Collectors.toSet());
    }

    public Set<String> getIscsiClientGroupAttachedSnapshots(String clientId) {
        GetSnapshotsInIscsiClientGroupRequest req = new GetSnapshotsInIscsiClientGroupRequest();
        req.setId(clientId);
        req.limit = 100L; // iscsi client group lun limit is 64
        GetSnapshotsInIscsiClientGroupResponse rsp = callErrorOut(req, GetSnapshotsInIscsiClientGroupResponse.class);
        return rsp.getLuns().stream().map(IscsiClientMappedLunModule::getId).collect(Collectors.toSet());
    }

    public List<String> getVolumeAttachedIscsiClientGroups(String volId) {
        GetVolumeBoundIscsiClientGroupRequest req = new GetVolumeBoundIscsiClientGroupRequest();
        req.setVolumeId(volId);
        GetVolumeBoundIscsiClientGroupResponse rsp = callErrorOut(req, GetVolumeBoundIscsiClientGroupResponse.class);
        return rsp.getClients().stream().map(LunBoundIscsiClientGroupModule::getId).collect(Collectors.toList());
    }

    public List<String> getSnapshotAttachedIscsiClientGroups(String snapId) {
        String cacheClientId = snapshotClientCache.getIfPresent(snapId);
        if (cacheClientId != null) {
            if (getIscsiClientGroupAttachedSnapshots(cacheClientId).contains(snapId)) {
                return Collections.singletonList(cacheClientId);
            } else {
                snapshotClientCache.invalidate(snapId);
            }
        }

        // TODO
        QueryIscsiClientGroupRequest req = new QueryIscsiClientGroupRequest();
        QueryIscsiClientGroupResponse rsp = queryErrorOut(req, QueryIscsiClientGroupResponse.class);
        for (IscsiClientGroupModule client : rsp.getClients()) {
            if (client.getSnapNum() > 0) {
                if (getIscsiClientGroupAttachedSnapshots(client.getId()).contains(snapId)) {
                    snapshotClientCache.put(snapId, client.getId());
                    return Collections.singletonList(client.getId());
                }
            }
        }

        return Collections.emptyList();
    }

    public void removeVolumeFromIscsiClientGroup(String volId, String clientId) {
        ChangeVolumeInIscsiClientGroupRequest req = new ChangeVolumeInIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.remove.name());
        req.setLuns(Collections.singletonList(new LunResource(volId, "volume")));
        ChangeVolumeInIscsiClientGroupResponse rsp = call(req, ChangeVolumeInIscsiClientGroupResponse.class);
        if (rsp.isError(ExponError.LUN_ALREADY_UNMAPPED_ISCSI_CLIENT)) {
            return;
        }

        errorOut(rsp);
    }

    public void removeSnapshotFromIscsiClientGroup(String snapId, String clientId) {
        ChangeSnapshotInIscsiClientGroupRequest req = new ChangeSnapshotInIscsiClientGroupRequest();
        req.setId(clientId);
        req.setAction(ExponAction.remove.name());
        req.setLuns(Collections.singletonList(new LunResource(snapId, "snapshot")));
        ChangeSnapshotInIscsiClientGroupResponse rsp = call(req, ChangeSnapshotInIscsiClientGroupResponse.class);
        if (rsp.isError(ExponError.LUN_ALREADY_UNMAPPED_ISCSI_CLIENT)) {
            return;
        }

        errorOut(rsp);
    }

    public List<String> getVolumeBoundPath(String volId, List<FailureDomainModule> pools) {
        VolumeModule volume = getVolume(volId);
        List<String> paths = Lists.newArrayList();
        for (FailureDomainModule pool : pools) {
            GetFailureDomainBlacklistRequest req = new GetFailureDomainBlacklistRequest();
            req.setId(pool.getId());
            GetFailureDomainBlacklistResponse rsp = callErrorOut(req, GetFailureDomainBlacklistResponse.class);
            paths.addAll(rsp.getEntries().stream()
                    .map(BlacklistModule::getPath)
                    .filter(path -> ExponNameHelper.getVolumeNameFromBoundPath(path).equals(volume.getVolumeName()))
                    .collect(Collectors.toList()));
        }

        return paths;
    }

    public void addVolumePathToBlacklist(String path) {
        AddVolumePathToBlacklistRequest req = new AddVolumePathToBlacklistRequest();
        req.setPath(path);
        callErrorOut(req, AddVolumePathToBlacklistResponse.class);
    }

    public void removeVolumePathFromBlacklist(String path, String volId, List<FailureDomainModule> pools) {
        List<String> paths = getVolumeBoundPath(volId, pools);
        if (!paths.contains(path)) {
            return;
        }

        RemoveVolumePathFromBlacklistRequest req = new RemoveVolumePathFromBlacklistRequest();
        req.setPath(path);

        RemoveVolumePathFromBlacklistResponse rsp = call(req, RemoveVolumePathFromBlacklistResponse.class);
        // {\"code\":200502,\"msg\":\"Black list not exist\"}
        if (rsp.isError(ExponError.BLACK_LIST_OPERATION_FAILED) && rsp.getMessage().contains("list not exist")) {
            return;
        }

        errorOut(rsp);
    }

    public void removeVolumePathsFromBlacklist(List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }
        for (String p : paths) {
            RemoveVolumePathFromBlacklistRequest req = new RemoveVolumePathFromBlacklistRequest();
            req.setPath(p);

            RemoveVolumePathFromBlacklistResponse rsp = call(req, RemoveVolumePathFromBlacklistResponse.class);
            // {\"code\":200502,\"msg\":\"Black list not exist\"}
            if (rsp.isError(ExponError.BLACK_LIST_OPERATION_FAILED) && rsp.getMessage().contains("list not exist")) {
                return;
            }

            errorOut(rsp);
        }
    }

    public List<BlacklistModule> getFailureDomainBlacklist() {
        GetFailureDomainBlacklistRequest req = new GetFailureDomainBlacklistRequest();
        GetFailureDomainBlacklistResponse rsp = callErrorOut(req, GetFailureDomainBlacklistResponse.class);
        return rsp.getEntries();
    }

    public void clearFailureDomainBlacklist() {
        ClearFailureDomainBlacklistRequest req = new ClearFailureDomainBlacklistRequest();
        callErrorOut(req, ClearFailureDomainBlacklistResponse.class);
    }

    public void setTrashExpireTime(int days) {
        SetTrashExpireTimeRequest req = new SetTrashExpireTimeRequest();
        req.setTrashRecycle(days);
        callErrorOut(req, SetTrashExpireTimeResponse.class);
    }

    @Override
    public String getResourceUuid() {
        return storageUuid != null ? storageUuid :
                UUID.nameUUIDFromBytes(client.getConfig().hostname.getBytes()).toString().replace("-", "");
    }

    public VolumeModule updateVolume(String volId, String name) {
        UpdateVolumeRequest req = new UpdateVolumeRequest();
        req.setSessionId(sessionId);
        req.setId(volId);
        req.setName(name);
        UpdateVolumeResponse rsp = callErrorOut(req, UpdateVolumeResponse.class);

        return getVolume(volId);
    }

    public VolumeSnapshotModule updateVolumeSnapshot(String snapshotId, String name, String description) {
        UpdateVolumeSnapshotRequest req = new UpdateVolumeSnapshotRequest();
        req.setSessionId(sessionId);
        req.setDescription(description);
        req.setId(snapshotId);
        if (queryVolumeSnapshot(name) == null) {
            req.setName(name);
        }
        UpdateVolumeSnapshotResponse rsp = callErrorOut(req, UpdateVolumeSnapshotResponse.class);

        return queryVolumeSnapshot(name);
    }
}
