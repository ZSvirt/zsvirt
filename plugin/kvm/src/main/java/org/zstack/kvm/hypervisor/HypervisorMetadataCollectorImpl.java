package org.zstack.kvm.hypervisor;

import org.zstack.kvm.KVMConstant;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.kvm.hypervisor.KvmHypervisorConstant.*;

/**
 * collect management node hypervisor metadata.
 * 
 * 1. scan current hypervisor info (include Qemu version)
 *    in local file system at {@link KvmHypervisorConstant#DVD_ROOT_PATH}
 * 2. collect hypervisor info by {@link KvmHypervisorConstant#VIRTUALIZER_INFO_SCRIPT_PATH}
 * 3. save hypervisor info to table {@link org.zstack.kvm.hypervisor.datatype.KvmHostHypervisorMetadataVO}
 * 
 * Created by Wenhao.Zhang on 23/03/01
 */
public class HypervisorMetadataCollectorImpl implements HypervisorMetadataCollector {
    private static final CLogger logger = Utils.getLogger(HypervisorMetadataCollectorImpl.class);

    @Override
    public List<HypervisorMetadataDefinition> collect() {
        List<HypervisorMetadataDefinition> definitions;

        try {
            definitions = scanFolder(DVD_ROOT_PATH);
        } catch (IOException e) {
            logger.warn("failed to scan folder : " + DVD_ROOT_PATH, e);
            return Collections.emptyList();
        }

        for (Iterator<HypervisorMetadataDefinition> it = definitions.iterator(); it.hasNext(); ) {
            HypervisorMetadataDefinition definition = it.next();

            Exception exception = null;
            boolean success = false;
            try {
                success = collectHypervisorMetadata(definition);
            } catch (Exception e) {
                exception = e;
            }
            
            if (success) {
                continue;
            }

            if (exception != null) {
                logger.warn("failed to collect hypervisor metadata at " + definition.virtualizerScriptPath(), exception);
            } else {
                logger.warn("failed to collect hypervisor metadata at " + definition.virtualizerScriptPath());
            }
            it.remove();
        }

        return definitions;
    }

    /**
     * scan iso folder from specific root path
     */
    protected List<HypervisorMetadataDefinition> scanFolder(Path rootPath) throws IOException {
        try (Stream<Path> stream = Files.list(rootPath)) { //  /opt/zstack-dvd/x86_64
            return stream
                .filter(path -> !IGNORE_DIR_AT_DVD.contains(path.getFileName().toString()))
                .filter(path -> path.toFile().isDirectory())
                .flatMap(path -> { //  /opt/zstack-dvd/x86_64/c76
                    try {
                        return Files.list(path);
                    } catch (IOException e) {
                        return Arrays.stream(new Path[0]);
                    }
                })
                .filter(path -> path.toFile().isDirectory())
                .map(this::mapFromPath)
                .collect(Collectors.toList());
        }
    }

    protected HypervisorMetadataDefinition mapFromPath(Path path) {
        HypervisorMetadataDefinition definition = new HypervisorMetadataDefinition();
        definition.setOsReleaseSimpleVersion(path.getFileName().toString());
        definition.setArchitecture(path.getParent().getFileName().toString());
        return definition;
    }

    /**
     * use shell to read metadata properties.
     * 
     * outputs like:
     *   qemu-kvm.version = 4.2.0-632.g6a6222b.el7
     *   platform.distname: centos
     *   platform.version: 7.6.1810
     *   platform.id: Core
     */
    protected String collectHypervisorMetadataAsProperties(HypervisorMetadataDefinition definition) throws IOException {
        String shellCmd = String.format("/bin/bash %s", definition.virtualizerScriptPath());
        ShellResult result = ShellUtils.runAndReturn(shellCmd);
        result.raiseExceptionIfFail();

        return result.getStdout();
    }

    private boolean collectHypervisorMetadata(HypervisorMetadataDefinition definition) throws IOException {
        if (!definition.isValid()) {
            return false;
        }

        Properties properties = new Properties();
        properties.load(new ByteArrayInputStream(
                collectHypervisorMetadataAsProperties(definition).getBytes()));

        Object qemuVersion = properties.get(KEY_QEMU_KVM_VERSION);
        if (qemuVersion != null && !qemuVersion.getClass().isArray()) {
            definition.setHypervisor(KVMConstant.VIRTUALIZER_QEMU_KVM);
            definition.setVersion(qemuVersion.toString());
        } else {
            return false;
        }

        Object platformDistName = properties.get(KEY_PLATFORM_DIST_NAME);
        Object platformVersion = properties.get(KEY_PLATFORM_VERSION);
        if (platformDistName != null && platformVersion != null) {
            definition.setOsReleaseVersion(String.format("%s %s", platformDistName, platformVersion));
        } else {
            return false;
        }

        return true;
    }
}
