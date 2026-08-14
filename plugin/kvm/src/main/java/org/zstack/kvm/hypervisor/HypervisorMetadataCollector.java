package org.zstack.kvm.hypervisor;

import org.zstack.header.host.HostAO;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by Wenhao.Zhang on 23/03/01
 */
public interface HypervisorMetadataCollector {
    List<HypervisorMetadataDefinition> collect();

    class HypervisorMetadataDefinition {
        /**
         * equals to {@link HostAO#getArchitecture()}
         */
        private String architecture;
        /**
         * "centos 7.6.1810" / "centos 7.4.1708" ...
         */
        private String osReleaseVersion;
        /**
         * "c76" / "c74" ...
         */
        private String osReleaseSimpleVersion;
        /**
         * "qemu-kvm"
         */
        private String hypervisor;
        /**
         * hypervisor version. "4.2.0-632"
         */
        private String version;

        public String getArchitecture() {
            return architecture;
        }

        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }

        public String getOsReleaseSimpleVersion() {
            return osReleaseSimpleVersion;
        }

        public void setOsReleaseSimpleVersion(String osReleaseSimpleVersion) {
            this.osReleaseSimpleVersion = osReleaseSimpleVersion;
        }

        public String getOsReleaseVersion() {
            return osReleaseVersion;
        }

        public void setOsReleaseVersion(String osReleaseVersion) {
            this.osReleaseVersion = osReleaseVersion;
        }

        public String getHypervisor() {
            return hypervisor;
        }

        public void setHypervisor(String hypervisor) {
            this.hypervisor = hypervisor;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public Path virtualizerScriptPath() {
            return Paths.get(
                    KvmHypervisorConstant.DVD_ROOT_PATH.toString(),
                    this.architecture,
                    this.osReleaseSimpleVersion,
                    KvmHypervisorConstant.VIRTUALIZER_INFO_SCRIPT_PATH.toString());
        }

        public boolean isValid() {
            return Files.exists(virtualizerScriptPath());
        }
    }
}
