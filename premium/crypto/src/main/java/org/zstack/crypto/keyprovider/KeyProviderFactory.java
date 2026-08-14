package org.zstack.crypto.keyprovider;

import org.zstack.crypto.keyprovider.api.APICreateKeyProviderMsg;
import org.zstack.header.keyprovider.KeyProviderInventory;
import org.zstack.header.keyprovider.KeyProviderType;
import org.zstack.header.keyprovider.KeyProviderVO;
import org.zstack.header.message.APIMessage;

public interface KeyProviderFactory {
    /**
     * @return the type of the key provider
     */
    KeyProviderType getType();

    /**
     * @param vo  the key provider vo to be created
     * @param msg api message for creating the key provider, it contains the parameters for creating the key provider
     * @return the created key provider vo
     */
    KeyProviderVO createKeyProvider(KeyProviderVO vo, APICreateKeyProviderMsg msg);

    /**
     * @param vo  the key provider vo to be updated
     * @param msg api message for updating the key provider, it contains the parameters for updating the key provider
     * @return the updated key provider vo
     */
    KeyProviderVO updateKeyProvider(KeyProviderVO vo, APIMessage msg);

    /**
     * @param vo the key provider vo to be transformed to inventory
     * @return the corresponding type of key provider inventory
     */
    KeyProviderInventory valueOf(KeyProviderVO vo);
}
