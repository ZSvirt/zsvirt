
## Name

```
consent.granted.at(Telemetry 用户授权同意时间)
```

### Description

```
UTC consent timestamp when granted, or None if not granted; writable only via APIUpdateTelemetryConsent
```

### 含义

```
Telemetry 用户体验计划的授权状态与同意时间。
用户同意采集与上传时写入 UTC 时间点（ISO-8601，如 2026-07-07T07:51Z）；
未同意或已关闭授权时为 None。判断是否已授权：值不为 None。
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
None 表示未授权；已授权时为 ISO-8601 格式的 UTC 时间字符串。
```

### DefaultValue

```
None
```

### 默认值补充说明

```
系统默认未加入 Telemetry 用户体验计划。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
Telemetry V1 授权机制：用户于系统设置或首次弹窗勾选同意后，仅可通过 APIUpdateTelemetryConsent 写入；关闭授权时重置为 None。
UpdateGlobalConfigMsg 禁止修改本键（TelemetryGlobalConfigApiInterceptor，TELEMETRY.2003）。
字段 validator 与调度启停见 TelemetryConsentGlobalConfigExtensions。
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
关闭授权后再次开启须重新勾选同意；该值不因本地 Telemetry 文件目录删除而丢失。
须使用 APIUpdateTelemetryConsent 变更 consent.granted.at（Enabled 时校验 agreedToTerms=true）。
```
