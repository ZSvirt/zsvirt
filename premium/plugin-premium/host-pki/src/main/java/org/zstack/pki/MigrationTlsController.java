package org.zstack.pki;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.Component;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.migration.KvmMigrateTlsExtensionPoint;
import org.zstack.header.migration.KvmMigrateTlsSpec;
import org.zstack.header.pki.HostCertificateService;
import org.zstack.header.pki.HostCertificateVO;
import org.zstack.header.pki.HostPkiConstant;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceMigrateExtensionPoint;

import static org.zstack.core.Platform.operr;

public class MigrationTlsController implements Component, KvmMigrateTlsExtensionPoint, VmInstanceMigrateExtensionPoint {
    @Autowired
    private HostCertificateService hostCertificateService;
    @Autowired
    private MigrationTlsPolicy migrationTlsPolicy;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void preMigrateVm(VmInstanceInventory inv, String destHostUuid, Boolean enableMigrationTls,
                             Completion completion) {
        if (!Boolean.TRUE.equals(enableMigrationTls)) {
            completion.success();
            return;
        }
        ensureCertificatesReady(inv.getHostUuid(), destHostUuid, completion);
    }

    @Override
    public boolean configureMigrateTls(String vmUuid, String srcHostUuid, String dstHostUuid,
                                       String dstMigrationAddress, KvmMigrateTlsSpec spec) {
        if (!Boolean.TRUE.equals(spec.getRequestedByUser())) {
            return false;
        }

        // preMigrateVm ensures certificates when enableMigrationTls=true; verify readiness here
        if (!hostCertificateService.isReady(srcHostUuid, HostPkiConstant.USAGE_MIGRATION)
                || !hostCertificateService.isReady(dstHostUuid, HostPkiConstant.USAGE_MIGRATION)) {
            if (migrationTlsPolicy.isStrict()) {
                throw new OperationFailureException(operr(
                        "source host[uuid:%s] or destination host[uuid:%s] migration TLS certificate is not ready",
                        srcHostUuid, dstHostUuid));
            }
            return false;
        }

        HostCertificateVO dstCert = hostCertificateService.getCertificateMetadata(dstHostUuid, HostPkiConstant.USAGE_MIGRATION);
        if (dstCert == null) {
            if (migrationTlsPolicy.isStrict()) {
                throw new OperationFailureException(operr(
                        "cannot enable migration TLS for vm[uuid:%s], destination host[uuid:%s] has no migration certificate",
                        vmUuid, dstHostUuid));
            }
            return false;
        }
        if (!isDestinationSanMatched(dstCert, dstMigrationAddress)) {
            if (migrationTlsPolicy.isStrict()) {
                throw new OperationFailureException(operr(
                        "cannot enable migration TLS for vm[uuid:%s], destination address[%s] is not covered by host[uuid:%s] certificate SAN[%s]",
                        vmUuid, dstMigrationAddress, dstHostUuid, dstCert.getSanSnapshot()));
            }
            return false;
        }

        spec.setMigrateTls(true);
        spec.setMutualTls(migrationTlsPolicy.isMutualTls());
        spec.setExpectedDestCertFingerprint(dstCert.getFingerprint());
        return true;
    }

    private void ensureCertificatesReady(String srcHostUuid, String dstHostUuid, Completion completion) {
        // Always reconcile via ensureProfile: DB Active does not guarantee agent-side materials exist.
        hostCertificateService.ensureProfile(srcHostUuid, HostPkiConstant.USAGE_MIGRATION, new Completion(completion) {
            @Override
            public void success() {
                hostCertificateService.ensureProfile(dstHostUuid, HostPkiConstant.USAGE_MIGRATION, new Completion(completion) {
                    @Override
                    public void success() {
                        boolean ready = hostCertificateService.isReady(srcHostUuid, HostPkiConstant.USAGE_MIGRATION)
                                && hostCertificateService.isReady(dstHostUuid, HostPkiConstant.USAGE_MIGRATION);
                        if (!ready && migrationTlsPolicy.isStrict()) {
                            completion.fail(operr(
                                    "source host[uuid:%s] or destination host[uuid:%s] migration TLS certificate is not ready after repair",
                                    srcHostUuid, dstHostUuid));
                            return;
                        }
                        completion.success();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        if (migrationTlsPolicy.isStrict()) {
                            completion.fail(operr(
                                    "failed to repair migration TLS certificate for destination host[uuid:%s]: %s",
                                    dstHostUuid, errorCode.getDetails()));
                        } else {
                            completion.success();
                        }
                    }
                });
            }

            @Override
            public void fail(ErrorCode errorCode) {
                if (migrationTlsPolicy.isStrict()) {
                    completion.fail(operr(
                            "failed to repair migration TLS certificate for source host[uuid:%s]: %s",
                            srcHostUuid, errorCode.getDetails()));
                } else {
                    completion.success();
                }
            }
        });
    }

    private boolean isDestinationSanMatched(HostCertificateVO dstCert, String dstMigrationAddress) {
        if (dstCert == null || StringUtils.isBlank(dstMigrationAddress) || StringUtils.isBlank(dstCert.getSanSnapshot())) {
            return false;
        }
        for (String san : dstCert.getSanSnapshot().split(",")) {
            if (StringUtils.trim(dstMigrationAddress).equals(StringUtils.trim(san))) {
                return true;
            }
        }
        return false;
    }
}
