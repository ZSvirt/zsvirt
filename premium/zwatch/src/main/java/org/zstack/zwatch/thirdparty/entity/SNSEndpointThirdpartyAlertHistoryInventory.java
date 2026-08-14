package org.zstack.zwatch.thirdparty.entity;

import org.zstack.core.Platform;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = SNSEndpointThirdpartyAlertHistoryVO.class, collectionValueOfMethod = "valueOf1")
public class SNSEndpointThirdpartyAlertHistoryInventory implements Serializable {
    private String alertUuid;
    private String endpointUuid;
    private String subscriptionUuid;
    private Timestamp createDate;

    protected SNSEndpointThirdpartyAlertHistoryInventory(SNSEndpointThirdpartyAlertHistoryVO vo) {
        this.setAlertUuid(vo.getAlertUuid());
        this.setEndpointUuid(vo.getEndpointUuid());
        this.setSubscriptionUuid(vo.getSubscriptionUuid());
        this.setCreateDate(vo.getCreateDate());
    }

    public static SNSEndpointThirdpartyAlertHistoryInventory valueOf(SNSEndpointThirdpartyAlertHistoryVO vo) {
        return new SNSEndpointThirdpartyAlertHistoryInventory(vo);
    }

    public static List<SNSEndpointThirdpartyAlertHistoryInventory> valueOf1(Collection<SNSEndpointThirdpartyAlertHistoryVO> vos) {
        List<SNSEndpointThirdpartyAlertHistoryInventory> invs = new ArrayList<SNSEndpointThirdpartyAlertHistoryInventory>(vos.size());
        for (SNSEndpointThirdpartyAlertHistoryVO vo : vos) {
            invs.add(SNSEndpointThirdpartyAlertHistoryInventory.valueOf(vo));
        }
        return invs;
    }

    public SNSEndpointThirdpartyAlertHistoryInventory() {
    }

    public static SNSEndpointThirdpartyAlertHistoryInventory __example__() {
        SNSEndpointThirdpartyAlertHistoryInventory inventory = new SNSEndpointThirdpartyAlertHistoryInventory();
        inventory.setEndpointUuid(Platform.getUuid());
        inventory.setAlertUuid(Platform.getUuid());
        inventory.createDate = new Timestamp(System.currentTimeMillis());
        return inventory;
    }

    public String getAlertUuid() {
        return alertUuid;
    }

    public void setAlertUuid(String $paramName) {
        alertUuid = $paramName;
    }

    public String getEndpointUuid() {
        return endpointUuid;
    }

    public void setEndpointUuid(String $paramName) {
        endpointUuid = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String subscriptionUuid) {
        this.subscriptionUuid = subscriptionUuid;
    }
}
