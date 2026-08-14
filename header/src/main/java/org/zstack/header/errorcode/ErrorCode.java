package org.zstack.header.errorcode;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.header.core.I18nMessage;
import org.zstack.header.message.NoJsonSchema;
import org.zstack.header.rest.APINoSee;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.opaque.OpaqueCollection;
import org.zstack.utils.opaque.OpaqueScripts;
import org.zstack.utils.string.ErrorCodeElaboration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.zstack.utils.CollectionUtils.isEmpty;
import static org.zstack.utils.opaque.OpaqueConstants.*;

/**
 * This is an example:  (since zsv_4.10.28)
 *
 * <blockquote><pre>
 * code = "SYS.1006"
 * elaboration = "An operation failed"  (from sys.xml)
 * details = "the quota[name:vm.num] of account[uuid:f2ac2633101d014b9d4307be7d45883b36215c28] can not be -6"
 * i18nDetails = "帐户[uuid：f2ac2633101d014b9d4307be7d45883b36215c28]的配额[名称：vm.num]不能为-6"
 *         (from i18n_identity.json => conf/i18n/messages_zh_CN.properties)
 *
 * opaque = {
 *     "template" : "the quota[name:%s] of account[uuid:%s] can not be %d",
 *     "0" : "vm.num",
 *     "1" : "f2ac2633101d014b9d4307be7d45883b36215c28",
 *     "2" : -6
 * }
 * </pre></blockquote>
 */
public class ErrorCode implements Serializable, Cloneable, OpaqueCollection, I18nMessage {
    private String code;
    private String description;
    private String details;
    private String i18nDetails;

    @Deprecated
    private String elaboration;
    @APINoSee
    @Deprecated
    private ErrorCodeElaboration messages;
    @APINoSee
    @Deprecated
    private String cost;
    /**
     * TODO: merge cause to causes
     */
    @Deprecated
    private ErrorCode cause;
    protected final List<ErrorCode> causes = Collections.synchronizedList(new ArrayList<>());
    @NoJsonSchema
    private LinkedHashMap<String, Object> opaque;

    public LinkedHashMap getOpaque() {
        return opaque;
    }

    public void setOpaque(LinkedHashMap opaque) {
        this.opaque = opaque;
    }

    @Override
    public ErrorCode withOpaque(String key, Object value) {
        if (opaque == null) {
            opaque = new LinkedHashMap<>();
        }
        opaque.put(key, value);
        return this;
    }

    @Override
    public ErrorCode withOpaque(OpaqueScripts scripts) {
        OpaqueCollection.super.withOpaque(scripts);
        return this;
    }

    public ErrorCode withException(String reason) {
        return withOpaque(OPAQUE_KEY_EXCEPTION, reason);
    }

    public ErrorCode withException(Throwable e) {
        if (e instanceof OpaqueScripts) {
            withOpaque((OpaqueScripts) e);
        } else {
            withOpaque(OPAQUE_KEY_EXCEPTION, e.getCause());
        }
        return this;
    }

    @Override
    public Object getFromOpaque(String key) {
        return opaque == null ? null : opaque.get(key);
    }

    public ErrorCode() {
    }

    public ErrorCode(String code, String description) {
        this(code, description, null, null);
    }

    public ErrorCode(String code, String description, String details) {
        this(code, description, details, details);
    }

    public ErrorCode(String code, String description, String details, String i18nDetails) {
        super();
        this.code = code;
        this.description = description;
        this.details = details;
        this.i18nDetails = i18nDetails;
    }

    public ErrorCode(ErrorCode other) {
        this.code = other.code;
        this.description = other.description;
        this.details = other.details;
        this.i18nDetails = other.i18nDetails;
        this.elaboration = other.elaboration;
        this.messages = other.messages;
        this.cause = other.cause;
        this.causes.addAll(other.causes);
        if (other.opaque != null) {
            this.opaque = new LinkedHashMap<>(other.opaque);
        }
    }

    public void setCode(String code) {
        this.code = code;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String getI18nDetails() {
        return i18nDetails;
    }

    public void setI18nDetails(String i18nDetails) {
        this.i18nDetails = i18nDetails;
    }

    public ErrorCode copy() {
        return new ErrorCode(this);
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }

    public ErrorCode getCause() {
        return cause;
    }

    public void setCause(ErrorCode cause) {
        this.cause = cause;
    }

    public List<ErrorCode> getCauses() {
        return causes;
    }

    public void setCauses(List<ErrorCode> causes) {
        this.causes.clear();
        if (!CollectionUtils.isEmpty(causes))
            this.causes.addAll(causes);
    }

    public ErrorCode withCause(ErrorCode cause) {
        causes.add(cause);
        return this;
    }

    public ErrorCode withCause(ErrorCodeList list) {
        this.causes.addAll(list.getCauses());
        return this;
    }

    public ErrorCode withCause(Collection<ErrorCode> causes) {
        this.causes.addAll(causes);
        return this;
    }

    public ErrorCode causedBy(ErrorCode cause) {
        setCause(cause);
        return this;
    }

    /**
     * use "withCause"
     */
    @Deprecated
    public ErrorCode causedBy(List<ErrorCode> cause) {
        return withCause(cause);
    }

    public String getElaboration() {
        return elaboration;
    }

    public void setElaboration(String elaboration) {
        this.elaboration = elaboration;
    }

    public boolean isError(Enum<?>... errorEnums) {
        for (Enum<?> e : errorEnums) {
            if (e.toString().equals(getCode())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(Object t) {
        if (this == t){
            return true;
        }
        if (!(t instanceof ErrorCode)){
            return false;
        }

        ErrorCode other = (ErrorCode)t;
        return Objects.equals(this.code, other.code) &&
                Objects.equals(this.cause, other.cause) &&
                Objects.equals(this.details, other.details) &&
                Objects.equals(this.opaque, other.opaque) &&
                Objects.equals(this.causes, other.causes);
    }

    @Override
    public int hashCode() {
        StringBuilder sb = new StringBuilder();
        sb.append(code == null ? "" : code);
        sb.append(details == null ? "" : details);
        sb.append(opaque == null ? "" : opaque);
        sb.append(cause == null ? "" : cause.toString());
        sb.append(causes.isEmpty() ? "" : causes.toString());
        return sb.toString().hashCode();
    }

    public String getReadableDetails() {
        final StringBuilder builder = new StringBuilder(2048);
        getReadableDetails(builder, 0);
        return builder.toString();
    }

    private void getReadableDetails(StringBuilder builder, int level) {
        if (level > 0) {
            builder.append("\n");
        }

        if (level >= 16) { // may loop
            builder.append("\n...");
            return;
        }

        for (int i = 0; i < level; i++) {
            builder.append("  ");
        }

        builder.append(String.format("[%s] %s", code == null ? "???" : code, details == null ? "" : details));
        if (opaque != null && !opaque.isEmpty()) {
            for (Map.Entry<String, Object> entry : opaque.entrySet()) {
                String key = entry.getKey();
                if (OPAQUE_KEY_TEMPLATE.equals(key) || isOpaqueKey(key)) {
                    continue;
                }

                builder.append("\n");
                for (int i = 0; i < level; i++) {
                    builder.append("  ");
                }
                builder.append(String.format("        * %s: %s", key, JSONObjectUtil.toJsonString(entry.getValue())));
            }
        }

        if (cause != null) { // TODO: merge cause to causes
            cause.getReadableDetails(builder, level + 1);
        }

        for (ErrorCode causeItem : causes) {
            if (causeItem == null) {
                continue;
            }
            causeItem.getReadableDetails(builder, level + 1);
        }
    }

    /**
     * use {@link #rootCause()}
     */
    @Deprecated
    public ErrorCode getRootCause() {
        ErrorCode root = this;
        do {
            if (root.cause != null) {
                root = root.cause;
            } else {
                break;
            }
        } while (true);
        return root;
    }

    public ErrorCode rootCause() {
        ErrorCode root = this;
        do {
            if (isEmpty(root.causes)) {
                break;
            }
            root = root.causes.get(0);
        } while (true);
        return root;
    }

    public String getRootCauseDetails() {
        if (cause == null) {
            return getDetails() != null ? getDetails() : getDescription();
        }

        ErrorCode root = getRootCause();
        return root.getReadableDetails();
    }

    public ErrorCodeElaboration getMessages() {
        return messages;
    }

    public void setMessages(ErrorCodeElaboration messages) {
        this.messages = messages;
    }

    public String getCost() {
        return cost;
    }

    public void setCost(String cost) {
        this.cost = cost;
    }

    public String getLocation() {
        return (String) getFromOpaque(OPAQUE_KEY_ERROR_LOCATION);
    }

    public void setLocation(String location) {
        withOpaque(OPAQUE_KEY_ERROR_LOCATION, location);
    }

    public OperationFailureException toException() {
        return new OperationFailureException(this);
    }

    public ErrorCode logError(CLogger logger) {
        try {
            logger.error(getReadableDetails());
        } catch (Exception ignored) {
            logger.error(details == null ? "some errors happened" : details);
        }
        return this;
    }

    public ErrorCode logWarn(CLogger logger) {
        try {
            logger.warn(getReadableDetails());
        } catch (Exception ignored) {
            logger.warn(details == null ? "some errors happened" : details);
        }
        return this;
    }

    public <T> ErrorableValue<T> toErrorableValue() {
        return ErrorableValue.ofErrorCode(this);
    }
}
