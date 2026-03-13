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
   * Indique si un caractère est une lettre valide (minuscule française ou tiret/apostrophe).
   * Implémenté récursivement sur une liste de plages de caractères autorisées.
   */
  private def isValidChar(c: Char): Boolean =
    def check(ranges: List[(Char, Char)]): Boolean = ranges match
      case Nil               => false
      case (lo, hi) :: rest  => if c >= lo && c <= hi then true else check(rest)
    check(List(
      ('a', 'z'),
      ('à', 'ä'),   // à á â ã ä
      ('è', 'ë'),   // è é ê ë
      ('î', 'ï'),
      ('ô', 'ô'),
      ('ù', 'ü'),   // ù ú û ü
      ('ç', 'ç'),
      ('\'', '\''),
      ('-', '-')
    ))

  /**
   * Convertit un caractère en son équivalent minuscule.
   * Gère les majuscules ASCII et les majuscules accentuées françaises.
   */
  private def toLower(c: Char): Char =
    if c >= 'A' && c <= 'Z' then (c + 32).toChar
    else c match
      case 'À' | 'Â'         => 'â'   // simplifie À→â, Â→â
      case 'Ä'               => 'ä'
      case 'É' | 'È' | 'Ê'  => 'é'
      case 'Ë'               => 'ë'
      case 'Î'               => 'î'
      case 'Ï'               => 'ï'
      case 'Ô'               => 'ô'
      case 'Ù' | 'Û'         => 'û'
      case 'Ü'               => 'ü'
      case 'Ç'               => 'ç'
      case _                 => c

  /**
   * Tokenise un texte en liste de mots en minuscules.
   * Seuls les caractères valides (lettres françaises, tiret, apostrophe) sont conservés.
   * Les séparations se font sur tout caractère non valide (espaces, ponctuation, etc.).
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
          val lc = toLower(c)
          if isValidChar(lc) then
            loop(rest, lc :: currentWord, acc)
          else
            // séparateur : on ferme le mot en cours si non vide
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
   * @param text le texte d'apprentissage
   * @param n    la taille du n-gramme (défaut : 2)
   * @return le modèle de langue sous forme de Trie
   */
  def learn(text: String, n: Int = 2): Model =
    val tokens       = tokenize(text)
    val ngrams       = extractNGrams(tokens, n)
    val counts       = countNGrams(ngrams)
    val distributions = normalize(counts)
    insertAll(distributions.toList, Trie.empty[Distribution])

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
