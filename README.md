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
implicit val context = SimpleTranslatorContext(translator, Seq(locale))

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
1. the line feed(\n) after the beginning and before the ending tag of `from`, `locale`s  in `to` tag(i.e. `<zh_CN>` and `</zh_CN>`), will be ignored.
2. you can break one line into multiple lines by puting an `\ ` to the end of the line.
For example:
```xml
<translator>
    <from>
        Hello, \
        Jack! I'm waiting \
        for you!
    </from>
    <to>
        <zh>捷克，你来了。\
        我已经等你好久了！
        </zh>
    </to>
</translator>
```
The code above is equal to:
```xml
<translator>
    <from>Hello, Jack! I'm waiting for you!</from>
    <to><zh>捷克，你来了。我已经等你好久了！</zh></to>
</translator>
```
3. The default location to load translations from is `l10n/translator.xml`, i.e.
```scala
val translator = Translator()
```
Is equal to:
```scala
val translator = Translator("cp://l10n/translator.xml")
```
4. You can `include` another xml for translations by `<include>`tag :
```xml
<translator>
    <include>http://localhost:9000/some/app/l10n/translations.xml</include>
</translator>
```
> The `<include>` tag **does not** support **relative path** 

### About the `stranslator.Translator`
It's the core API for translating. You can initialize it with an URL, a class path resource with "cp://" (no prefix is identical to it too),
or an external resource on a **Server**(i.e. http://localhost:9090/l10n/my_app.xml).

### Enjoy it! :tea:
