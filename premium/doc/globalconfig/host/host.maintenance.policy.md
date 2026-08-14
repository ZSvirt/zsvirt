
## Name

```
host.maintenance.policy(##中文名-必填##)
```

### Description

```
the behavior of maintain a host, options are [JustMigrate, StopVmOnMigrationFailure]. JustMigrate: just try to migrate all vm on the host, if there is any migration failure, host will fail to change to maintenance state; StopVmOnMigrationFailure: when migrate vm from current host fail, try to stop vm on the host.
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
host
```

### 取值范围

```
{JustMigrate, StopVmOnMigrationFailure}
```

### 取值范围补充说明

```
##对取值范围的解读-如无需写：无##
```

### DefaultValue

```
StopVmOnMigrationFailure
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
