# Autocomplétion de texte — Scala 3

Projet d'autocomplétion : on donne un début de phrase au programme, il propose les mots suivants les plus probables et peut aussi générer un texte complet. Le corpus utilisé est le roman Tolla de Edmond en français (text.txt, ~67 000 mots) https://www.gutenberg.org/ebooks/63937

## Lancer le projet

Tout se fait dans Docker, pas besoin d'installer Scala ou sbt sur sa machine.

```bash
docker-compose run --rm dev sbt run
```

### Tests

```bash
docker-compose run --rm dev sbt test
```

### Arguments en ligne de commande

```bash
# Par défaut, le programme teste les 4 mots les plus fréquents du corpus
docker-compose run --rm dev sbt run

# On peut passer nos propres mots en paramètre
docker-compose run --rm dev sbt 'run "je suis" "nous allons"'

# Le premier mot sert aussi de point de départ pour la génération
```

### Shell interactif sbt

```bash
docker-compose run --rm dev sbt
# Puis dans sbt : run, test, console, exit
```

## Structure du projet

- `Trie.scala` — Arbre préfixe (insert, lookup, withPrefix)
- `LanguageModel.scala` — Tokenisation, apprentissage n-grammes, normalisation
- `Autocomplete.scala` — Suggestions avec back-off, génération de texte
- `Main.scala` — Point d'entrée
- `src/test/` — 43 tests unitaires (MUnit)
- `text.txt` — Corpus d'apprentissage

## Comment ça marche

1. **Tokenisation** : on découpe le texte en mots (tout en minuscules, accents gérés)
2. **N-grammes** : pour chaque séquence de n mots consécutifs, on note quel mot vient après
3. **Comptage** : on compte combien de fois chaque mot suivant apparaît pour chaque contexte
4. **Normalisation** : on transforme les comptages en probabilités (qui somment à 1)
5. **Stockage** : les distributions sont stockées dans un Trie (arbre préfixe)
6. **Suggestions** : on cherche le contexte dans le Trie et on retourne les mots les plus probables
7. **Back-off** : si le contexte trigramme est inconnu, on essaie bigramme puis unigramme
8. **Génération** : on part d'une amorce et on ajoute le mot le plus probable à chaque étape

On utilise n=3 (trigramme) : le modèle regarde les 2 mots précédents pour prédire le suivant. Ça donne du texte plus varié que le bigramme.
