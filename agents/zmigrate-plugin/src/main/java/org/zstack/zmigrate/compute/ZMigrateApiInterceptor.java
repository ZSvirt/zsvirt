package org.zstack.zmigrate.compute;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.message.APIMessage;
import org.zstack.softwarePackage.entity.SoftwarePackageStatus;
import org.zstack.softwarePackage.entity.SoftwarePackageVO;
import org.zstack.softwarePackage.entity.SoftwarePackageVO_;
import org.zstack.softwarePackage.entity.UpgradeType;
import org.zstack.softwarePackage.header.*;
import org.zstack.utils.CollectionDSL;
import org.zstack.zmigrate.api.APIGetZMigrateGatewayVmInstancesMsg;
import org.zstack.zmigrate.api.APIGetZMigrateInfosMsg;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;
import static org.zstack.zmigrate.ZMigrateConstant.ZMIGRATE_SOFTWARE_IMAGE_COUNT;
import static org.zstack.zmigrate.ZMigrateConstant.ZMIGRATE_SOFTWARE_PACKAGE_TYPE;
import static org.zstack.zmigrate.compute.ZMigrateUtils.getSoftwarePackageUuid;
import static org.zstack.zmigrate.compute.ZMigrateUtils.findZMigrateVmUuid;
import static org.zstack.zmigrate.compute.ZMigrateUtils.getZMigrateUpgradeImages;
import static org.zstack.zmigrate.ZMigrateSystemTags.ZMIGRATE_MANAGEMENT;

@InterceptorForService("ZMigratePlugin")
public class ZMigrateApiInterceptor implements ApiMessageInterceptor, GlobalApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade databases;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIUploadAndExecuteSoftwareUpgradePackageMsg) {
            validate((APIUploadAndExecuteSoftwareUpgradePackageMsg) msg);
        } else if (msg instanceof APIUploadSoftwarePackageToBackupStorageMsg) {
            validate((APIUploadSoftwarePackageToBackupStorageMsg) msg);
        } else if (msg instanceof APIGetZMigrateInfosMsg) {
            validate((APIGetZMigrateInfosMsg) msg);
        } else if (msg instanceof APIGetZMigrateGatewayVmInstancesMsg) {
            validate((APIGetZMigrateGatewayVmInstancesMsg) msg);
        }
        return msg;
    }

    private void validate(APIUploadAndExecuteSoftwareUpgradePackageMsg msg) {
        // Wrap both DB queries in a single transaction with pessimistic lock
        // to prevent concurrent validations from both passing.
        new SQLBatchWithReturn<Void>() {
            @Override
            protected Void scripts() {
                SoftwarePackageVO vo = databases.getEntityManager()
                        .find(SoftwarePackageVO.class, msg.getUuid(), LockModeType.PESSIMISTIC_WRITE);

                if (vo == null) {
                    throw new ApiMessageInterceptionException(argerr("software package [uuid:%s] not found", msg.getUuid()));
                }

                if (!Objects.equals(vo.getType(), ZMIGRATE_SOFTWARE_PACKAGE_TYPE)) {
                    return null;
                }

                String status = vo.getStatus();
                if (Objects.equals(status, SoftwarePackageStatus.UpgradePackageUploaded.toString())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "the software package [uuid:%s] is currently in [%s] state, " +
                                    "indicating an upgrade is still executing. " +
                                    "Please wait for it to complete before starting a new upgrade",
                            msg.getUuid(), status));
                }

                if (Objects.equals(status, SoftwarePackageStatus.UpgradePackageUploadFailed.toString())
                        && Objects.equals(msg.getUpgradeType(), UpgradeType.Reexecute.toString())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "cannot re-execute upgrade for software package [uuid:%s] " +
                                    "because the upgrade package upload has failed (current status: %s). " +
                                    "Please re-upload the upgrade package using upgradeType=Normal",
                            msg.getUuid(), status));
                }

                Map<String, ImageInventory> upgradeImages = getZMigrateUpgradeImages();

                if (upgradeImages.isEmpty()) {
                    if (Objects.equals(msg.getUpgradeType(), UpgradeType.Reexecute.toString())) {
                        throw new ApiMessageInterceptionException(argerr(
                                "no upgrade images found for software package [uuid:%s] to re-execute. " +
                                        "Please re-upload the upgrade package or clean the existing one first",
                                msg.getUuid()));
                    }
                    return null;
                }

                if (Objects.equals(msg.getUpgradeType(), UpgradeType.Normal.toString())) {
                    throw new ApiMessageInterceptionException(argerr(
                            "residual upgrade images (count: %s) exist for software package [uuid:%s]. " +
                                    "Please clean them using APICleanUpgradeSoftwarePackageMsg before uploading a new upgrade package",
                            upgradeImages.size(), msg.getUuid()));
                }

                if (Objects.equals(msg.getUpgradeType(), UpgradeType.Reexecute.toString())) {
                    if (upgradeImages.size() != ZMIGRATE_SOFTWARE_IMAGE_COUNT) {
                        throw new ApiMessageInterceptionException(argerr(
                                "the upgrade images for software package [uuid:%s] are incomplete: " +
                                        "expected %s images but found %s. " +
                                        "Please clean the residual images using APICleanUpgradeSoftwarePackageMsg " +
                                        "and re-upload the upgrade package",
                                msg.getUuid(), ZMIGRATE_SOFTWARE_IMAGE_COUNT, upgradeImages.size()));
                    }
                }

                return null;
            }
        }.execute();
    }

    private void validate(APIUploadSoftwarePackageToBackupStorageMsg msg) {
        if (!Objects.equals(msg.getType(), ZMIGRATE_SOFTWARE_PACKAGE_TYPE)) {
            return;
        }
        boolean exists = Q.New(SoftwarePackageVO.class)
                .eq(SoftwarePackageVO_.type, ZMIGRATE_SOFTWARE_PACKAGE_TYPE)
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(argerr("ZMigrate software package already exists"));
        }
    }

    private void validate(APIGetZMigrateInfosMsg msg) {
        ZMigrateContext ctx = resolveZMigrateContext();
        msg.setSoftwarePackageUuid(ctx.softwarePackageUuid);
        msg.setManagementVmUuid(ctx.managementVmUuid);
    }

    private void validate(APIGetZMigrateGatewayVmInstancesMsg msg) {
        ZMigrateContext ctx = resolveZMigrateContext();
        msg.setSoftwarePackageUuid(ctx.softwarePackageUuid);
        msg.setManagementVmUuid(ctx.managementVmUuid);
    }

    private static class ZMigrateContext {
        final String softwarePackageUuid;
        final String managementVmUuid;

        ZMigrateContext(String softwarePackageUuid, String managementVmUuid) {
            this.softwarePackageUuid = softwarePackageUuid;
            this.managementVmUuid = managementVmUuid;
        }
    }

    private ZMigrateContext resolveZMigrateContext() {
        String softwarePackageUuid = validateSoftwarePackageExists();
        String managementVmUuid = findZMigrateVmUuid(ZMIGRATE_MANAGEMENT);
        if (StringUtils.isEmpty(managementVmUuid)) {
            throw new ApiMessageInterceptionException(argerr("no ZMigrate management VM found in the current environment"));
        }
        return new ZMigrateContext(softwarePackageUuid, managementVmUuid);
    }

    private String validateSoftwarePackageExists() {
        String softwarePackageUuid = getSoftwarePackageUuid();
        if (softwarePackageUuid == null) {
            throw new ApiMessageInterceptionException(argerr("no ZMigrate software package found in the current environment"));
        }
        return softwarePackageUuid;
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return CollectionDSL.list(
                APIUploadAndExecuteSoftwareUpgradePackageMsg.class,
                APIUploadSoftwarePackageToBackupStorageMsg.class,
                APIGetZMigrateInfosMsg.class,
                APIGetZMigrateGatewayVmInstancesMsg.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }
}
