package stranslator

import java.net.URI
import java.util.Locale

import scala.collection.mutable
import scala.util.control.Breaks._
import scala.xml.Elem
import scala.xml.factory.XMLLoader

/** A holder for a specific message and it's translations.
  *
  * @constructor Create a holder the `from` message with it's translations in `to`.
  * @param from the original message
  * @param to the translations for the message a.k. `from`
  */
case class Message(from: String, to: Map[Seq[String], String])

/** Create a translator and load translations from `source`.
  *
  * @constructor Create a new translator which can translate messages using a resource who's url is `source`.
  * @param source the url of the resource which contains translations, a resource from class path should start with `cp://`
  *               or no prefix, and a url start with other scheme(e.g. file://, http://, and etc.) will be loaded
  *               with [[java.net.URI]].
  */
case class Translator(val source:String) {

  import Translator._

  private val rSplit = "(_|-)".r
  private val rClasspath = "cp://(.*)|(?<![a-z]+://)(.*)".r
  private val rURL = "([a-zA-Z0-9]+://)(.*)".r

  @volatile
  private var caches = Map[String, Option[String]]()

  lazy val translations = {
    val messages = mutable.Buffer[Message]()
    read(load(source), messages)
    messages.toIndexedSeq
  }

  /** Create a translator using classpath resource `l10n/translator.xml`.
    */
  def this() = {
    this("cp://l10n/translator.xml")
  }

  protected def load(source: String) = {
    loader.load(source match {
      case rClasspath(s1, s2) =>
        val s = if (s1 == null) s2 else s1
        fromClasspath(s) match {
          case Some(resource) => resource
          case None => throw new IllegalArgumentException(s"""Can't load translations "$s" from class path, please check if it is exists!""")
        }
      case rURL(s) =>
        new URI(s).toURL()
      case other =>
        throw new IllegalArgumentException(s"""Bad source path: "$other"""")
    })
  }

  protected def read(root: Elem, messages: mutable.Buffer[Message]): Unit = {
    for (node <- root \ "_") {
      node.label match {
        case "include" =>
          val subSource = node.text.trim
          if (subSource != "") read(load(subSource), messages)
        case "message" =>
          var to = mutable.Map[Seq[String], String]()
          val from = cleanup((node \\ "from").text)
          for (toNode <- (node \\ "to" \ "_")) {
            to += (rSplit.split(toNode.label).toSeq -> cleanup(toNode.text))
          }
          messages += Message(from, to.toMap)
      }
    }
  }

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

  protected def cleanup(s: String) = {
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

object Translator {
  private object loader extends XMLLoader[Elem] {}

  private val defaultCL = classOf[Translator].getClassLoader

  private def fromClasspath(name: String) = {
    var cl = Thread.currentThread().getContextClassLoader
    cl = if (cl == null) defaultCL else cl
    Option(cl.getResource(name))
  }

  private def localeToSeq(locale: Locale) = {
    var seq = Seq[String]()
    if (locale.getVariant != "") seq +:= locale.getVariant
    if (locale.getCountry != "") seq +:= locale.getCountry
    if (locale.getLanguage != "") seq +:= locale.getLanguage
    seq
  }

  def apply() = new Translator()

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

