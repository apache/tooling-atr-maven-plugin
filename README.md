# Maven ATR Plugin

Studying ATR integration for Maven.

It starts with a Maven plugin to [upload Apache distribution artifacts](https://release-test.apache.org/tutorial) to ATR before vote, like [`atr upload` CLI](https://github.com/apache/tooling-releases-client/blob/main/COMMANDS.md#atr-upload).

This can in the future be triggered during `mvn deploy` in an `atr` profile along `apache-release` one.
