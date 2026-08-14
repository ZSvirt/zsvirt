package org.zstack.kvm.hypervisor;

import org.zstack.header.host.CpuArchitecture;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * override function for scanning folder and reading from shell
 * 
 * Created by Wenhao.Zhang on 23/03/01
 */
public class HypervisorMetadataCollectorSimulator extends HypervisorMetadataCollectorImpl {
    public static final String QEMU_VERSION_FOR_TEST = "4.2.0-632.g6a6222b.el7";
    private Function<Path, List<HypervisorMetadataDefinition>> folderScannerSimulator;
    private Function<HypervisorMetadataDefinition, String> collectMetadataSimulator;

    {
        cleanSimulators();
    }

    public void cleanSimulators() {
        setFolderScannerSimulator(null);
        setCollectMetadataSimulator(null);
    }

    public void setFolderScannerSimulator(Function<Path, List<HypervisorMetadataDefinition>> simulator) {
        this.folderScannerSimulator = simulator == null ? this::scanFolderForDefault : simulator;
    }

    public void setCollectMetadataSimulator(Function<HypervisorMetadataDefinition, String> simulator) {
        this.collectMetadataSimulator = simulator == null ? this::collectMetadataForDefault : simulator;
    }

    @Override
    protected List<HypervisorMetadataDefinition> scanFolder(Path rootPath) throws IOException {
        return folderScannerSimulator.apply(rootPath);
    }

    public List<HypervisorMetadataDefinition> scanFolderForDefault(Path rootPath) {
        HypervisorMetadataDefinitionSimulator d1 = new HypervisorMetadataDefinitionSimulator();
        d1.setArchitecture(CpuArchitecture.x86_64.name());
        d1.setOsReleaseSimpleVersion("c76");

        HypervisorMetadataDefinitionSimulator d2 = new HypervisorMetadataDefinitionSimulator();
        d2.setArchitecture(CpuArchitecture.x86_64.name());
        d2.setOsReleaseSimpleVersion("c79");

        return Arrays.asList(d1, d2);
    }

    @Override
    protected String collectHypervisorMetadataAsProperties(HypervisorMetadataDefinition definition) throws IOException {
        return collectMetadataSimulator.apply(definition);
    }

    public String collectMetadataForDefault(HypervisorMetadataDefinition definition) {
        String[] osReleaseVersion;
        switch (definition.getOsReleaseSimpleVersion()) {
            case "ns10": osReleaseVersion = "kylin10 tercel 10".split(" "); break;
            case "uos20": osReleaseVersion = "uniontech fou 20".split(" "); break;
            case "uos1021a": osReleaseVersion = "uniontech kongzi 20".split(" "); break;
            case "c79": osReleaseVersion = "centos Core 7.9.2009".split(" "); break;
            case "c76": osReleaseVersion = "centos Core 7.6.1810".split(" "); break;
            case "c74": osReleaseVersion = "centos Core 7.4.1708".split(" "); break;
            default: osReleaseVersion = new String[] {"unknown", "unknown", "unknown"}; break;
        }

        return String.format("qemu-kvm.version: %s\n" +
                "platform.distname: %s\nplatform.id: %s\nplatform.version: %s\n",
                QEMU_VERSION_FOR_TEST, osReleaseVersion[0], osReleaseVersion[1], osReleaseVersion[2]);
    }

    public static class HypervisorMetadataDefinitionSimulator extends HypervisorMetadataDefinition {
        @Override
        public boolean isValid() {
            return true;
        }
    }
}
