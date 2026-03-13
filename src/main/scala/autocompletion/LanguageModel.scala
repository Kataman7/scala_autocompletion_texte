package autocompletion

/**
 * Module d'apprentissage de la distribution de probabilité d'un texte.
 *
 * Approche n-grammes :
 *   - un n-gramme est une séquence de n mots consécutifs utilisée comme contexte.
 *   - pour chaque n-gramme observé, on compte combien de fois chaque mot suivant apparaît.
 *   - on normalise ensuite les comptages en probabilités.
 *
 * Le résultat est stocké dans un Trie dont :
 *   - la clé est la jointure du n-gramme par un espace (ex. "le chat")
 *   - la valeur est une Map[String, Double] représentant la distribution de
 *     probabilité du mot suivant.
 *
 * Tous les traitements sont purs et récursifs ; aucun état mutable.
 */
object LanguageModel:

  /** Alias de type pour la distribution de probabilité d'un mot suivant. */
  type Distribution = Map[String, Double]

  /** Alias de type pour le modèle stocké dans un Trie. */
  type Model = Trie[Distribution]

  // ── Normalisation du texte ──────────────────────────────────────────────────

  /**
   * Normalise un caractère :
   *   - majuscules ASCII → minuscules
   *   - majuscules accentuées françaises → minuscules correspondantes
   *   - apostrophes typographiques ' ' (U+2018/U+2019) → apostrophe droite '
   *   - ligature œ/Œ → oe
   * Retourne '\u0000' pour tout caractère à ignorer (séparateur).
   *
   * On n'utilise que des listes de cas explicites : aucune regex, aucune lib externe.
   */
  private def normalizeChar(c: Char): Char =
    c match
      // apostrophes typographiques → apostrophe droite
      case '\u2018' | '\u2019' => '\''
      // ligatures
      case 'œ' | 'Œ'          => 'o'   // "œ" devient "o" (le "e" sera perdu, acceptable)
      // majuscules ASCII
      case _ if c >= 'A' && c <= 'Z' => (c + 32).toChar
      // majuscules accentuées
      case 'À' | 'Â'          => 'à'
      case 'Ä'                => 'ä'
      case 'É'                => 'é'
      case 'È'                => 'è'
      case 'Ê'                => 'ê'
      case 'Ë'                => 'ë'
      case 'Î'                => 'î'
      case 'Ï'                => 'ï'
      case 'Ô'                => 'ô'
      case 'Ù'                => 'ù'
      case 'Û'                => 'û'
      case 'Ü'                => 'ü'
      case 'Ç'                => 'ç'
      case _                  => c

  /**
   * Indique si un caractère normalisé fait partie d'un mot.
   * Lettres minuscules françaises, tiret et apostrophe droite sont acceptés.
   * Implémenté par recherche récursive dans une liste de caractères valides.
   */
  private def isValidChar(c: Char): Boolean =
    def check(cs: List[Char]): Boolean = cs match
      case Nil       => false
      case h :: rest => if h == c then true else check(rest)
    check(List(
      'a','b','c','d','e','f','g','h','i','j','k','l','m',
      'n','o','p','q','r','s','t','u','v','w','x','y','z',
      'à','â','ä','é','è','ê','ë','î','ï','ô','ù','û','ü','ç',
      '\'','-'
    ))

  /**
   * Tokenise un texte en liste de mots en minuscules.
   * Seuls les caractères valides (lettres françaises, tiret, apostrophe) sont conservés.
   * Les apostrophes typographiques sont normalisées en apostrophe droite.
   * Implémentation récursive pure, sans regex ni bibliothèque externe.
   *
   * @param text le texte brut
   * @return la liste ordonnée des tokens
   */
  def tokenize(text: String): List[String] =

    /** Accumule les caractères d'un mot en cours, avance récursivement sur `chars`. */
    def loop(chars: List[Char], currentWord: List[Char], acc: List[String]): List[String] =
      chars match
        case Nil =>
          val words = if currentWord.isEmpty then acc else currentWord.reverse.mkString :: acc
          words.reverse
        case c :: rest =>
          val nc = normalizeChar(c)
          if isValidChar(nc) then
            loop(rest, nc :: currentWord, acc)
          else
            val newAcc = if currentWord.isEmpty then acc else currentWord.reverse.mkString :: acc
            loop(rest, Nil, newAcc)

    loop(text.toList, Nil, Nil)

  // ── Construction des n-grammes ──────────────────────────────────────────────

  /**
   * Extrait tous les n-grammes d'une liste de tokens et le mot qui les suit.
   * Retourne une liste de paires (contexte : List[String], motSuivant : String).
   *
   * @param tokens les tokens du texte
   * @param n      la taille du n-gramme (1 = unigramme, 2 = bigramme, …)
   * @return liste de (contexte, motSuivant)
   */
  def extractNGrams(tokens: List[String], n: Int): List[(List[String], String)] =
    require(n >= 1, "n doit être >= 1")
    tokens match
      case Nil => Nil
      case _ =>
        val window = tokens.take(n + 1)
        if window.length < n + 1 then Nil
        else
          val context = window.init
          val next    = window.last
          (context, next) :: extractNGrams(tokens.tail, n)

  // ── Comptage des occurrences ────────────────────────────────────────────────

  /**
   * Construit une Map[contexte, Map[motSuivant, comptage]] à partir
   * des n-grammes extraits.
   *
   * @param ngrams liste de (contexte, motSuivant) produite par `extractNGrams`
   * @return table de comptages
   */
  def countNGrams(
      ngrams: List[(List[String], String)]
  ): Map[String, Map[String, Int]] =
    ngrams.foldLeft(Map.empty[String, Map[String, Int]]) {
      case (acc, (context, next)) =>
        val key     = context.mkString(" ")
        val current = acc.getOrElse(key, Map.empty[String, Int])
        val updated = current.updated(next, current.getOrElse(next, 0) + 1)
        acc.updated(key, updated)
    }

  // ── Normalisation en probabilités ──────────────────────────────────────────

  /**
   * Normalise une table de comptages en distributions de probabilité.
   * Chaque distribution somme à 1.0.
   *
   * @param counts table de comptages issue de `countNGrams`
   * @return table de distributions de probabilité
   */
  def normalize(counts: Map[String, Map[String, Int]]): Map[String, Distribution] =
    counts.map { case (context, nextCounts) =>
      val total = nextCounts.values.sum.toDouble
      val dist  = nextCounts.map { case (word, c) => word -> c / total }
      context -> dist
    }

  // ── Construction du modèle (Trie) ──────────────────────────────────────────

  /**
   * Insère toutes les entrées d'une Map dans un Trie.
   * Opération récursive et pure.
   *
   * @param entries liste des paires (clé, valeur) à insérer
   * @param trie    trie accumulateur
   * @return le trie résultant
   */
  private def insertAll(
      entries: List[(String, Distribution)],
      trie: Model
  ): Model =
    entries match
      case Nil                  => trie
      case (key, dist) :: rest  => insertAll(rest, Trie.insert(trie, key, dist))

  /**
   * Apprend la distribution de probabilité d'un texte et la stocke dans un Trie.
   *
   * Le modèle intègre tous les n-grammes de taille 1 jusqu'à n inclus,
   * ce qui permet au back-off de trouver toujours une distribution même
   * quand le contexte long est inconnu.
   *
   * @param text le texte d'apprentissage
   * @param n    la taille maximale du n-gramme (défaut : 2)
   * @return le modèle de langue sous forme de Trie
   */
  def learn(text: String, n: Int = 2): Model =
    val tokens = tokenize(text)

    /** Construit le trie en ajoutant récursivement les n-grammes de taille k à 1. */
    def learnAllOrders(k: Int, trie: Model): Model =
      if k <= 0 then trie
      else
        val ngrams        = extractNGrams(tokens, k)
        val counts        = countNGrams(ngrams)
        val distributions = normalize(counts)
        learnAllOrders(k - 1, insertAll(distributions.toList, trie))

    learnAllOrders(n, Trie.empty[Distribution])

  // ── Mots les plus fréquents ────────────────────────────────────────────────

  /**
   * Compte le nombre d'occurrences de chaque token dans une liste de tokens.
   * Implémentation récursive pure, sans structure mutable.
   *
   * @param tokens liste de tokens
   * @param acc    accumulateur de comptages
   * @return Map[mot, nombre d'occurrences]
   */
  private def countTokens(
      tokens: List[String],
      acc: Map[String, Int]
  ): Map[String, Int] =
    tokens match
      case Nil         => acc
      case head :: rest =>
        countTokens(rest, acc.updated(head, acc.getOrElse(head, 0) + 1))

  /**
   * Retourne les n mots les plus fréquents du texte, triés par ordre décroissant
   * d'occurrences. Utile pour initialiser automatiquement les mots-clés de démo
   * quelle que soit la teneur du texte d'entrée.
   *
   * @param text le texte brut
   * @param n    le nombre de mots à retourner (défaut : 5)
   * @return liste des n mots les plus fréquents
   */
  def topNWords(text: String, n: Int = 5): List[String] =
    val tokens = tokenize(text)
    val counts = countTokens(tokens, Map.empty)
    counts.toList
      .sortBy { case (_, count) => -count }
      .take(n)
      .map(_._1)

  // ── Interrogation du modèle ─────────────────────────────────────────────────

  /**
   * Retourne la distribution de probabilité pour un contexte donné.
   *
   * @param model   le modèle appris
   * @param context la liste des derniers mots du contexte
   * @return Some(distribution) si le contexte a été observé, None sinon
   */
  def distributionFor(model: Model, context: List[String]): Option[Distribution] =
    Trie.lookup(model, context.mkString(" "))
