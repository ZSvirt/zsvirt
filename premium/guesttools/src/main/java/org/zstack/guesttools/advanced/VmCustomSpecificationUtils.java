package org.zstack.guesttools.advanced;

import org.apache.commons.lang.StringUtils;
import org.zstack.compute.vm.VmHostnameUtils;
import org.zstack.compute.vm.VmPasswordStrengthConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.guesttools.GuestToolsConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.vm.VmConfigSyncStruct;
import org.zstack.mevoco.MevocoGlobalConfig;

import java.util.Objects;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class VmCustomSpecificationUtils {
    private static <T> T chooseNonNull(T first, T second) {
        return first != null ? first : second;
    }

    public static void updateVmCustomSpecByVO(VmCustomSpecificationStruct spec, VmCustomSpecificationVO vo) {
        spec.setPlatform(chooseNonNull(spec.getPlatform(), vo.getPlatform()));
        spec.setHostname(chooseNonNull(spec.getHostname(), vo.getHostname()));
        spec.setRootPassword(chooseNonNull(spec.getRootPassword(), vo.getRootPassword()));
        spec.setGenerateSID(chooseNonNull(spec.getGenerateSID(), vo.isGenerateSID()));
        spec.setDomainMode(chooseNonNull(spec.getDomainMode(), vo.getDomainMode()));
        spec.setDomainName(chooseNonNull(spec.getDomainName(), vo.getDomainName()));
        spec.setDomainUsername(chooseNonNull(spec.getDomainUsername(), vo.getDomainUsername()));
        spec.setDomainPassword(chooseNonNull(spec.getDomainPassword(), vo.getDomainPassword()));
        spec.setOrganization(chooseNonNull(spec.getOrganization(), vo.getOrganization()));
    }

    public static void validate(VmCustomSpecificationStruct spec, String vmPlatform) {
        if (spec == null) {
            return;
        }

        validate(spec);

        if (!Objects.equals(vmPlatform, spec.getPlatform())) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the platform of vm custom specification[%s] is not consistent with the platform in source vm[%s]",
                    spec.getPlatform(), vmPlatform));
        }
    }

    public static void validate(VmCustomSpecificationStruct spec) {
        if (!StringUtils.isEmpty(spec.getUuid())) {
            VmCustomSpecificationVO vo = Q.New(VmCustomSpecificationVO.class)
                    .eq(VmCustomSpecificationVO_.uuid, spec.getUuid()).find();
            if (vo == null) {
                throw new ApiMessageInterceptionException(argerr("the custom specification[uuid:%s] not found", spec.getUuid()));
            }
            updateVmCustomSpecByVO(spec, vo);
        }

        if (!ImagePlatform.Windows.toString().equals(spec.getPlatform()) && !ImagePlatform.Linux.toString().equals(spec.getPlatform())) {
            throw new ApiMessageInterceptionException(argerr("unsupported platform[%s], only [Windows, Linux] are supported",
                    spec.getPlatform()));
        }

        boolean isWindows = ImagePlatform.Windows.toString().equals(spec.getPlatform());
        if (!StringUtils.isEmpty(spec.getHostname())) {
            VmHostnameUtils.validateHostname(spec.getHostname(), isWindows);
        } else if (spec.getHostname() != null) {
            spec.setHostname(null);
        }

        if (!StringUtils.isEmpty(spec.getRootPassword())) {
            VmPasswordStrengthConfig vmPasswordStrengthConfig = VmPasswordStrengthConfig.toObject(MevocoGlobalConfig.VM_PASSWORD_STRENGTH_CHECK_CONFIG.value());
            if (vmPasswordStrengthConfig.isCheckPasswordStrength()) {
                vmPasswordStrengthConfig.validatePasswordStrengthConfig(spec.getRootPassword());
            }
        } else if (spec.getRootPassword() != null) {
            spec.setRootPassword(null);
        }

        if (!isWindows) {
            spec.setGenerateSID(null);
            spec.setDomainMode(null);
            spec.setDomainName(null);
            spec.setDomainUsername(null);
            spec.setDomainPassword(null);
            spec.setOrganization(null);
            return;
        }

        if (!VmCustomSpecificationDomainMode.Domain.equals(spec.getDomainMode()) && !VmCustomSpecificationDomainMode.WorkGroup.equals(spec.getDomainMode())) {
            throw new ApiMessageInterceptionException(argerr("unsupported domain mode[%s], only [Domain, WorkGroup] are supported",
                    spec.getDomainMode()));
        }

        if (StringUtils.isEmpty(spec.getDomainName())) {
            throw new ApiMessageInterceptionException(argerr("%s name is required", spec.getDomainMode()));
        }

        if (Boolean.TRUE.equals(spec.getGenerateSID()) && StringUtils.isEmpty(spec.getRootPassword())) {
            throw new ApiMessageInterceptionException(argerr("root password is required when generating SID"));
        }

        if (VmCustomSpecificationDomainMode.Domain.equals(spec.getDomainMode())) {
            if (!Boolean.TRUE.equals(spec.getGenerateSID())) {
                throw new ApiMessageInterceptionException(argerr("generateSID should be set when joining domain"));
            }

            VmHostnameUtils.validateDomain(spec.getDomainName());

            if (StringUtils.isEmpty(spec.getDomainUsername())) {
                throw new ApiMessageInterceptionException(argerr("domain username is required"));
            }

            if (!StringUtils.isEmpty(spec.getDomainPassword())) {
                VmPasswordStrengthConfig vmPasswordStrengthConfig = VmPasswordStrengthConfig.toObject(MevocoGlobalConfig.VM_DOMAIN_PASSWORD_STRENGTH_CHECK_CONFIG.value());
                if (vmPasswordStrengthConfig.isCheckPasswordStrength()) {
                    vmPasswordStrengthConfig.validatePasswordStrengthConfig(spec.getDomainPassword());
                }
            }

            if (!StringUtils.isEmpty(spec.getOrganization())) {
                Pattern pattern = Pattern.compile(GuestToolsConstant.OU_PATTERN);
                if (!pattern.matcher(spec.getOrganization().trim()).matches()) {
                    throw new ApiMessageInterceptionException(argerr(
                            "organization[%s] format is invalid, it should be like OU=xxx[,OU=xxx],DC=xxx[,DC=xxx]", spec.getOrganization()));
                }

            }
        } else {
            spec.setDomainUsername(null);
            spec.setDomainPassword(null);
            spec.setOrganization(null);
        }
    }

    public static VmConfigSyncStruct.VmSpecificationConfig getVmSpecificationConfig(VmCustomSpecificationVO vo) {
        VmConfigSyncStruct.VmSpecificationConfig spec = new VmConfigSyncStruct.VmSpecificationConfig();
        spec.setGenerateSID(vo.isGenerateSID());
        String hostname = VmSystemTags.HOSTNAME.getTokenByResourceUuid(vo.getVmInstanceUuid(), VmSystemTags.HOSTNAME_TOKEN);
        boolean isWindows = ImagePlatform.Windows.toString().equals(vo.getPlatform());
        if (Boolean.TRUE.equals(spec.getGenerateSID()) && isWindows) {
            spec.setHostname(VmHostnameUtils.safeSubstringForHostname(hostname != null ? hostname : vo.getHostname()));
        } else {
            spec.setHostname(hostname != null ? hostname : vo.getHostname());
        }

        VmConfigSyncStruct.VmDomainConfig domain = new VmConfigSyncStruct.VmDomainConfig();
        if (vo.getDomainMode() != null) {
            spec.setDomainMode(vo.getDomainMode().toString());
            spec.setDomain(domain);
        }

        domain.setName(vo.getDomainName());
        domain.setUsername(vo.getDomainUsername());
        domain.setPassword(vo.getDomainPassword());
        domain.setOu(vo.getOrganization());

        VmConfigSyncStruct.VmUserConfig user = new VmConfigSyncStruct.VmUserConfig();
        if (vo.getRootPassword() != null) {
            spec.setUser(user);
        }
        if (isWindows) {
            user.setUsername(GuestToolsConstant.WINDOWS_ADMIN_USERNAME);
        } else {
            user.setUsername(GuestToolsConstant.LINUX_ROOT_USERNAME);
        }
        user.setPassword(vo.getRootPassword());
        user.setActive(true);

        return spec;
    }
}
