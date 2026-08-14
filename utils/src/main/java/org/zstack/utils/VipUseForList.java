package org.zstack.utils;

import org.zstack.utils.logging.CLogger;

import java.util.*;

/**
 * Created by shixin 2017/10/06
 */
public class VipUseForList {

    public static final String EIP_NETWORK_SERVICE_TYPE = "Eip";
    public static final String LB_NETWORK_SERVICE_TYPE = "LoadBalancer";
    public static final String SLB_NETWORK_SERVICE_TYPE = "SLB";
    public static final String PORTFORWARDING_NETWORK_SERVICE_TYPE = "PortForwarding";
    public static final String IPSEC_NETWORK_SERVICE_TYPE = "IPsec";
    public static final String SNAT_NETWORK_SERVICE_TYPE = "SNAT";

    /* vip usefor field will be encoded like 'PortForwarding,LoadBalancer' */
    private List<String> useForList;
    private static CLogger logger = Utils.getLogger(VipUseForList.class);

    public VipUseForList() {
        useForList = new ArrayList<>();
    }

    public VipUseForList(Collection<String> useFor) {
        if (useFor != null) {
            useForList = new ArrayList<>(new HashSet<>(useFor));
        } else {
            useForList = new ArrayList<>();
        }
    }

    public List<String> getUseForList() {
        return this.useForList;
    }

    public boolean isIncluded(String item){
        return useForList.contains(item);
    }

    /* true -- sucess, false -- failed*/
    public boolean validate(){
        /* eip and other services(SNAT,LB,IPSec,PF) can not be attached to same vip */
        return  (isIncluded(EIP_NETWORK_SERVICE_TYPE) && useForList.size() > 1);
    }

    public boolean validateNewAdded(String item){
        /* eip and other services(SNAT,LB,IPSec,PF) can not be attached to same vip */
        if(EIP_NETWORK_SERVICE_TYPE.equals(item)){
            return useForList.isEmpty();
        } if(SLB_NETWORK_SERVICE_TYPE.equals(item)){
            return useForList.isEmpty();
        } else {
            return !isIncluded(EIP_NETWORK_SERVICE_TYPE) && !isIncluded(SLB_NETWORK_SERVICE_TYPE);
        }
    }

    public boolean validateNewAdded(VipUseForList newList){
        for (String useFor : newList.getUseForList()) {
            if (!validateNewAdded(useFor)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String toString() {
        final String delimiter = ",";

        if (useForList.isEmpty()){
            return "";
        }
        else {
            return String.join(delimiter, useForList);
        }
    }
}

