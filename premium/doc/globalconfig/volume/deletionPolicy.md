
## Name

```
deletionPolicy(##中文名-必填##)
```

### Description

```
the behavior of deleting a data volume, options are [Direct, Delay, Never]. Direct: delete the volume from database and primary storage; Delay: change the volume's state to Deleted in database; after the period controlled by 'expungePeriod' passes, delete the volume from database and primary storage; Never: delete the volume from database but from the primary storage
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
volume
```

### 取值范围

```
{Direct, Delay, Never}
```

### 取值范围补充说明

```
##对取值范围的解读-如无需写：无##
```

### DefaultValue

```
Delay
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
