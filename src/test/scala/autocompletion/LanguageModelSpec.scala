package autocompletion

class LanguageModelSpec extends munit.FunSuite:

  // ── tokenize ───────────────────────────────────────────────────────────────

  test("tokenize splits on whitespace and lowercases") {
    assertEquals(
      LanguageModel.tokenize("Le Chat MANGE"),
      List("le", "chat", "mange")
    )
  }

  test("tokenize removes punctuation") {
    assertEquals(
      LanguageModel.tokenize("Bonjour, monde!"),
      List("bonjour", "monde")
    )
  }

  test("tokenize filters empty tokens") {
    assertEquals(LanguageModel.tokenize("  a  b  "), List("a", "b"))
  }

  test("tokenize empty string returns Nil") {
    assertEquals(LanguageModel.tokenize(""), Nil)
  }

  // ── extractNGrams ──────────────────────────────────────────────────────────

  test("extractNGrams n=1 produces unigram pairs") {
    val tokens = List("a", "b", "c")
    assertEquals(
      LanguageModel.extractNGrams(tokens, 1),
      List(List("a") -> "b", List("b") -> "c")
    )
  }

  test("extractNGrams n=2 produces bigram pairs") {
    val tokens = List("a", "b", "c", "d")
    assertEquals(
      LanguageModel.extractNGrams(tokens, 2),
      List(
        List("a", "b") -> "c",
        List("b", "c") -> "d"
      )
    )
  }

  test("extractNGrams returns Nil when tokens shorter than n+1") {
    assertEquals(LanguageModel.extractNGrams(List("a"), 2), Nil)
  }

  test("extractNGrams empty tokens returns Nil") {
    assertEquals(LanguageModel.extractNGrams(Nil, 1), Nil)
  }

  // ── countNGrams ────────────────────────────────────────────────────────────

  test("countNGrams counts occurrences correctly") {
    val ngrams = List(
      List("le") -> "chat",
      List("le") -> "chat",
      List("le") -> "chien"
    )
    val counts = LanguageModel.countNGrams(ngrams)
    assertEquals(counts("le")("chat"), 2)
    assertEquals(counts("le")("chien"), 1)
  }

  // ── normalize ──────────────────────────────────────────────────────────────

  test("normalize produces probabilities that sum to 1") {
    val counts = Map("le" -> Map("chat" -> 2, "chien" -> 2))
    val dist   = LanguageModel.normalize(counts)("le")
    assertEqualsDouble(dist("chat") + dist("chien"), 1.0, 1e-9)
  }

  test("normalize single successor has probability 1.0") {
    val counts = Map("chat" -> Map("mange" -> 5))
    val dist   = LanguageModel.normalize(counts)("chat")
    assertEqualsDouble(dist("mange"), 1.0, 1e-9)
  }

  // ── learn / distributionFor ────────────────────────────────────────────────

  test("learn then distributionFor returns distribution for known context") {
    val text  = "le chat mange le fromage le chat dort"
    val model = LanguageModel.learn(text, n = 2)
    val dist  = LanguageModel.distributionFor(model, List("le", "chat"))
    assert(dist.isDefined)
    // "le chat" est suivi de "mange" et de "dort"
    val d = dist.get
    assert(d.contains("mange"))
    assert(d.contains("dort"))
    assertEqualsDouble(d("mange") + d("dort"), 1.0, 1e-9)
  }

  test("distributionFor returns None for unknown context") {
    val model = LanguageModel.learn("le chat mange", n = 2)
    assertEquals(LanguageModel.distributionFor(model, List("la", "souris")), None)
  }

  test("learn with n=1 works as unigram model") {
    val text  = "a b c b c b"
    val model = LanguageModel.learn(text, n = 1)
    val dist  = LanguageModel.distributionFor(model, List("b"))
    assert(dist.isDefined)
    // "b" est suivi de "c" 2 fois, rien d'autre
    assertEqualsDouble(dist.get("c"), 1.0, 1e-9)
  }
