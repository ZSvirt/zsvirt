package org.zstack.billing.spendingcalculator.pcidevice;

import org.zstack.billing.PriceVO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.pciDevice.PciDeviceOfferingVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by shixin on 2018/05/04.
 */
@Entity
@Table
public class PricePciDeviceOfferingRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    @ForeignKey(parentEntityClass = PriceVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String priceUuid;

    @Column
    @ForeignKey(parentEntityClass = PciDeviceOfferingVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String pciDeviceOfferingUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getPriceUuid() {
        return priceUuid;
    }

    public void setPriceUuid(String priceUuid) {
        this.priceUuid = priceUuid;
    }

    public String getPciDeviceOfferingUuid() {
        return pciDeviceOfferingUuid;
    }

    public void setPciDeviceOfferingUuid(String pciDeviceOfferingUuid) {
        this.pciDeviceOfferingUuid = pciDeviceOfferingUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
