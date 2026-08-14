
## Name

```
deletionPolicy(##中文名-必填##)
```

### Description

```
the behavior of deleting a vm, options are [Direct, Delay, Never]. Direct: delete the vm from database and directly delete its root volume; Delay: change the vm's state to Destroyed in database; after the period controlled by 'expungePeriod' passes, delete the vm from database and delete it's root volume; Never: delete the vm from database but never delete its root volume
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
vm
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
