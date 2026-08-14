package org.zstack.sns.platform.snmp;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.sns.SNSApplicationEndpointInventory;
import org.zstack.sns.SNSApplicationPlatformInventory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = SNSSnmpPlatformVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "endpoints", inventoryClass = SNSApplicationEndpointInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "platformUuid")
})
public class SNSSnmpPlatformInventory extends SNSApplicationPlatformInventory implements Serializable {
    private String snmpAddress;
    private int snmpPort;

    protected SNSSnmpPlatformInventory(SNSSnmpPlatformVO vo) {
        super(vo);
        this.setSnmpAddress(vo.getSnmpAddress());
        this.setSnmpPort(vo.getSnmpPort());
    }

    public static SNSSnmpPlatformInventory valueOf(SNSSnmpPlatformVO vo) {
        return new SNSSnmpPlatformInventory(vo);
    }

    public static List<SNSSnmpPlatformInventory> valueOf1(Collection<SNSSnmpPlatformVO> vos) {
        List<SNSSnmpPlatformInventory> invs = new ArrayList<SNSSnmpPlatformInventory>(vos.size());
        for (SNSSnmpPlatformVO vo : vos) {
            invs.add(SNSSnmpPlatformInventory.valueOf(vo));
        }
        return invs;
    }

    public SNSSnmpPlatformInventory() {
    }

    String getSnmpAddress() {
        return snmpAddress;
    }

    void setSnmpAddress(String $paramName) {
        snmpAddress = $paramName;
    }

    int getSnmpPort() {
        return snmpPort;
    }

    void setSnmpPort(int $paramName) {
        snmpPort = $paramName;
    }
}
