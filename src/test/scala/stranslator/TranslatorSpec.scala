package stranslator

import java.util.Locale

import stranslator.Translator.$

/**
  * Translator test spec.
  */
class TranslatorSpec extends org.specs2.mutable.Specification {
  "Translator" title

  "Translations Loading" should {

    "load translations in the resource directed by <include> command" in {
      val translator = Translator("load-test.xml")
      val messageInTheIncluded = "This is in l10n/load-test-included.xml"
      translator.tr(messageInTheIncluded, Seq()) must_== None
      translator.tr(messageInTheIncluded, Seq("zh", "CN")) must_== Some("这是从l10n/load-test-included.xml里加载的")
    }

    "translate message using the implicit context" in {
      implicit val translatorContext = SimpleTranslatorContext(Translator(), Seq(new Locale("zh", "CN", "TW")))
      ${"""Hello, Mr. "Sauntor"! Welcome!"""} must_== "您好，适然先生！ 欢迎来到台湾！"
    }

  }

}
