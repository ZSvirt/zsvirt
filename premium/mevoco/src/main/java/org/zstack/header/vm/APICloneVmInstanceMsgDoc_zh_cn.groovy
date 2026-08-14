package org.zstack.header.vm

import org.zstack.header.vm.APICloneVmInstanceEvent

doc {
	title "CloneVmInstance"

	category "mevoco"

	desc """克隆云主机到指定物理机上"""

	rest {
		request {
			url "PUT /v1/vm-instances/{vmInstanceUuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICloneVmInstanceMsg.class

			desc """克隆云主机到指定物理机上"""

			params {

				column {
					name "vmInstanceUuid"
					enclosedIn "cloneVmInstance"
					desc "云主机UUID"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "strategy"
					enclosedIn "cloneVmInstance"
					desc "策略"
					location "body"
					type "String"
					optional true
					since "0.6"
					values ("InstantStart","JustCreate")
				}
				column {
					name "names"
					enclosedIn "cloneVmInstance"
					desc "云主机的名字清单"
					location "body"
					type "List"
					optional false
					since "0.6"
				}
				column {
					name "full"
					enclosedIn "cloneVmInstance"
					desc "是否克隆已挂载数据云盘"
					location "body"
					type "Boolean"
					optional true
					since "2.5"
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
					name "primaryStorageUuidForRootVolume"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "primaryStorageUuidForDataVolume"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "String"
					optional true
					since "0.6"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "dataVolumeSystemTags"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "primaryStorageUuidForRootVolume"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "primaryStorageUuidForDataVolume"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.0.0"
				}
				column {
					name "rootVolumeSystemTags"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "dataVolumeSystemTags"
					enclosedIn "cloneVmInstance"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.0.0"
				}
				column {
					name "clusterUuid"
					enclosedIn "cloneVmInstance"
					desc "集群UUID"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
				column {
					name "hostUuid"
					enclosedIn "cloneVmInstance"
					desc "物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.16.11"
				}
				column {
					name "diskAOs"
					enclosedIn "cloneVmInstance"
					desc "硬盘参数"
					location "body"
					type "List"
					optional true
					since "4.10.10"
				}
				column {
					name "description"
					enclosedIn "cloneVmInstance"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.10.10"
				}
				column {
					name "vmNicParams"
					enclosedIn "cloneVmInstance"
					desc "网卡参数"
					location "body"
					type "String"
					optional true
					since "4.10.16"
				}
				column {
					name "vmCustomSpecification"
					enclosedIn "cloneVmInstance"
					desc "虚拟机自定义操作系统"
					location "body"
					type "VmCustomSpecificationStruct"
					optional true
					since "4.10.18"
				}
				column {
					name "resetTpm"
					enclosedIn "cloneVmInstance"
					desc "创建的虚拟机是否重置 TPM 状态"
					location "body"
					type "Boolean"
					optional true
					since "5.0.0"
				}
			}
		}

		response {
			clz APICloneVmInstanceEvent.class
		}
	}
}