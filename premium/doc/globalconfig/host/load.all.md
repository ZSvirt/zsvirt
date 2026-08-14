
## Name

```
load.all(##中文名-必填##)
```

### Description

```
A boolean value indicating whether management server connects all hosts during boot. Management server will connect hosts when booting up or other management node die. When total number of hosts is small, for example several hundreds, setting this to true can significantly reduce management server boot time; however if there are a large number of hosts, for example tens of thousands, setting this to true will make management server very busy; instead, setting it to false and use another global config 'load.parallelismDegree'
```

### 含义

```
##该条目的作用是什么-必填##
```

### Type

```
java.lang.Boolean
```

### Category

```
host
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
true
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
