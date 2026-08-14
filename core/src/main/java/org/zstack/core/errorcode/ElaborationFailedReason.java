package org.zstack.core.errorcode;

/**
 * Created by mingjian.deng on 2018/12/21.
 */
public enum ElaborationFailedReason {
    FileNameWithoutJson("file name must endWith '.json'"),
    RegexAlreadyExisted("regex already existed in zstack"),
    DuplicatedRegex("regex duplicated at least twice"),
    CategoryNotFound("can not found category for the segment"),
    MessageNotFound("can not found message_cn for the regex"),
    RegexNotFound("can not found regex for the segment"),
    InValidJsonSchema("invalid json schema"),
    InValidJsonArraySchema("contents must be json array"),
    DuplicatedErrorCode("error code duplicated at least twice"),
    ErrorCodeAlreadyExisted("error code already existed in zstack"),
    OtherReason("OtherReason");

    String type;

    ElaborationFailedReason(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static ElaborationFailedReason get(String type) {
        for (ElaborationFailedReason tmp: ElaborationFailedReason.values()) {
            if (tmp.toString().equalsIgnoreCase(type)) {
                return tmp;
            }
        }
        return ElaborationFailedReason.valueOf(type);
    }
}
