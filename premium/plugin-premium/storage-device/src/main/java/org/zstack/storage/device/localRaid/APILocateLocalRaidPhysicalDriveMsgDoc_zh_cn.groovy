package org.zstack.storage.device.localRaid

import org.zstack.storage.device.localRaid.APILocateLocalRaidPhysicalDriveEvent

doc {
	title "LocateLocalRaidPhysicalDrive"

	category "storageDevice"

	desc """操作Raid物理盘定位灯"""

	rest {
		request {
			url "PUT /v1/storage-devices/local-raid/physical-drives/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APILocateLocalRaidPhysicalDriveMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "locateLocalRaidPhysicalDrive"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.6"
				}
				column {
					name "locate"
					enclosedIn "locateLocalRaidPhysicalDrive"
					desc "打开或关闭定位灯"
					location "body"
					type "Boolean"
					optional true
					since "3.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.6"
				}
			}
		}

		response {
			clz APILocateLocalRaidPhysicalDriveEvent.class
		}
	}
}