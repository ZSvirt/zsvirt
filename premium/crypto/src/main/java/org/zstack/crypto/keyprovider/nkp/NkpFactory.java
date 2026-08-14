package org.zstack.crypto.keyprovider.nkp;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.crypto.keyprovider.KeyProviderFactory;
import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.crypto.keyprovider.nkp.api.APICreateNkpMsg;
import org.zstack.crypto.keyprovider.nkp.api.APIUpdateNkpMsg;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.keyprovider.NkpInventory;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.message.APIMessage;

import static org.zstack.core.Platform.operr;

public class NkpFactory implements KeyProviderFactory {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public KeyProviderType getType() {
        return KeyProviderType.NKP;
    }

    @Override
    public KeyProviderVO createKeyProvider(KeyProviderVO vo, APICreateKeyProviderMsg msg) {
        if (!(msg instanceof APICreateNkpMsg)) {
            throw new OperationFailureException(operr("keyProvider[uuid:%s] type is not NKP", vo.getUuid()));
        }

        APICreateNkpMsg createMsg = (APICreateNkpMsg) msg;
        NkpVO nkpVo = new NkpVO();
        nkpVo.setUuid(vo.getUuid());
        nkpVo.setName(vo.getName());
        nkpVo.setDescription(vo.getDescription());
        nkpVo.setType(vo.getType());
        nkpVo.setConnected(vo.isConnected());
        nkpVo.setKdf(createMsg.getKdf());
        nkpVo.setSaltPolicy(createMsg.getSaltPolicy());
        nkpVo.setCurrentVersion(1);
        nkpVo.setBackedUp(Boolean.FALSE);
        return dbf.persistAndRefresh(nkpVo);
    }

    @Override
    public KeyProviderVO updateKeyProvider(KeyProviderVO vo, APIMessage msg) {
        NkpVO nkpVo = (vo instanceof NkpVO) ? (NkpVO) vo :
                dbf.findByUuid(vo.getUuid(), NkpVO.class);
        if (nkpVo == null) {
            throw new OperationFailureException(operr("cannot find NKP keyProvider[uuid:%s]", vo.getUuid()));
        }

        if (msg instanceof APIUpdateNkpMsg) {
            APIUpdateNkpMsg updateMsg = (APIUpdateNkpMsg) msg;
            if (updateMsg.getDescription() != null) {
                nkpVo.setDescription(updateMsg.getDescription());
            }
        } else {
            throw new OperationFailureException(operr("unsupported message[%s] for NKP keyProvider", msg.getClass().getSimpleName()));
        }

        return dbf.updateAndRefresh(nkpVo);
    }

    @Override
    public KeyProviderInventory valueOf(KeyProviderVO vo) {
        NkpVO nkpVo = (vo instanceof NkpVO) ? (NkpVO) vo :
                dbf.findByUuid(vo.getUuid(), NkpVO.class);
        return nkpVo == null ? null : NkpInventory.valueOf(nkpVo);
    }
}
