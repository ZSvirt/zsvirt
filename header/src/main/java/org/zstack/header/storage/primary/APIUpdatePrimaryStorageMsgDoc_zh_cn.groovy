package org.zstack.header.storage.primary

import org.zstack.header.storage.primary.APIUpdatePrimaryStorageEvent

doc {
	title "根系更新主存储信息(UpdatePrimaryStorage)"

	category "storage.primary"

	desc """更新主存储信息"""

	rest {
		request {
			url "PUT /v1/primary-storage/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdatePrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updatePrimaryStorage"
					desc "主存储的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "name"
					enclosedIn "updatePrimaryStorage"
					desc "主存储的新名称"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "description"
					enclosedIn "updatePrimaryStorage"
					desc "主存储的新详细描述"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "url"
					enclosedIn "updatePrimaryStorage"
					desc "主存储的新地址"
					location "body"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIUpdatePrimaryStorageEvent.class
		}
	}
}