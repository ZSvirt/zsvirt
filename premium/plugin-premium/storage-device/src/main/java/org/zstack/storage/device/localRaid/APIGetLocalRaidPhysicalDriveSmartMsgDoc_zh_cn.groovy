package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APIGetLocalRaidPhysicalDriveSmartReply

doc {
	title "GetLocalRaidPhysicalDriveSmart"

	category "storageDevice"

	desc """获取Raid物理盘SMART信息"""

	rest {
		request {
			url "GET /v1/storage-devices/local-raid/physical-drives/{uuid}/smart"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetLocalRaidPhysicalDriveSmartMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.6"
				}
			}
		}

		response {
			clz APIGetLocalRaidPhysicalDriveSmartReply.class
		}
	}
}