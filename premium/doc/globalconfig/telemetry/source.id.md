
## Name

```
source.id(Telemetry 数据源标识)
```

### Description

```
Management node UUID written on first startup, never changed afterward
```

### 含义

```
Telemetry 数据源标识（source_id）。
首次启动时写入当前管理节点 UUID（managementNodeUuid），之后永不修改；
作为 Daily Report 的 source_id 字段上报至 Telemetry Cloud。
```

### Type

```
java.lang.String
```

### Category

```
telemetry
```

### 取值范围

```
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
应为管理节点 UUID 字符串；首次启动初始化后只读，运维不应手动修改。
```

### DefaultValue

```
None
```

### 默认值补充说明

```
首次启动时由 ZsvTelemetryGlobalConfigInitExtensionPoint 写入当前管理节点 UUID，无预设默认值。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 身份口径：source_id 在集群生命周期内保持不变，用于云端按数据源聚合与幂等去重（snapshot_date + source_id）。
```

### UI暴露

```
否
```

### CLI手册暴露

```
否
```

## 注意事项

```
升级、重启、后续启动均不修改；禁止通过 GlobalConfig 手工覆盖已有值。
```
