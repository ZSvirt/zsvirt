package org.zstack.header.managementnode;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.serializable.SerializableHelper;

import java.io.IOException;
import java.io.Serializable;

public class ManagementNodeContextInventory implements Serializable {
    private String version;

    public ManagementNodeContextInventory() {
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public byte[] toBytes() {
        try {
            return SerializableHelper.writeObject(this);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to serialize ManagementNodeContextInventory", e);
        }
    }

    public static ManagementNodeContextInventory fromBytes(byte[] bytes) {
        try {
            return SerializableHelper.readObject(bytes);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deserialize ManagementNodeContextInventory", e);
        }
    }
}
