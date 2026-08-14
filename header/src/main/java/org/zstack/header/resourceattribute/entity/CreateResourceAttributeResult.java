package org.zstack.header.resourceattribute.entity;

import org.zstack.header.configuration.PythonClass;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;

import java.util.Collection;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transform;

@PythonClass
public class CreateResourceAttributeResult {
    private ErrorCode error;
    private ResourceAttributeValueInventory inventory;
    private boolean success = true;

    public static CreateResourceAttributeResult valueOf(ErrorableValue<ResourceAttributeValueVO> value) {
        CreateResourceAttributeResult result = new CreateResourceAttributeResult();

        if (value.isSuccess()) {
            result.setInventory(ResourceAttributeValueInventory.valueOf(value.result));
        } else {
            result.setError(value.error);
            result.setSuccess(false);
        }

        return result;
    }

    public static List<CreateResourceAttributeResult> valueOf(Collection<ErrorableValue<ResourceAttributeValueVO>> value) {
        return transform(value, CreateResourceAttributeResult::valueOf);
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode error) {
        this.error = error;
        this.success = error == null;
    }

    public ResourceAttributeValueInventory getInventory() {
        return inventory;
    }

    public void setInventory(ResourceAttributeValueInventory inventory) {
        this.inventory = inventory;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public static CreateResourceAttributeResult __example__() {
        CreateResourceAttributeResult result = new CreateResourceAttributeResult();
        result.setInventory(ResourceAttributeValueInventory.__example__());
        return result;
    }
}
