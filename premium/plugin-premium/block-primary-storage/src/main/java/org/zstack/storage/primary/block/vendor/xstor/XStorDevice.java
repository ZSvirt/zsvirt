package org.zstack.storage.primary.block.vendor.xstor;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.http.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.rest.RestHttp;
import org.zstack.header.volume.VolumeProvisioningStrategy;
import org.zstack.storage.primary.block.BlockScsiLunVO;
import org.zstack.storage.primary.block.LunErrors;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.zstack.core.Platform.*;
import static org.zstack.core.Platform.err;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/6 09:57
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class XStorDevice extends XStorMetadata{
    private static final CLogger logger = Utils.getLogger(XStorDevice.class);
    public static final String REST_API_LOGIN_PATH = "/login";
    public static final String REST_API_ADD_HOST_PATH = "/host/add/";
    public static final String REST_API_GET_HOST_PATH = "/host/get/";
    public static final String REST_API_DELETE_HOST_PATH = "/host/delete/";
    public static final String REST_API_CREATE_HOST_GROUP_PATH = "/hostgroup/create/";
    public static final String REST_API_GET_HOST_GROUP_PATH = "/hostgroup/get";
    public static final String REST_API_DELETE_HOST_GROUP_PATH = "/hostgroup/delete/";
    public static final String REST_API_GET_ACCESS_ZONES_PATH = "/accesszone/list";
    public static final String REST_API_GET_STORAGE_POOLS_PATH = "/storagepools/list";
    public static final String REST_API_ADD_INITIATOR_PATH = "/host/addInitiator";
    public static final String REST_API_GET_INITIATOR_PATH = "/host/getInitiator";
    public static final String REST_API_GET_INITIATOR_BY_NAME_PATH = "/host/getInitiatorsByName/";
    public static final String REST_API_DELETE_INITIATOR_PATH = "/host/removeInitiator/";
    public static final String REST_API_CREATE_LUN_PATH = "/lun/create";
    public static final String REST_API_UPDATE_LUN_PATH = "/lun/update";
    public static final String REST_API_CLONE_LUN_PATH = "/lun/createClone";
    public static final String REST_API_COPY_LUN_PATH = "/lun/createCopyLun";
    public static final String REST_API_GET_LUN_PATH = "/lun/get";
    public static final String REST_API_GET_LUN_BY_NAME_PATH = "/lun/getByName/";
    public static final String REST_API_GET_REMAIN_CREATED_LUN_NUMBER_PATH = "/lun/getRemainCreatedLunNumber";
    public static final String REST_API_GET_LUN_SESSION_PATH = "/lun/getSession/";
    public static final String REST_API_DELETE_LUN_PATH = "/lun/tryDelete";
    public static final String REST_API_GET_LUN_MAP_PATH = "/lun/getLunMap";
    public static final String REST_API_GET_LUN_MAP_BY_LUN_ID_PATH = "/lun/getLunMapByLunId";
    public static final String REST_API_GET_LUN_MAP_BY_HOST_GROUP_PATH = "/lun/getLunMapByHostGroupId";
    public static final String REST_API_CREATE_LUN_MAP_PATH = "/lun/createLunMap";
    public static final String REST_API_DELETE_LUN_MAP_PATH = "/lun/deleteLunMap";
    public static final String REST_API_GET_ACCESS_ZONE_SUBNET_PATH = "/subnet/accesszone/";
    public static final String REST_API_GET_STORAGE_POOL_INFO_PATH = "/storagepools/info/";
    public static final String REST_API_CREATE_SNAPSHOT_PATH = "/blocksnapshot/create";
    public static final String REST_API_GET_SNAPSHOT_PATH = "/blocksnapshot/get";
    public static final String REST_API_DELETE_SNAPSHOT_PATH = "/blocksnapshot/delete/";
    public static final String REST_API_REVERT_SNAPSHOT_PATH = "/blocksnapshot/revert";
    public static final String REST_API_GET_CLUSTER_OVERVIEW_PATH = "/cluster/overview";

    public static final String LUN_TYPE_THICK = "LUN_TYPE_THICK";
    public static final String LUN_TYPE_THIN = "LUN_TYPE_THIN";
    public static final String BLOCK_RE_LUN = "BLOCK_RE_LUN";
    public static final String BLOCK_RE_CLONE = "BLOCK_RE_CLONE";
    public static final String BLOCK_RE_SNAP = "BLOCK_RE_SNAP";
    public static final String XSTOR_STORAGE_DEVICE_SIMULATOR_PATH_PREFIX = "/xstor/ut/simulator/api";
    public static final Integer XSTOR_STORAGE_DEVICE_SIMULATOR_PORT = 8989;

    private static final String HEADER_TOKEN = "token";

    @Autowired
    private RESTFacade restf;
    @Autowired
    private ThreadFacade thdf;

    @NoLogging
    private String token;

    public XStorDevice() {
    }

    public XStorDevice(String metadata) {
       setIp(getIp(metadata));
       setPort(getPort(metadata));
       setUsername(getUsername(metadata));
       setPassword(getPassword(metadata));
       setStoragePools(getStoragePools(metadata));
       setAccessZones(getAccessZones(metadata));
    }

    private String getIp(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getIp();
    }

    private Integer getPort(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getPort();
    }

    private String getUsername(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getUsername();
    }

    private String getPassword(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getPassword();
    }

    private List<AccessZoneRsp.AccessZone> getAccessZones(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getAccessZones();
    }

    private List<StoragePoolRsp.StoragePool> getStoragePools(String metadata) {
        Gson gson = new GsonBuilder().create();
        XStorMetadata xStorMetadata = gson.fromJson(metadata, XStorMetadata.class);
        return xStorMetadata.getStoragePools();
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public List<Integer> getHostGroupId(List<Integer> idList) {
        return new ArrayList<>();
    }

    public List<Integer> getHostGroupId(String name) {
        return new ArrayList<>();
    }

    private String buildUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            path = XSTOR_STORAGE_DEVICE_SIMULATOR_PATH_PREFIX + path;
            ub.scheme("http");
            ub.port(XSTOR_STORAGE_DEVICE_SIMULATOR_PORT);
        } else {
            ub.scheme("https");
            ub.port(getPort());
        }
        ub.host(getIp());
        ub.path(path);
        return ub.build().toUriString();
    }

    public class LoginCmd {
        private String username;
        private String password;
        private Integer passwordOffset = 5;
        public final String clientType = "REST";

        public void setUsername(String username) {
            this.username = username;
        }

        public void setPassword(String password) {
            List<String> passwordList = new ArrayList<>();
            for ( char c : password.toCharArray()) {
                int newC = (int)c + passwordOffset;
                passwordList.add(String.valueOf(newC));
            }
            String offsetPassword = String.join("_", passwordList);
            String encodePassword = Base64.getEncoder().encodeToString(offsetPassword.getBytes());
            this.password = encodePassword;
        }
    }

    public class CreateLunCmd {
        public String name;
        public String lun_type;
        public Integer storage_pool_id;
        public Integer access_zone_id;
        public long total_bytes;
        public String enable_data_cache;
        public String authority_type;
        public Boolean create_block_snapshot_strategy;
        public StoragePoolRsp.Layout layout;

        public CreateLunCmd() {
            lun_type = LUN_TYPE_THICK;
            create_block_snapshot_strategy = false;
        }

        public void setAccess_zone_id(Integer access_zone_id) {
            this.access_zone_id = access_zone_id;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setLayout(StoragePoolRsp.Layout layout) {
            this.layout = layout;
        }

        public void setStorage_pool_id(Integer storage_pool_id) {
            this.storage_pool_id = storage_pool_id;
        }

        public void setTotal_bytes(long total_bytes) {
            this.total_bytes = total_bytes;
        }

        public void setLun_type(String lun_type) {
            this.lun_type = lun_type;
        }

        public void setAuthorityType(String authorityType) {
            this.authority_type = authorityType;
        }

        public void setEnableDataCache(String enableDataCache) {
            this.enable_data_cache = enableDataCache;
        }
    }

    public class LunInfo {
        private List<CreateLunCmd> luns;

        public void setLuns(List<CreateLunCmd> luns) {
            this.luns = luns;
        }
    }

    public class CreateCopyLunCmd {
        private String source_type;
        private Integer source_id;
        private Integer target_storage_poll_id;
        private LunInfo lun_info;

        public void setSource_id(Integer source_id) {
            this.source_id = source_id;
        }

        public void setLun_info(LunInfo lun_info) {
            this.lun_info = lun_info;
        }

        public void setSource_type(String source_type) {
            this.source_type = source_type;
        }

        public void setTarget_storage_poll_id(Integer target_storage_poll_id) {
            this.target_storage_poll_id = target_storage_poll_id;
        }
    }

    public class CreateCloneLunCmd extends CreateLunCmd {
        private Boolean is_create_snapshot;
        private Integer source_id;
        private String source_type;

        public CreateCloneLunCmd() {
            super();
            is_create_snapshot = false;
        }

        public void setSource_id(Integer source_id) {
            this.source_id = source_id;
        }

        public String getSource_type() {
            return source_type;
        }

        public void setSourceType(String source_type) {
            this.source_type = source_type;
        }
    }

    public class UpdateLunCmd {
        public String name;
        public long total_bytes;
        public Integer id;

        public void setId(Integer id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setTotalBytes(long total_bytes) {
            this.total_bytes = total_bytes;
        }

        public long getTotalBytes() {
            return total_bytes;
        }
    }

    public class CreateLunMapCmd {
        public Integer source_id;
        public String source_type;
        public Integer host_group_id;
        public Integer subnet_id;
        public String multi_path_mode;

        public CreateLunMapCmd() {
            source_type = "LUN";
        }

        public void setHost_group_id(Integer host_group_id) {
            this.host_group_id = host_group_id;
        }

        public void setSource_id(Integer source_id) {
            this.source_id = source_id;
        }

        public void setSource_type(String source_type) {
            this.source_type = source_type;
        }

        public void setSubnetId(Integer subnet_id) {
            this.subnet_id = subnet_id;
        }

        public void setMulti_path_mode(String multi_path_mode) {
            this.multi_path_mode = multi_path_mode;
        }
    }

    public class CreateSnapshotCmd {
        public String name;
        public String access_mode;
        public List<String> deletion_strategy = new ArrayList<>();
        public Integer retain_period;
        public String snap_source_type;
        public Integer snap_source_id;
        public String description;

        public CreateSnapshotCmd() {
            access_mode = "READONLY";
            deletion_strategy.add("MANUAL");
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setSnapSourceType(String snap_source_type) {
            this.snap_source_type = snap_source_type;
        }

        public void setAccessMode(String access_mode) {
            this.access_mode = access_mode;
        }

        public void setDeletionStrategy(List<String> deletion_strategy) {
            this.deletion_strategy = deletion_strategy;
        }

        public void setRetainPeriod(Integer retain_period) {
            this.retain_period = retain_period;
        }

        public void setSnapSourceId(Integer snap_source_id) {
            this.snap_source_id = snap_source_id;
        }
    }

    public class QuerySnapshotCmd {
        protected List<Integer> ids = new ArrayList<>();
        protected String source_type;
        protected String tier_type;

        public String getSourceType() {
            return source_type;
        }

        public void setSourceType(String source_type) {
            this.source_type = source_type;
        }

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }

        public void setTierType(String tier_type) {
            this.tier_type = tier_type;
        }

        public String getTierType() {
            return tier_type;
        }
    }

    public class RevertSnapshotCmd {
        public Integer snapshot_id;
        public Boolean is_create_snapshot;
        public Integer target_resource_id;
        public String target_resource_type;
        public CreateSnapshotCmd create_snapshot;

        public RevertSnapshotCmd() {
            is_create_snapshot = false;
        }

        public void setCreateSnapshot(CreateSnapshotCmd create_snapshot) {
            this.create_snapshot = create_snapshot;
        }

        public void setIsCreateSnapshot(Boolean is_create_snapshot) {
            this.is_create_snapshot = is_create_snapshot;
        }

        public void setSnapshotId(Integer snapshot_id) {
            this.snapshot_id = snapshot_id;
        }

        public void setTargetResourceId(Integer target_resource_id) {
            this.target_resource_id = target_resource_id;
        }

        public void setTargetResourceType(String target_resource_type) {
            this.target_resource_type = target_resource_type;
        }
    }

    private void login() {
        String loginUrl = buildUrl(REST_API_LOGIN_PATH);
        final String authorizationToken = "Basic b3B0YWRtaW46b380YWRtaW6hZG1pbg==";
        LoginCmd  loginCmd = new LoginCmd();
        loginCmd.setUsername(getUsername());
        loginCmd.setPassword(getPassword());

        logger.debug(String.format("login to xstor device, url:%s, body:%s, header:%s", loginUrl, JSONObjectUtil.toJsonString(loginCmd), authorizationToken));

        ResponseEntity<String> rsp = restf.http()
                .withPath(loginUrl)
                .withHeader("Authorization", authorizationToken)
                .withBodyJson(loginCmd)
                .exchange(HttpMethod.POST);

        List<String> hds = rsp.getHeaders().get("token");
        if (hds != null) {
            setToken(hds.get(0));
        } else {
            logger.debug("fail to login xstor");
        }
    }

    public void login(Completion completion) {
        String loginUrl = buildUrl(REST_API_LOGIN_PATH);
        Map<String, String> header = new HashMap<>();
        header.put("Authorization", "Basic b3B0YWRtaW46b380YWRtaW6hZG1pbg==");
        LoginCmd  loginCmd = new LoginCmd();
        loginCmd.setUsername(getUsername());
        loginCmd.setPassword(getPassword());
        try {
            ServerCommonRsp rsp = restf.syncJsonPost(loginUrl, JSONObjectUtil.toJsonString(loginCmd), header, ServerCommonRsp.class);
            if (rsp.getErr_no().equals(200)) {
                completion.success();
            } else {
                completion.fail(operr(String.format("fail to login XStor device: %s", loginUrl)));
            }
        } catch (OperationFailureException e) {
            completion.fail(e.getErrorCode());
        }
    }

    private Map<String, String> buildHeader() {
        Map<String, String> header = new HashMap<>();
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return header;
        }
        if (token == null) {
            login();
        }
        header.put(HEADER_TOKEN, token);
        return header;
    }

    public class ServerCommonRsp {
        public String detail_err_msg;
        public String err_msg;
        public Integer err_no;

        public Integer getErr_no() {
            return err_no;
        }

        public String getDetail_err_msg() {
            return detail_err_msg;
        }
    }

    public class ServerRsp extends ServerCommonRsp {
        public List<Integer> result = new ArrayList<>();
        public List<Integer> getResult() {
            return result;
        }
    }

    public class AddInitiatorRsp extends ServerCommonRsp {
        public Integer result;
    }

    public class CreateSnapshotRsp extends AddInitiatorRsp {
    }


    public class QueryLunResult {
        public Integer limit;
        public List<Lun> luns;

        public List<Lun> getLuns() {
            return luns;
        }
    }

    public class QueryLunRsp extends ServerCommonRsp {
        public QueryLunResult result;

        public QueryLunResult getResult() {
            return result;
        }
    }

    public class QuerySnapshotResult {
        public Integer limit;
        public List<Snapshot> snapshots = new ArrayList<>();

        public List<Snapshot> getSnapshots() {
            return snapshots;
        }
    }

    public class QuerySnapshotRsp extends ServerCommonRsp {
        public QuerySnapshotResult result;

        public QuerySnapshotResult getResult() {
            return result;
        }
    }

    public class QueryHostResult {
        public List<Host> hosts = new ArrayList<>();

        public Host getHostByName(String name) {
            for (Host host : hosts) {
                if (host.name.equals(name)) {
                    return host;
                }
            }
            return null;
        }

        public List<Host> getHosts() {
            return hosts;
        }
    }

    public class QueryHostRsp extends ServerCommonRsp{
        public QueryHostResult result;
    }

    public class QueryHostGroupResult {
        public List<HostGroup> host_groups = new ArrayList<>();

        public HostGroup getHostGroupByName(String name) {
            for ( HostGroup hostGroup : host_groups) {
                if (hostGroup.name.equals(name)) {
                    return hostGroup;
                }
            }
            return null;
        }
    }

    public class QueryHostGroupRsp extends ServerCommonRsp {
        public QueryHostGroupResult result;
    }

    public class QueryAccessZoneSubnetResult {
        public Integer limit;
        public String sort;
        public List<AccessZoneRsp.Subnet> subnets = new ArrayList<>();

        public List<AccessZoneRsp.Subnet> getSubnets() {
            return subnets;
        }
    }

    public class QueryAccessZoneSubnetRsp extends ServerCommonRsp {
        public QueryAccessZoneSubnetResult result;

        public QueryAccessZoneSubnetResult getResult() {
            return result;
        }
    }

    public class QueryLunMapResult {
        public Integer limit;
        public List<LunMap> lun_maps = new ArrayList<>();
        public String sort;
        public Integer start;
        public Integer total;
        public List<Object> searches = new ArrayList<>();

        public List<LunMap> getLunMaps() {
            return lun_maps;
        }
    }

    public class QueryLunMapRsp extends ServerCommonRsp {
        public QueryLunMapResult result;

        public QueryLunMapResult getResult() {
            return result;
        }
    }

    public class GetRemainCreatedLunNumberCmd {
        public Integer id;
        public String type;

        public void setType(String type) {
            this.type = type;
        }

        public void setId(Integer id) {
            this.id = id;
        }
    }

    public class ClusterOverviewRsp extends ServerCommonRsp {
        public ClusterOverview result;

        public ClusterOverview getResult() {
            return result;
        }
    }

    public class RemainCreatedLunNumber {
        public Integer remain;

        public Integer getRemain() {
            return remain;
        }
    }

    public class LunQuantityInfoRsp extends ServerCommonRsp {
        public RemainCreatedLunNumber result;

        public RemainCreatedLunNumber getResult() {
            return result;
        }
    }

    public class LunSessionState {
        public Boolean active;
        public Integer host_id;
        public String host_name;
        public Integer lun_id;
        public String lun_name;
        public Integer target_id;

        public Integer getHostId() {
            return host_id;
        }

        public Boolean getActive() {
            return active;
        }

        public Integer getLunId() {
            return lun_id;
        }

        public Integer getTargetId() {
            return target_id;
        }

        public String getHostName() {
            return host_name;
        }

        public String getLunName() {
            return lun_name;
        }
    }

    public class GetLunSessionRsp extends ServerCommonRsp {
        public List<LunSessionState> result = new ArrayList<>();

        public List<LunSessionState> getResult() {
            return result;
        }
    }

    public class AddInitiatorCmd {
        public String name;
        public String alias;
        public Integer host_id;
        public String auth_type;
        public String chap_username;
        public String chap_password;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }

        public void setAuth_type(String auth_type) {
            this.auth_type = auth_type;
        }

        public void setChap_password(String chap_password) {
            this.chap_password = chap_password;
        }

        public void setChap_username(String chap_username) {
            this.chap_username = chap_username;
        }

        public void setHost_id(Integer host_id) {
            this.host_id = host_id;
        }

        public Integer getHost_id() {
            return host_id;
        }

        public String getAlias() {
            return alias;
        }

        public String getAuth_type() {
            return auth_type;
        }

        public String getChap_password() {
            return chap_password;
        }

        public String getChap_username() {
            return chap_username;
        }
    }

    public class QueryInitiatorsRsp extends ServerCommonRsp {
        public QueryInitiatorsResult result;

        public QueryInitiatorsResult getResult() {
            return result;
        }
    }

    public class QueryInitiatorsResult {
        List<Initiator> initiators = new ArrayList<>();

        public List<Initiator> getInitiators() {
            return initiators;
        }
    }

    public class RevertSnapshotRsp extends ServerCommonRsp {
        public Integer result;

        public Integer getResult() {
            return result;
        }
    }

    public <T> T syncCall(HttpMethod method, String url, String body, Class<T> returnClass) {
        return syncCall(method, url, body, buildHeader(), returnClass);
    }

    public <T> T syncCall(HttpMethod method, String url, String body, Map<String, String> headers, Class<T> returnClass) {
        try {
            final RestHttp<T> http = restf.http(returnClass)
                    .withPath(buildUrl(url))
                    .withBody(body);

            if (headers != null) {
                if (!headers.containsKey(HEADER_TOKEN)) {
                    headers.putAll(buildHeader());
                }
                headers.forEach(http::withHeader);
            }

            T result = http.call(method);
            logger.debug("rsp get body stats: " + JSONObjectUtil.toJsonString(result));
            return result;
        } catch (OperationFailureException e) {
            login();

            final RestHttp<T> http = restf.http(returnClass)
                    .withPath(buildUrl(url))
                    .withBody(body);

            Map<String, String> header = buildHeader();
            header.forEach(http::withHeader);

            if (method == HttpMethod.DELETE) {
                http.withHeader("customizeHeaderSet", "True");
            }
            if (headers != null) {
                headers.forEach(http::withHeader);
            }
            return http.call(method);
        }
    }

    public void addInitiator(Integer hostId, String name) {
        Map<String, String> header = buildHeader();
        AddInitiatorCmd addInitiatorCmd = new AddInitiatorCmd();
        addInitiatorCmd.setName(name);
        addInitiatorCmd.setHost_id(hostId);
        addInitiatorCmd.setAuth_type("NONE");

        AddInitiatorRsp rsp = syncCall(HttpMethod.POST, REST_API_ADD_INITIATOR_PATH, JSONObjectUtil.toJsonString(addInitiatorCmd), header, AddInitiatorRsp.class);

        if (rsp.getErr_no() != 0 && (!rsp.getDetail_err_msg().contains("already exist"))) {
            throw new OperationFailureException(operr(String.format("fail to add initiator: %s to host, because of %s", name, rsp.getDetail_err_msg())));
        }

        return;
    }

    public Initiator getInitiatorByName(String initiatorName) {
        String queryInitiatorByNamePath = REST_API_GET_INITIATOR_BY_NAME_PATH + initiatorName;
        QueryInitiatorsRsp rsp = syncCall(HttpMethod.GET, queryInitiatorByNamePath, "", QueryInitiatorsRsp.class);
        if (rsp.getResult().getInitiators().isEmpty()) {
            return null;
        }
        return rsp.getResult().getInitiators().get(0);
    }

    public void copyLun(Lun source, String newName, VolumeProvisioningStrategy volumeProvisioningStrategy) {
        AccessZoneRsp.AccessZone accessZone = getAccessZones().get(0);
        StoragePoolRsp.StoragePool storagePool = getStoragePools().get(0);

        logger.debug(String.format("start to create lun on xstor server, access zone info %s, storage pool info:%s, lun layout:%s",
                getAccessZones().toString(), getStoragePools().toString(), JSONObjectUtil.toJsonString(accessZone), JSONObjectUtil.toJsonString(storagePool)));

        List<CreateLunCmd> createLunCmds = new ArrayList<>();
        CreateLunCmd createLunCmd = new CreateLunCmd();
        createLunCmd.setAccess_zone_id(accessZone.getId());
        createLunCmd.setStorage_pool_id(storagePool.getId());
        createLunCmd.setLayout(storagePool.getLayout());
        createLunCmd.setAuthorityType(storagePool.getAuthorityType());
        createLunCmd.setEnableDataCache(storagePool.getEnableDataCache());
        createLunCmd.setLun_type(transferToXStorProvisioningType(volumeProvisioningStrategy));
        createLunCmd.setName(newName);
        createLunCmds.add(createLunCmd);

        LunInfo lunInfo = new LunInfo();
        lunInfo.setLuns(createLunCmds);

        CreateCopyLunCmd createCopyLunCmd = new CreateCopyLunCmd();
        createCopyLunCmd.setSource_id(source.getId());
        createCopyLunCmd.setSource_type(source.getRootType());
        if (createCopyLunCmd.source_type == null) {
            createCopyLunCmd.setSource_type(BLOCK_RE_LUN);
        }
        createCopyLunCmd.setTarget_storage_poll_id(storagePool.getId());
        createCopyLunCmd.setLun_info(lunInfo);

        ServerCommonRsp serverCommonRsp = syncCall(HttpMethod.POST, REST_API_COPY_LUN_PATH, JSONObjectUtil.toJsonString(createCopyLunCmd), ServerCommonRsp.class);
        if (serverCommonRsp.getErr_no() != 0 ) {
            throw new OperationFailureException(operr(String.format("fail to copy lun: %s to %s, because of %s", String.valueOf(source.getId()), newName, serverCommonRsp.getDetail_err_msg())));
        }
        return;
    }

    public List<StoragePoolRsp.StoragePool> syncStoragePools() {
        Map<String, String> header = buildHeader();
        header.put("Content-Type", "application/json");
        StoragePoolRsp storagePoolRsp = syncCall(HttpMethod.GET, REST_API_GET_STORAGE_POOLS_PATH, "", header, StoragePoolRsp.class);
        if (!storagePoolRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to sync access zones because %s", storagePoolRsp.getDetail_err_msg()));
        }
        return storagePoolRsp.getStoragePools();
    }

    public List<AccessZoneRsp.AccessZone> syncAccessZones() {
        List<AccessZoneRsp.AccessZone> accessZoneList = new ArrayList<>();
        Map<String, String> header = buildHeader();
        header.put("Content-Type", "application/json");
        AccessZoneRsp accessZoneRsp = syncCall(HttpMethod.GET, REST_API_GET_ACCESS_ZONES_PATH, "", header, AccessZoneRsp.class);
        if (!accessZoneRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to sync access zones because %s", accessZoneRsp.getDetail_err_msg()));
        }
        for (AccessZoneRsp.AccessZone accessZone : accessZoneRsp.getAccessZones()) {
            String getAccessZoneSubnetPath = REST_API_GET_ACCESS_ZONE_SUBNET_PATH + String.valueOf(accessZone.getId());
            QueryAccessZoneSubnetRsp queryAccessZoneSubnetRsp = syncCall(HttpMethod.GET, getAccessZoneSubnetPath, "", header, QueryAccessZoneSubnetRsp.class);
            if (!queryAccessZoneSubnetRsp.getErr_no().equals(0)) {
                throw new OperationFailureException(operr("fail to get access zone's subnet because %s", queryAccessZoneSubnetRsp.getDetail_err_msg()));
            }
            accessZone.setSubnets(queryAccessZoneSubnetRsp.getResult().getSubnets());
            accessZoneList.add(accessZone);
        }
        return accessZoneList;
    }

    public QueryHostResult queryHost() {
        Map<String, String> header = buildHeader();
        QueryHostRsp queryHostRsp = syncCall(HttpMethod.GET, REST_API_GET_HOST_PATH, "", header, QueryHostRsp.class);
        if (!queryHostRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query all hosts, because of %s", queryHostRsp.getDetail_err_msg()));
        }
        return queryHostRsp.result;
    }

    public List<Host> queryHost(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            QueryHostResult result = queryHost();
            return result.getHosts();
        }
        Map<String, String> header = buildHeader();
        List<String> idList = Lists.transform(ids, Functions.toStringFunction());
        String queryHostPath = REST_API_GET_HOST_PATH + "?ids=" + String.join(",", idList);
        QueryHostRsp queryHostRsp = syncCall(HttpMethod.GET, queryHostPath, "", header, QueryHostRsp.class);
        if (!queryHostRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query hosts %s, because of %s",
                    ids.toString(), queryHostRsp.getDetail_err_msg()));
        }
        return queryHostRsp.result.getHosts();
    }

    public Integer addHost(Integer hostGroupId, String hostUuid) {
        Integer hostId = 0;
        String addHostPath = String.format("%s%s/LINUX/%s", REST_API_ADD_HOST_PATH, hostUuid, String.valueOf(hostGroupId));
        Map<String, String> header = buildHeader();
        ServerRsp addHostRsp = syncCall(HttpMethod.POST, addHostPath, "", header, ServerRsp.class);
        if (addHostRsp.detail_err_msg.contains("already exist")) {
            QueryHostResult result = queryHost();
            Host host = result.getHostByName(hostUuid);
            if (host == null) {
                return 0;
            }
            hostId = host.getId();
        } else if(!addHostRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to add host %s into hostGroup %s, because of %s",
                    String.valueOf(hostId), String.valueOf(hostGroupId), addHostRsp.getDetail_err_msg()));
        } else {
            hostId = addHostRsp.result.get(0);
        }
        return hostId;
    }

    public void deleteHost(Integer hostId) {
        String deleteHostPath = REST_API_DELETE_HOST_PATH + String.valueOf(hostId);
        if (hostId.equals(0) || hostId == null) {
            throw new OperationFailureException(operr("host id is mandatory but get:%s", String.valueOf(hostId)));
        }
        Map<String, String> header = buildHeader();
        ServerRsp rsp = syncCall(HttpMethod.DELETE, deleteHostPath, "", header, ServerRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete host %s, because of %s", String.valueOf(hostId), rsp.getDetail_err_msg()));
        }
    }

    public void deleteHostGroup(Integer hostGroupId) {
        String deleteHostGroupPath = REST_API_DELETE_HOST_GROUP_PATH + String.valueOf(hostGroupId);
        if (hostGroupId.equals(0) || hostGroupId == null) {
            throw new OperationFailureException(operr("host id is mandatory but get:%s", String.valueOf(hostGroupId)));
        }
        Map<String, String> header = buildHeader();
        ServerRsp rsp = syncCall(HttpMethod.DELETE, deleteHostGroupPath, "", header, ServerRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete host group %s, because of %s", String.valueOf(hostGroupId), rsp.getDetail_err_msg()));
        }
    }

    public void deleteInitiator(Integer initiatorId) {
        String deleteInitiatorPath = REST_API_DELETE_INITIATOR_PATH + String.valueOf(initiatorId);
        if (initiatorId.equals(0) || initiatorId == null) {
            throw new OperationFailureException(operr("host id is mandatory but get:%s", String.valueOf(initiatorId)));
        }
        Map<String, String> header = buildHeader();
        ServerRsp rsp = syncCall(HttpMethod.DELETE, deleteInitiatorPath, "", header, ServerRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete initiator %s, because of %s", String.valueOf(initiatorId), rsp.getDetail_err_msg()));
        }
    }

    public QueryHostGroupResult queryHostGroup() {
        Map<String, String> header = buildHeader();
        QueryHostGroupRsp queryHostGroupRsp = syncCall(HttpMethod.GET, REST_API_GET_HOST_GROUP_PATH, "", header, QueryHostGroupRsp.class);
        if (!queryHostGroupRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query host group, because of %s", queryHostGroupRsp.getDetail_err_msg()));
        }
        return queryHostGroupRsp.result;
    }

    public Integer addHostGroup(String name) {
        Integer hostGroupId = 0;
        String addHostGroupPATH = REST_API_CREATE_HOST_GROUP_PATH + name;
        Map<String, String> header = buildHeader();
        ServerRsp addHostGroupRsp = syncCall(HttpMethod.POST, addHostGroupPATH, "", header, ServerRsp.class);
        if (addHostGroupRsp.detail_err_msg.contains("already exist")) {
            QueryHostGroupResult result = queryHostGroup();
            HostGroup hostGroup = result.getHostGroupByName(name);
            if (hostGroup == null) {
                return 0;
            }
            hostGroupId = hostGroup.getId();
        } else if (!addHostGroupRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to add host group: %s, error message:%s ", name, addHostGroupRsp.getDetail_err_msg()));
        } else {
            hostGroupId = addHostGroupRsp.result.get(0);
        }
        return hostGroupId;
    }

    public List<BlockScsiLunVO> queryLun(List<Integer> ids) {
        List<String> idList = Lists.transform(ids, Functions.toStringFunction());
        String queryPath;
        if (ids.isEmpty()) {
            queryPath = REST_API_GET_LUN_PATH;
        } else {
            queryPath = REST_API_GET_LUN_PATH + "?ids=" + String.join(",", idList);
        }
        Map<String, String> header = buildHeader();
        QueryLunRsp queryLunRsp = syncCall(HttpMethod.GET, queryPath, "", header, QueryLunRsp.class);
        if (!queryLunRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query lun : %s, error message:%s ", ids.toString(), queryLunRsp.getDetail_err_msg()));
        }
        List<BlockScsiLunVO> blockScsiLunVOList = new ArrayList<>();
        List<Lun> lunList = queryLunRsp.getResult().getLuns();

        for (Lun lun : lunList) {
            BlockScsiLunVO blockScsiLunVO = lun.toBlockScsiLun();
            blockScsiLunVOList.add(blockScsiLunVO);
        }
        return blockScsiLunVOList;
    }

    public Lun queryLun(Integer id) {
        String queryPath;
        if (id == 0 || id == null) {
            return null;
        } else {
            queryPath = REST_API_GET_LUN_PATH + "?ids=" + String.valueOf(id);
        }
        return queryLunByPath(queryPath);
    }

    public Lun queryLun(String name) {
        String queryPath;
        if (name == null || name.equals("")) {
            return null;
        } else {
            queryPath = REST_API_GET_LUN_BY_NAME_PATH + name;
        }
        return queryLunByPath(queryPath);
    }

    private Lun queryLunByPath(String queryPath) {
        Map<String, String> header = buildHeader();
        QueryLunRsp queryLunRsp = syncCall(HttpMethod.GET, queryPath, "", header, QueryLunRsp.class);
        if (!queryLunRsp.getErr_no().equals(0)) {
            if (queryLunRsp.getDetail_err_msg() != null && queryLunRsp.getDetail_err_msg().contains("Can not find lun")) {
                throw new OperationFailureException(operr(LunErrors.LUN_CAN_NOT_BE_FOUND.toString()));
            } else {
                throw new OperationFailureException(operr("fail to query lun by path: %s, error message:%s ", queryPath, queryLunRsp.getDetail_err_msg()));
            }
        }
        List<Lun> lunList = queryLunRsp.getResult().getLuns();
        if (lunList.isEmpty()) {
            return null;
        }
        return lunList.get(0);
    }

    public void updateLun(String name, long totalBytes, Integer id) {
        UpdateLunCmd updateLunCmd = new UpdateLunCmd();
        updateLunCmd.setId(id);
        updateLunCmd.setName(name);
        updateLunCmd.setTotalBytes(totalBytes);
        ServerCommonRsp serverCommonRsp = syncCall(HttpMethod.PUT, REST_API_UPDATE_LUN_PATH, JSONObjectUtil.toJsonString(updateLunCmd), ServerCommonRsp.class);
        if (!serverCommonRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to update lun name: %s, error message:%s ", name, serverCommonRsp.getDetail_err_msg()));
        }
    }

    public BlockScsiLunVO createLun(BlockScsiLunVO blockScsiLunVO, VolumeProvisioningStrategy volumeProvisioningStrategy) {
        AccessZoneRsp.AccessZone accessZone = getAccessZones().get(0);
        StoragePoolRsp.StoragePool storagePool = getStoragePools().get(0);

        logger.debug(String.format("start to create lun on xstor server, access zone info %s, storage pool info:%s, lun layout:%s",
                getAccessZones().toString(), getStoragePools().toString(), JSONObjectUtil.toJsonString(accessZone), JSONObjectUtil.toJsonString(storagePool)));

        List<CreateLunCmd> createLunCmds = new ArrayList<>();
        CreateLunCmd createLunCmd = new CreateLunCmd();
        createLunCmd.setAccess_zone_id(accessZone.getId());
        createLunCmd.setStorage_pool_id(storagePool.getId());
        createLunCmd.setLayout(storagePool.getLayout());
        createLunCmd.setTotal_bytes(blockScsiLunVO.getSize());
        createLunCmd.setEnableDataCache(storagePool.getEnableDataCache());
        createLunCmd.setAuthorityType(storagePool.getAuthorityType());
        createLunCmd.setName(blockScsiLunVO.getName());
        createLunCmd.setLun_type(transferToXStorProvisioningType(volumeProvisioningStrategy));
        if (blockScsiLunVO.getName().contains("image-cache")) {
            createLunCmd.setLun_type(LUN_TYPE_THIN);
        }
        createLunCmds.add(createLunCmd);

        ServerRsp serverRsp = syncCall(HttpMethod.POST, REST_API_CREATE_LUN_PATH, JSONObjectUtil.toJsonString(createLunCmds), ServerRsp.class);

        if (serverRsp.getErr_no().equals(0) && serverRsp.getDetail_err_msg().contains("already exist")) {
            throw err(LunErrors.LUN_HAS_BEEN_CREATED, "unable to do the operation because the lun[%s] has been occupied", blockScsiLunVO.getName())
                    .withOpaque("lun.name", blockScsiLunVO.getName())
                    .toException();
        }

        if (serverRsp.getResult() == null && !serverRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to create lun name: %s, error message:%s ", blockScsiLunVO.getName(), serverRsp.getDetail_err_msg()));
        }

        Lun lun = queryLun(blockScsiLunVO.getName());
        if (lun == null || lun.getId() == 0 || lun.getWwn() == null) {
            throw new OperationFailureException(operr("fail to create lun name: %s, can not find root cause", blockScsiLunVO.getName()));
        }
        logger.debug("successfully create lun: " + JSONObjectUtil.toJsonString(lun));
        blockScsiLunVO.setId(lun.getId());
        blockScsiLunVO.setLunType(lun.getType());
        blockScsiLunVO.setWwn(lun.getWwn());
        return blockScsiLunVO;
    }

    public BlockScsiLunVO createLunFromTemplate(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy) {
        if (blockScsiLunVO.getLunType() == null) {
            blockScsiLunVO.setLunType(BLOCK_RE_LUN);
        }
        blockScsiLunVO.setName(name);
        return createLunFromOtherSource(blockScsiLunVO, volumeProvisioningStrategy);
    }

    public BlockScsiLunVO createLunFromSnapshot(BlockScsiLunVO blockScsiLunVO, String name, VolumeProvisioningStrategy volumeProvisioningStrategy) {
        blockScsiLunVO.setName(name);
        blockScsiLunVO.setLunType(BLOCK_RE_SNAP);
        return  createLunFromOtherSource(blockScsiLunVO, volumeProvisioningStrategy);
    }

    private BlockScsiLunVO createLunFromOtherSource(BlockScsiLunVO blockScsiLunVO, VolumeProvisioningStrategy volumeProvisioningStrategy) {
        AccessZoneRsp.AccessZone accessZone = getAccessZones().get(0);
        StoragePoolRsp.StoragePool storagePool = getStoragePools().get(0);

        logger.debug(String.format("start to create clone lun on xstor server, access zone info %s, storage pool info:%s, lun layout:%s",
                getAccessZones().toString(), getStoragePools().toString(), JSONObjectUtil.toJsonString(accessZone), JSONObjectUtil.toJsonString(storagePool)));

        List<CreateCloneLunCmd> createCloneLunCmds = new ArrayList<>();
        CreateCloneLunCmd createCloneLunCmd = new CreateCloneLunCmd();
        createCloneLunCmd.setAccess_zone_id(accessZone.getId());
        createCloneLunCmd.setSourceType(blockScsiLunVO.getLunType());
        createCloneLunCmd.setStorage_pool_id(storagePool.getId());
        createCloneLunCmd.setLayout(storagePool.getLayout());
        createCloneLunCmd.setLun_type(transferToXStorProvisioningType(volumeProvisioningStrategy));
        createCloneLunCmd.setTotal_bytes(blockScsiLunVO.getSize());
        createCloneLunCmd.setName(blockScsiLunVO.getName());
        createCloneLunCmd.setSource_id(blockScsiLunVO.getId());
        createCloneLunCmds.add(createCloneLunCmd);

        ServerRsp serverRsp = syncCall(HttpMethod.POST, REST_API_CLONE_LUN_PATH, JSONObjectUtil.toJsonString(createCloneLunCmds), ServerRsp.class);

        if (serverRsp.getErr_no() == 0 && serverRsp.getDetail_err_msg().contains("already exist")) {
            throw err(LunErrors.LUN_HAS_BEEN_CREATED, "unable to do the operation because the lun[%s] has been occupied", blockScsiLunVO.getName())
                    .withOpaque("lun.name", blockScsiLunVO.getName())
                    .toException();
        }

        if (serverRsp.getResult() == null && !serverRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to create lun name: %s, error message:%s ", blockScsiLunVO.getName(), serverRsp.getDetail_err_msg()));
        }

        Integer lunId = serverRsp.result.get(0);
        blockScsiLunVO.setId(lunId);

        String getLunPath = REST_API_GET_LUN_PATH + "?ids=" + String.valueOf(lunId);
        Map<String, String> header = buildHeader();
        QueryLunRsp queryLunRsp = syncCall(HttpMethod.GET, getLunPath, "", header, QueryLunRsp.class);
        if (!queryLunRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query lun %s, because of %s", String.valueOf(lunId), queryLunRsp.getDetail_err_msg()));
        }
        Lun lun = queryLunRsp.getResult().getLuns().get(0);

        blockScsiLunVO.setWwn(lun.getWwn());
        String lunType = lun.getType() == null ? BLOCK_RE_CLONE:lun.getType();
        blockScsiLunVO.setLunType(lunType);
        return blockScsiLunVO;
    }

    public Integer queryLunMap(Integer hostGroupId, Integer lunId) {

        if (lunId == null || lunId.equals(0)) {
            throw new OperationFailureException(operr("lun id is mandatory when query lun map"));
        }
        List<LunMap> lunMaps = getLunMapByLunId(lunId);
        if (lunMaps.isEmpty()) {
            return 0;
        }
        for (LunMap lunMap : lunMaps) {
            if (lunMap.getHost_group_id().equals(hostGroupId) && lunMap.getSource_id().equals(lunId)) {
                return lunMap.getId();
            }
        }
        return 0;
    }

    public List<LunMap> queryLunMap(Integer hostGroupId) {
        String queryPath;
        if (hostGroupId.equals(0) || hostGroupId == null) {
            return Collections.EMPTY_LIST;
        } else {
            queryPath = REST_API_GET_LUN_MAP_BY_HOST_GROUP_PATH + "?ids=" + String.valueOf(hostGroupId);
        }
        Map<String, String> header = buildHeader();
        QueryLunMapRsp queryLunMapRsp = syncCall(HttpMethod.GET, queryPath, "", QueryLunMapRsp.class);
        if (!queryLunMapRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query lun map for host group %s, because of %s", String.valueOf(hostGroupId), queryLunMapRsp.getDetail_err_msg()));
        }

        if (queryLunMapRsp.getResult().getLunMaps() == null) {
            return Collections.EMPTY_LIST;
        }
        return queryLunMapRsp.getResult().getLunMaps();
    }

    public List<LunMap> queryLunMap(List<Integer> lunMapIds) {
        List<String> idList = Lists.transform(lunMapIds, Functions.toStringFunction());
        String queryPath;
        if (lunMapIds.isEmpty()) {
            queryPath = REST_API_GET_LUN_MAP_PATH;
        } else {
            queryPath = REST_API_GET_LUN_MAP_PATH + "?ids=" + String.join(",", idList);
        }
        QueryLunMapRsp queryLunMapRsp = syncCall(HttpMethod.GET, queryPath, "", QueryLunMapRsp.class);
        if (!queryLunMapRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query lun map %s, because of %s", lunMapIds.toString(), queryLunMapRsp.getDetail_err_msg()));
        }

        if (queryLunMapRsp.getResult().getLunMaps() == null) {
            return Collections.EMPTY_LIST;
        }
        return queryLunMapRsp.getResult().getLunMaps();
    }

    public ClusterOverview queryCluster() {

        Map<String, String> header = buildHeader();
        header.put("Content-Type", "application/json");
        ClusterOverviewRsp clusterOverviewRsp = syncCall(HttpMethod.GET, REST_API_GET_CLUSTER_OVERVIEW_PATH, "", header, ClusterOverviewRsp.class);
        if (!clusterOverviewRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to get cluster info, because of %s", clusterOverviewRsp.getDetail_err_msg()));
        }
        return clusterOverviewRsp.getResult();
    }

    public Integer createLunMap(Integer hostGroupId, Integer lunId, String lunType) {
        AccessZoneRsp.Subnet subnet = getAccessZones().get(0).getSubnets().get(0);
        List<CreateLunMapCmd> createLunMapCmds = new ArrayList<>();
        CreateLunMapCmd createLunMapCmd = new CreateLunMapCmd();
        createLunMapCmd.setHost_group_id(hostGroupId);
        createLunMapCmd.setSource_id(lunId);
        if (!StringUtils.isEmpty(subnet.getPathMode())) {
            createLunMapCmd.setSubnetId(subnet.id);
        }
        if (lunType != null && lunType.equals("SNAP_LUN")) {
            createLunMapCmd.setSource_type(lunType);
        }
        createLunMapCmds.add(createLunMapCmd);

        Integer lunMapId = 0;
        ServerRsp serverRsp = syncCall(HttpMethod.POST, REST_API_CREATE_LUN_MAP_PATH, JSONObjectUtil.toJsonString(createLunMapCmds), ServerRsp.class);

        if (!serverRsp.getErr_no().equals(0) && serverRsp.detail_err_msg.contains("already exist")) {
            lunMapId = queryLunMap(hostGroupId, lunId);
        } else if (serverRsp.getErr_no().equals(0)){
            lunMapId = serverRsp.result.get(0);
        } else {
            throw new OperationFailureException(operr("fail to map lun %s to host group %s, because of %s",
                    String.valueOf(lunId), String.valueOf(hostGroupId), serverRsp.getDetail_err_msg()));
        }
        return lunMapId;
    }

    public void deleteLunMap(Integer lunMapId) {
        if (lunMapId == 0 || lunMapId == null) {
            throw new OperationFailureException(operr("lun map id is mandatory but get:%s", String.valueOf(lunMapId)));
        }
        String deleteLunMapPath = REST_API_DELETE_LUN_MAP_PATH + "/" + String.valueOf(lunMapId);
        Map<String, String> header = buildHeader();
        ServerRsp serverRsp = syncCall(HttpMethod.DELETE, deleteLunMapPath, "", header, ServerRsp.class);
        if (!serverRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete lun map %s, because of %s", String.valueOf(lunMapId), serverRsp.getDetail_err_msg()));
        }
    }

    public void deleteLun(Integer lunId) {
        if (lunId == null || lunId == 0) {
            throw new OperationFailureException(operr("lun id is mandatory but get:%s", String.valueOf(lunId)));
        }
        String deleteLunPath = REST_API_DELETE_LUN_PATH + "/" + String.valueOf(lunId);
        Map<String, String> header = buildHeader();
        ServerRsp serverRsp = syncCall(HttpMethod.DELETE, deleteLunPath, "", header, ServerRsp.class);
        if (!serverRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete lun %s, because of %s", String.valueOf(lunId), serverRsp.getDetail_err_msg()));
        }
    }

    public StoragePoolRsp.StoragePool getStoragePool(Integer id) {
        String getStoragePoolInfoPath = REST_API_GET_STORAGE_POOL_INFO_PATH + String.valueOf(id);
        Map<String, String> header = buildHeader();
        header.put("Content-Type", "application/json");
        StoragePoolRsp storagePoolRsp = syncCall(HttpMethod.GET, getStoragePoolInfoPath, "", header, StoragePoolRsp.class);
        if (!storagePoolRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to get storage pool %s, because of %s", String.valueOf(id), storagePoolRsp.getDetail_err_msg()));
        }
        return storagePoolRsp.getStoragePools().get(0);
    }

    public Integer createSnapshot(BlockScsiLunVO blockScsiLunVO) {
        CreateSnapshotCmd createSnapshotCmd = new CreateSnapshotCmd();
        createSnapshotCmd.setName(blockScsiLunVO.getName());
        createSnapshotCmd.setSnapSourceId(blockScsiLunVO.getId());
        createSnapshotCmd.setSnapSourceType(blockScsiLunVO.getLunType());

        CreateSnapshotRsp rsp = syncCall(HttpMethod.POST, REST_API_CREATE_SNAPSHOT_PATH, JSONObjectUtil.toJsonString(createSnapshotCmd), CreateSnapshotRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to create snapshot for lun %s, because of %s", JSONObjectUtil.toJsonString(blockScsiLunVO), rsp.getDetail_err_msg()));
        }
        return rsp.result;
    }

    public List<Snapshot> querySnapshot(List<Integer> ids, String sourceType) {
        QuerySnapshotCmd querySnapshotCmd = new QuerySnapshotCmd();
        querySnapshotCmd.setIds(ids);
        querySnapshotCmd.setSourceType(sourceType);

        QuerySnapshotRsp rsp = syncCall(HttpMethod.POST, REST_API_GET_SNAPSHOT_PATH, JSONObjectUtil.toJsonString(querySnapshotCmd), QuerySnapshotRsp.class);
        if (rsp.getResult().getSnapshots().size() == 0) {
            logger.debug("fail to find snapshot sleep 3s retry");
            try {
                TimeUnit.SECONDS.sleep(3l);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
            rsp = syncCall(HttpMethod.POST, REST_API_GET_SNAPSHOT_PATH, JSONObjectUtil.toJsonString(querySnapshotCmd), QuerySnapshotRsp.class);

        }
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to query snapshots %s, because of %s", ids.toString(), rsp.getDetail_err_msg()));
        }
        return rsp.getResult().getSnapshots();
    }

    public void deleteSnapshot(Integer snapshotId) {
        if (snapshotId == null || snapshotId == 0) {
            throw new OperationFailureException(operr("snapshot id is mandatory but get:%s", String.valueOf(snapshotId)));
        }
        String deleteLunPath = REST_API_DELETE_SNAPSHOT_PATH + String.valueOf(snapshotId);
        Map<String, String> header = buildHeader();
        header.put("customizeHeaderSet", "True");
        ServerRsp rsp = syncCall(HttpMethod.DELETE, deleteLunPath, "", header, ServerRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to delete snapshot %s, because of %s", String.valueOf(snapshotId), rsp.getDetail_err_msg()));
        }
    }

    public Lun revertSnapshot(Integer snapshotId, Integer sourceId, String sourceType) {
        RevertSnapshotCmd revertSnapshotCmd = new RevertSnapshotCmd();
        revertSnapshotCmd.setSnapshotId(snapshotId);
        revertSnapshotCmd.setTargetResourceId(sourceId);
        revertSnapshotCmd.setTargetResourceType(sourceType);
        RevertSnapshotRsp rsp = syncCall(HttpMethod.POST, REST_API_REVERT_SNAPSHOT_PATH, JSONObjectUtil.toJsonString(revertSnapshotCmd), RevertSnapshotRsp.class);
        if (!rsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to revert snapshot:%s, because of: %s", String.valueOf(snapshotId), rsp.getDetail_err_msg()));
        }

        return queryLun(sourceId);
    }

    public Boolean  checkLunSession(Integer lunId, String hostUuid) {
        String getLunSessionPath = REST_API_GET_LUN_SESSION_PATH + String.valueOf(lunId);
        Map<String, String> header = buildHeader();
        GetLunSessionRsp getLunSessionRsp = syncCall(HttpMethod.GET, getLunSessionPath, "", header, GetLunSessionRsp.class);
        if (!getLunSessionRsp.getErr_no().equals(0)) {
            throw new OperationFailureException(operr("fail to check lun %s session state , because of: %s", String.valueOf(lunId), getLunSessionRsp.getDetail_err_msg()));
        }

        for ( LunSessionState lunSessionState : getLunSessionRsp.getResult()) {
            if (lunSessionState.getHostName().equals(hostUuid)) {
                return  lunSessionState.getActive();
            }
        }
        return  false;
    }

    public List<LunMap> getLunMapByLunId(Integer lunId) {
        String getLunMapByLunIdPath = REST_API_GET_LUN_MAP_BY_LUN_ID_PATH + "?ids=" + String.valueOf(lunId);
        Map<String, String> header = buildHeader();
        QueryLunMapRsp queryLunMapRsp = syncCall(HttpMethod.GET, getLunMapByLunIdPath, "", header, QueryLunMapRsp.class);
        if (!queryLunMapRsp.getErr_no().equals(0) || queryLunMapRsp.getResult() == null) {
            throw new OperationFailureException(operr("fail to get lun %s maps, because of: %s", String.valueOf(lunId), queryLunMapRsp.getDetail_err_msg()));
        }

        if (queryLunMapRsp.getResult().getLunMaps() == null) {
            List<LunMap> lunMaps = new ArrayList<>();
            return lunMaps;
        }
        return queryLunMapRsp.getResult().getLunMaps();
    }

    public void getRemainCreatedLunNumber(Integer lunId, String type, ReturnValueCompletion<Integer> completion) {
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return String.format("get-remain-created-lun-number-of-lun-%s", String.valueOf(lunId));
            }

            @Override
            public void run(SyncTaskChain chain) {
                Map<String, String> header = buildHeader();
                header.put("customizeHeaderSet", "True");
                header.put("Content-Type", "application/json");
                GetRemainCreatedLunNumberCmd getRemainCreatedLunNumberCmd = new GetRemainCreatedLunNumberCmd();
                getRemainCreatedLunNumberCmd.setId(lunId);
                getRemainCreatedLunNumberCmd.setType(type);
                LunQuantityInfoRsp lunQuantityInfoRsp = syncCall(HttpMethod.POST, REST_API_GET_REMAIN_CREATED_LUN_NUMBER_PATH, JSONObjectUtil.toJsonString(getRemainCreatedLunNumberCmd), header, LunQuantityInfoRsp.class);
                if (!lunQuantityInfoRsp.getErr_no().equals(0) || lunQuantityInfoRsp.getResult() == null) {
                    completion.fail(operr("fail to get lun %s remain created lun number, because of: %s", String.valueOf(lunId), lunQuantityInfoRsp.getDetail_err_msg()));
                    chain.next();
                    return;
                }
                completion.success(lunQuantityInfoRsp.getResult().getRemain());
                chain.next();
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private String transferToXStorProvisioningType(VolumeProvisioningStrategy volumeProvisioningStrategy) {
        if (volumeProvisioningStrategy == null || volumeProvisioningStrategy.toString().equals(VolumeProvisioningStrategy.ThickProvisioning.toString())) {
            return LUN_TYPE_THICK;
        } else {
            return LUN_TYPE_THIN;
        }
    }
}