
### 自定义混淆字典格式说明

如果你希望使用自定义的混淆规则，那么可尝试创建混淆字典文件，格式如下：

```json
{
  "fields": [
    "a#public#Ljava/lang/String;",
    "b#private#Ljava/lang/String;",
    "c#protected#Ljava/lang/String;",
    "d#private#Ljava/lang/String;",
    "e#public#Ljava/lang/String;",
    "f#protected#Ljava/lang/String;",
    "g#public#Ljava/lang/String;",
    "h#private#Ljava/lang/String;",
    "i#protected#Ljava/lang/String;",
    "j#private#Ljava/lang/String;",
    "k#private#Ljava/lang/String;",
    "l#private#Ljava/lang/String;"
  ],
  "methods": [
    "com/example/util/LogUtil#v#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#v#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
    "com/example/util/LogUtil#d#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#d#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
    "com/example/util/LogUtil#i#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#i#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V"
  ],
  "codeBlock": [
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#x#()V",
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#y#(I)V",
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#z#(J)V"
  ],
  "whitelist": [
    "com/example/SampleActivity"
  ]
}
```