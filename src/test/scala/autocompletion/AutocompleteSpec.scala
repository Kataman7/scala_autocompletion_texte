package autocompletion

class AutocompleteSpec extends munit.FunSuite:

  val text: String =
    "le chat mange le fromage le chat boit le lait le chat dort le chat mange"

  val model: LanguageModel.Model = LanguageModel.learn(text, n = 2)

  test("extractContext takes last n tokens") {
    assertEquals(Autocomplete.extractContext(List("a", "b", "c", "d"), 2), List("c", "d"))
  }

  test("extractContext returns all tokens when fewer than n") {
    assertEquals(Autocomplete.extractContext(List("a"), 3), List("a"))
  }

  test("extractContext empty list returns Nil") {
    assertEquals(Autocomplete.extractContext(Nil, 2), Nil)
  }

  test("topK returns k most probable words in order") {
    val dist = Map("mange" -> 0.5, "dort" -> 0.3, "boit" -> 0.2)
    assertEquals(Autocomplete.topK(dist, 2).map(_._1), List("mange", "dort"))
  }

  test("topK with k larger than dist returns all entries sorted") {
    val dist = Map("a" -> 0.4, "b" -> 0.6)
    assertEquals(Autocomplete.topK(dist, 10).map(_._1), List("b", "a"))
  }

  test("topK on empty distribution returns Nil") {
    assertEquals(Autocomplete.topK(Map.empty, 3), Nil)
  }

  test("nextWord returns Some for known context") {
    val result = Autocomplete.nextWord(model, "le chat", n = 2)
    assert(result.isDefined)
  }

  test("nextWord uses back-off when bigram unknown but unigram known") {
    val result = Autocomplete.nextWord(model, "le fromage", n = 2)
    assert(result.isDefined)
  }

  test("nextWord returns None for completely unknown context") {
    val result = Autocomplete.nextWord(model, "xyz foo bar", n = 2)
    assertEquals(result, None)
  }

  test("topNextWords returns up to k suggestions") {
    val results = Autocomplete.topNextWords(model, "le chat", k = 3, n = 2)
    assert(results.nonEmpty)
    assert(results.length <= 3)
  }

  test("topNextWords suggestions are distinct") {
    val results = Autocomplete.topNextWords(model, "le chat", k = 3, n = 2)
    assertEquals(results.distinct, results)
  }

  test("topNextWords returns Nil for unknown context without backoff match") {
    val results = Autocomplete.topNextWords(model, "xyz abc def", k = 3, n = 2)
    assertEquals(results, Nil)
  }

  test("generateText produces at least seed words") {
    val result = Autocomplete.generateText(model, "le chat", maxWords = 5, n = 2)
    assert(result.split("\\s+").length >= 2)
  }

  test("generateText respects maxWords limit") {
    val result = Autocomplete.generateText(model, "le chat", maxWords = 4, n = 2)
    assert(result.split("\\s+").length <= 6)
  }
