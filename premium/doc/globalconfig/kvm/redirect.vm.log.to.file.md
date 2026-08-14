
## Name

```
redirect.vm.log.to.file(重定向云主机日志到文件)
```

### Description

```
Redirect vm console log to file
```

### 含义

```
重定向云主机日志到文件
```

### Type

```
java.lang.Boolean
```

### Category

```
kvm
```

### 取值范围

```
{true, false}
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
false
```

### 默认值补充说明

```
无
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

### 资源粒度说明

```
无
```

### 背景信息

```
无
```

### UI暴露

```
不暴露
```

### CLI手册暴露

```
暴露
```

## 注意事项

```
主要用于日志分析，避免全局打开，导致日志过多
```
