package org.zstack.ovf.datatype;

import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.rest.NoSDK;

import javax.annotation.Nullable;

/**
 * Created by Wenhao.Zhang on 24-01-22
 */
@NoSDK
public class OvfPreAnalysisInfo {
    private ImagePlatform inferredPlatform;
    private ImageArchitecture inferredArchitecture;
    private Long inferredRootDiskSize;

    public ImagePlatform getInferredPlatform() {
        return inferredPlatform;
    }

    public void setInferredPlatform(ImagePlatform inferredPlatform) {
        this.inferredPlatform = inferredPlatform;
    }

    @Nullable
    public ImageArchitecture getInferredArchitecture() {
        return inferredArchitecture;
    }

    public ImageArchitecture getInferredArchitectureOrDefault(ImageArchitecture defaults) {
        return inferredArchitecture == null ? defaults : inferredArchitecture;
    }

    public void setInferredArchitecture(ImageArchitecture inferredArchitecture) {
        this.inferredArchitecture = inferredArchitecture;
    }

    @Nullable
    public Long getInferredRootDiskSize() {
        return inferredRootDiskSize;
    }

    public void setInferredRootDiskSize(Long inferredRootDiskSize) {
        this.inferredRootDiskSize = inferredRootDiskSize;
    }
}
