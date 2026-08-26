package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.APIUpdateResNotifySubscriptionEvent

doc {
	title "更新资源变更通知订阅"

	category "zwatch"

	desc """更新资源变更通知订阅配置，包括订阅名称、资源类型、事件类型、状态、webhook 地址、签名密钥以及自定义请求头。"""

	rest {
		request {
			url "PUT /v1/zwatch/resnotify/subscriptions/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResNotifySubscriptionMsg.class

			desc """更新资源变更通知订阅请求。"""

			params {

				column {
					name "uuid"
					enclosedIn "updateResNotifySubscription"
					desc "资源变更通知订阅的UUID，唯一标识该订阅"
					location "url"
					type "String"
					optional false
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "name"
					enclosedIn "updateResNotifySubscription"
					desc "资源变更通知订阅名称"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "description"
					enclosedIn "updateResNotifySubscription"
					desc "资源变更通知订阅的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "resourceTypes"
					enclosedIn "updateResNotifySubscription"
					desc "需要订阅的资源类型列表，例如 ResourceVO、HostVO、VmInstanceVO 或 AuditsVO"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "eventTypes"
					enclosedIn "updateResNotifySubscription"
					desc "需要订阅的事件类型列表，可选 CREATE、UPDATE、DELETE"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "state"
					enclosedIn "updateResNotifySubscription"
					desc "订阅状态，可设置为 Enabled 或 Disabled"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
					values ("Enabled","Disabled")
				}
				column {
					name "webhookUrl"
					enclosedIn "updateResNotifySubscription"
					desc "接收资源变更通知的 webhook URL"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "secret"
					enclosedIn "updateResNotifySubscription"
					desc "用于生成 X-ZStack-Signature 签名的密钥"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "customHeaders"
					enclosedIn "updateResNotifySubscription"
					desc "投递 webhook 时附加的自定义 HTTP 请求头，JSON 字符串格式"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIUpdateResNotifySubscriptionEvent.class
		}
	}
}