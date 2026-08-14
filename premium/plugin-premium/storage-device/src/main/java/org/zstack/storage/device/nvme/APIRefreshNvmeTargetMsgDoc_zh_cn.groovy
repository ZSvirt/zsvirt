package org.zstack.storage.device.nvme

import org.zstack.storage.device.nvme.APIRefreshNvmeTargetEvent

doc {
	title "RefreshNvmeTarget"

	category "storageDevice"

	desc """刷新NVMe设备结果"""

	rest {
		request {
			url "POST /v1/storage-devices/nvme/controllers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRefreshNvmeTargetMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.16.21"
				}
				column {
					name "nvmeLunUuids"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.16.21"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.21"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.21"
				}
			}
		}

		response {
			clz APIRefreshNvmeTargetEvent.class
		}
	}
}