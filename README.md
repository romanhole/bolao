# Bolão de Futebol — KMP App

Aplicativo de palpites esportivos construído com **Kotlin Multiplatform** e **Compose Multiplatform**.

## Stack Tecnológico

| Camada | Tecnologia |
|---|---|
| UI | Compose Multiplatform 1.7.3 |
| Networking | Ktor Client 3.0.3 |
| Serialização | kotlinx.serialization 1.7.3 |
| Coroutines | kotlinx.coroutines 1.9.0 |
| Datas | kotlinx.datetime 0.6.1 |
| DI | Koin 4.0.0 |
| Linguagem | Kotlin 2.0.21 |

## Estrutura do Projeto

```
bolao/
├── androidApp/                          # Entry point Android
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/com/bolao/android/
│           ├── BolaoApplication.kt      # Inicializa Koin
│           └── MainActivity.kt          # Única Activity
│
├── iosApp/                              # Projeto Xcode (a criar)
│
└── shared/                              # Módulo KMP compartilhado
    └── src/
        ├── commonMain/kotlin/com/bolao/
        │   │
        │   ├── domain/                  # 🔵 Camada de Domínio (pura, sem frameworks)
        │   │   ├── model/
        │   │   │   ├── GameStatus.kt    # Sealed class dos estados da partida
        │   │   │   ├── Match.kt         # Entidade central (partida)
        │   │   │   ├── Team.kt          # Entidade time
        │   │   │   └── Prediction.kt    # Entidade palpite do usuário
        │   │   ├── repository/
        │   │   │   ├── MatchRepository.kt      # Interface (contrato)
        │   │   │   └── PredictionRepository.kt # Interface (contrato)
        │   │   └── usecase/             # (próxima iteração)
        │   │
        │   ├── data/                    # 🟡 Camada de Dados
        │   │   ├── network/
        │   │   │   └── HttpClientFactory.kt   # expect — configuração comum Ktor
        │   │   └── repository/          # (implementações — próxima iteração)
        │   │
        │   ├── presentation/            # 🟠 Camada de Apresentação
        │   │   └── viewmodel/           # (próxima iteração)
        │   │
        │   └── di/
        │       └── NetworkModule.kt     # Módulo Koin de rede
        │
        ├── androidMain/kotlin/com/bolao/
        │   └── data/network/
        │       └── HttpClientFactory.android.kt  # actual — engine Android
        │
        └── iosMain/kotlin/com/bolao/
            └── data/network/
                └── HttpClientFactory.ios.kt      # actual — engine Darwin (iOS)
```

## Arquitetura

```
UI (Compose) ──► ViewModel ──► UseCase ──► Repository (interface)
                                                    │
                                             Implementation
                                                    │
                                            Firebase / Supabase
                                         (NUNCA APIs externas de futebol)
```

### Regra de Ouro Arquitetural
O módulo `shared` **nunca** chama APIs externas de futebol (API-Football, etc.).
Todos os dados chegam exclusivamente via nosso backend. Isso garante:
- Controle total sobre o modelo de dados
- Sem dependência de uptime de terceiros
- Lógica de pontuação centralizada no servidor

## Como Abrir no Android Studio

1. `File → Open` → selecione a pasta raiz `bolao/`
2. Aguarde a sincronização do Gradle
3. Selecione o run configuration `androidApp`

## Próximas Iterações

- [ ] Implementar `MatchRepositoryImpl` (Firestore listener)
- [ ] Implementar `PredictionRepositoryImpl`
- [ ] Use Cases: `GetMatchesUseCase`, `SavePredictionUseCase`
- [ ] ViewModels compartilhados com `StateFlow`
- [ ] Telas Compose: Home, Detalhes da Partida, Ranking
- [ ] Projeto Xcode para iOS
