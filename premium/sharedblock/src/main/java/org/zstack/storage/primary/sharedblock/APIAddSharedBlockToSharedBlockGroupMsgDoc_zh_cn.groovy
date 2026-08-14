package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIAddSharedBlockToSharedBlockGroupEvent

doc {
	title "AddSharedBlockToSharedBlockGroup"

	category "storage.primary"

	desc """添加共享块设备到共享块存储"""

	rest {
		request {
			url "POST /v1/primary-storage/sharedblockgroup/{uuid}/sharedblocks"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSharedBlockToSharedBlockGroupMsg.class

			desc """"""

			params {

				column {
					name "diskUuid"
					enclosedIn "params"
					desc "磁盘维一标示（例如UUID, WWN, WWID）"
					location "body"
					type "String"
					optional false
					since "2.3.2"
				}
				column {
					name "uuid"
					enclosedIn "params"
					desc "主存储UUID"
					location "url"
					type "String"
					optional false
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
			}
		}

		response {
			clz APIAddSharedBlockToSharedBlockGroupEvent.class
		}
	}
}