package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.APIDeleteResNotifySubscriptionEvent

doc {
	title "删除资源变更通知订阅"

	category "zwatch"

	desc """删除指定的资源变更通知订阅，删除后该订阅不再接收资源变更事件并停止 webhook 投递。"""

	rest {
		request {
			url "DELETE /v1/zwatch/resnotify/subscriptions/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteResNotifySubscriptionMsg.class

			desc """删除资源变更通知订阅请求。"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源变更通知订阅的UUID，唯一标识该订阅"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式，可选 Permissive 或 Enforcing，默认 Permissive"
					location "query"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIDeleteResNotifySubscriptionEvent.class
		}
	}
}