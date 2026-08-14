package org.zstack.simulator2

/**
 * Created by xing5 on 2017/9/15.
 */
class RestApiHooker {
    enum HookMethod {
        byJobUuid,
        byScript
    }

    enum ScriptType {
        javascript,
        python
    }

    String path
    HookMethod hookMethod
    String jobUuid
    Integer errorCode
    String errorMessage

    String script
    ScriptType scriptType

    void validate() {
        assert path != null : "path cannot be null"
        assert hookMethod != null : "hookMethod cannot be null"

        if (hookMethod == HookMethod.byScript) {
            assert scriptType != null : "scripType cannot be null when hookMethod is byScript"
            assert script != null : "script cannot be null when scriptType is set"
        } else if (hookMethod == HookMethod.byJobUuid) {
            assert jobUuid != null : "jobUuid cannot be null when hookMethod is byJobUuid"
            assert errorCode != null : "errorCode cannot be null when hookMethod is byJobUuid"
            assert errorMessage != null : "errorMessage cannot be null when hookMethod is byJobUuid"
        }

    }
}
