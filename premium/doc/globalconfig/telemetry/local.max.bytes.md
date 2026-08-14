
## Name

```
local.max.bytes(Telemetry 本地数据容量上限)
```

### Description

```
Local telemetry data size limit in bytes
```

### 含义

```
管理节点本地 Telemetry 数据（日报、pending 队列、上传日志等）总容量上限，单位字节。
超出后删除最旧的可丢弃数据，不得修改 consent.granted.at、source.id、anonymization.salt。
```

### Type

```
java.lang.Long
```

### Category

```
telemetry
```

### 取值范围

```
[1, 9223372036854775807]
```

### 取值范围补充说明

```
须大于 0；V1 默认 52428800 字节（50 MB）。
```

### DefaultValue

```
52428800
```

### 默认值补充说明

```
50 MB（52428800 字节）。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 本地存储目录建议权限 0700；日报与 pending 不落 DB，仅文件系统。
```

### UI暴露

```
否
```

### CLI手册暴露

```
是
```

## 注意事项

```
容量清理不得修改 GlobalConfig 中的授权状态、source_id 与盐值；目录被删后下次运行自动重建。
```
