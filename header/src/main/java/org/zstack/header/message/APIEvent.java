package org.zstack.header.message;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.APINoSee;

public class APIEvent extends Event implements APIResponse {
    /**
     * @ignore
     */
    public static final String API_EVENT = "API_EVENT";

    /**
     * @ignore
     */
    @APINoSee
    @NoJsonSchema
    private Type type = null;
    /**
     * @ignore
     */
    @APINoSee
    protected final String apiId;
    /**
     * @desc indicate the failure or success. Client should evaluate this field before evaluating
     * inventory field
     * @choices true false
     */
    protected boolean success;
    /**
     * @desc indicate the reason of api failure. It presents only if success = false
     * @nullable
     */
    @NeedJsonSchema
    protected ErrorCode error;

    @Override
    public final Type getType() {
        if (type == null) {
            type = new Type(Event.Category.API, getSubCategory());
        }
        return type;
    }

    public APIEvent() {
        apiId = null;
    }

    public APIEvent(String apiId) {
        this.apiId = apiId;
        this.success = true;
    }

    public String getApiId() {
        return apiId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean isSuccess) {
        this.success = isSuccess;
    }

    public ErrorCode getError() {
        return error;
    }

    public void setError(ErrorCode errorCode) {
        this.success = false;
        this.error = errorCode;
    }

    @Override
    public final String getSubCategory() {
        return API_EVENT;
    }
 
    public static APIEvent __example__() {
        APIEvent event = new APIEvent();


        return event;
    }

}
