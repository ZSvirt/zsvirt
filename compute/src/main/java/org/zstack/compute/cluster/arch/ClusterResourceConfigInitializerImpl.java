package org.zstack.compute.cluster.arch;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.resourceconfig.*;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/1/26 13:19
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ClusterResourceConfigInitializerImpl implements ClusterResourceConfigInitializer, Component {
    protected static final CLogger logger = Utils.getLogger(ClusterResourceConfigInitializerImpl.class);
    protected Map<String, Map<String, String>> architectureClusterConfigValueMap = new HashMap<>();
    protected Map<String, Set<String>>  autoSetClusterResourceConfigs = new HashMap<>();

    @Autowired
    private ResourceConfigFacade rcf;

    @Override
    public boolean start() {
        initClusterResourceConfig();
        autoSetClusterResourceConfigForConfigMissingCluster();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void initClusterResourceConfig() {
        for (Class<? extends ClusterArchitectureResourceConfig> clz : BeanUtils.reflections.getSubTypesOf(ClusterArchitectureResourceConfig.class)) {
            logger.debug("init cluster resource config: " + clz.toString());
            try {
                ClusterArchitectureResourceConfig instance = clz.getConstructor().newInstance();
                String architecture = instance.getArchitecture();
                Map<String, String> configValueMap = instance.getResourceConfigDefaultValueMap();
                architectureClusterConfigValueMap.putIfAbsent(architecture, configValueMap);
                if (autoSetClusterResourceConfigs.containsKey(architecture)) {
                    logger.debug(String.format("The [architecture:%s] has already configured by other class, do not do this", architecture));
                    return;
                }
                Set<String> emptySet = new HashSet<>();
                autoSetClusterResourceConfigs.put(architecture, emptySet);
                instance.getAutoSetIfNotConfiguredClusterResourceConfigSet().forEach(configName -> {
                    if (!autoSetClusterResourceConfigs.get(architecture).contains(configName)) {
                        autoSetClusterResourceConfigs.get(architecture).add(configName);
                    }
                });

                Map<String, List<String>> resourceConfigValidValuesMap = instance.getResourceConfigValidValuesMap();
                if (resourceConfigValidValuesMap != null) {
                    resourceConfigValidValuesMap.forEach((identity, validValues) ->
                            registerClusterResourceConfigValidator(instance, identity, validValues));
                }
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            }
        }
    };

    private void registerClusterResourceConfigValidator(ClusterArchitectureResourceConfig instance, String identity, List<String> validValues) {
        if (validValues == null) {
            throw new CloudRuntimeException(String.format("missing validValues for resource config" +
                    "[identity: %s, architecture: %s]", identity, instance.getArchitecture()));
        }

        logger.debug(String.format("install validator for resource config[identity: %s]", identity));

        ResourceConfig rf = rcf.getResourceConfig(identity);
        if (rf == null) {
            logger.debug(String.format("resource config[identity: %s] not supported, skip validator installation", identity));
            return;
        }

        rf.installValidatorExtension((resourceUuid, oldValue, newValue) -> {
            if (!instance.clusterArchitectureMatched(resourceUuid)) {
                return;
            }

            if (!validValues.contains(newValue)) {
                throw new GlobalConfigException(String.format("cluster[uuid: %s, architecture: %s]" +
                                " supported values for resource config[identity: %s] are %s",
                        resourceUuid, instance.getArchitecture(), identity, validValues));
            }
        });
    }

    private Map<String, String> getArchitectureClusterConfigValueMap(String architecture) {
        return architectureClusterConfigValueMap.get(architecture);
    }

    public void initClusterResourceConfigValue(ClusterInventory cluster){
        String architecture = cluster.getArchitecture();
        logger.debug(String.format("start to init cluster resource config cluster[uuid:%s, architecture:%s]", cluster.getUuid(), architecture));
        if (architecture == null) {
            logger.debug(String.format("cluster[uuid:%s] architecture is none just skip this cluster", cluster.getUuid()));
            return;
        }
        Map<String, String> clusterConfigValueMap = getArchitectureClusterConfigValueMap(architecture);
        if (clusterConfigValueMap == null) {
            logger.debug("None of cluster resource config is found just skip");
            return;
        }
        clusterConfigValueMap.forEach((configName, value) -> {
            logger.debug(String.format("start to set cluster resource [config:%s, value:%s]", configName, value));
            ResourceConfig rc = rcf.getResourceConfig(configName);
            if (rc == null) {
                logger.debug(String.format("fail to resource config %s, please check its identity", configName));
                return;
            }
            rc.updateValue(cluster.getUuid(), value);
            logger.debug(String.format("update cluster resource config," +
                            " config[name:%s, value:%s, architecture:%s], cluster[uuid:%s]",
                    configName, value, cluster.getArchitecture(), cluster.getUuid()));
        });
    };

    private void autoSetClusterResourceConfigForConfigMissingCluster() {
        List<Tuple> clusters = Q.New(ClusterVO.class)
                .select(ClusterVO_.uuid, ClusterVO_.architecture)
                .listTuple();

        if (clusters == null) {
            return;
        }

        for (Tuple cluster : clusters) {
            String clusterUuid = cluster.get(0, String.class);
            String clusterArchitecture = cluster.get(1, String.class);
            Set<String> configNameList = autoSetClusterResourceConfigs.get(clusterArchitecture);

            if (configNameList == null) {
                continue;
            }

            for (String configName : configNameList) {
                ResourceConfig rc = rcf.getResourceConfig(configName);

                if (rc == null) {
                    continue;
                }

                if (rc.resourceConfigCreated(clusterUuid)) {
                    continue;
                }

                Map<String, String> configMap = getArchitectureClusterConfigValueMap(clusterArchitecture);
                String value = configMap.get(configName);
                rc.updateValue(clusterUuid, value);
            }
        }
    }
}
