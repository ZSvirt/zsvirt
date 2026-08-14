
## Name

```
host.ksm(物理机KSM)
```

### Description

```
enable host ksm
```

### 含义

```
启用物理机KSM
```

### Type

```
java.lang.String
```

### Category

```
kvm
```

### 取值范围

```
{true, false, none}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
none
```

### 默认值补充说明

```
默认值含义为不修改系统当前配置
```

### 支持的资源级配置

||
|---|
|org.zstack.header.host.HostVO

### 资源粒度说明

```
支持物理机级别配置
```

### 背景信息

```
无
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
这个配置在物理机上默认启用，如果禁用，会导致物理机上的KSM功能失效
```
