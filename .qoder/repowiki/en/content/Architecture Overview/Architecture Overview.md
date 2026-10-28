# Architecture Overview

<cite>
**Referenced Files in This Document**
- [MainActivity.kt](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt)
- [MainScreen.kt](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt)
- [AppNavHost.kt](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt)
- [CanLuaApplication.kt](file://app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt)
- [AppDatabase.kt](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt)
- [DatabaseModule.kt](file://app/src/main/java/com/giathinh/canlua/di/DatabaseModule.kt)
- [FirebaseModule.kt](file://app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt)
- [NetworkModule.kt](file://app/src/main/java/com/giathinh/canlua/di/NetworkModule.kt)
- [CardRepository.kt](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt)
- [SyncManager.kt](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt)
- [AuthManager.kt](file://app/src/main/java/com/giathinh/canlua/repository/AuthManager.kt)
- [MarketRepository.kt](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt)
- [KnowledgeBaseRepository.kt](file://app/src/main/java/com/giathinh/canlua/repository/KnowledgeBaseRepository.kt)
- [CardListViewModel.kt](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt)
- [AuthViewModel.kt](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/AuthViewModel.kt)
</cite>

## Table of Contents
1. Introduction
2. Project Structure
3. Core Components
4. Architecture Overview
5. Detailed Component Analysis
6. Dependency Analysis
7. Performance Considerations
8. Troubleshooting Guide
9. Conclusion

## Introduction
Cân Lúa Mobile is a Kotlin 2.3, Jetpack Compose offline-first Android application following MVVM and Clean Architecture with unidirectional data flow through StateFlow and coroutines. The app serves three roles — FARMER, TRADER, and GUEST — with role-based portals and AI features powered by OpenRouter (Gemini Flash) using local RAG context from JSON knowledge files. Local persistence uses AndroidX Room with schema version 19 and an explicit migration chain. Cloud synchronization is bidirectional to Firebase Firestore and Supabase, scheduled idempotently via WorkManager, while the app remains fully functional without network access.

## Project Structure
The codebase is organized by architectural layers:
- UI layer: Jetpack Compose screens, navigation, theme, and reusable components.
- ViewModel layer: state holders exposing StateFlow and handling user actions.
- Repository layer: business logic, orchestration between local and remote sources.
- Data layer: Room database, DAOs, models, Firestore mappings, network clients, and configuration modules.

```mermaid
graph TB
MainActivity["MainActivity<br/>Root navigation + auth routing"] --> MainScreen["MainScreen<br/>Shell, top bar, bottom bar, offline banner"]
MainScreen --> AppNavHost["AppNavHost<br/>Feature routes and role-aware profile wrapper"]
AppNavHost --> Screens["Feature Screens<br/>Scale, Market, AI Chat, Profile, Trader"]
Screens --> ViewModels["ViewModels<br/>StateFlow + coroutines"]
ViewModels --> Repositories["Repositories<br/>Business logic + multi-source abstraction"]
Repositories --> Database["Room Database<br/>AppDatabase + DAOs"]
Repositories --> Firebase["Firebase Auth + Firestore"]
Repositories --> Supabase["Supabase Price API"]
Repositories --> Knowledge["Knowledge Base<br/>Local JSON RAG"]
```

**Diagram sources**
- [MainActivity.kt:39-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L39-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [AppDatabase.kt:24-46](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L24-L46)
- [FirebaseModule.kt:12-33](file://app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt#L12-L33)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)
- [KnowledgeBaseRepository.kt:10-105](file://app/src/main/java/com/giathinh/canlua/repository/KnowledgeBaseRepository.kt#L10-L105)

**Section sources**
- [MainActivity.kt:39-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L39-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)

## Core Components
- Application lifecycle: CanLuaApplication initializes analytics, premium state, Firebase Remote Config, early adopter eligibility, orphan claim scheduling, and auto-sync preference observation. It configures WorkManager with Hilt worker factory and installs Firebase App Check provider.
- Single-activity architecture: MainActivity hosts Compose content, manages splash screen retention until initialization and authentication are ready, sets up edge-to-edge UI, ad consent, locale, and root navigation graph.
- Navigation: MainScreen provides shell UI and delegates feature routing to AppNavHost, which defines tab routes, sub-screens, deep links, and role-aware profile switching.
- State management: ViewModels expose StateFlow for UI state and use coroutines for background work; they depend on repositories for business logic and data operations.
- Repository layer: CardRepository encapsulates per-user isolation, card CRUD, weight entries, transactions, calculations, tombstone handling, and statistics. SyncManager orchestrates bidirectional sync with Firestore and Supabase, enforces constraints, and schedules WorkManager tasks. MarketRepository implements offline-first price caching with TTL refresh. KnowledgeBaseRepository provides local RAG context for AI chat.
- Data layer: AppDatabase declares entities, migrations, and DAO providers. DI modules provide Room database, DAOs, Firebase services, OkHttp client with retry interceptor, and settings repository.

**Section sources**
- [CanLuaApplication.kt:28-125](file://app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt#L28-L125)
- [MainActivity.kt:39-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L39-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)
- [KnowledgeBaseRepository.kt:10-105](file://app/src/main/java/com/giathinh/canlua/repository/KnowledgeBaseRepository.kt#L10-L105)
- [AppDatabase.kt:24-46](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L24-L46)
- [DatabaseModule.kt:18-76](file://app/src/main/java/com/giathinh/canlua/di/DatabaseModule.kt#L18-L76)
- [FirebaseModule.kt:12-33](file://app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt#L12-L33)
- [NetworkModule.kt:13-77](file://app/src/main/java/com/giathinh/canlua/di/NetworkModule.kt#L13-L77)

## Architecture Overview
The system follows MVVM + Clean Architecture with unidirectional data flow:
- UI composable reads StateFlow from ViewModels.
- ViewModels call repositories for business logic and data operations.
- Repositories abstract multiple data sources: Room (offline cache), Firebase Firestore (cloud), Supabase (market prices), and local assets (knowledge base).
- Application-level services initialize cross-cutting concerns: analytics, premium gating, remote config, auth-driven workflows, and background sync.

```mermaid
sequenceDiagram
participant User as "User"
participant MainActivity as "MainActivity"
participant MainScreen as "MainScreen"
participant AppNavHost as "AppNavHost"
participant Screen as "Feature Screen"
participant VM as "ViewModel"
participant Repo as "Repository"
participant DB as "Room Database"
participant Firebase as "Firebase Firestore"
participant Supabase as "Supabase"
User->>MainActivity : Launch app
MainActivity->>MainActivity : Initialize splash, auth, settings
MainActivity->>MainScreen : Render shell + NavHost
MainScreen->>AppNavHost : Route to start destination
AppNavHost->>Screen : Compose feature screen
Screen->>VM : Collect StateFlow
VM->>Repo : Business operation
Repo->>DB : Read/write Room
Repo->>Firebase : Push/pull cloud data
Repo->>Supabase : Fetch market prices
DB-->>Repo : Flow<List<Entity>>
Firebase-->>Repo : Cloud documents
Supabase-->>Repo : Price list
Repo-->>VM : StateFlow / Result
VM-->>Screen : UI state updates
```

**Diagram sources**
- [MainActivity.kt:39-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L39-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)

## Detailed Component Analysis

### Application Lifecycle and Initialization
CanLuaApplication performs critical startup tasks:
- Installs Firebase App Check provider based on build variant.
- Initializes analytics, premium state, and Firebase Remote Config.
- Applies early adopter eligibility and force-fetches remote config.
- Schedules orphan card claiming and observes auto-sync preferences.
- Configures WorkManager with HiltWorkerFactory for dependency injection in workers.

```mermaid
flowchart TD
Start([Application onCreate]) --> InitFirebase["Install Firebase App Check Provider"]
InitFirebase --> InitServices["Initialize Analytics, Premium, Remote Config"]
InitServices --> ApplyEarlyAdopter["Apply Early Adopter Eligibility"]
ApplyEarlyAdopter --> ForceFetch["Force Fetch Remote Config"]
ForceFetch --> ScheduleOrphan["Schedule Orphan Claim Workflow"]
ScheduleOrphan --> ObserveSyncPrefs["Observe Auto-Sync Preferences"]
ObserveSyncPrefs --> End([Ready for runtime])
```

**Diagram sources**
- [CanLuaApplication.kt:53-77](file://app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt#L53-L77)
- [CanLuaApplication.kt:79-125](file://app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt#L79-L125)

**Section sources**
- [CanLuaApplication.kt:28-125](file://app/src/main/java/com/giathinh/canlua/CanLuaApplication.kt#L28-L125)

### Single-Activity Navigation and Role-Based Routing
MainActivity sets up the root navigation graph:
- Splash screen remains visible until data readiness and authentication state are resolved.
- Root routes include splash, login, profile setup, and main app shell.
- Deep links route to specific cards within the main shell.
- MainScreen provides shell UI with top bar, bottom navigation, offline status banner, and delegates feature routing to AppNavHost.
- AppNavHost defines tab routes (Scale, Market, AI Chat, Account) and trader-only routes, with role-aware profile wrapper switching between farmer and trader profiles.

```mermaid
sequenceDiagram
participant MainActivity as "MainActivity"
participant MainScreen as "MainScreen"
participant AppNavHost as "AppNavHost"
participant ProfileVM as "ProfileViewModel"
MainActivity->>MainActivity : Determine initialDestination
MainActivity->>MainScreen : Navigate to splash/login/profile_setup/main
MainScreen->>AppNavHost : Set startDestination based on role
AppNavHost->>ProfileVM : Collect profile.role
ProfileVM-->>AppNavHost : Farmer or Trader profile
AppNavHost->>AppNavHost : Render FarmerProfileScreen or TraderProfileScreen
```

**Diagram sources**
- [MainActivity.kt:45-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L45-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)

**Section sources**
- [MainActivity.kt:39-145](file://app/src/main/java/com/giathinh/canlua/MainActivity.kt#L39-L145)
- [MainScreen.kt:71-383](file://app/src/main/java/com/giathinh/canlua/ui/MainScreen.kt#L71-L383)
- [AppNavHost.kt:27-150](file://app/src/main/java/com/giathinh/canlua/ui/navigation/AppNavHost.kt#L27-L150)

### ViewModel Layer and Unidirectional Data Flow
ViewModels manage UI state through StateFlow and handle user interactions:
- AuthViewModel subscribes to AuthManager.authStateFlow, updates UI state, and coordinates profile setup routing.
- CardListViewModel combines repository flows with local filters, exposes filtered card lists, and handles card creation/deletion events.
- ViewModels delegate business logic to repositories and use coroutines for background operations.

```mermaid
classDiagram
class AuthViewModel {
+StateFlow<AuthUiState> uiState
+signInEmail(email, password)
+registerEmail(...)
+signInAnonymously()
+signInWithGoogle(account)
+sendOtp(activity, phoneNumber)
+verifyOtp(code)
+signOut()
}
class CardListViewModel {
+StateFlow<Boolean> isLoading
+StateFlow<List<Card>> cards
+StateFlow<List<String>> availableVarieties
+createNewCard(...)
+deleteCard(card)
+setVarietyFilter(variety)
+setSeasonFilter(season)
}
class AuthManager {
+FirebaseUser? currentUser
+Boolean isAuthenticated
+Flow<FirebaseUser?> authStateFlow
+signInAnonymously()
+signInWithEmailAndPassword(...)
+createUserWithEmailAndPassword(...)
+signOut()
}
class SyncableCardRepository {
+getAllCards()
+insertCard(card)
+deleteCard(card)
+getDistinctRiceVarieties()
+getDistinctSeasons()
}
AuthViewModel --> AuthManager : "uses"
CardListViewModel --> SyncableCardRepository : "depends on"
```

**Diagram sources**
- [AuthViewModel.kt:25-361](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/AuthViewModel.kt#L25-L361)
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)
- [AuthManager.kt:12-76](file://app/src/main/java/com/giathinh/canlua/repository/AuthManager.kt#L12-L76)

**Section sources**
- [AuthViewModel.kt:25-361](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/AuthViewModel.kt#L25-L361)
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)

### Repository Layer and Multi-Source Abstraction
Repositories implement business logic and abstract data sources:
- CardRepository ensures per-user isolation, encrypts sensitive fields, handles card lifecycle including tombstones for deleted cards, and performs atomic recalculations using RiceSettlement.
- SyncManager orchestrates bidirectional sync with Firestore, batches operations, handles deduplication, and schedules WorkManager tasks with constraints.
- MarketRepository implements offline-first caching with TTL refresh from Supabase.
- KnowledgeBaseRepository provides local RAG context for AI chat using keyword scoring over JSON assets.

```mermaid
flowchart TD
RepoStart([Repository Operation]) --> CheckAuth{"User authenticated?"}
CheckAuth --> |No| ReturnEmpty["Return empty/null Flow"]
CheckAuth --> |Yes| LocalFirst["Read from Room"]
LocalFirst --> NetworkNeeded{"Network required?"}
NetworkNeeded --> |No| ReturnLocal["Return cached data"]
NetworkNeeded --> |Yes| FetchRemote["Fetch from Firebase/Supabase"]
FetchRemote --> UpsertLocal["Upsert to Room"]
UpsertLocal --> ReturnUpdated["Return updated Flow"]
ReturnEmpty --> End([Operation Complete])
ReturnLocal --> End
ReturnUpdated --> End
```

**Diagram sources**
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)

**Section sources**
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)
- [KnowledgeBaseRepository.kt:10-105](file://app/src/main/java/com/giathinh/canlua/repository/KnowledgeBaseRepository.kt#L10-L105)

### Data Layer and Offline-First Strategy
Room database serves as the single source of truth for local data:
- AppDatabase declares entities, migrations, and DAO providers.
- Schema version 19 includes explicit migration chain covering user isolation, cloud sync fields, conflict resolution, tombstone table, per-card weigh modes, and settlement improvements.
- DI module provides database instance and DAOs for repository injection.

```mermaid
erDiagram
CARD {
integer id PK
text ownerUid
text firestoreId
text name
real totalWeight
real netWeight
real bagWeight
real impurityWeight
real pricePerKg
real totalAmount
real paidAmount
real depositAmount
real remainingAmount
real overpaidAmount
integer bagCount
boolean isLocked
boolean isPaid
text riceVariety
real moisturePercent
text seasonLabel
text qrToken
text lockedByTraderId
real latitude
real longitude
text traderPhone
text fieldAddress
integer lastModifiedMs
boolean impurityIsPercent
boolean bagMethodIsSampling
integer bagSampleCount
real bagSampleTotalWeight
text weightInputMode
}
WEIGHT_ENTRY {
integer id PK
integer cardId FK
text firestoreId
real weight
real bagWeight
real impurityWeight
real netWeight
integer timestamp
}
TRANSACTION {
integer id PK
integer cardId FK
text firestoreId
real amount
text type
text description
integer date
}
PROFILE {
text uid PK
text name
text phone
text region
text note
text role
text cccd
text username
text email
text roleGrantedBy
integer roleGrantedAt
}
RICE_PRICE {
text id PK
text variety
real priceMin
real priceMax
real priceAvg7d
text region
integer updatedAt
text traderId
text traderName
text trend
text riceType
}
NEWS_ARTICLE {
text id PK
text title
text description
text link
text source
text thumbnail
integer publishedAt
text topic
integer cachedAt
}
DELETED_CARD {
integer id PK
text ownerUid
text firestoreId
integer localId
text cardJson
integer deletedAt
text name
text traderName
real totalWeight
real totalAmount
integer cardDate
text seasonLabel
text riceVariety
integer cloudDeleted
}
CARD ||--o{ WEIGHT_ENTRY : "has many"
CARD ||--o{ TRANSACTION : "has many"
```

**Diagram sources**
- [AppDatabase.kt:24-46](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L24-L46)
- [AppDatabase.kt:84-398](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L84-L398)

**Section sources**
- [AppDatabase.kt:24-46](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L24-L46)
- [AppDatabase.kt:84-398](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L84-L398)
- [DatabaseModule.kt:18-76](file://app/src/main/java/com/giathinh/canlua/di/DatabaseModule.kt#L18-L76)

### Dependency Injection Setup with Hilt
Hilt provides centralized dependency management:
- DatabaseModule provides AppDatabase singleton and all DAO instances.
- FirebaseModule provides FirebaseAuth, FirebaseFirestore, and FirebaseStorage singletons.
- NetworkModule provides OkHttpClient with timeouts, logging interceptor, and retry logic.
- SettingsRepository and TextToSpeechManager are provided as singletons.

```mermaid
graph TB
Hilt["Hilt Container"] --> DatabaseModule["DatabaseModule"]
Hilt --> FirebaseModule["FirebaseModule"]
Hilt --> NetworkModule["NetworkModule"]
DatabaseModule --> AppDatabase["AppDatabase"]
DatabaseModule --> DAOs["DAOs"]
FirebaseModule --> FirebaseAuth["FirebaseAuth"]
FirebaseModule --> FirebaseFirestore["FirebaseFirestore"]
FirebaseModule --> FirebaseStorage["FirebaseStorage"]
NetworkModule --> OkHttpClient["OkHttpClient"]
```

**Diagram sources**
- [DatabaseModule.kt:18-76](file://app/src/main/java/com/giathinh/canlua/di/DatabaseModule.kt#L18-L76)
- [FirebaseModule.kt:12-33](file://app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt#L12-L33)
- [NetworkModule.kt:13-77](file://app/src/main/java/com/giathinh/canlua/di/NetworkModule.kt#L13-L77)

**Section sources**
- [DatabaseModule.kt:18-76](file://app/src/main/java/com/giathinh/canlua/di/DatabaseModule.kt#L18-L76)
- [FirebaseModule.kt:12-33](file://app/src/main/java/com/giathinh/canlua/di/FirebaseModule.kt#L12-L33)
- [NetworkModule.kt:13-77](file://app/src/main/java/com/giathinh/canlua/di/NetworkModule.kt#L13-L77)

## Dependency Analysis
Component dependencies follow Clean Architecture principles:
- UI depends only on ViewModels through StateFlow.
- ViewModels depend on repositories for business logic.
- Repositories depend on data layer components (Room, Firebase, Supabase, local assets).
- Cross-cutting concerns like analytics, premium state, and remote config are initialized at application level.

```mermaid
graph LR
UI["UI Layer<br/>Compose Screens"] --> VM["ViewModel Layer<br/>StateFlow + Coroutines"]
VM --> Repo["Repository Layer<br/>Business Logic"]
Repo --> Data["Data Layer<br/>Room + Firebase + Supabase + Assets"]
Repo --> Auth["AuthManager<br/>Firebase Auth State"]
Repo --> Sync["SyncManager<br/>WorkManager + Firestore"]
Repo --> Market["MarketRepository<br/>Supabase + Room"]
Repo --> Knowledge["KnowledgeBaseRepository<br/>Local JSON"]
```

**Diagram sources**
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [MarketRepository.kt:16-78](file://app/src/main/java/com/giathinh/canlua/repository/MarketRepository.kt#L16-L78)
- [KnowledgeBaseRepository.kt:10-105](file://app/src/main/java/com/giathinh/canlua/repository/KnowledgeBaseRepository.kt#L10-L105)

**Section sources**
- [CardListViewModel.kt:24-169](file://app/src/main/java/com/giathinh/canlua/ui/viewmodel/CardListViewModel.kt#L24-L169)
- [CardRepository.kt:26-397](file://app/src/main/java/com/giathinh/canlua/repository/CardRepository.kt#L26-L397)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)

## Performance Considerations
- Room database uses WRITE_AHEAD_LOGGING journal mode for concurrent read/write performance.
- Network requests include retry interceptor with exponential backoff for transient failures.
- MarketRepository implements TTL-based caching to reduce API calls.
- SyncManager batches Firestore operations to stay within limits and reduces network overhead.
- ViewModel flows use SharingStarted.WhileSubscribed to avoid unnecessary recomposition when not observed.
- PerformanceTracker monitors screen transitions and frame rates for optimization insights.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
Common issues and their resolutions:
- Authentication state inconsistencies: Ensure AuthManager.authStateFlow is properly subscribed and that ViewModels handle null states during transitions.
- Sync failures: Check network connectivity, Wi-Fi-only settings, and WorkManager constraints. Review analytics logs for sync failure stages.
- Room migration errors: Verify migration chain completeness and test schema changes thoroughly.
- Offline behavior: Confirm that UI reads from Room first and gracefully degrades when network is unavailable.
- Role-based routing: Validate profile setup completion and ensure role checks are consistent across screens.

**Section sources**
- [AuthManager.kt:12-76](file://app/src/main/java/com/giathinh/canlua/repository/AuthManager.kt#L12-L76)
- [SyncManager.kt:35-557](file://app/src/main/java/com/giathinh/canlua/repository/SyncManager.kt#L35-L557)
- [AppDatabase.kt:84-398](file://app/src/main/java/com/giathinh/canlua/data/database/AppDatabase.kt#L84-L398)

## Conclusion
Cân Lúa Mobile implements a robust MVVM + Clean Architecture with unidirectional data flow, offline-first strategy, and role-based access control. The separation of concerns through ViewModels and repositories enables maintainable code, while Hilt provides clean dependency injection. Room ensures reliable local persistence with comprehensive migrations, and WorkManager guarantees resilient background synchronization. The architecture supports scalability, testability, and a smooth user experience across different roles and network conditions.

[No sources needed since this section summarizes without analyzing specific files]