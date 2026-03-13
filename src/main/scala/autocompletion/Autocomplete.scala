package autocompletion

/**
 * Module d'autocomplétion et de génération de texte.
 *
 * Utilise un modèle de langue (Trie de distributions de probabilité)
 * construit par `LanguageModel.learn` pour suggérer les mots suivants
 * les plus probables étant donné un contexte.
 *
 * Toutes les fonctions sont pures ; aucun état mutable.
 */
object Autocomplete:

  // ── Extraction du contexte ──────────────────────────────────────────────────

  /**
   * Extrait les `n` derniers mots d'une phrase tokenisée pour former le
   * contexte de recherche dans le modèle.
   *
   * @param tokens la phrase tokenisée
   * @param n      la taille du n-gramme utilisé lors de l'apprentissage
   * @return les n derniers tokens (ou moins si la phrase est courte)
   */
  def extractContext(tokens: List[String], n: Int): List[String] =
    tokens.takeRight(n)

  // ── Sélection des candidats ─────────────────────────────────────────────────

  /**
   * Retourne les `k` mots les plus probables étant donné une distribution,
   * triés par probabilité décroissante.
   *
   * @param dist la distribution de probabilité
   * @param k    le nombre de suggestions souhaitées
   * @return liste de paires (mot, probabilité) triée par probabilité décroissante
   */
  def topK(dist: LanguageModel.Distribution, k: Int): List[(String, Double)] =
    dist.toList
      .sortBy { case (_, prob) => -prob }
      .take(k)

  // ── Suggestion du mot suivant ───────────────────────────────────────────────

  /**
   * Retourne le mot suivant le plus probable pour une phrase incomplète donnée.
   *
   * La recherche est effectuée avec le contexte de taille `n` (n-gramme),
   * puis redescend vers des contextes plus courts si aucun n-gramme n'est trouvé
   * (stratégie de back-off récursif).
   *
   * @param model    le modèle de langue
   * @param sentence la phrase incomplète (chaîne brute)
   * @param n        la taille du n-gramme utilisé lors de l'apprentissage
   * @return Some(mot) si une suggestion existe, None sinon
   */
  def nextWord(model: LanguageModel.Model, sentence: String, n: Int = 2): Option[String] =
    val tokens = LanguageModel.tokenize(sentence)
    suggestWithBackoff(model, tokens, n).map(_._1)

  /**
   * Retourne les `k` mots suivants les plus probables pour une phrase incomplète.
   *
   * @param model    le modèle de langue
   * @param sentence la phrase incomplète (chaîne brute)
   * @param k        le nombre de suggestions (défaut : 3)
   * @param n        la taille du n-gramme utilisé lors de l'apprentissage
   * @return une liste de mots triée par probabilité décroissante
   */
  def topNextWords(
      model: LanguageModel.Model,
      sentence: String,
      k: Int = 3,
      n: Int = 2
  ): List[String] =
    val tokens = LanguageModel.tokenize(sentence)
    suggestTopKWithBackoff(model, tokens, k, n).map(_._1)

  // ── Back-off récursif ───────────────────────────────────────────────────────

  /**
   * Cherche la meilleure suggestion en réduisant progressivement la taille
   * du contexte (back-off) si le contexte courant n'est pas connu du modèle.
   *
   * @param model  le modèle de langue
   * @param tokens les tokens de la phrase
   * @param n      taille du contexte à tenter (décrémenté récursivement)
   * @return Some((mot, probabilité)) ou None si aucune suggestion n'est possible
   */
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

  /**
   * Même logique de back-off, mais retourne les `k` meilleures suggestions.
   */
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

  // ── Génération automatique de texte ────────────────────────────────────────

  /**
   * Génère automatiquement un texte de `maxWords` mots à partir d'une amorce.
   *
   * À chaque étape, le mot suivant le plus probable est ajouté à la phrase.
   * La génération s'arrête si aucune suggestion n'est disponible ou si
   * `maxWords` mots ont été générés.
   *
   * @param model    le modèle de langue
   * @param seed     la phrase d'amorçage
   * @param maxWords le nombre maximum de mots à générer
   * @param n        la taille du n-gramme
   * @return le texte généré (amorce + mots générés)
   */
  def generateText(
      model: LanguageModel.Model,
      seed: String,
      maxWords: Int,
      n: Int = 2
  ): String =
    val seedTokens = LanguageModel.tokenize(seed)

    /** Accumule récursivement les mots générés. */
    def loop(currentTokens: List[String], remaining: Int): List[String] =
      if remaining <= 0 then currentTokens
      else
        suggestWithBackoff(model, currentTokens, n) match
          case None          => currentTokens
          case Some((w, _))  => loop(currentTokens :+ w, remaining - 1)

    loop(seedTokens, maxWords).mkString(" ")
