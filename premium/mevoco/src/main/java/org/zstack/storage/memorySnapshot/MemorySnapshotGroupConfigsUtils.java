package org.zstack.storage.memorySnapshot;

import org.zstack.core.Platform;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.tag.TagInventory;
import org.zstack.header.vm.*;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.resourceconfig.ResourceConfig;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.resourceconfig.ResourceConfigVO;
import org.zstack.resourceconfig.ResourceConfigVO_;
import org.zstack.tag.SystemTag;
import org.zstack.tag.TagManager;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.storage.memorySnapshot.CoreMemorySnapshotConfigs.*;

public class MemorySnapshotGroupConfigsUtils {
    private static final CLogger logger = Utils.getLogger(MemorySnapshotGroupConfigsUtils.class);

    public static List<SystemTagBundle> getSystemTagsForArchive(String resourceUuid) {
        if (resourceUuid == null) {
            logger.warn("resourceUuid is null, cannot get system tags for archive");
            return Collections.emptyList();
        }

        List<SystemTagBundle> bundleList = new ArrayList<>();
        List<SystemTagVO> systemTags = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid).list();
        systemTags.forEach(vo -> {
            SystemTagBundle bundle = new SystemTagBundle();
            bundle.setUuid(vo.getUuid());
            bundle.setResourceUuid(vo.getResourceUuid());
            bundle.setResourceType(vo.getResourceType());
            bundle.setTag(vo.getTag());
            bundleList.add(bundle);
        });
        return bundleList;
    }

    public static List<ResourceConfigBundle> getVmResourceConfigsForArchive(String resourceUuid) {
        if (!Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, resourceUuid).isExists()) {
            throw new RuntimeException(String.format("resourceUuid[%s] is not a valid VmInstanceVO uuid, cannot get vm resource configs for archive", resourceUuid));
        }
        return buildBundleList(vmRestoreCandidateConfigs, resourceUuid);
    }

    public static List<ResourceConfigBundle> getVolumeResourceConfigsForArchive(String resourceUuid) {
        if (!Q.New(VolumeVO.class).eq(VolumeVO_.uuid, resourceUuid).isExists()) {
            throw new RuntimeException(String.format("resourceUuid[%s] is not a valid VolumeVO uuid, cannot get volume resource configs for archive", resourceUuid));
        }
        return buildBundleList(volumeRestoreCandidateConfigs, resourceUuid);
    }

    public static List<ResourceConfigBundle> getVmNicResourceConfigsForArchive(String resourceUuid) {
        if (!Q.New(VmNicVO.class).eq(VmNicVO_.uuid, resourceUuid).isExists()) {
            throw new RuntimeException(String.format("resourceUuid[%s] is not a valid VmNicVO uuid, cannot get vmNic resource configs for archive", resourceUuid));
        }
        return buildBundleList(vmNicRestoreCandidateConfigs, resourceUuid);
    }

    public static List<ResourceConfigBundle> buildBundleList(List<GlobalConfig> restoreCandidateConfigs, String resourceUuid) {
        List<ResourceConfigBundle> bundleList = new ArrayList<>();
        restoreCandidateConfigs.forEach(it -> {
            ResourceConfigFacade rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
            String value = rcf.getResourceConfigValue(it, resourceUuid, String.class);
            ResourceConfigBundle bundle = new ResourceConfigBundle();
            bundle.setResourceUuid(resourceUuid);
            bundle.setValue(value);
            bundle.setCategory(it.getCategory());
            bundle.setName(it.getName());
            bundleList.add(bundle);
        });
        return bundleList;
    }

    public static void restoreConfigs(String resourceUuid, ArchiveBundle archiveBundle) {
        if (resourceUuid == null) {
            logger.warn("resourceUuid is null, skipping restore configs");
            return;
        }
        if (archiveBundle == null) {
            logger.warn(String.format("archiveBundle is null for resourceUuid[%s], skipping restore configs", resourceUuid));
            return;
        }

        restoreSystemTags(resourceUuid, archiveBundle.getSystemTagBundles());
        restoreResourceConfigs(resourceUuid, archiveBundle.getResourceConfigBundles());
    }

    public static void restoreSystemTags(String resourceUuid, List<SystemTagBundle> archivedSystemTagBundles) {
        if (resourceUuid == null) {
            logger.warn("resourceUuid is null, skipping restore systemTags");
            return;
        }

        TagManager tagMgr = Platform.getComponentLoader().getComponent(TagManager.class);
        List<SystemTagVO> currentSystemTagVOs = Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceUuid, resourceUuid).list();

        if (CollectionUtils.isEmpty(archivedSystemTagBundles) && currentSystemTagVOs.isEmpty()) {
            logger.debug("no archived system tag bundles to restore and no current system tags to manage. skipping restore.");
            return;
        }

        List<SystemTag> restoreCandidateTags = new ArrayList<>();
        restoreCandidateTags.addAll(restoreCandidatePatternedSystemTags);
        restoreCandidateTags.addAll(restoreCandidateSystemTags);

        logger.info(String.format("Before system tag restoration for resourceUuid[%s]: Current SystemTags: %s",
                resourceUuid,
                currentSystemTagVOs.stream()
                        .map(vo -> String.format("uuid[%s], tag[%s]", vo.getUuid(), vo.getTag()))
                        .collect(Collectors.joining("; "))));

        logger.info(String.format("Before system tag restoration for resourceUuid[%s]: Candidate tags for restoration (patterned and regular): %s",
                resourceUuid,
                restoreCandidateTags.stream()
                        .map(SystemTag::getTagFormat)
                        .collect(Collectors.joining("; "))));

        logger.info(String.format("Before system tag restoration for resourceUuid[%s]: Archived system tag bundles: %s",
                resourceUuid,
                archivedSystemTagBundles.stream()
                        .map(bundle -> String.format("tag[%s]", bundle.getTag()))
                        .collect(Collectors.joining("; "))));

        new SQLBatch() {
            @Override
            protected void scripts() {
                // First, retain current system tags that are not in the restoreCandidateTags
                Set<String> uuidsToRetain = new HashSet<>();

                if (!currentSystemTagVOs.isEmpty()) {
                    currentSystemTagVOs.forEach(vo -> {
                        SystemTag currentSystemTag = tagMgr.findMatchingSystemTag(vo.getTag());
                        if (currentSystemTag == null) {
                            logger.debug(String.format("SystemTagVO[%s] with tag[%s] is not found in TagManager, skipping", vo.getUuid(), vo.getTag()));
                            return;
                        }
                        if (restoreCandidateTags.stream().noneMatch(candidateTag -> Objects.equals(candidateTag.getTagFormat(), currentSystemTag.getTagFormat()))) {
                            uuidsToRetain.add(vo.getUuid());
                            logger.debug(String.format("SystemTagVO[%s] with tag[%s] is not in restore candidate tags, retaining it", vo.getUuid(), vo.getTag()));
                        }
                    });
                }

                // Then, restore system tags from the archived bundles
                archivedSystemTagBundles.forEach(tagBundle -> {
                    SystemTag tagBundleSystemTag = tagMgr.findMatchingSystemTag(tagBundle.getTag());
                    if (tagBundleSystemTag == null) {
                        logger.warn(String.format("cannot find matching SystemTag for tag[%s], skipping restore", tagBundle.getTag()));
                        return;
                    }

                    boolean systemTagIsDeleted = true;
                    for (SystemTagVO vo : currentSystemTagVOs) {
                        SystemTag currentSystemTag = tagMgr.findMatchingSystemTag(vo.getTag());
                        if (currentSystemTag == null) {
                            logger.warn(String.format("cannot find matching SystemTag for tag[%s], skipping restore", tagBundle.getTag()));
                            continue;
                        }

                        if (!Objects.equals(tagBundleSystemTag.getTagFormat(), currentSystemTag.getTagFormat())) {
                            continue;
                        }
                        systemTagIsDeleted = false;
                        if (!Objects.equals(tagBundle.getTag(), vo.getTag())) {
                            tagMgr.updateSystemTag(vo.getUuid(), tagBundle.getTag());
                            logger.debug(String.format("updating SystemTagVO[%s] from '%s' to '%s'", vo.getUuid(), vo.getTag(), tagBundle.getTag()));
                        }
                        uuidsToRetain.add(vo.getUuid());
                        break;
                    }
                    if (systemTagIsDeleted) {
                        TagInventory tagInventory = tagMgr.createNonInherentSystemTag(resourceUuid, tagBundle.getTag(), tagBundle.getResourceType());
                        uuidsToRetain.add(tagInventory.getUuid());
                        logger.debug(String.format("systemTag[%s] is deleted, creating new SystemTagVO[%s] for resourceUuid[%s]", tagBundle.getTag(), tagInventory.getUuid(), resourceUuid));
                    }
                });

                Set<String> currentUuids = currentSystemTagVOs.stream().map(SystemTagVO::getUuid).collect(Collectors.toSet());
                Set<String> uuidsToDelete = new HashSet<>();
                currentUuids.forEach(uuid -> {
                    if (!uuidsToRetain.contains(uuid)) {
                        uuidsToDelete.add(uuid);
                    }
                });

                // Finally, delete obsolete system tags that are not retained
                if (!uuidsToDelete.isEmpty()) {
                    sql(SystemTagVO.class).in(SystemTagVO_.uuid, uuidsToDelete).delete();
                    logger.debug(String.format("deleted SystemTagVOs[uuid:%s]", uuidsToDelete));
                }
            }
        }.execute();
    }

    public static void restoreResourceConfigs(String resourceUuid, List<ResourceConfigBundle> archivedResourceConfigBundles) {
        if (resourceUuid == null) {
            logger.warn("resourceUuid is null, skipping restore resource configs");
            return;
        }

        ResourceConfigFacade rcf = Platform.getComponentLoader().getComponent(ResourceConfigFacade.class);
        List<ResourceConfigVO> currentConfigVOs = Q.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, resourceUuid).list();

        if (CollectionUtils.isEmpty(archivedResourceConfigBundles) && currentConfigVOs.isEmpty()) {
            logger.debug("no archived resource configs bundles to restore and no current resource configs to manage. skipping restore.");
            return;
        }

        logger.info(String.format("Before resource configs restoration for resourceUuid[%s]: Current resource configs: %s",
                resourceUuid,
                currentConfigVOs.stream()
                        .map(vo -> String.format("uuid[%s], category[%s], name[%s], value[%s]",
                                vo.getUuid(), vo.getCategory(), vo.getName(), vo.getValue()))
                        .collect(Collectors.joining("; "))));

        logger.info(String.format("Before resource configs restoration for resourceUuid[%s]: Candidate resource configs for restoration (category and name): %s",
                resourceUuid, String.join("; ", getRestoreCandidateConfigsIdentity())));

        logger.info(String.format("Before resource configs restoration for resourceUuid[%s]: Archived resource config bundles: %s",
                resourceUuid,
                archivedResourceConfigBundles.stream()
                        .map(vo -> String.format("uuid[%s], category[%s], name[%s], value[%s]",
                                vo.getUuid(), vo.getCategory(), vo.getName(), vo.getValue()))
                        .collect(Collectors.joining("; "))));

        new SQLBatch() {
            @Override
            protected void scripts() {
                Set<String> uuidsToRetain = new HashSet<>();
                if (!currentConfigVOs.isEmpty()) {
                    currentConfigVOs.forEach(vo -> {
                        if (!getRestoreCandidateConfigsIdentity().contains(vo.getIdentity())) {
                            uuidsToRetain.add(vo.getUuid());
                            logger.debug(String.format("ResourceConfigVO[%s] with identity[%s] is not in restore candidate configs, retaining it", vo.getUuid(), vo.getIdentity()));
                        }
                    });
                }

                archivedResourceConfigBundles.forEach(archivedResourceConfigBundle -> {
                    String identity = archivedResourceConfigBundle.getIdentity();
                    String value = archivedResourceConfigBundle.getValue();
                    ResourceConfig resourceConfig = rcf.getResourceConfig(identity);

                    boolean configIsDeleted = true;
                    for (ResourceConfigVO vo : currentConfigVOs) {
                        if (!resourceConfig.isMatch(vo.getIdentity())) {
                            continue;
                        }
                        configIsDeleted = false;
                        if (!vo.getValue().equals(value)) {
                            resourceConfig.updateValue(resourceUuid, value);
                            logger.debug(String.format("updating ResourceConfigVO[%s] from '%s' to '%s'", vo.getUuid(), vo.getValue(), value));
                        }
                        uuidsToRetain.add(vo.getUuid());
                        break;
                    }

                    if (configIsDeleted) {
                        resourceConfig.updateValue(resourceUuid, value);
                        String newUuid = resourceConfig.getResourceConfigUuid(resourceUuid, value);
                        if (newUuid != null) {
                            uuidsToRetain.add(newUuid);
                        }
                    }
                });

                Set<String> currentUuids = currentConfigVOs.stream().map(ResourceConfigVO::getUuid).collect(Collectors.toSet());
                Set<String> uuidsToDelete = new HashSet<>();
                currentUuids.forEach(uuid -> {
                    if (!uuidsToRetain.contains(uuid)) {
                        uuidsToDelete.add(uuid);
                    }
                });
                if (!uuidsToDelete.isEmpty()) {
                    sql(ResourceConfigVO.class).in(ResourceConfigVO_.uuid, uuidsToDelete).delete();
                    logger.debug(String.format("deleted ResourceConfigVOs[uuids:%s]", uuidsToDelete));
                }
            }
        }.execute();
    }
}
