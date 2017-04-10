# Stranslator
A most lightweight library for translating you application to the local languages.

### The XML format for translation file
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<translator>
    <message>
        <from>This is the original message!</from>
        <to>
            <!-- the translation for chinese -->
            <zh>这是原始文本！</zh>
            <!-- the translation for traditional chinese(Hong Kong) -->
            <zh_HK>這是原始文本！</zh_HK>
        </to>
    </message>
</translator>
```
### Usage
1. Add to your project
```sbtshell
libraryDependencies += "com.lingcreative" %% "stranslator" % "1.0.1"
```

2. Code sample in your project
```scala
val translator = Translator()
val locale = new Locale("zh", "CN")
implicit val context = SimpleTranslatorContext(translator, locale)

val welcome = ${"Hello, Sauntor! Welcome to China!"}
```
If you create a translation file which located in `l10n/translator.xml`(within class path) with the flowing content:
```xml
<?xml version="1.0" encoding="UTF-8" ?>
<translator>
  <message>
    <from>Hello, Sauntor! Welcome to China!</from>
    <to>
      <zh_CN>适然，你好！欢迎来到中国！</zh_CN>
    </to>
  </message>
</translator>
```
The `welcome` would be:
`适然，你好！欢迎来到中国！`
###### Notice
1. the line feed flowing the begin tag of `from`, `locale`s in `to`(i.e. <zh_CN>), will be ignored.
2. you can break one line into multiple line by put an `\ ` to the end of the line.
For example:
```xml
<from>
    Hello, \
    Jack! I'm waiting \
    for you!
</from>
```
The code above is equal to:
```xml
<from>Hello, Jack! I'm waiting for you!</from>
```