package org.zstack.storage.primary.sharedblock

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "AddSharedBlockGroupPrimaryStorage"

	category "storage.primary"

	desc """添加共享块存储类型的主存储"""

	rest {
		request {
			url "POST /v1/primary-storage/sharedblockgroup"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSharedBlockGroupPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "diskUuids"
					enclosedIn "params"
					desc "磁盘唯一表示（例如UUID, WWN, WWID）"
					location "body"
					type "List"
					optional false
					since "2.3.2"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.3.2"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3.2"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "主存储类型，此处为 SharedBlock"
					location "body"
					type "String"
					optional true
					since "2.3.2"
				}
				column {
					name "url"
					enclosedIn "params"
					desc "未使用"
					location "body"
					type "String"
					optional false
					since "2.3.2"
				}
				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "2.3.2"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3.2"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3.2"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.3.2"
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