# Autocomplétion de texte - Scala 3

Système d'autocomplétion basé sur des n-grammes et un Trie.

## Lancer sans Docker

```bash
cd /home/depinfo/scala_autocompletion_texte
java -jar /home/depinfo/.local/share/JetBrains/IntelliJIdea2025.1/Scala/launcher/sbt-launch.jar test   # tests
java -jar /home/depinfo/.local/share/JetBrains/IntelliJIdea2025.1/Scala/launcher/sbt-launch.jar run    # programme
java -jar /home/depinfo/.local/share/JetBrains/IntelliJIdea2025.1/Scala/launcher/sbt-launch.jar console # REPL
```

## Lancer avec Docker

```bash
cd /home/depinfo/scala_autocompletion_texte
docker-compose up --build
```

Cela démarre un conteneur avec Scala/SBT et ouvre un shell interactif.

### Dans le conteneur

```bash
sbt> test        # lance les 40 tests
sbt> run         # lance le programme (suggestions + génération de texte)
sbt> console    # REPL Scala
sbt> exit       # quitter
```

### En une commande (sans shell interactif)

```bash
docker-compose run --rm dev sbt run
```

## Structure du projet

- `src/main/scala/autocompletion/Trie.scala` - Arbre préfixe immuable
- `src/main/scala/autocompletion/LanguageModel.scala` - Apprentissage n-grammes
- `src/main/scala/autocompletion/Autocomplete.scala` - Suggestions et back-off
- `src/main/scala/autocompletion/Main.scala` - Point d'entrée
- `text.txt` - Corpus d'apprentissage (roman SF français)

## Fonctionnalités

- **Tokenisation** : récursive pure, gère accents français, apostrophes typographiques
- **N-grammes** : apprentissage de tous les ordres (1 à n) dans le même modèle
- **Back-off** : si un contexte long est inconnu, utilise unigrammes
- **Génération** : texte automatique basé sur les probabilités apprises
