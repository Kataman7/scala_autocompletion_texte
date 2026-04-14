package autocompletion

/**
 * Apprentissage de la distribution de probabilité d'un texte par n-grammes.
 *
 * Pour chaque séquence de n mots consécutifs (le contexte), on compte
 * combien de fois chaque mot suivant apparaît, puis on normalise en probabilités.
 * Le résultat est stocké dans un Trie.
 */
object LanguageModel:

  type Distribution = Map[String, Double]
  type Model = Trie[Distribution]

  private def normalizeChar(c: Char): Char =
    c match
      case '\u2018' | '\u2019' => '\''
      case 'œ' | 'Œ'          => 'o'
      case _ if c >= 'A' && c <= 'Z' => (c + 32).toChar
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
   * Découpe un texte en liste de mots en minuscules.
   * Gère les accents français et les apostrophes typographiques.
   */
  def tokenize(text: String): List[String] =
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

  /**
   * Extrait tous les n-grammes de taille n avec leur mot suivant.
   * Retourne des paires (contexte, motSuivant).
   */
  @scala.annotation.tailrec
  def extractNGrams(tokens: List[String], n: Int, acc: List[(List[String], String)] = Nil): List[(List[String], String)] =
    require(n >= 1, "n doit être >= 1")
    val window = tokens.take(n + 1)
    if window.length < n + 1 then acc.reverse
    else
      val context = window.init
      val next    = window.last
      extractNGrams(tokens.tail, n, (context, next) :: acc)

  /** Compte les occurrences de chaque mot suivant pour chaque contexte. */
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

  /** Convertit les comptages en probabilités (chaque distribution somme à 1). */
  def normalize(counts: Map[String, Map[String, Int]]): Map[String, Distribution] =
    counts.map { case (context, nextCounts) =>
      val total = nextCounts.values.sum.toDouble
      val dist  = nextCounts.map { case (word, c) => word -> c / total }
      context -> dist
    }

  private def insertAll(
      entries: List[(String, Distribution)],
      trie: Model
  ): Model =
    entries match
      case Nil                  => trie
      case (key, dist) :: rest  => insertAll(rest, Trie.insert(trie, key, dist))

  /**
   * Apprend les probabilités d'un texte et les stocke dans un Trie.
   * Apprend les n-grammes de taille 1 à n d'un coup pour que le back-off fonctionne.
   */
  def learn(text: String, n: Int = 2): Model =
    val tokens = tokenize(text)

    def learnAllOrders(k: Int, trie: Model): Model =
      if k <= 0 then trie
      else
        val ngrams        = extractNGrams(tokens, k)
        val counts        = countNGrams(ngrams)
        val distributions = normalize(counts)
        learnAllOrders(k - 1, insertAll(distributions.toList, trie))

    learnAllOrders(n, Trie.empty[Distribution])

  private def countTokens(
      tokens: List[String],
      acc: Map[String, Int]
  ): Map[String, Int] =
    tokens match
      case Nil         => acc
      case head :: rest =>
        countTokens(rest, acc.updated(head, acc.getOrElse(head, 0) + 1))

  /**
   * Retourne les n mots les plus fréquents du texte.
   */
  def topNWords(text: String, n: Int = 5): List[String] =
    val tokens = tokenize(text)
    val counts = countTokens(tokens, Map.empty)
    counts.toList
      .sortBy { case (_, count) => -count }
      .take(n)
      .map(_._1)

  /**
   * Cherche la distribution de probabilité pour un contexte donné.
   */
  def distributionFor(model: Model, context: List[String]): Option[Distribution] =
    Trie.lookup(model, context.mkString(" "))
