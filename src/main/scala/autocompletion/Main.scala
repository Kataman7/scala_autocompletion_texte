package autocompletion

/**
 * Point d'entrée principal pour démontrer les fonctionnalités.
 * Apprend un texte d'exemple, puis affiche des suggestions et un texte généré.
 */
object Main:

  val sampleText: String =
    """
    le chat mange la souris. la souris court vite.
    le chien court après le chat. le chat court vite aussi.
    la souris mange le fromage. le fromage est bon.
    le chien mange le fromage aussi. le chat est content.
    la souris est rapide. le chat est rapide aussi.
    """

  @main def run(): Unit =
    val n     = 2
    val model = LanguageModel.learn(sampleText, n)

    val tests = List(
      "le chat",
      "la souris",
      "le chien",
      "le fromage"
    )

    println("=== Suggestions de mots suivants ===")
    tests.foreach { sentence =>
      val top3 = Autocomplete.topNextWords(model, sentence, k = 3, n = n)
      println(s"  '$sentence'  →  ${top3.mkString(", ")}")
    }

    println("\n=== Génération automatique de texte ===")
    val generated = Autocomplete.generateText(model, "le chat", maxWords = 15, n = n)
    println(s"  $generated")
