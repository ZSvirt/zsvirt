package org.zstack.monitoring.actions

doc {
    title "CreateMonitorTriggerAction"

    category "monitoring"

    desc """创建报警器动作"""

    rest {
        request {
			url "POST /v1/monitoring/trigger-actions"


            header(Authorization: 'OAuth the-session-uuid')

            clz APICreateMonitorTriggerActionMsg.class

            desc """"""
            
			params {

				column {
					name "name"
					enclosedIn ""
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "description"
					enclosedIn ""
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "postScript"
					enclosedIn ""
					desc "保留字段"
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "mediaUuids"
					enclosedIn ""
					desc "媒体UUIDs"
					location "body"
					type "List"
					optional false
					since "0.6"
					
				}
				column {
					name "triggerUuids"
					enclosedIn ""
					desc "报警器UUIDs"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "resourceUuid"
					enclosedIn ""
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
					
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APICreateMonitorTriggerActionEvent.class
        }
    }
}