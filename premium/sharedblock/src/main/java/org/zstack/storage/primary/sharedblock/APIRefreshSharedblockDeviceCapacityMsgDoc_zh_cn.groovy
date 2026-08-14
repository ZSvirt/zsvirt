package org.zstack.storage.primary.sharedblock

import org.zstack.storage.primary.sharedblock.APIRefreshSharedBlockDeviceCapacityEvent

doc {
	title "RefreshSharedblockDeviceCapacity"

	category "storage.primary"

	desc """更新共享块设备容量"""

	rest {
		request {
			url "POST /v1/primary-storage/sharedblockgroup/{sharedBlockGroupUuid}/sharedblocks/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshSharedblockDeviceCapacityMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "共享块设备UUID，如为空则更新共享块存储下所有设备"
					location "url"
					type "String"
					optional true
					since "2.6"
				}
				column {
					name "sharedBlockGroupUuid"
					enclosedIn "params"
					desc "共享块存储UUID"
					location "url"
					type "String"
					optional false
					since "2.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "2.6"
				}
			}
		}

		response {
			clz APIRefreshSharedBlockDeviceCapacityEvent.class
		}
	}
}