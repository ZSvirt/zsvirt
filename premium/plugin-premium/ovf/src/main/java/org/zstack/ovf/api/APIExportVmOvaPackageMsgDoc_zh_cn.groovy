package org.zstack.ovf.api

import org.zstack.ovf.api.APIExportVmOvaPackageEvent

doc {
	title "ExportVmOvaPackage"

	category "ovf"

	desc """导出云主机的OVA包"""

	rest {
		request {
			url "POST /v1/ovf/ova-packages"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIExportVmOvaPackageMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "vmUuid"
					enclosedIn "params"
					desc "源云主机UUID"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "backupStorageUuid"
					enclosedIn "params"
					desc "导出目的镜像存储UUID"
					location "body"
					type "String"
					optional false
					since "3.14.6"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.14.6"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.14.6"
				}
			}
		}

		response {
			clz APIExportVmOvaPackageEvent.class
		}
	}
}