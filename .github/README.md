<h1 align="center">Discord Music Bot</h1>

<div align="center">

[![](https://jitpack.io/v/Despical/MusicBot.svg)](https://jitpack.io/#Despical/MusicBot)
[![](https://img.shields.io/badge/JavaDocs-latest-lime.svg)](https://javadoc.jitpack.io/com/github/Despical/MusicBot/latest/javadoc/index.html)
[![Support](https://img.shields.io/badge/Patreon-Support-lime.svg?logo=Patreon)](https://www.patreon.com/despical)

Example implementation of LavaPlayer for Discord bots.

</div>

## Documentation
- [Wiki](https://github.com/Despical/MusicBot/wiki)
- [JavaDocs](https://javadoc.jitpack.io/com/github/Despical/MusicBot/latest/javadoc/index.html)

## Donations
- [Patreon](https://www.patreon.com/despical)
- [Buy Me A Coffe](https://www.buymeacoffee.com/despical)

## Using Music Bot API
The project isn't in the Central Repository yet, so specifying a repository is needed.<br>
To add this project as a dependency to your project, add the following to your pom.xml:

<details>
<summary>Maven dependency</summary>

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```
```xml
<dependency>
    <groupId>com.github.Despical</groupId>
    <artifactId>MusicBot</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>compile</scope>
</dependency>
```

</details>

<details>
<summary>Gradle dependency</summary>

```
repositories {
    maven { url 'https://jitpack.io' }
}
```
```
dependencies {
    compileOnly group: "com.github.Despical", name: "MusicBot", version: "1.0.0-SNAPSHOT";
}
```
</details>

## License
This code is under [GPL-3.0 License](http://www.gnu.org/licenses/gpl-3.0.html)

See the [LICENSE](https://github.com/Despical/MusicBot/blob/master/LICENSE) file for required notices and attributions.

## Contributing

I accept Pull Requests via GitHub. There are some guidelines which will make applying PRs easier for me:
+ No spaces! Please use tabs for indentation.
+ Respect the code style.
+ Create minimal diffs. If you feel the source code should be reformatted create a separate PR for this change.

You can learn more about contributing via GitHub in [contribution guidelines](https://github.com/Despical/MusicBot/blob/master/CONTRIBUTING.md).

## Building from source
If you want to build this project from source code, run the following from Git Bash:
```
git clone https://www.github.com/Despical/MusicBot.git && cd MusicBot
mvn clean package
```
The build can then be found in ``/MusicBot/target/``
> **Note** Don't forget to install Maven before building.
