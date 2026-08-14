package org.zstack.storage.backup.imagestore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.workflow.FlowChain;
import org.zstack.header.core.workflow.FlowDoneHandler;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccessLevel;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.header.storage.backup.*;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.primary.PrimaryStorageVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagType;
import org.zstack.image.ImageSystemTags;
import org.zstack.storage.ceph.CephConstants;
import org.zstack.tag.SystemTagCreator;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;
import static org.zstack.header.storage.backup.BackupStorageConstant.IMPORT_IMAGES_FAKE_RESOURCE_UUID;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by Mei Lei <meilei007@gmail.com> on 11/3/16.
 */
public class ImageStoreBackupStorageMetaDataMaker implements AddImageExtensionPoint, AddBackupStorageExtensionPoint,
        ExpungeImageExtensionPoint, CreateImageExtensionPoint, CreateTemplateExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ImageStoreBackupStorageMetaDataMaker.class);
    @Autowired
    protected RESTFacade restf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    TagManager tagMgr;
    @Autowired
    protected CloudBus bus;

    private String buildUrl(String subPath, String hostName) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(ImageStoreBackupStorageGlobalProperty.AGENT_URL_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
        } else {
            ub.host(hostName);
        }

        ub.port(ImageStoreBackupStorageGlobalProperty.AGENT_PORT);
        if (!"".equals(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH)) {
            ub.path(ImageStoreBackupStorageGlobalProperty.AGENT_URL_ROOT_PATH);
        }
        ub.path(subPath);
        return ub.build().toUriString();
    }

    private String getAllImageInventories(ImageStoreBackupStorageDumpMetadataInfo dumpInfo) {
        TypedQuery<ImageVO> q;
        String allImageInventories = null;
        ImageInventory img = dumpInfo.getImg();

        // According to ZSTAC-14609, make sure only ready and deleted img can be dump to meta data
        String sql = "select img from ImageVO img where img.status = :status and uuid in (select imageUuid from ImageBackupStorageRefVO ref where ref.backupStorageUuid= :bsUuid and ref.status = :status)";
        q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
        q.setParameter("status", ImageStatus.Ready);
        if (dumpInfo.getImg() != null ) {
            q.setParameter("bsUuid", getBackupStorageUuidFromImageInventory(img));
        } else {
            q.setParameter("bsUuid", dumpInfo.getBackupStorageUuid());
        }
        List<ImageInventory> allImageInv = q.getResultList().stream()
                .map(imageVO -> ImageInventory.valueOf(imageVO)).collect(Collectors.toList());
        setAllImagesSystemTags(allImageInv);
        for (ImageInventory imageInv : allImageInv) {
            if (allImageInventories != null) {
                allImageInventories = JSONObjectUtil.toJsonString(imageInv) + "\n" + allImageInventories;
            } else {
                allImageInventories = JSONObjectUtil.toJsonString(imageInv);
            }
        }
        return allImageInventories;
    }

    private void setAllImagesSystemTags(List<ImageInventory> imageInventories) {
        //Load all systemTags
        if (imageInventories == null || imageInventories.size() == 0) {
            return;
        }
        List<SystemTagVO> allSystemTags = Q.New(SystemTagVO.class)
                .in(SystemTagVO_.resourceUuid, imageInventories.stream()
                        .map(img -> img.getUuid()).collect(Collectors.toList()))
                .list();
        Map<String, List<SystemTagInventory>> listMap = new HashMap<>();
        for (SystemTagVO tagVO : allSystemTags) {
            String key = tagVO.getResourceUuid();
            listMap.putIfAbsent(key, new ArrayList<>());
            listMap.get(key).add(SystemTagInventory.valueOf(tagVO));
        }
        imageInventories.forEach(img -> img.setSystemTags(listMap.get(img.getUuid())));
    }

    public void restoreImagesBackupStorageMetadataToDatabase(String imagesMetadata, String backupStorageUuid) {
        List<ImageVO> imageVOs = new ArrayList<ImageVO>();
        List<ImageBackupStorageRefVO> backupStorageRefVOs = new ArrayList<ImageBackupStorageRefVO>();
        List<SystemTagVO> systemTagVOs = new ArrayList<>();
        String[] metadatas = imagesMetadata.split("\n");

        List<String> existingImageUuids = Q.New(ImageBackupStorageRefVO.class)
                .select(ImageBackupStorageRefVO_.imageUuid)
                .eq(ImageBackupStorageRefVO_.backupStorageUuid, backupStorageUuid).listValues();

        for (String metadata : metadatas) {
            if (metadata.contains("backupStorageRefs")) {
                ImageInventory imageInventory = ImageStoreMetadataHelper.buildImageInventoryFromMetadata(metadata);

                if (!imageInventory.getStatus().equals(ImageStatus.Ready.toString())
                        || imageVOs.stream().anyMatch(image -> image.getUuid().equals(imageInventory.getUuid()))) {
                    continue;
                }

                List<ImageBackupStorageRefVO> tempBSRefVOs = new ArrayList<ImageBackupStorageRefVO>();
                for (ImageBackupStorageRefInventory ref : imageInventory.getBackupStorageRefs()) {
                    if (!ref.getStatus().equals(ImageStatus.Ready.toString())) {
                        continue;
                    }

                    if (!existingImageUuids.isEmpty()) {
                        boolean refIsExisted = existingImageUuids.contains(imageInventory.getUuid());
                        if (refIsExisted) {
                            break;
                        }
                    }

                    ImageBackupStorageRefVO backupStorageRefVO = new ImageBackupStorageRefVO();
                    backupStorageRefVO.setStatus(ImageStatus.valueOf(ref.getStatus()));
                    backupStorageRefVO.setInstallPath(ref.getInstallPath());
                    backupStorageRefVO.setImageUuid(ref.getImageUuid());
                    backupStorageRefVO.setBackupStorageUuid(backupStorageUuid);
                    backupStorageRefVO.setExportMd5Sum(ref.getExportMd5Sum());
                    backupStorageRefVO.setExportUrl(ref.getExportUrl());
                    backupStorageRefVO.setCreateDate(ref.getCreateDate());
                    backupStorageRefVO.setLastOpDate(ref.getLastOpDate());
                    tempBSRefVOs.add(backupStorageRefVO);
                    // break to avoid add duplicate ref with same backupStorageUuid and imageUuid
                    break;
                }
                if (tempBSRefVOs.isEmpty()) {
                    continue;
                }
                backupStorageRefVOs.addAll(tempBSRefVOs);

                if ((long) SQL.New("select count(*) from ImageEO where uuid = :imageUuid")
                        .param("imageUuid", imageInventory.getUuid())
                        .find() > 0) {
                    SQL.New("update ImageEO set status = :status, " +
                            "deleted = null where uuid = :imageUuid")
                            .param("status", ImageStatus.Ready)
                            .param("imageUuid", imageInventory.getUuid())
                            .execute();
                    AccountResourceRefVO ref = new AccountResourceRefVO();
                    ref.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    ref.setResourceType(ImageVO.class.getSimpleName());
                    ref.setResourceUuid(imageInventory.getUuid());
                    ref.setType(AccessLevel.Own);
                    boolean isImageExists = Q.New(AccountResourceRefVO.class)
                            .eq(AccountResourceRefVO_.resourceType, ImageVO.class.getSimpleName())
                            .eq(AccountResourceRefVO_.resourceUuid, imageInventory.getUuid())
                            .eq(AccountResourceRefVO_.type, AccessLevel.Own)
                            .isExists();
                    if (!isImageExists) {
                        dbf.persistAndRefresh(ref);
                    }
                } else {

                    ImageVO imageVO = new ImageVO();
                    imageVO.setActualSize(imageInventory.getActualSize());
                    imageVO.setDescription(imageInventory.getDescription());
                    imageVO.setStatus(ImageStatus.valueOf(imageInventory.getStatus()));
                    imageVO.setFormat(imageInventory.getFormat());
                    imageVO.setGuestOsType(imageInventory.getGuestOsType());
                    imageVO.setVirtio(imageInventory.getVirtio());
                    imageVO.setMd5Sum(imageInventory.getMd5Sum());
                    imageVO.setMediaType(ImageConstant.ImageMediaType.valueOf(imageInventory.getMediaType()));
                    imageVO.setName(imageInventory.getName());
                    if (imageInventory.getPlatform() != null) {
                        imageVO.setPlatform(ImagePlatform.valueOf(imageInventory.getPlatform()));
                    }
                    imageVO.setArchitecture(imageInventory.getArchitecture());
                    imageVO.setSize(imageInventory.getSize());
                    imageVO.setState(ImageState.valueOf(imageInventory.getState()));
                    imageVO.setSystem(imageInventory.isSystem());
                    imageVO.setType(imageInventory.getType());
                    imageVO.setUrl(imageInventory.getUrl());
                    imageVO.setUuid(imageInventory.getUuid());
                    imageVO.setCreateDate(imageInventory.getCreateDate());
                    imageVO.setLastOpDate(imageInventory.getLastOpDate());
                    imageVO.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                    imageVOs.add(imageVO);
                }

                if (imageInventory.getSystemTags() != null) {
                    for (SystemTagInventory tagInv : imageInventory.getSystemTags()) {
                        SystemTagVO systemTagVO = new SystemTagVO();
                        systemTagVO.setCreateDate(tagInv.getCreateDate());
                        systemTagVO.setLastOpDate(tagInv.getLastOpDate());
                        systemTagVO.setResourceType(tagInv.getResourceType());
                        systemTagVO.setResourceUuid(tagInv.getResourceUuid());
                        systemTagVO.setTag(tagInv.getTag());
                        systemTagVO.setType(TagType.System.toString().equals(tagInv.getType()) ? TagType.System : TagType.User);
                        systemTagVO.setUuid(tagInv.getUuid());
                        systemTagVOs.add(systemTagVO);
                    }
                }
            }
        }
        dbf.persistCollection(imageVOs);
        dbf.persistCollection(backupStorageRefVOs);
        dbf.persistCollection(systemTagVOs);
    }

    private String getBsUrlFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        List<String> bsUuids = q.listValue();
        if (bsUuids.isEmpty()) {
            return null;
        }
        String bsUuid = bsUuids.get(0);

        SimpleQuery<ImageStoreBackupStorageVO> q2 = dbf.createQuery(ImageStoreBackupStorageVO.class);
        q2.select(ImageStoreBackupStorageVO_.url);
        q2.add(ImageStoreBackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
        List<String> urls = q2.listValue();
        if (urls.isEmpty()) {
            return null;
        }

        return urls.get(0);
    }

    private String getHostNameFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        List<String> bsUuids = q.listValue();
        if (bsUuids.isEmpty()) {
            throw new CloudRuntimeException("Didn't find any available backup storage");
        }
        String bsUuid = bsUuids.get(0);

        SimpleQuery<ImageStoreBackupStorageVO> q2 = dbf.createQuery(ImageStoreBackupStorageVO.class);
        q2.select(ImageStoreBackupStorageVO_.hostname);
        q2.add(ImageStoreBackupStorageVO_.uuid, SimpleQuery.Op.EQ, bsUuid);
        return q2.findValue();
    }

    private String getBackupStorageTypeFromImageInventory(ImageInventory img) {
        String sql = "select bs.type from BackupStorageVO bs, ImageBackupStorageRefVO refVo where  " +
                "bs.uuid = refVo.backupStorageUuid and refVo.imageUuid = :uuid";

        return SQL.New(sql, String.class).param("uuid", img.getUuid()).find();
    }

    private String getBackupStorageUuidFromImageInventory(ImageInventory img) {
        SimpleQuery<ImageBackupStorageRefVO> q = dbf.createQuery(ImageBackupStorageRefVO.class);
        q.select(ImageBackupStorageRefVO_.backupStorageUuid);
        q.add(ImageBackupStorageRefVO_.imageUuid, SimpleQuery.Op.EQ, img.getUuid());
        String backupStorageUuid = q.findValue();
        DebugUtils.Assert(backupStorageUuid != null, String.format("cannot find backup storage for image [uuid:%s]", img.getUuid()));
        return backupStorageUuid;
    }

    protected void dumpImagesBackupStorageInfoToMetaDataFile(ImageStoreBackupStorageDumpMetadataInfo dumpInfo) {
        logger.debug("dump images info to meta data file");
        boolean allImagesInfo = dumpInfo.getDumpAllInfo();
        ImageInventory img = dumpInfo.getImg();
        String bsUrl = dumpInfo.getBackupStorageUrl();
        String hostName = dumpInfo.getBackupStorageHostname();
        ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileCmd dumpCmd = new ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileCmd();
        String metaData;
        if (allImagesInfo) {
            metaData = getAllImageInventories(dumpInfo);
        } else {
            metaData = JSONObjectUtil.toJsonString(img);
        }
        dumpCmd.setImageMetaData(metaData);
        dumpCmd.setDumpAllMetaData(allImagesInfo);
        if (bsUrl != null) {
            dumpCmd.setBackupStoragePath(bsUrl);
        } else {
            dumpCmd.setBackupStoragePath(getBsUrlFromImageInventory(img));
        }
        if (hostName == null || hostName.isEmpty()) {
            hostName = getHostNameFromImageInventory(img);
        }

        boolean remote = false;
        if (img != null && ImageStoreBackupStorageSelector.isRemote(img.getUuid())) {
            remote = true;
        } else if (hostName != null) {
            String bsUuid = Q.New(ImageStoreBackupStorageVO.class).eq(ImageStoreBackupStorageVO_.hostname, hostName).select(ImageStoreBackupStorageVO_.uuid).findValue();
            if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
                remote = true;
            }
        }

        if (remote) {
            thdf.syncSubmit(new SyncTask<Object>() {
                @Override
                public String getSyncSignature() {
                    if (img != null) {
                        return String.format("dump-image-metadata-file-thdf-for-image-%s", img.getUuid());
                    } else {
                        return "dump-image-metadata-file-thdf-for-all";
                    }
                }

                @Override
                public int getSyncLevel() {
                    return 10;
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }

                @Override
                public Object call() throws Exception {
                    ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp rsp = restf.syncJsonPost(
                            buildUrl(ImageStoreBackupStorageConstant.SYNC_DUMP_IMAGE_METADATA_TO_FILE, getHostNameFromImageInventory(img)),
                            dumpCmd, ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp.class, TimeUnit.SECONDS, 5);
                    if (!rsp.isSuccess()) {
                        logger.error("Dump image metadata failed");
                    } else {
                        logger.info("Dump image metadata successfully");
                    }
                    return null;
                }
            });
        } else {
            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DUMP_IMAGE_METADATA_TO_FILE, hostName), dumpCmd,
                    new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp>(null) {
                        @Override
                        public void fail(ErrorCode err) {
                            logger.error("Dump image metadata failed" + err.toString());
                        }

                        @Override
                        public void success(ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp rsp) {
                            if (!rsp.isSuccess()) {
                                logger.error("Dump image metadata failed");
                            } else {
                                logger.info("Dump image metadata successfully");
                            }
                        }

                        @Override
                        public Class<ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp> getReturnClass() {
                            return ImageStoreBackupStorageCommands.DumpImageInfoToMetaDataFileRsp.class;
                        }
                    });
        }
    }

    @Override
    public void validateAddImage(List<String> bsUuids) {
        for (String bsUuid: bsUuids) {
            if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
                // AddImage is forbidden in Disaster BS
                throw new ApiMessageInterceptionException(argerr("AddImage is forbidden in Disaster BS: [%s]", bsUuid));
            }
        }
    }

    @Override
    public void preAddImage(ImageInventory img) {
        final String uploadProtocol = "upload://";
        if (img.getUrl().startsWith(uploadProtocol)) {
            img.setUrl(String.format("%s%s/%s",
                    uploadProtocol,
                    img.getUuid(),
                    img.getUrl().substring(uploadProtocol.length())));
        }
    }

    @Override
    public void beforeAddImage(ImageInventory img) {

    }

    private void bakeImageToMetadata(ImageInventory img) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();

        chain.setName("add-image-metadata-to-backupStorage-file");
        chain.then(new ShareFlow() {
            boolean metaDataExist = false;
            String hostname = getHostNameFromImageInventory(img);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-image-metadata-file-exist";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistCmd cmd = new ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistCmd();
                        cmd.setBackupStoragePath(getBsUrlFromImageInventory(img));

                        if (ImageStoreBackupStorageSelector.isRemote(img.getUuid())) {
                            thdf.syncSubmit(new SyncTask<Object>() {
                                @Override
                                public String getSyncSignature() {
                                    return String.format("check-image-metadata-file-existed-thdf-for-image-%s", img.getUuid());
                                }

                                @Override
                                public int getSyncLevel() {
                                    return 10;
                                }

                                @Override
                                public String getName() {
                                    return getSyncSignature();
                                }

                                @Override
                                public Object call() throws Exception {
                                    ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp = restf.syncJsonPost(buildUrl(
                                            ImageStoreBackupStorageConstant.SYNC_CHECK_IMAGE_METADATA_FILE_EXIST, getHostNameFromImageInventory(img)),
                                            cmd, ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp.class, TimeUnit.SECONDS, 5);
                                    if (!rsp.isSuccess()) {
                                        logger.error(String.format("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                        ErrorCode ec = operr("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                        trigger.fail(ec);
                                    } else {
                                        if (!rsp.getExist()) {
                                            logger.info(String.format("Image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                            // call generate and dump all image info to yaml
                                            trigger.next();
                                        } else {
                                            logger.info(String.format("Image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                            metaDataExist = true;
                                            trigger.next();
                                        }
                                    }
                                    return null;
                                }
                            });
                        } else {
                            thdf.syncSubmit(new SyncTask<Object>() {
                                @Override
                                public String getSyncSignature() {
                                    return String.format("check-image-metadata-file-existed-thdf-for-image-%s-on-%s", img.getUuid(), hostname);
                                }

                                @Override
                                public int getSyncLevel() {
                                    return 10;
                                }

                                @Override
                                public String getName() {
                                    return getSyncSignature();
                                }

                                @Override
                                public Object call() throws Exception {
                                    restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST, getHostNameFromImageInventory(img)), cmd,
                                            new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp>(trigger) {
                                                @Override
                                                public void fail(ErrorCode err) {
                                                    logger.error("Check image metadata file exist failed" + err.toString());
                                                    trigger.fail(err);
                                                }

                                                @Override
                                                public void success(ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp) {
                                                    if (!rsp.isSuccess()) {
                                                        logger.error(String.format("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                                        ErrorCode ec = operr("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                                        trigger.fail(ec);
                                                    } else {
                                                        if (!rsp.getExist()) {
                                                            logger.info(String.format("Image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                                            // call generate and dump all image info to yaml
                                                            trigger.next();
                                                        } else {
                                                            logger.info(String.format("Image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                                            metaDataExist = true;
                                                            trigger.next();
                                                        }
                                                    }
                                                }

                                                @Override
                                                public Class<ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp> getReturnClass() {
                                                    return ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp.class;
                                                }
                                            });

                                    return null;
                                }
                            });
                        }
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "create-image-metadata-file";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        setAllImagesSystemTags(Collections.singletonList(img));
                        if (!metaDataExist) {
                            ImageStoreBackupStorageCommands.GenerateImageMetaDataFileCmd generateCmd = new ImageStoreBackupStorageCommands.GenerateImageMetaDataFileCmd();
                            generateCmd.setBackupStoragePath(getBsUrlFromImageInventory(img));

                            if (ImageStoreBackupStorageSelector.isRemote(img.getUuid())) {
                                thdf.syncSubmit(new SyncTask<Object>() {
                                    @Override
                                    public String getSyncSignature() {
                                        return String.format("create-image-metadata-file-thdf-for-image-%s", img.getUuid());
                                    }

                                    @Override
                                    public int getSyncLevel() {
                                        return 10;
                                    }

                                    @Override
                                    public String getName() {
                                        return getSyncSignature();
                                    }

                                    @Override
                                    public Object call() throws Exception {
                                        ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp rsp = restf.syncJsonPost(buildUrl(
                                                ImageStoreBackupStorageConstant.SYNC_GENERATE_IMAGE_METADATA_FILE, getHostNameFromImageInventory(img)),
                                                generateCmd, ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp.class, TimeUnit.SECONDS, 5);
                                        if (!rsp.isSuccess()) {
                                            ErrorCode ec = operr("Create image metadata file sync : %s failed", rsp.getBackupStorageMetaFileName());
                                            trigger.fail(ec);
                                        } else {
                                            logger.info("Create image metadata file sync successfully");
                                            ImageStoreBackupStorageDumpMetadataInfo dumpInfo = new ImageStoreBackupStorageDumpMetadataInfo();
                                            dumpInfo.setImg(img);
                                            dumpInfo.setDumpAllInfo(true);
                                            dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
                                            trigger.next();
                                        }
                                        return null;
                                    }
                                });
                            } else {
                                restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GENERATE_IMAGE_METADATA_FILE, getHostNameFromImageInventory(img)), generateCmd,
                                        new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp>(trigger) {
                                            @Override
                                            public void fail(ErrorCode err) {
                                                logger.error("Create image metadata file failed" + err.toString());
                                                trigger.fail(err);
                                            }

                                            @Override
                                            public void success(ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp rsp) {
                                                if (!rsp.isSuccess()) {
                                                    ErrorCode ec = operr("Create image metadata file : %s failed", rsp.getBackupStorageMetaFileName());
                                                    trigger.fail(ec);
                                                } else {
                                                    logger.info("Create image metadata file successfully");
                                                    ImageStoreBackupStorageDumpMetadataInfo dumpInfo = new ImageStoreBackupStorageDumpMetadataInfo();
                                                    dumpInfo.setImg(img);
                                                    dumpInfo.setDumpAllInfo(true);
                                                    dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
                                                    trigger.next();
                                                }
                                            }

                                            @Override
                                            public Class<ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp> getReturnClass() {
                                                return ImageStoreBackupStorageCommands.GenerateImageMetaDataFileRsp.class;
                                            }
                                        });
                            }

                        } else {
                            ImageStoreBackupStorageDumpMetadataInfo dumpInfo = new ImageStoreBackupStorageDumpMetadataInfo();
                            dumpInfo.setImg(img);
                            dumpInfo.setDumpAllInfo(false);
                            dumpImagesBackupStorageInfoToMetaDataFile(dumpInfo);
                            trigger.next();
                        }
                    }
                });


                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        // do nothing
                    }
                });

            }

        }).start();
    }

    @Override
    public void afterAddImage(ImageInventory img) {
        if (!ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE.equals(getBackupStorageTypeFromImageInventory(img))) {
            return;
        }

        bakeImageToMetadata(img);
    }

    @Override
    public void failedToAddImage(ImageInventory img, ErrorCode err) {

    }

    @Override
    public void preAddBackupStorage(AddBackupStorageStruct backupStorage) {

    }

    @Override
    public void beforeAddBackupStorage(AddBackupStorageStruct backupStorage) {

    }

    @Override
    public void afterAddBackupStorage(AddBackupStorageStruct backupStorage) {
        String backupStorageType = backupStorage.getBackupStorageInventory().getType();
        if (!backupStorageType.equals(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)) {
            return;
        }

        BackupStorageInventory backupStorageInventory = backupStorage.getBackupStorageInventory();
        if (backupStorage.getSystemTags() != null) {
            List<String> imageStoreTags = tagMgr.filterSystemTags( backupStorage.getSystemTags(), ImageStoreBackupStorageVO.class.getSimpleName());
            tagMgr.createTags(imageStoreTags, null, backupStorageInventory.getUuid(), ImageStoreBackupStorageVO.class.getSimpleName());
            backupStorage.getSystemTags().removeAll(imageStoreTags);
        }

        if (!backupStorage.isImportImages()) {
            return;
        }
        ImageStoreBackupStorageInventory inv = (ImageStoreBackupStorageInventory) backupStorage.getBackupStorageInventory();
        logger.debug("Start importing images metadata");
        restoreImagesBackupStorageMetadataToDatabase(inv, new NopeCompletion());
    }

    public void restoreImagesBackupStorageMetadataToDatabase(ImageStoreBackupStorageInventory inv, Completion completion) {
        ImageStoreBackupStorageCommands.GetImagesMetaDataCmd cmd = new ImageStoreBackupStorageCommands.GetImagesMetaDataCmd();
        cmd.setBackupStoragePath(inv.getUrl());
        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.GET_IMAGES_METADATA, inv.getHostname()), cmd,
                new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.GetImagesMetaDataRsp>(null) {
                    @Override
                    public void fail(ErrorCode err) {
                        logger.error("Check image metadata file exist failed" + err.toString());
                        completion.fail(err);
                    }

                    @Override
                    public void success(ImageStoreBackupStorageCommands.GetImagesMetaDataRsp rsp) {
                        if (!rsp.isSuccess()) {
                            String error = String.format("Get images metadata: %s failed", rsp.getImagesMetaData());
                            logger.error(error);
                            completion.fail(operr(error));
                        } else {
                            logger.info(String.format("Get images metadata: %s success", rsp.getImagesMetaData()));
                            RestoreImagesBackupStorageMetadataToDatabaseMsg rmsg = new RestoreImagesBackupStorageMetadataToDatabaseMsg();
                            rmsg.setImagesMetadata(rsp.getImagesMetaData());
                            rmsg.setBackupStorageUuid(inv.getUuid());
                            bus.makeTargetServiceIdByResourceUuid(rmsg, BackupStorageConstant.SERVICE_ID, IMPORT_IMAGES_FAKE_RESOURCE_UUID);
                            bus.send(rmsg, new CloudBusCallBack(completion) {
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
                    }

                    @Override
                    public Class<ImageStoreBackupStorageCommands.GetImagesMetaDataRsp> getReturnClass() {
                        return ImageStoreBackupStorageCommands.GetImagesMetaDataRsp.class;
                    }
                });
    }

    public void failedToAddBackupStorage(AddBackupStorageStruct backupStorage, ErrorCode err) {

    }

    public void preExpungeImage(ImageInventory img) {

    }

    public void beforeExpungeImage(ImageInventory img) {

    }

    public void afterExpungeImage(ImageInventory img, String backupStorageUuid) {
        ImageStoreBackupStorageVO imageStoreBackupStorageVO = Q.New(ImageStoreBackupStorageVO.class)
                .eq(ImageStoreBackupStorageVO_.uuid, backupStorageUuid)
                .find();

        if (imageStoreBackupStorageVO == null) {
            return;
        }
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName("delete-image-info-from-metadata-file");
        String hostName = imageStoreBackupStorageVO.getHostname();
        String bsUrl = imageStoreBackupStorageVO.getUrl();
        chain.then(new ShareFlow() {
            boolean metaDataExist = false;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "check-image-metadata-file-exist";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistCmd cmd = new ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistCmd();
                        cmd.setBackupStoragePath(bsUrl);

                        if (ImageStoreBackupStorageSelector.isRemote(img.getUuid())) {
                            thdf.syncSubmit(new SyncTask<Object>() {
                                @Override
                                public String getSyncSignature() {
                                    return String.format("check-image-metadata-file-existed-thdf-for-image-%s", img.getUuid());
                                }

                                @Override
                                public int getSyncLevel() {
                                    return 10;
                                }

                                @Override
                                public String getName() {
                                    return getSyncSignature();
                                }

                                @Override
                                public Object call() throws Exception {
                                    ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp = restf.syncJsonPost(buildUrl(
                                            ImageStoreBackupStorageConstant.SYNC_CHECK_IMAGE_METADATA_FILE_EXIST, getHostNameFromImageInventory(img)),
                                            cmd, ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp.class, TimeUnit.SECONDS, 5);
                                    if (!rsp.isSuccess()) {
                                        logger.error(String.format("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                        ErrorCode ec = operr("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                        trigger.fail(ec);
                                    } else {
                                        if (!rsp.getExist()) {
                                            logger.info(String.format("Image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                            // call generate and dump all image info to yaml
                                            trigger.next();
                                        } else {
                                            logger.info(String.format("Image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                            metaDataExist = true;
                                            trigger.next();
                                        }
                                    }
                                    return null;
                                }
                            });
                        } else {
                            restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.CHECK_IMAGE_METADATA_FILE_EXIST, hostName), cmd,
                                    new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp>(trigger) {
                                        @Override
                                        public void fail(ErrorCode err) {
                                            logger.error("Check image metadata file exist failed" + err.toString());
                                            trigger.fail(err);
                                        }

                                        @Override
                                        public void success(ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp rsp) {
                                            if (!rsp.isSuccess()) {
                                                logger.error(String.format("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName()));
                                                ErrorCode ec = operr("Check image metadata file: %s failed", rsp.getBackupStorageMetaFileName());
                                                trigger.fail(ec);
                                            } else {
                                                if (!rsp.getExist()) {
                                                    logger.info(String.format("Image metadata file %s is not exist", rsp.getBackupStorageMetaFileName()));
                                                } else {
                                                    logger.info(String.format("Image metadata file %s exist", rsp.getBackupStorageMetaFileName()));
                                                }
                                                trigger.next();
                                            }
                                        }

                                        @Override
                                        public Class<ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp> getReturnClass() {
                                            return ImageStoreBackupStorageCommands.CheckImageMetaDataFileExistRsp.class;
                                        }
                                    });
                        }
                    }
                });


                flow(new NoRollbackFlow() {
                    String __name__ = "delete-image-info";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileCmd deleteCmd = new ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileCmd();
                        deleteCmd.setImageUuid(img.getUuid());
                        deleteCmd.setImageBackupStorageUuid(backupStorageUuid);
                        deleteCmd.setBackupStoragePath(bsUrl);
                        restf.asyncJsonPost(buildUrl(ImageStoreBackupStorageConstant.DELETE_IMAGES_METADATA, hostName), deleteCmd,
                                new JsonAsyncRESTCallback<ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp>(trigger) {
                                    @Override
                                    public void fail(ErrorCode err) {
                                        logger.error("delete image metadata file failed" + err.toString());
                                        trigger.fail(err);
                                    }

                                    @Override
                                    public void success(ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp rsp) {
                                        if (!rsp.isSuccess()) {
                                            ErrorCode ec = operr("delete image metadata file failed: %s", rsp.getError());
                                            trigger.fail(ec);
                                        } else {
                                            // agent will reply return code 0 if success
                                                logger.info(String.format("delete image %s metadata successfully", img.getUuid()));
                                                trigger.next();
                                        }
                                    }

                                    @Override
                                    public Class<ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp> getReturnClass() {
                                        return ImageStoreBackupStorageCommands.DeleteImageInfoFromMetaDataFileRsp.class;
                                    }
                                });


                    }
                });

                done(new FlowDoneHandler(null) {
                    @Override
                    public void handle(Map data) {
                        // do nothing
                    }
                });

            }
        }).start();
    }

    public void failedToExpungeImage(ImageInventory img, ErrorCode err) {

    }

    private void createImageSystemTagForSourcePSType(ImageInventory inv) {
        SystemTagCreator creator = ImageSystemTags.IMAGE_SOURCE_TYPE.newSystemTagCreator(inv.getUuid());
        creator.inherent = true;
        creator.recreate = false;
        creator.unique = false;
        creator.setTagByTokens(map(e(ImageSystemTags.IMAGE_SOURCE_TYPE_TOKEN, CephConstants.CEPH_PRIMARY_STORAGE_TYPE)));
        creator.create();
    }

    private void createImageSystemTagForDisaster(ImageInventory inv) {
        // indicate it is image for backup remote
        SystemTagCreator creator = ImageSystemTags.IMAGE_DEPLOY_REMOTE.newSystemTagCreator(inv.getUuid());
        creator.inherent = true;
        creator.recreate = false;
        creator.unique = false;
        creator.resourceClass = ImageVO.class;
        creator.create();
    }

    @Override
    public void beforeSyncImage(ImageInventory inv, String bsUuid) {
        ImageStoreBackupStorageVO bvo = dbf.findByUuid(bsUuid, ImageStoreBackupStorageVO.class);
        if (bvo == null) {
            return;
        }

        if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
            createImageSystemTagForDisaster(inv);
        }
    }

    @Override
    public void beforeCreateImage(ImageInventory inv, String bsUuid, String psUuid) {
        if (bsUuid == null || !dbf.isExist(bsUuid, ImageStoreBackupStorageVO.class)) {
            return;
        }

        if (ImageStoreBackupStorageSelector.isRemote(bsUuid)) {
            createImageSystemTagForDisaster(inv);
        }

        String psType = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, psUuid)
                .select(PrimaryStorageVO_.type)
                .findValue();
        switch (psType) {
            case CephConstants.CEPH_PRIMARY_STORAGE_TYPE: {
                createImageSystemTagForSourcePSType(inv);
                break;
            }
            default: break;
        }
    }

    @Override
    public void afterCreateTemplate(ImageInventory inv) {
        if (!ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE.equals(getBackupStorageTypeFromImageInventory(inv))) {
            return;
        }

        bakeImageToMetadata(inv);
    }

    class ImageDescription {
        String name;
        String desc;
        String mediaType;
        String platform;
        String format;
        Long actualSize;
    }

    @Override
    public String getImageDescription(ImageInventory img) {
        ImageDescription desc = new ImageDescription();
        desc.name = img.getName();
        desc.desc = img.getDescription();
        desc.mediaType = img.getMediaType();
        desc.platform = img.getPlatform();
        desc.format = img.getFormat();
        desc.actualSize = img.getActualSize();
        return JSONObjectUtil.toJsonString(desc);
    }
}
