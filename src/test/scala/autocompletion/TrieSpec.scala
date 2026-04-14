package autocompletion

class TrieSpec extends munit.FunSuite:

  test("empty trie has no value at root") {
    val t = Trie.empty[Int]
    assertEquals(Trie.lookup(t, ""), None)
  }

  test("lookup on empty trie returns None") {
    val t = Trie.empty[String]
    assertEquals(Trie.lookup(t, "hello"), None)
  }

  test("insert then lookup single key returns value") {
    val t = Trie.insert(Trie.empty[Int], "chat", 42)
    assertEquals(Trie.lookup(t, "chat"), Some(42))
  }

  test("lookup missing key after insert returns None") {
    val t = Trie.insert(Trie.empty[Int], "chat", 42)
    assertEquals(Trie.lookup(t, "chien"), None)
  }

  test("insert overwrites existing key") {
    val t0 = Trie.insert(Trie.empty[Int], "chat", 1)
    val t1 = Trie.insert(t0, "chat", 99)
    assertEquals(Trie.lookup(t1, "chat"), Some(99))
  }

  test("insert empty-string key") {
    val t = Trie.insert(Trie.empty[String], "", "root")
    assertEquals(Trie.lookup(t, ""), Some("root"))
  }

  test("two keys with shared prefix coexist") {
    val t = Trie.insert(
      Trie.insert(Trie.empty[Int], "chat", 1),
      "chien", 2
    )
    assertEquals(Trie.lookup(t, "chat"), Some(1))
    assertEquals(Trie.lookup(t, "chien"), Some(2))
    assertEquals(Trie.lookup(t, "ch"), None)
  }

  test("prefix key and longer key coexist") {
    val t = Trie.insert(
      Trie.insert(Trie.empty[Int], "ch", 10),
      "chat", 20
    )
    assertEquals(Trie.lookup(t, "ch"), Some(10))
    assertEquals(Trie.lookup(t, "chat"), Some(20))
  }

  test("insert is immutable – original trie unchanged") {
    val t0 = Trie.empty[Int]
    val _  = Trie.insert(t0, "chat", 1)
    assertEquals(Trie.lookup(t0, "chat"), None)
  }

  test("withPrefix returns all entries sharing a prefix") {
    val t = List("chat" -> 1, "chien" -> 2, "souris" -> 3)
      .foldLeft(Trie.empty[Int]) { case (acc, (k, v)) => Trie.insert(acc, k, v) }
    val results = Trie.withPrefix(t, "ch").toMap
    assertEquals(results, Map("chat" -> 1, "chien" -> 2))
  }

  test("withPrefix on unknown prefix returns Nil") {
    val t = Trie.insert(Trie.empty[Int], "chat", 1)
    assertEquals(Trie.withPrefix(t, "xyz"), Nil)
  }

  test("withPrefix with empty prefix returns all entries") {
    val entries = List("a" -> 1, "b" -> 2, "c" -> 3)
    val t = entries.foldLeft(Trie.empty[Int]) { case (acc, (k, v)) => Trie.insert(acc, k, v) }
    val results = Trie.withPrefix(t, "").toMap
    assertEquals(results, entries.toMap)
  }
