package org.zstack.compute.vm;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.network.l3.*;
import org.zstack.header.vm.*;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Created by camile on 2017/12/14.
 */
public class MacOperator {
    private static final CLogger logger = Utils.getLogger(MacOperator.class);
    private static final Pattern pattern = Pattern.compile("([a-f0-9]{2}:){5}[a-f0-9]{2}");

    class VmMacStruct {
        private String l3Uuid;
        private String mac;

        public VmMacStruct(String l3Uuid, String mac) {
            this.l3Uuid = l3Uuid;
            this.mac = mac;
        }
    }

    @Autowired
    private DatabaseFacade dbf;

    private PatternedSystemTag that = VmSystemTags.CUSTOM_MAC;

    public List<VmMacStruct> getMacInfobyVmUuid(String vmUuid) {
        List<VmMacStruct> structs = Lists.newArrayList();
        List<Map<String, String>> tokenList = that.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            structs.add(new VmMacStruct(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN),
                    VmSystemTags.MAC_TOKEN));

        }
        return structs;
    }

    public String getMac(String vmUuid, String l3Uuid) {
        List<Map<String, String>> tokenList = that.getTokensOfTagsByResourceUuid(vmUuid);
        for (Map<String, String> tokens : tokenList) {
            if (StringUtils.equals(tokens.get(VmSystemTags.STATIC_IP_L3_UUID_TOKEN), l3Uuid)) {
                return  tokens.get(VmSystemTags.MAC_TOKEN);
            }
        }
        return null;
    }

    public void deleteCustomMacSystemTag(String vmUuid, String l3uuid, String mac) {
        String str = VmSystemTags.CUSTOM_MAC.instantiateTag(map(
                e(VmSystemTags.STATIC_IP_L3_UUID_TOKEN, l3uuid),
                e(VmSystemTags.MAC_TOKEN, mac)));
        that.delete(vmUuid, str);
    }

    private boolean isMulticastMac(String mac) {
        if (!pattern.matcher(mac.toLowerCase()).matches()){
            throw new OperationFailureException(operr("This is not a valid MAC address [%s]", mac));
        }
        String binaryString = new BigInteger(mac.substring(0,2), 16).toString(2);
        return binaryString.substring(binaryString.length() - 1).equals("1");
    }

    public void validateAvailableMac(String mac) {
        String lowercaseMac = mac.toLowerCase();
        Matcher matcher = pattern.matcher(lowercaseMac);
        if (!matcher.matches()) {
            throw new OperationFailureException(operr("Not a valid MAC address [%s]", mac));
        }
        if ("00:00:00:00:00:00".equals(lowercaseMac) || "ff:ff:ff:ff:ff:ff".equals(lowercaseMac)) {
            throw new OperationFailureException(operr("Disallowed address"));
        }
        if (isMulticastMac(lowercaseMac)){
            throw new OperationFailureException(operr("Expected unicast mac address, found multicast MAC address [%s]", mac));
        }
    }

    public boolean checkDuplicateMac(String hypervisorType, String mac) {
        if (!VmInstanceConstant.KVM_HYPERVISOR_TYPE.equals(hypervisorType)) {
            return false;
        }

        return Q.New(VmNicVO.class)
                .eq(VmNicVO_.hypervisorType, hypervisorType)
                .eq(VmNicVO_.mac, mac.toLowerCase())
                .notEq(VmNicVO_.state, VmNicState.disable)
                .isExists();
    }
}
