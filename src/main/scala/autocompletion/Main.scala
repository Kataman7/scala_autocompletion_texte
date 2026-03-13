package autocompletion

/**
 * Point d'entrée principal pour démontrer les fonctionnalités.
 * Apprend un texte depuis un fichier, puis affiche des suggestions et un texte généré.
 */
object Main:

  /** Lit le contenu du fichier text.txt (situé à la racine du projet). */
  private def readTextFile(path: String): String =
    val source = scala.io.Source.fromFile(path)
    try source.mkString
    finally source.close()

  @main def run(): Unit =
    val text   = readTextFile("text.txt")
    val n      = 2
    val model  = LanguageModel.learn(text, n)

    // Mots-clés extraits automatiquement du texte (les 4 plus fréquents)
    val tests = LanguageModel.topNWords(text, n = 4)

    println("=== Suggestions de mots suivants ===")
    tests.foreach { sentence =>
      val top3 = Autocomplete.topNextWords(model, sentence, k = 3, n = n)
      println(s"  '$sentence'  →  ${top3.mkString(", ")}")
    }

    println("\n=== Génération automatique de texte ===")
    val seed      = tests.headOption.getOrElse("le")
    val generated = Autocomplete.generateText(model, seed, maxWords = 15, n = n)
    println(s"  $generated")
