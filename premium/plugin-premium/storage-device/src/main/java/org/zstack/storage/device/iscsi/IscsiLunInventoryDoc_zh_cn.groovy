package org.zstack.storage.device.iscsi

import java.lang.Long
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "iSCSI磁盘清单"

	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.0.0"
	}
	field {
		name "iscsiTargetUuid"
		desc "iSCSI目标UUID"
		type "String"
		since "3.0.0"
	}
	field {
		name "wwid"
		desc "唯一识别ID"
		type "String"
		since "3.0.0"
	}
	field {
		name "vendor"
		desc "设备供应商"
		type "String"
		since "3.0.0"
	}
	field {
		name "model"
		desc "设备型号"
		type "String"
		since "3.0.0"
	}
	field {
		name "wwn"
		desc "设备WWN"
		type "String"
		since "3.0.0"
	}
	field {
		name "serial"
		desc "设备序列号"
		type "String"
		since "3.0.0"
	}
	field {
		name "hctl"
		desc "SCSI设备HCTL"
		type "String"
		since "3.0.0"
	}
	field {
		name "type"
		desc "设备类型"
		type "String"
		since "3.0.0"
	}
	field {
		name "path"
		desc "设备路径"
		type "String"
		since "3.0.0"
	}
	field {
		name "size"
		desc "设备大小"
		type "Long"
		since "3.0.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.0.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.0.0"
	}
}
