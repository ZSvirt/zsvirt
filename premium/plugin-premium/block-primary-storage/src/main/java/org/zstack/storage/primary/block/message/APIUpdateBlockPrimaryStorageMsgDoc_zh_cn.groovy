package org.zstack.storage.primary.block.message

import org.zstack.storage.primary.block.message.APIUpdateBlockPrimaryStorageEvent

doc {
	title "UpdateBlockPrimaryStorage"

	category "storage.primary"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/primary-storage/block/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateBlockPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "vendorName"
					enclosedIn "updateBlockPrimaryStorage"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "metadata"
					enclosedIn "updateBlockPrimaryStorage"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.18.0"
				}
				column {
					name "uuid"
					enclosedIn "updateBlockPrimaryStorage"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.18.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.18.0"
				}
			}
		}

		response {
			clz APIUpdateBlockPrimaryStorageEvent.class
		}
	}
}