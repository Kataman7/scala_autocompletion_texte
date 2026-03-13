package autocompletion

/**
 * Trie (arbre préfixe) fonctionnel et immuable.
 *
 * Chaque nœud contient :
 *   - `children` : une Map de caractère → sous-trie
 *   - `value`    : une valeur optionnelle stockée à ce nœud
 *
 * Toutes les opérations sont pures et récursives ; aucun état mutable.
 */
enum Trie[+V]:
  /** Nœud interne (ou racine) : enfants indexés par caractère, valeur optionnelle. */
  case Node(children: Map[Char, Trie[V]], value: Option[V])

object Trie:

  /** Crée un trie vide. */
  def empty[V]: Trie[V] = Trie.Node(Map.empty, None)

  /**
   * Insère l'association `key → value` dans `trie`.
   * Si la clé existe déjà, la valeur est remplacée.
   *
   * @param trie  le trie dans lequel insérer
   * @param key   la clé (chaîne de caractères)
   * @param value la valeur à associer
   * @return un nouveau trie contenant l'association
   */
  def insert[V](trie: Trie[V], key: String, value: V): Trie[V] =
    trie match
      case Trie.Node(children, v) =>
        if key.isEmpty then Trie.Node(children, Some(value))
        else
          val c        = key.head
          val rest     = key.tail
          val subtrie  = children.getOrElse(c, Trie.empty[V])
          val newChild = insert(subtrie, rest, value)
          Trie.Node(children.updated(c, newChild), v)

  /**
   * Recherche la valeur associée à `key` dans `trie`.
   *
   * @param trie le trie dans lequel chercher
   * @param key  la clé recherchée
   * @return Some(value) si la clé existe, None sinon
   */
  def lookup[V](trie: Trie[V], key: String): Option[V] =
    trie match
      case Trie.Node(children, value) =>
        if key.isEmpty then value
        else
          children.get(key.head) match
            case None         => None
            case Some(child)  => lookup(child, key.tail)

  /**
   * Retourne toutes les associations (clé, valeur) dont la clé commence par `prefix`.
   * Utile pour l'autocomplétion sur les préfixes de mots.
   *
   * @param trie   le trie dans lequel chercher
   * @param prefix le préfixe commun recherché
   * @return une liste de paires (suffixe_restant + prefix, valeur)
   */
  def withPrefix[V](trie: Trie[V], prefix: String): List[(String, V)] =
    /** Descend dans le trie jusqu'au nœud correspondant au préfixe. */
    def navigate(t: Trie[V], remaining: String): Option[Trie[V]] =
      remaining.headOption match
        case None    => Some(t)
        case Some(c) =>
          t match
            case Trie.Node(children, _) =>
              children.get(c).flatMap(child => navigate(child, remaining.tail))

    /** Collecte récursivement toutes les (clé, valeur) à partir d'un nœud. */
    def collectAll(t: Trie[V], acc: String): List[(String, V)] =
      t match
        case Trie.Node(children, value) =>
          val here = value.map(v => (acc, v)).toList
          val below = children.toList.flatMap { case (c, child) =>
            collectAll(child, acc + c)
          }
          here ++ below

    navigate(trie, prefix) match
      case None       => Nil
      case Some(node) => collectAll(node, prefix)
