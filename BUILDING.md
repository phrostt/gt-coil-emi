# Сборка

## 1. JDK 17

Forge 1.20.1 собирается **только на JDK 17**. Java 25, которая уже стоит в системе,
не подойдёт — Gradle 8.1.1 её не запустит.

    winget install EclipseAdoptium.Temurin.17.JDK

После установки либо выстави `JAVA_HOME` на JDK 17, либо раскомментируй в
`gradle.properties` строку `org.gradle.java.home` и укажи путь к нему.

## 2. Gradle wrapper

В репозитории нет `gradlew` / `gradle-wrapper.jar` (это бинарники). Проще всего
взять их из официального Forge MDK:

    curl -L -o forge-mdk.zip https://maven.minecraftforge.net/net/minecraftforge/forge/1.20.1-47.4.10/forge-1.20.1-47.4.10-mdk.zip
    powershell -NoProfile -Command "Expand-Archive -Force forge-mdk.zip forge-mdk"
    cp forge-mdk/gradlew forge-mdk/gradlew.bat gt-coil-emi/
    cp forge-mdk/gradle/wrapper/gradle-wrapper.jar gt-coil-emi/gradle/wrapper/

## 3. Сборка

    ./gradlew build

Первый запуск качает ~1 ГБ и декомпилирует Minecraft — это 5–15 минут. Готовый
мод: `build/libs/gtcoilemi-0.1.0.jar`.

## 4. Проверка

Скопируй jar в папку `mods` сборки рядом с EMI и GregTech. Мод клиентский,
на сервер его ставить не нужно.

Для отладки в dev-клиенте: `./gradlew runClient`, предварительно положив jar-ники
GregTech, LDLib и EMI в папку `run/mods`.

## Версии

Все версии вынесены в `gradle.properties`. Перед первой реальной сборкой сверь
`forge_version` и `emi_version` с тем, что стоит в сборке.
