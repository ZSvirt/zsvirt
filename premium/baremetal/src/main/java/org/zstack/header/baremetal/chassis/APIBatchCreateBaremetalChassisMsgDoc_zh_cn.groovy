package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIBatchCreateBaremetalChassisEvent

doc {
	title "BatchCreateBaremetalChassis"

	category "baremetal.chassis"

	desc """批量添加裸金属设备"""

	rest {
		request {
			url "POST /v1/baremetal/chassis/from-file"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIBatchCreateBaremetalChassisMsg.class

			desc """批量添加裸金属设备"""

			params {

				column {
					name "baremetalChassisInfo"
					enclosedIn "params"
					desc "经过base64编码的裸金属设备信息"
					location "body"
					type "String"
					optional false
					since "3.1.1"
				}
				column {
					name "longJobName"
					enclosedIn "params"
					desc "长任务名称"
					location "body"
					type "String"
					optional true
					since "3.1.1"
				}
				column {
					name "longJobDescription"
					enclosedIn "params"
					desc "长任务简介"
					location "body"
					type "String"
					optional true
					since "3.1.1"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.1.1"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.1.1"
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
			clz APIBatchCreateBaremetalChassisEvent.class
		}
	}
}