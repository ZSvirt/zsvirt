package org.zstack.header.sriov;

public enum EthernetVfStatus {
    Available,
    Reserved,  /* new allocated to vm, it will be used when vm started */
    Attached,
    Releasing,   /* to be deleted when vm migrate to new host,
    if migrate success, change to Available, or change to Attached */
}
