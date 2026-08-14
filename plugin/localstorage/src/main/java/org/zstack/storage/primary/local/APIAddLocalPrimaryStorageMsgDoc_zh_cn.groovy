package org.zstack.storage.primary.local

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "添加本地存储为主存储(AddLocalPrimaryStorage)"

	category "storage.primary"

	desc """添加类型为本地存储的主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/local-storage"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddLocalPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "url"
					enclosedIn "params"
					desc "本地存储的路径"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "本地存储主存储名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "本地存储主存储的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "类型为 LocalStorage"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID。若指定，本地存储主存储会使用该字段值作为UUID。"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
				}
			}
		}

		response {
			clz APIAddPrimaryStorageEvent.class
		}
	}
}