package stranslator

import java.io.{File, FileDescriptor, InputStream, Reader}
import java.net.URL
import java.util.Locale

import scala.collection.mutable
import scala.util.control.Breaks._
import scala.xml.factory.XMLLoader
import scala.xml.{Elem, InputSource}

/** The core API for `Stranslator`, which declares how an translator implementation should behave.
  */
trait Translator {

  /** translate the original message `from` to the target dialect `to`.
    * @param from the original message
    * @param to the target dialect, such as `Seq("zh", "CN")`
    * @return the translated message
    */
  def tr(from: String, to: Seq[String]): Option[String]

  /** translate the original message `from` to the target [[java.util.Locale Locale]] `to`.
    *
    * @param from the original message
    * @param to the target dialect, and it will be turned to a [[scala.collection.Seq Seq&#91;String&#93;]] such as `Seq("zh", "CN")`. But, it's not forced
    *           to follow this rule, you can implement your own.
    * @return
    */
  def tr(from: String, to: Locale): Option[String]
}

object Translator {
  /** Build a translator from a class path xml file: `l10n/translator.xml`.
    *
    * @return the translator
    */
  def apply() = new XmlTranslator()

  /** Build a translator from a xml file who's url is `source`. The url of the resource which contains translations,
    * a resource from class path should start with `cp://` or no prefix, and a url start with other scheme(e.g. file://,
    * http://, and etc.) will be loaded with [[java.net.URL]].
    *
    * @return the translator
    */
  def apply(source: String) = new XmlTranslator(source)
  def apply(source: Elem) = new XmlTranslator(() => source)
  def apply(source: InputStream) = new XmlTranslator(() => Sources.load(source))
  def apply(source: InputSource) = new XmlTranslator(() => Sources.load(source))
  def apply(source: Reader) = new XmlTranslator(() => Sources.load(source))
  def apply(source: URL) = new XmlTranslator(() => Sources.load(source))
  def apply(source: File) = new XmlTranslator(() => Sources.loadFile(source))
  def apply(source: FileDescriptor) = new XmlTranslator(() => Sources.loadFile(source))

  protected trait TranslatorSupport {
    def apply(message: =>String)(implicit context: TranslatorContext): String = {
      var translated: Option[String] = None
      breakable {
        for (locale <- context.locales) {
          translated = context.translator.tr(message, localeToSeq(locale))
          if (translated.isDefined) break
        }
      }
      translated match {
        case Some(str) => str
        case None => ""
      }
    }
  }

  implicit def localeToSeq(locale: Locale) = {
    var seq = Seq[String]()
    if (locale.getVariant != "") seq +:= locale.getVariant
    if (locale.getCountry != "") seq +:= locale.getCountry
    if (locale.getLanguage != "") seq +:= locale.getLanguage
    seq
  }

  /** An helper make translator easy to use.
    *
    * For example: {{{
    *   val translator = Translator()
    *   val locale = new Locale("zh", "CN")
    *   implicit val context = SimpleTranslatorContext(translator, locale)
    *
    *   val welcome = ${"Hello, Sauntor! Welcome to China!"}
    * }}}
    * If you create a translation file which located in `l10n/translator.xml`(within class path) with the flowing content: {{{
    *   <translator>
    *     <message>
    *       <from>Hello, Sauntor! Welcome to China!</from>
    *       <to>
    *         <zh_CN>适然，你好！欢迎来到中国！</zh_CN>
    *       </to>
    *   </translator>
    * }}}
    * The `welcome` would be: {{{
    *   适然，你好！欢迎来到中国！
    * }}}
    *
    */
  object $ extends TranslatorSupport

  object ! extends TranslatorSupport

  implicit class StringInterpolator(private val sc: StringContext) {
    def $(args: Any*)(implicit context: TranslatorContext): String = {
      sc.checkLengths(args)
      if (args.size > 1) throw new IllegalArgumentException("The message to be translated can't take any place holder!")
      Translator.$(sc.parts(0))
    }
  }
}

/** A holder for a specific message and it's translations.
  *
  * @constructor Create a holder the `from` message with it's translations in `to`.
  * @param from the original message
  * @param to the translations for the message a.k. `from`
  */
case class Message(from: String, to: Map[Seq[String], String])

/** A translator loading it's translations from an xml file. The format of the translation xml file is: {{{
  * <?xml version="1.0" encoding="UTF-8" ?>
  * <translator>
  * <message>
  *   <from>This is the original message!</from>
  *     <to>
  *         <!-- the translation for chinese -->
  *         <zh>这是原始文本！</zh>
  *         <!-- the translation for traditional chinese(Hong Kong) -->
  *         <zh_HK>這是原始文本！</zh_HK>
  *     </to>
  *   </message>
  * </translator>
  * }}}
  *
  * @constructor Create a new translator which can translate messages using a [[scala.xml.Elem]] as the `source`.
  * @param source the factory to build an [[scala.xml.Elem]]
  */
case class XmlTranslator(val source: () => Elem) extends Translator {

  import XmlTranslator._

  @volatile
  private var caches = Map[String, Option[String]]()

  lazy val translations = {
    val messages = mutable.Buffer[Message]()
    read(source(), messages)
    messages.toIndexedSeq
  }

  /** Create a new translator which can translate messages using a resource who's url is `source`.
    * @param source the url of the resource which contains translations, a resource from class path should start with `cp://`
    *               or no prefix, and a url start with other scheme(e.g. file://, http://, and etc.) will be loaded
    *               with [[java.net.URL]].
    */
  def this(source: String) = {
    this(() => Sources.loadSource(source))
  }

  /** Create a translator using classpath resource `l10n/translator.xml`.
    */
  def this() = {
    this("cp://l10n/translator.xml")
  }

  protected def read(root: Elem, messages: mutable.Buffer[Message]): Unit = {
    for (node <- root \ "_") {
      node.label match {
        case "include" =>
          val subSource = node.text.trim
          if (subSource != "") read(Sources.loadSource(subSource), messages)
        case "message" =>
          var to = mutable.Map[Seq[String], String]()
          val from = filter((node \\ "from").text)
          for (toNode <- (node \\ "to" \ "_")) {
            to += (rSplit.split(toNode.label).toSeq -> filter(toNode.text))
          }
          messages += Message(from, to.toMap)
      }
    }
  }

  def tr(from: String, to: Locale): Option[String] = tr(from, Translator.localeToSeq(to))

  /** Translate the message `from` to the target dialect `to`, and the `to` is in general form.
    * @param from the original message to translate
    * @param to the target dialect, may build from a [[java.util.Locale]]
    * @return the translated message for dialect `to`, or None if no translation suit for it
    */
  def tr(from: String, to: Seq[String]): Option[String] = {
    if (caches.contains(from + to.fold("")(_ + "$$$" + _))) {
      return caches(from)
    }

    var messageMatched: Option[String] = None

    def tryMatch(to: Seq[String], tos: Map[Seq[String], String]) = {
      for (toM <- tos) {
        if (to == toM._1) {
          messageMatched = Some(toM._2)
          break
        }
      }
    }

    breakable {
      for (message <- translations if (message.from == from)) {
        for (n <- (1 to to.size).reverse) {
          tryMatch(to.take(n), message.to)
        }
      }
    }

    caches += (from -> messageMatched)

    messageMatched
  }

  protected def filter(s: String): String = {
    val builder = StringBuilder.newBuilder
    var bs = -2
    var begin = true
    var have = false
    var i = -1
    for (c <- s) {
      i += 1
      c match {
        case '\\' =>
          if (begin) { begin = false; bs = -2 } //skip
          else bs = i
        case '\n' =>
          if (bs + 1 == i) { bs = -2} //skip
          else if(i == 0) {} //skip
          else if (!have) builder append c
          else builder append c
          begin = true
        case ' '|'\t' =>
          if (begin) { have = false } //skip
          else {
            begin = false
            have = true
            builder append c
          }
        case _ =>
          begin = false
          have = true
          if (bs + 1 == i)
            builder append '\\'
          builder append c
      }
    }
    if (builder.last == '\n') builder deleteCharAt builder.length - 1
    builder.toString()
  }
}


object XmlTranslator {
  private val rSplit = "(_|-)".r
}

/** The context which provides a translator and the current candidate locales.
  */
trait TranslatorContext {
  def translator: Translator
  def locales: Seq[Locale]
}

case class SimpleTranslatorContext(private val _translator: Translator, private val _locale: Seq[Locale]) extends TranslatorContext {
  override def translator: Translator = _translator
  override def locales: Seq[Locale] = _locale
}

private[stranslator] object Sources extends XMLLoader[Elem] {

  private val rClasspath = "cp://(.*)|(?<![a-z]+://)(.*)".r
  private val rURL = "([a-zA-Z0-9]+://)(.*)".r

  private val defaultCL = classOf[XmlTranslator].getClassLoader
  def fromClasspath(name: String) = {
    var cl = Thread.currentThread().getContextClassLoader
    cl = if (cl == null) defaultCL else cl
    Option(cl.getResource(name))
  }
  def loadSource(source: String) = {
    Sources.load(source match {
      case rClasspath(s1, s2) =>
        val s = if (s1 == null) s2 else s1
        fromClasspath(s) match {
          case Some(resource) => resource
          case None => throw new IllegalArgumentException(s"""Can't load translations "$s" from class path, please check if it is exists!""")
        }
      case rURL(s) =>
        new URL(s)
      case other =>
        throw new IllegalArgumentException(s"""Bad source path: "$other"""")
    })
  }
}
