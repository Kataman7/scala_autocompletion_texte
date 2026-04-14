package autocompletion

object Main:

  private def readTextFile(path: String): String =
    val source = scala.io.Source.fromFile(path)
    try source.mkString
    finally source.close()

  @main def run(args: String*): Unit =
    val text  = readTextFile("text.txt")
    val n     = 3
    val model = LanguageModel.learn(text, n)

    val tests: List[String] =
      if args.nonEmpty then args.toList
      else LanguageModel.topNWords(text, n = 4)

    val seed: String = tests.headOption.getOrElse("le")

    println("=== Suggestions de mots suivants ===")
    tests.foreach { sentence =>
      val top3 = Autocomplete.topNextWords(model, sentence, k = 3, n = n)
      println(s"  '$sentence'  →  ${top3.mkString(", ")}")
    }

    println(s"\n=== Génération automatique (seed: '$seed', maxWords=200) ===")
    val generated = Autocomplete.generateText(model, seed, maxWords = 200, n = n)
    println(s"  $generated")
