package autocompletion

/** Trie (arbre préfixe). Chaque nœud a des enfants indexés par caractère et une valeur optionnelle. */
enum Trie[+V]:
  case Node(children: Map[Char, Trie[V]], value: Option[V])

object Trie:

  /** Crée un trie vide. */
  def empty[V]: Trie[V] = Trie.Node(Map.empty, None)

  /**
   * Insère key → value dans le trie.
   * Si la clé existe déjà, on remplace la valeur.
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
   * Cherche la valeur associée à une clé.
   * Retourne Some(value) si trouvé, None sinon.
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
   * Retourne toutes les (clé, valeur) dont la clé commence par le préfixe donné.
   */
  def withPrefix[V](trie: Trie[V], prefix: String): List[(String, V)] =
    def navigate(t: Trie[V], remaining: String): Option[Trie[V]] =
      remaining.headOption match
        case None    => Some(t)
        case Some(c) =>
          t match
            case Trie.Node(children, _) =>
              children.get(c).flatMap(child => navigate(child, remaining.tail))

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
