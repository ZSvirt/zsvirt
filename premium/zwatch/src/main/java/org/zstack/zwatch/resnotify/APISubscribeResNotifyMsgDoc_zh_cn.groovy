package org.zstack.zwatch.resnotify

import org.zstack.zwatch.resnotify.APISubscribeResNotifyEvent

doc {
	title "订阅资源变更通知"

	category "zwatch"

	desc """创建资源变更通知订阅。订阅启用后，管理节点会在匹配的资源创建、更新或删除事件发生时向指定 webhook 地址投递通知。"""

	rest {
		request {
			url "POST /v1/zwatch/resnotify/subscriptions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APISubscribeResNotifyMsg.class

			desc """创建资源变更通知订阅请求。"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源变更通知订阅名称"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源变更通知订阅的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "resourceTypes"
					enclosedIn "params"
					desc "需要订阅的资源类型列表，例如 ResourceVO、HostVO、VmInstanceVO 或 AuditsVO"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "eventTypes"
					enclosedIn "params"
					desc "需要订阅的事件类型列表，可选 CREATE、UPDATE、DELETE"
					location "body"
					type "List"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "通知订阅类型，当前支持 WEBHOOK"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
					values ("WEBHOOK")
				}
				column {
					name "webhookUrl"
					enclosedIn "params"
					desc "接收资源变更通知的 webhook URL"
					location "body"
					type "String"
					optional false
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "secret"
					enclosedIn "params"
					desc "用于生成 X-ZStack-Signature 签名的密钥"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "customHeaders"
					enclosedIn "params"
					desc "投递 webhook 时附加的自定义 HTTP 请求头，JSON 字符串格式"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "1.0.1, zsv_5.1.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APISubscribeResNotifyEvent.class
		}
	}
}