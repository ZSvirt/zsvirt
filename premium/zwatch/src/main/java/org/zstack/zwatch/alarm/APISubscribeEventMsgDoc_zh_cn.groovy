package org.zstack.zwatch.alarm

import org.zstack.zwatch.alarm.APISubscribeEventEvent

doc {
	title "SubscribeEvent"

	category "zwatch.alarm"

	desc """订阅事件"""

	rest {
		request {
			url "POST /v1/zwatch/events/subscriptions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISubscribeEventMsg.class

			desc """"""

			params {

				column {
					name "namespace"
					enclosedIn "params"
					desc "名字空间"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "eventName"
					enclosedIn "params"
					desc "事件名"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "actions"
					enclosedIn "params"
					desc "事件动作"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "labels"
					enclosedIn "params"
					desc "事件标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "emergencyLevel"
					enclosedIn "params"
					desc "报警等级"
					location "body"
					type "String"
					optional true
					since "3.8"
					values ("Emergent","Important","Normal")
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.8"
				}
			}
		}

		response {
			clz APISubscribeEventEvent.class
		}
	}
}