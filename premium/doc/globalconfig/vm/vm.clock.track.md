
## Name

```
vm.clock.track(云主机时钟同步目标)
```

### Description

```
set vm clock track guest or host
```

### 含义

```
设置云主机时钟同步的目标，设置为host时，含义时同步物理机的时钟
设置为guest时含义为不同步任何时钟，以云主机自身为准
```

### Type

```
java.lang.String
```

### Category

```
vm
```

### 取值范围

```
{guest, host}
```

### 取值范围补充说明

```
无补充说明解释见含义词条
```

### DefaultValue

```
host
```

### 默认值补充说明

```
默认云主机启动/重启/克隆操作后会和物理机器做时钟同步
linux云主机仅在启动时同步
windows云主机则会在启动时和运行中同步
```

### 支持的资源级配置

||
|---|
|org.zstack.header.vm.VmInstanceVO

### 资源粒度说明

```
支持云主机粒度设置时钟同步目标
```

### 背景信息

```
ZSTAC-43023
```

### UI暴露

```
UI暴露
```

### CLI手册暴露

```
CLI手册暴露
```

## 注意事项

```
注意当前该配置仅保证对windows操作系统的云主机有实际作用
对Linux操作系统的支持并未经过研发和测试，实际上不起作用
```
