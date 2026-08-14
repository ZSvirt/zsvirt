
## Name

```
reservedMemory(##中文名-必填##)
```

### Description

```
The memory capacity reserved on all KVM hosts. ZStack KVM agent is a python web server that needs some memory capacity to run. this value reserves a portion of memory for the agent as well as other host applications. The value can be overriden by system tag on individual host, cluster and zone level
```

### 含义

```
##该条目的作用是什么-必填##
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
[-9223372036854775808, 9223372036854775807]
```

### 取值范围补充说明

```
##对取值范围的解读-如无需写：无##
```

### DefaultValue

```
0
```

### 默认值补充说明

```
##对默认值的解读-如无需写：无##
```

### 支持的资源级配置

||
|---|
|org.zstack.header.host.HostVO
|org.zstack.header.cluster.ClusterVO
|org.zstack.header.zone.ZoneVO

### 资源粒度说明

```
##该条目支持的资源粒度-如无需写：无##
```

### 背景信息

```
##触发该条目增删改的背景-如无需写：无##
```

### UI暴露

```
##该条目是否需UI暴露？-必填##
```

### CLI手册暴露

```
##该条目是否需CLI手册暴露？-必填##
```

## 注意事项

```
##该条目有哪些注意事项-如无需写：无##
```
