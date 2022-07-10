# Discord Music Bot
[![](https://jitpack.io/v/Despical/MusicBot.svg)](https://jitpack.io/#Despical/MusicBot)
[![](https://img.shields.io/badge/Javadocs-latest-lime.svg)](https://javadoc.jitpack.io/com/github/Despical/MusicBot/latest/javadoc/index.html)
[![Discord](https://img.shields.io/discord/719922452259668000.svg?color=lime&label=Discord)](https://discord.gg/Vhyy4HA)

This is the base project for DBot. DBot is a music bot for Discord.

## Documentation
More information will be found on the [wiki page](https://github.com/Despical/MusicBot/wiki) soon. The [Javadoc](https://javadoc.jitpack.io/com/github/Despical/MusicBot/latest/javadoc/index.html) can be browsed. Questions
related to the usage of Music Bot should be posted on my [Discord server](https://discord.com/invite/Vhyy4HA).

## Donations
You like the Music Bot? Then [donate](https://www.patreon.com/despical) back me to support the development.

## Using Music Bot API
The project isn't in the Central Repository yet, so specifying a repository is needed.<br>
To add this project as a dependency to your project, add the following to your pom.xml:

### Maven dependency

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

### Gradle dependency
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
And also don't forget to install Maven before building.
