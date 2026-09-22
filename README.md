# Wearstral

A native Wear OS client for chatting with [Mistral AI](https://mistral.ai) models through the public
Mistral API. Built with Kotlin and Compose for Wear OS; voice and keyboard input, streaming replies,
round-screen layouts.

This is **not** an official Mistral project. It does not wrap chat.mistral.ai; it talks directly to
`api.mistral.ai` using an API key that you create yourself at [console.mistral.ai/api-keys](https://console.mistral.ai/api-keys). Later, I will try to add support for people that don't use their API or that pay for the pro sub like me.

## Status

Early development. Currently a wearable app scaffold that builds and installs

## Roadmap

1. Wear OS + Compose scaffold
2. API key screen and storage
3. Single conversation with non-streaming replies
4. SSE streaming responses with haptic feedback
5. Conversation history, model picker, round-screen polish

## Building

```shell
./gradlew assembleDebug
```

Requires the Android SDK (API 36) and JDK 17+.

## License

[GPL-3.0](LICENSE)
