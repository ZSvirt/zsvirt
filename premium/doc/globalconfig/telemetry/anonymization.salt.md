
## Name

```
anonymization.salt(Telemetry 匿名化盐值)
```

### Description

```
Random salt for SHA-256 anonymization, generated on first startup, never changed afterward
```

### 含义

```
Host ID / MN ID 匿名化所用的固定盐值。
首次启动时写入代码内预定义的固定字符串（非随机 UUID），之后永不修改；
用于 SHA256(原始设备 UUID + salt) 生成 64 位小写十六进制匿名 ID。
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
无
```

### DefaultValue

```
None
```

### 默认值补充说明

```
首次启动时由 ZsvTelemetryGlobalConfigInitExtensionPoint 写入固定盐值，无预设默认值。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 隐私要求：日报与 API 中仅出现匿名化后的 host_ids、mn_ids，禁止泄露原始 UUID。
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
盐值一旦写入不可变更，否则同一物理设备的历史匿名 ID 将无法对齐；禁止通过 GlobalConfig 手工覆盖已有值。
```
