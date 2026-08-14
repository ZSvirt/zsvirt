package org.zstack.sdk;



public class CdRomTO extends org.zstack.sdk.IsoTO {

    public boolean isEmpty;
    public void setIsEmpty(boolean isEmpty) {
        this.isEmpty = isEmpty;
    }
    public boolean getIsEmpty() {
        return this.isEmpty;
    }

    public int bootOrder;
    public void setBootOrder(int bootOrder) {
        this.bootOrder = bootOrder;
    }
    public int getBootOrder() {
        return this.bootOrder;
    }

}
