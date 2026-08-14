
## Name

```
vm.domain.password.strength.check.config(虚拟机域密码强度检查配置)
```

### Description

```
vm domain password strength check config, the default value is 01111,8-18, which means [(checkPasswordStrength,lowercase,uppercase,number,specialWords),minimum-maximum], and 0 means false, 1 means true
```

### 含义

```
用于配置虚拟机域密码的强度检查
```

### Type

```
java.lang.String
```

### Category

```
mevoco
```

### 取值范围

```
notNull
```

### 取值范围补充说明

```
无
```

### DefaultValue

```
01111,8-18
```

### 默认值补充说明

```
默认关闭强度检查。若启用（将首位设置为1），则密码长度需在8-18位，且必须包含小写字母、大写字母、数字和特殊字符。
```

### 支持的资源级配置



### 资源粒度说明

```
无
```

### 背景信息

```
用户在设置虚拟机域密码时，可以通过该配置项来启用或禁用密码强度检查，并定义密码的复杂性要求（如是否包含小写字母、大写字母、数字和特殊字符）以及密码的长度范围。
```

### UI暴露

```
是
```

### CLI手册暴露

```
是
```

## 注意事项

```
无
```
