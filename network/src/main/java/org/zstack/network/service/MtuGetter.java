package org.zstack.network.service;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.*;
import org.zstack.header.network.l3.L3Network;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.network.l3.L3NetworkVO_;
import org.zstack.network.l2.L2NetworkDefaultMtu;
import org.zstack.network.l2.L2NetworkManager;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

/**
 * Created by weiwang on 19/05/2017.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MtuGetter {
    private static final CLogger logger = Utils.getLogger(MtuGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public Integer getMtu(String l3NetworkUuid) {
        String l2NetworkUuid = Q.New(L3NetworkVO.class).select(L3NetworkVO_.l2NetworkUuid).eq(L3NetworkVO_.uuid, l3NetworkUuid).findValue();

        Integer mtu = null;

        List<Map<String, String>> tokenList = NetworkServiceSystemTag.L3_MTU.getTokensOfTagsByResourceUuid(l3NetworkUuid);
        for (Map<String, String> token : tokenList) {
            mtu = Integer.valueOf(token.get(NetworkServiceSystemTag.MTU_TOKEN));
        }

        if (mtu != null) {
            return mtu;
        }

        L2NetworkVO l2VO = Q.New(L2NetworkVO.class).eq(L2NetworkVO_.uuid, l2NetworkUuid).find();
        for (L2NetworkDefaultMtu e : pluginRgty.getExtensionList(L2NetworkDefaultMtu.class)) {
            if (l2VO.getType().equals(e.getL2NetworkType())) {
                mtu = e.getDefaultMtu(L2NetworkInventory.valueOf(l2VO));
            }
        }

        if (mtu != null) {
            return mtu;
        } else {
            mtu = Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_DUMMY.value());
            logger.warn(String.format("unknown network type [%s], set mtu as default [%s]",
                    l2VO.getType(), mtu));
            return mtu;
        }
    }

    public Integer getL2Mtu(L2NetworkInventory l2Inv) {

        Integer mtu = null;

        /* get max mtu of l3 network in l2 network */
        List<String> l3Uuids = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.l2NetworkUuid, l2Inv.getUuid()).select(L3NetworkVO_.uuid).listValues();
        for (String uuid : l3Uuids) {
            String token = NetworkServiceSystemTag.L3_MTU.getTokenByResourceUuid(uuid, NetworkServiceSystemTag.MTU_TOKEN);
            if (token == null) {
                continue;
            }

            if (mtu == null) {
                mtu = Integer.valueOf(token);
                continue;
            }
            Integer newMtu = Integer.valueOf(token);
            if (newMtu > mtu) {
                mtu = newMtu;
            }
        }

        /* compare to default mtu */
        for (L2NetworkDefaultMtu e : pluginRgty.getExtensionList(L2NetworkDefaultMtu.class)) {
            if (l2Inv.getType().equals(e.getL2NetworkType())) {
                Integer l2mtu = e.getDefaultMtu(l2Inv);
                if (mtu == null) {
                    mtu = l2mtu;
                    break;
                }
                if (l2mtu > mtu) {
                    mtu = l2mtu;
                }
                break;
            }
        }

        if (mtu != null) {
            return mtu;
        }

        logger.warn(String.format("unknown network type [%s], set mtu as default [%s]",
                l2Inv.getType(), mtu));
        return Integer.valueOf(NetworkServiceGlobalConfig.DHCP_MTU_DUMMY.value());
    }

}
