package org.zstack.header.simulator.storage.primary

import org.zstack.header.storage.primary.APIAddPrimaryStorageEvent

doc {
	title "AddSimulatorPrimaryStorage"

	category "storage.primary"

	desc """在这里填写API描述"""

	rest {
		request {
			url "POST /v1/primary-storage/simulators"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSimulatorPrimaryStorageMsg.class

			desc """"""

			params {

				column {
					name "totalCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional true
					since "0.6"
				}
				column {
					name "availableCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional true
					since "0.6"
				}
				column {
					name "url"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "type"
					enclosedIn "params"
					desc ""
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
					desc ""
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
					name "availablePhysicalCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
					optional true
					since "0.6"
				}
				column {
					name "totalPhysicalCapacity"
					enclosedIn "params"
					desc ""
					location "body"
					type "long"
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