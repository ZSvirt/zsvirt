package org.zstack.crypto.keyprovider.kms;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.crypto.keyprovider.KeyProviderFactory;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.crypto.keyprovider.kms.api.APICreateKmsMsg;
import org.zstack.crypto.keyprovider.kms.api.APIUpdateKmsMsg;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.KmipVersion;
import org.zstack.header.keyprovider.KmsInventory;
import org.zstack.header.keyprovider.KmsTrustState;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.operr;

public class KmsFactory implements KeyProviderFactory {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public KeyProviderType getType() {
        return KeyProviderType.KMS;
    }

    @Override
    public KeyProviderVO createKeyProvider(KeyProviderVO vo, APICreateKeyProviderMsg msg) {
        if (!(msg instanceof APICreateKmsMsg)) {
            throw new OperationFailureException(operr("keyProvider[uuid:%s] type is not KMS", vo.getUuid()));
        }

        APICreateKmsMsg createMsg = (APICreateKmsMsg) msg;
        KmsVO kmsVo = new KmsVO();
        kmsVo.setUuid(vo.getUuid());
        kmsVo.setName(vo.getName());
        kmsVo.setDescription(vo.getDescription());
        kmsVo.setType(vo.getType());
        kmsVo.setConnected(vo.isConnected());
        kmsVo.setEndpoint(createMsg.getEndpoint());
        kmsVo.setPort(createMsg.getPort());
        kmsVo.setKmipVersion(KmipVersion.fromString(createMsg.getKmipVersion()));
        kmsVo.setUsername(createMsg.getUsername());
        kmsVo.setPassword(createMsg.getPassword());
        kmsVo.setTrustState(KmsTrustState.MUTUAL_UNTRUSTED);
        return dbf.persistAndRefresh(kmsVo);
    }

    @Override
    public KeyProviderVO updateKeyProvider(KeyProviderVO vo, APIMessage msg) {
        KmsVO kmsVo = (vo instanceof KmsVO) ? (KmsVO) vo :
                dbf.findByUuid(vo.getUuid(), KmsVO.class);
        if (kmsVo == null) {
            throw new OperationFailureException(operr("cannot find KMS keyProvider[uuid:%s]", vo.getUuid()));
        }

        if (msg instanceof APIUpdateKmsMsg) {
            APIUpdateKmsMsg updateMsg = (APIUpdateKmsMsg) msg;
            if (updateMsg.getDescription() != null) {
                kmsVo.setDescription(updateMsg.getDescription());
            }
            if (updateMsg.getEndpoint() != null) {
                kmsVo.setEndpoint(updateMsg.getEndpoint());
            }
            if (updateMsg.getPort() != null) {
                kmsVo.setPort(updateMsg.getPort());
            }
            if (updateMsg.getKmipVersion() != null) {
                kmsVo.setKmipVersion(KmipVersion.fromString(updateMsg.getKmipVersion()));
            }
            if (updateMsg.getUsername() != null) {
                kmsVo.setUsername(updateMsg.getUsername());
            }
            if (updateMsg.getPassword() != null) {
                kmsVo.setPassword(updateMsg.getPassword());
            }
        } else {
            throw new OperationFailureException(operr("unsupported message[%s] for KMS keyProvider", msg.getClass().getSimpleName()));
        }

        return dbf.updateAndRefresh(kmsVo);
    }

    @Override
    public KeyProviderInventory valueOf(KeyProviderVO vo) {
        KmsVO kmsVo = (vo instanceof KmsVO) ? (KmsVO) vo :
                dbf.findByUuid(vo.getUuid(), KmsVO.class);
        return kmsVo == null ? null : KmsInventory.valueOf(kmsVo);
    }

}
