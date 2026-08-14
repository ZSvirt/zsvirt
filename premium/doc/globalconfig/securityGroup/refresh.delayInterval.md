
## Name

```
refresh.delayInterval(##中文名-必填##)
```

### Description

```
A delay in milliseconds. When a vm instance changes state from Unknown to Running, its security group rules will be refreshed. However, when zstack reconnects to a host, all vms on that host will change state from Unknown to Running if vms are really running on host. This may lead to security group on that host to be refreshed may times if there are lots of vm.To avoid this situation, zstack uses a delayed thread to accumulate vm in a time window of 'refresh.delayInterval'. That means, when a vm changes state from Unknown to Running, its security group will be refreshed after 'refresh.delayInterval' milliseconds.
        
```

### 含义

```
##该条目的作用是什么-必填##
```

### Type

```
java.lang.Long
```

### Category

```
securityGroup
```

### 取值范围

```
[-1, 9223372036854775807]
```

### 取值范围补充说明

```
##对取值范围的解读-如无需写：无##
```

### DefaultValue

```
1000
```

### 默认值补充说明

```
##对默认值的解读-如无需写：无##
```

### 支持的资源级配置



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
