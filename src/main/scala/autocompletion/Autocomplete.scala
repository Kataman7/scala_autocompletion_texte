package autocompletion

/**
 * Suggestions de mots et génération de texte.
 * Utilise le modèle appris par LanguageModel pour prédire le mot suivant.
 */
object Autocomplete:

  /** Retourne les n derniers tokens pour former le contexte de recherche. */
  def extractContext(tokens: List[String], n: Int): List[String] =
    tokens.takeRight(n)

  /** Retourne les k mots les plus probables d'une distribution, triés par proba décroissante. */
  def topK(dist: LanguageModel.Distribution, k: Int): List[(String, Double)] =
    dist.toList
      .sortBy { case (_, prob) => -prob }
      .take(k)

  /**
   * Retourne le mot suivant le plus probable pour une phrase incomplète.
   * Utilise le back-off : si le contexte long est inconnu, essaie des contextes plus courts.
   */
  def nextWord(model: LanguageModel.Model, sentence: String, n: Int = 2): Option[String] =
    val tokens = LanguageModel.tokenize(sentence)
    suggestWithBackoff(model, tokens, n).map(_._1)

  /**
   * Retourne les k mots suivants les plus probables pour une phrase incomplète.
   */
  def topNextWords(
      model: LanguageModel.Model,
      sentence: String,
      k: Int = 3,
      n: Int = 2
  ): List[String] =
    val tokens = LanguageModel.tokenize(sentence)
    suggestTopKWithBackoff(model, tokens, k, n).map(_._1)

  private def suggestWithBackoff(
      model: LanguageModel.Model,
      tokens: List[String],
      n: Int
  ): Option[(String, Double)] =
    if n <= 0 then None
    else
      val context = extractContext(tokens, n)
      LanguageModel.distributionFor(model, context) match
        case Some(dist) => topK(dist, 1).headOption
        case None       => suggestWithBackoff(model, tokens, n - 1)

  private def suggestTopKWithBackoff(
      model: LanguageModel.Model,
      tokens: List[String],
      k: Int,
      n: Int
  ): List[(String, Double)] =
    if n <= 0 then Nil
    else
      val context = extractContext(tokens, n)
      LanguageModel.distributionFor(model, context) match
        case Some(dist) => topK(dist, k)
        case None       => suggestTopKWithBackoff(model, tokens, k, n - 1)

  /**
   * Génère un texte de maxWords mots à partir d'une amorce.
   * À chaque étape, on ajoute le mot le plus probable.
   */
  def generateText(
      model: LanguageModel.Model,
      seed: String,
      maxWords: Int,
      n: Int = 2
  ): String =
    val seedTokens = LanguageModel.tokenize(seed)

    def loop(currentTokens: List[String], remaining: Int): List[String] =
      if remaining <= 0 then currentTokens
      else
        suggestWithBackoff(model, currentTokens, n) match
          case None          => currentTokens
          case Some((w, _))  => loop(currentTokens :+ w, remaining - 1)

    loop(seedTokens, maxWords).mkString(" ")
