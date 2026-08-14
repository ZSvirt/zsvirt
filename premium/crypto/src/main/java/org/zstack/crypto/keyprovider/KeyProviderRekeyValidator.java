package org.zstack.crypto.keyprovider;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KeyProviderVO_;
import org.zstack.header.keyprovider.KmsTrustState;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.keyprovider.KmsVO_;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.keyprovider.NkpVO_;

import static org.zstack.core.Platform.operr;

final class KeyProviderRekeyValidator {
    private KeyProviderRekeyValidator() {
    }

    static KeyProviderVO getAvailableTargetProvider(String uuid) {
        KeyProviderType type = Q.New(KeyProviderVO.class)
                .eq(KeyProviderVO_.uuid, uuid)
                .select(KeyProviderVO_.type)
                .findValue();
        if (type == null) {
            throw new OperationFailureException(operr("cannot find key provider[uuid:%s]", uuid));
        }

        if (type == KeyProviderType.NKP) {
            return getAvailableTargetNkp(uuid);
        }
        if (type == KeyProviderType.KMS) {
            return getAvailableTargetKms(uuid);
        }
        throw new OperationFailureException(operr(
                "unsupported target key provider type[%s]", type));
    }

    static NkpVO getAvailableTargetNkp(String uuid) {
        NkpVO nkp = Q.New(NkpVO.class)
                .eq(NkpVO_.uuid, uuid)
                .find();
        if (nkp == null) {
            throw new OperationFailureException(operr("cannot find nkp[uuid:%s]", uuid));
        }
        if (!nkp.isConnected()) {
            throw new OperationFailureException(operr(
                    "target nkp[uuid:%s, name:%s] is unavailable, cannot rekey resources",
                    nkp.getUuid(), nkp.getName()));
        }
        if (!nkp.isBackedUp()) {
            throw new OperationFailureException(operr(
                    "target nkp[uuid:%s, name:%s] is not backed up, cannot rekey resources",
                    nkp.getUuid(), nkp.getName()));
        }
        return nkp;
    }

    static KmsVO getAvailableTargetKms(String uuid) {
        KmsVO kms = Q.New(KmsVO.class)
                .eq(KmsVO_.uuid, uuid)
                .find();
        if (kms == null) {
            throw new OperationFailureException(operr("cannot find kms[uuid:%s]", uuid));
        }
        if (!kms.isConnected() || kms.getTrustState() != KmsTrustState.MUTUAL_TRUSTED) {
            throw new OperationFailureException(operr(
                    "target kms[uuid:%s, name:%s] is unavailable, cannot rekey resources",
                    kms.getUuid(), kms.getName()));
        }
        return kms;
    }
}
