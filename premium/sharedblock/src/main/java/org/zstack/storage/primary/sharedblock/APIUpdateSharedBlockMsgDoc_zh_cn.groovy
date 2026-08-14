package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIUpdateSharedBlockEvent

doc {
	title "UpdateSharedBlock"

	category "storage.primary"

	desc """修改共享块存储中的共享块信息"""

	rest {
		request {
			url "PUT /v1/primary-storage/sharedblockgroup/{sharedBlockGroupUuid}/sharedblocks/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSharedBlockMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSharedBlock"
					desc "共享块设备UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "sharedBlockGroupUuid"
					enclosedIn "updateSharedBlock"
					desc "共享块存储UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "name"
					enclosedIn "updateSharedBlock"
					desc "共享块名称"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "description"
					enclosedIn "updateSharedBlock"
					desc "共享块的详细描述"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "diskUuid"
					enclosedIn "updateSharedBlock"
					desc "磁盘唯一标识符"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
			}
		}

		response {
			clz APIUpdateSharedBlockEvent.class
		}
	}
}