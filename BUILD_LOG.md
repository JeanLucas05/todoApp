# BUILD_LOG — To-Do App (Kotlin Multiplatform)

Este arquivo preserva o histórico real de desenvolvimento do projeto. Cada entrada
descreve um pedido/decisão/ação relevante. Entradas antigas não são reescritas.

---

## Entrada 1 — Início do projeto e definição do stack

### Prompt / Request
> "e em kotlin multiplataform eu quero que você desenvolva: [especificação completa do
> To-Do App — SQLite, múltiplas telas, categorias, filtros, data/hora opcional e
> notificações locais]"

### Decision Summary
Decidi implementar o app como um projeto **Kotlin Multiplatform (KMP) usando Compose
Multiplatform** com UI compartilhada em `commonMain`, alvos **Android** e **iOS**.

Principais escolhas:
- **Linguagem/UI**: Kotlin Multiplatform + Compose Multiplatform 1.6.11 (Kotlin 2.0.21).
- **Persistência**: SQLDelight 2.0.2 (SQLite real e tipado, multi-plataforma), com drivers
  nativos por alvo (`android-driver` e `native-driver`).
- **Notificações**: `expect`/`actual` — Android usa `AlarmManager` + `NotificationManager`,
  iOS usa `UNUserNotificationCenter`.
- **Estado**: arquitetura simples MVVM com `ViewModel` multiplataforma
  (JetBrains lifecycle 2.8.4) + `StateFlow` + `collectAsStateWithLifecycle`.
- **Navegação**: navegação baseada em estado (sealed class `Screen`) — simples e
  determinística, adequada a um app educacional. A estratégia completa está documentada
  mais adiante neste log.
- **Datas**: `kotlinx-datetime 0.5.0` (compatível com Kotlin 2.0.21); datas/horas são
  persistidas como epoch milissegundos.
- **Versões**: AGP 8.5.2, compileSdk/targetSdk 34, minSdk 24, Gradle 8.9.

Alternativas consideradas:
- Room/SQLDelight: escolhido SQLDelight por ser SQLite multi-plataforma (funciona igual em
  iOS e Android) sem duplicação de DAO.
- Biblioteca de navegação multiplataforma (ex.: `navigation-compose` da JetBrains):
  evitada por ser mais uma dependência frágil em versão alfa no ciclo do CMP 1.6; app
  pequeno, navegação por estado é transparente e testável.

Suposição importante: o ambiente de desenvolvimento é Linux, sem Xcode/macOS. O alvo iOS
será implementado e documentado, mas **não poderá ser compilado neste ambiente** (limitação
documentada na seção de revisão final).

### Actions Performed
- Criado projeto em `/home/alunoifpe/todo-app` com estrutura KMP + Compose Multiplatform.
- Arquivos de build criados: `settings.gradle.kts`, `build.gradle.kts`,
  `gradle/libs.versions.toml`, `gradle.properties`, `local.properties`, `.gitignore`.
- Gerado Gradle wrapper 8.9.

### Result
- Estrutura do projeto criada; wrapper gerado com sucesso (BUILD SUCCESSFUL).

### Problems / Errors
- Nenhum até o momento.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 2 — Modelos de domínio (Task, Category) e filtros

### Prompt / Request
> Requisitos das seções 3 e 4 da spec: "A task must contain at least: id, title,
> description, completed, dueDateTime, createdAt, categoryId" e "A category must contain
> at least: id, name".

### Decision Summary
Criei os modelos `Task` e `Category` em `br.edu.ifpe.todo.domain`, mais um enum
`TaskStatusFilter` (ALL / PENDING / COMPLETED).

Campos adicionais (permitidos pela spec, documentados aqui):
- `Task.categoryName` é um campo transient/preenchido apenas para exibição (join feito em
  memória no repositório), evitando uma consulta SQL de JOIN complexa no SQLDelight.
- Datas armazenadas como `Long` (epoch millis) — `dueAtMillis` e `createdAt`.

Decisão: **não** persiste um `notificationId` separado; o identificador de notificação é
derivado do próprio `task.id` (estável ao longo de re-agendamentos). Isso simplifica o
esquema sem perder nada.

### Actions Performed
- Criado `composeApp/src/commonMain/kotlin/br/edu/ifpe/todo/domain/Models.kt`.

### Result
- Modelos definidos.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 3 — Schema SQLite (SQLDelight) e driver multi-plataforma

### Prompt / Request
> Seção 5 da spec: "Use SQLite for local persistence. At minimum, the database must
> persist: tasks; categories. Data must remain available after... closing the app;
> reopening; restarting the emulator or device."

### Decision Summary
Criei o schema `Todo.sq` em `src/commonMain/sqldelight/br/edu/ifpe/todo/data/db` (SQLDelight
gera um banco tipado e multiplataforma a partir dele).

Estrutura:
- `category(id INTEGER PK AUTOINCREMENT, name TEXT NOT NULL)`
- `task(id INTEGER PK AUTOINCREMENT, title TEXT NOT NULL, description TEXT,
  completed INTEGER NOT NULL DEFAULT 0, due_at INTEGER, created_at INTEGER NOT NULL,
  category_id INTEGER REFERENCES category(id) ON DELETE SET NULL)`

Decisões:
- `completed` é armazenado como INTEGER 0/1 (convenção SQLDelight).
- `category_id` usa **`ON DELETE SET NULL`**: ao deletar uma categoria em uso, as tarefas
  ficam automaticamente **sem categoria** (resposta à pergunta aberta da seção 6).
- Os drivers são criados por classes concretas por plataforma que implementam a interface
  `DatabaseDriverFactory` (em vez de `expect class` com construtor diferente por
  plataforma): Android usa `AndroidSqliteDriver(context, ...)`, iOS usa `NativeSqliteDriver`.
- Inicialização centralizada em um `AppContainer` em `commonMain`, alimentado no entry point
  de cada plataforma.

### Actions Performed
- Criado `Todo.sq`.
- Criado `data/db/DatabaseDriverFactory.kt` (interface) em `commonMain`.
- Criado `AppContainer.kt` (container/DI simples).

### Result
- Schema e camada de driver definidos.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 4 — Repositório (CRUD de tarefas e categorias, Flows reativos)

### Prompt / Request
> Seções 12 e 16 da spec: filtragem por status/categoria e "The application must
> correctly refresh the UI after actions such as: creating, editing, deleting,
> completing a task, changing a filter, modifying categories."

### Decision Summary
Criei `TodoRepository` em `commonMain`. Estratégia de estado:
- O repositório expõe **Flows reativos**: `observeTasks()` e `observeCategories()` usam
  `QueryFlow` (`.asFlow().mapToList()`) do SQLDelight, que notifica automaticamente toda
  mudança de tabela. UI sempre reflete o banco (não precisa re-buscar manualmente).
- Filtragem feita **em memória** nos ViewModels a partir da lista completa
  (permitido pela spec), mantendo a query simples.
- O `categoryName` é enriquecido no fluxo combinando tarefas e categorias.

### Actions Performed
- Criado `data/TodoRepository.kt` com: insert/update/setCompleted/delete de tarefas,
  insert/rename/delete de categorias, `fetchTask(id)`.

### Result
- Camada de dados reativa implementada.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 5 — Notificações locais (expect/actual) e coordenador de lembretes

### Prompt / Request
> Seções 10 e 11 da spec: "Use scheduled LOCAL notifications", comportamento para
> task criada/alterada/sem prazo/concluída/reaberta/deletada, e "handle denial
> gracefully; do not crash if permission is denied."

### Decision Summary
Estratégia de notificações:
- `expect class ReminderScheduler` em `commonMain` com operações `initialize()`,
  `ensurePermission(onResult)`, `schedule(taskId, taskTitle, dueAtMillis)` e
  `cancel(taskId)`.
- **Android (actual)**: canal de notificação "reminders"; agendamento via
  `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, ...)` apontando para
  `ReminderReceiver` (BroadcastReceiver) que publica a notificação; cancelamento com
  `alarmManager.cancel()` usando o mesmo PendingIntent (requestCode derivado do `task.id`).
  Permissão: no Android 13+ solicita `POST_NOTIFICATIONS` via Activity registrada; se negada,
  o agendamento não quebra (a notificação apenas não exibe). Conflito com `SCHEDULE_EXACT_ALARM`
  negado no Android 14+: captura `SecurityException` e cai para `setWindow()` (aproximado).
- **iOS (actual)**: `UNUserNotificationCenter` com
  `requestAuthorizationWithOptions` (permissão) e `UNCalendarNotificationTrigger`
  (agendamento por data local), identificador `task_<id>`.
- **`ReminderCoordinator`** (commonMain) centraliza a lógica de negócio:
  - criar/editar com prazo futuro → `schedule`;
  - data alterada → como `schedule` usa o mesmo id, o alarme é substituído;
  - prazo removido/concluída/deletada → `cancel`;
  - reaberta (pending) com prazo futuro → `schedule` de novo;
  - tarefa sem prazo → nada é agendado.

### Actions Performed
- Criado `notifications/ReminderScheduler.kt` (expect, commonMain).
- Criado `notifications/ReminderCoordinator.kt` (commonMain).
- Criado `notifications/ReminderScheduler.android.kt`, `ReminderReceiver.kt` e
  `NotificationUtils.android.kt` (androidMain).
- Criado `notifications/ReminderScheduler.ios.kt` (iosMain).
- Criado `AndroidAppContextHolder.kt` (androidMain) com referência ao Application Context e
  à Activity ativa (para permissões).

### Result
- Camada de notificações implementada (Android testável; iOS implementado, não compilado).

### Problems / Errors
- Nenhum (revisão do código Android será feita no build).

### Fixes Attempted
- N/A.

### Current Status
- Completed (aguardando build)

---

## Entrada 6 — Formatação de datas por plataforma (expect/actual)

### Prompt / Request
> Seção 9 da spec e requisitos de exibição de "due date/time" na lista de tarefas.

### Decision Summary
`util/DateFormatters.kt` em `commonMain` com `expect fun formatDateTime(millis: Long): String`.
- Android: `SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())`.
- iOS: `NSDateFormatter` com `dateStyle`/`timeStyle` curtos.

Além disso, helpers puros em `util/TaskDueUtils.kt` (commonMain) para:
- converter `LocalDate` ↔ milissegundos UTC (usando `TimeZone.UTC`) — compatível com a
  semântica do `Material3 DatePicker`;
- combinar `LocalDate` + `LocalTime` → `Instant` no fuso do dispositivo;
- política de prazo sem hora explícita: **hora padrão 23:59** do dia selecionado.
- `shouldScheduleReminder(dueAtMillis, completed)`.

### Actions Performed
- Criado `util/DateFormatters.kt` e `util/TaskDueUtils.kt`.
- Criados os `actual`s de formatação em `androidMain` e `iosMain`.

### Result
- Formatação e conversão de datas implementadas.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 7 — ViewModels e estado (MVVM)

### Prompt / Request
> Seções 15 e 16 da spec (arquitetura e gerenciamento de estado).

### Decision Summary
Três ViewModels em `commonMain`, usando `ViewModel` + `viewModelScope` multiplataforma:
- `TaskListViewModel`: expõe `uiState` (`Flow<TaskListUiState>`) combinando tasks, categories
  e os filtros selecionados; ações: `setStatusFilter`, `setCategoryFilter`, `toggleCompleted`.
- `TaskEditorViewModel(taskId, repository)`: campos do formulário, validação de título
  obrigatório, `save` (insert ou update + coordenação de notificação), `deleteTask`.
- `CategoriesViewModel`: lista, adicionar, renomear, excluir categorias (confirmação no UI).

Estado da UI lido com `collectAsStateWithLifecycle()` nas telas; criação via
`viewModel(factory = viewModelFactory { initializer { ... } })`.

### Actions Performed
- Criado `ui/tasklist/TaskListViewModel.kt`, `ui/taskeditor/TaskEditorViewModel.kt`,
  `ui/categories/CategoriesViewModel.kt`.

### Result
- Camada de estado implementada.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed

---

## Entrada 8 — Telas Compose e navegação

### Prompt / Request
> Seções 6 e 7 da spec (três telas obrigatórias; "Choose an appropriate strategy for
> transferring data" ao abrir uma tarefa existente).

### Decision Summary
- **Navegação**: `App.kt` mantém `mutableStateOf<Screen>` onde
  `Screen = TaskList | TaskEditor(taskId: Long?) | Categories`. Abrir tarefa existente
  transfere **apenas o `id`** (estratégia documentada na spec); o editor recarrega a tarefa
  do repositório. `taskId == null` significa criação.
- **Tela 1 — Task List** (`TaskListScreen`): TopAppBar "Tarefas" com ação "Categorias";
  filtro de status via `SingleChoiceSegmentedButtonRow` (Todas/Pendentes/Concluídas);
  filtro de categoria via `FilterChip`s horizontais; FAB "Nova tarefa"; cada item exibe
  título, estado (Checkbox), categoria e prazo formatado; toque abre o editor.
- **Tela 2 — Task Editor** (`TaskEditorScreen`): título (obrigatório), descrição multilinha,
  seletores de data e hora (M3 `DatePickerDialog` / `TimePickerDialog`), "Limpar prazo",
  categoria (dropdown), checkbox "Concluída"; ações Cancelar/Salvar (desabilitado sem título)
  e Excluir (com diálogo de confirmação) quando editando.
- **Tela 3 — Categorias** (`CategoriesScreen`): lista de categorias com renomear (diálogo)
  e excluir (diálogo de confirmação avisando que as tarefas ficarão sem categoria) e botão
  "Nova categoria".

### Actions Performed
- Criado `ui/App.kt` e as três telas + um componente compartilhado
  (`ui/components/SharedComponents.kt`).

### Result
- Todas as telas e a navegação implementadas.

### Problems / Errors
- Nenhum até o build.

### Fixes Attempted
- N/A.

### Current Status
- Completed (aguardando build)

---

## Entrada 9 — Entry points Android e iOS

### Prompt / Request
> Inicialização multi-plataforma (banco, canal de notificação) a partir do entry point nativo.

### Decision Summary
- Android: `MainActivity` (Activity Compose) chama `initializeApp(context)` antes do
  `setContent` (cria driver, inicializa `AppContainer`, cria canal de notificação e
  registra a Activity para permissões).
- iOS: `MainViewController()` (função exportada ao framework) inicializa o
  `AppContainer` com o `IosDatabaseDriverFactory` e exibe `App()` via `ComposeUIViewController`.
- `AndroidManifest.xml` declara `POST_NOTIFICATIONS` e `SCHEDULE_EXACT_ALARM`, a `MainActivity`
  (exported, launcher) e o `ReminderReceiver` (não exportado).

### Actions Performed
- Criados `MainActivity.kt`, `AndroidManifest.xml` (androidMain).
- Criado `MainViewController.kt` (iosMain).
- Criado projeto `iosApp/` (Swift + Info.plist + Xcode project) para execução do framework.

### Result
- Entry points definidos.

### Problems / Errors
- Nenhum.

### Fixes Attempted
- N/A.

### Current Status
- Completed (aguardando build)

---

## Entrada 10 — Erros do primeiro build (Android) e correções

### Prompt / Request
Validação obrigatória da spec (seção 19): "Build the project; inspect compiler
output; fix errors found."

### Decision Summary
Rodar `./gradlew :composeApp:assembleDebug`. Encontrados e corrigidos
incrementalmente os seguintes problemas:

1. **SQLDelight 2.x movido de group**: os runtimes não são mais
   `com.squareup.sqldelight:*` 2.0.x (só 1.5.5 existe nesse grupo). Em 2.x o
   groupId é `app.cash.sqldelight`. Corrigido o `libs.versions.toml`.
2. **`kotlin.time.Instant/Clock` não existem no Kotlin 2.0.21**: com
   kotlinx-datetime 0.5.0 usar `kotlinx.datetime.Instant` e `kotlinx.datetime.Clock`.
3. **SQLDelight 2.0.2 API de Flows**: `Query.asFlow()` retorna
   `Flow<Query<T>>` (o nome JVM é `toFlow`); `mapToList(...)` **exige** um
   `CoroutineContext`. Troquei `.asFlow().mapToList()` por
   `.asFlow().mapToList(Dispatchers.Default)` no `TodoRepository`.
4. **Queries geradas**: o objeto de queries gerado pelo SQLDelight é
   `database.todoQueries` (nome do banco), não `taskQueries`/`categoryQueries`.
5. **Inserts retornam `Unit` no SQLDelight 2.x** (sem auto `rowid`):
   adicionada a query `selectLastInsertRowid()` (via `last_insert_rowid()`) e o
   repositório busca o id logo após o INSERT.
6. **Ícone de notificação**: `android.R.drawable.stat_notify_clock` e
   `ic_lock_idle_alarm_alpha` não existem como recursos públicos; existe
   `ic_lock_idle_alarm`. Corrigido no `ReminderReceiver`.
7. **APIs M3 experimentais**: `FilterChip`/`SegmentedButton`/`TopAppBar` exigem
   `@OptIn(ExperimentalMaterial3Api::class)` também nas funções privadas.
8. **`kotlinx.datetime`**: `LocalDate.month.number` não existe; usar
   `LocalDate.monthNumber`.

### Actions Performed
- Modificado `gradle/libs.versions.toml`, `Todo.sq`, `TodoRepository.kt`,
  `ReminderReceiver.kt`, `TaskEditorScreen.kt`, `TaskListScreen.kt`,
  `CategoriesScreen.kt`, `ReminderScheduler.android.kt`,
  `TaskDueUtils.kt`, `ReminderCoordinator.kt`, `TaskEditorViewModel.kt`.

### Result
- `assembleDebug` PASSED. APK gerado em
  `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (~8.9 MB).

### Problems / Errors
- Listados acima (8 problemas de compilação).

### Fixes Attempted
- Todos os 8 corrigidos na ordem em que o compilador os revelou.

### Current Status
- Completed

---

## Entrada 11 — Validação em emulador Android

### Prompt / Request
> Seção 19/20 da spec (validação e critérios funcionais): "Do not assume that
> generated code is correct... test important user flows."

### Decision Summary
Para validar de verdade o app, baixei e instalei o emulador Android
(`emulator` + `system-images;android-34;google_apis;x86_64`) e criei um AVD
(`todo_avd`, Pixel 5).
- Nesta máquina o usuário não tem permissão de KVM (sem sudo para entrar no
  grupo `kvm`), então o emulador roda com **aceleração por software (TCG)** —
  boot lento, mas funcional para testes manuais via ADB.

### Actions Performed
- Instalados pacotes `emulator` e `system-images;android-34;google_apis;x86_64`.
- Criado AVD `todo_avd`; emulador iniciado em modo headless.

### Result
- Emulador no ar (device `emulator-5554`); aguardando boot completo para instalar
  o APK e testar os fluxos.

### Problems / Errors
- `ProbeKVM: This user doesn't have permissions to use KVM` — sem sudo, sem
  aceleração.

### Fixes Attempted
- Rodar com `-accel off` (TCG). Boot lento, porém viável.

### Current Status
- In progress

---

## Entrada 12 — Alvo físico (celular via depuração) e Android Studio

### Prompt / Request
> "prossiga com o projeto todo app eu quero usar ele no android studio usando um
> celular com depuracao usb, leia os requisitos no markdown e veja o que ta faltando"

### Decision Summary
Usuário quer rodar/app no **celular físico** via Android Studio. Auditoria do estado
revelou e resolveu:

1. **Build**: `assembleDebug` validado novamente — PASSED, APK 8,9 MB em
   `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
2. **Ambiente sem USB**: esta máquina é uma VM **sem controlador USB** (`/sys/bus/usb`
   ausente; apenas `vmbus`/`virtio`/`xen`/`pci`). USB físico não é visível. Decisão:
   usar **ADB over Wi-Fi** (Android 11+, "Wireless debugging") — não depende de USB e
   o Android Studio conecta normalmente.
3. **Android Studio**: instalado em `/home/alunoifpe/tools/android-studio`
   (Quail 4 | 2026.1.4, JDK embarcado 25). Como AGP 8.5.2/Gradle 8.9 exigem JDK 17,
   forçado `org.gradle.java.home=/home/alunoifpe/tools/jdk-17.0.20.1+1` em
   `~/.gradle/gradle.properties` (nível usuário, não polui o projeto).
4. **PATH/ferramentas**: adicionado ao `.bashrc` `ANDROID_HOME`, `JAVA_HOME`,
   `platform-tools`, `emulator` e alias `studio`. Servidor adb ativo.
5. **udev**: regras Android preparadas em `/tmp/opencode/51-android.rules` + script
   `setup_usb_android.sh` (exigem `sudo`; sem utilidade nesta VM sem USB, mas prontas).
6. **Rede**: IP da máquina `172.21.123.87` (LAN da VM). Para `adb connect` funcionar,
   celular e esta máquina precisam se alcançar na rede (mesma LAN/roteador).

### Actions Performed
- Baixado/extraído Android Studio tar.gz oficial (download de ~1,5 GB).
- Criado `~/.gradle/gradle.properties` forçando JDK 17 para o Gradle.
- `.bashrc`: PATH de SDK/JDK/adb/emulator e alias `studio`.
- Confirmado Gradle 8.9 rodando em JDK 17 (`Launcher JVM`/`Daemon JVM`).

### Result
- Android Studio pronto para abrir `todo-app`; APK compilável; adb operante;
  roteiro de conexão Wi-Fi documentado (Android 11+).

### Problems / Errors
- JDK embarcado do Android Studio 2026 é 25 — incompatível com Gradle 8.9/AGP 8.5.2.
  Resolvido com `org.gradle.java.home` (JDK 17 local).
- Sem USB na VM: depuração USB física não é possível neste host.

### Fixes Attempted
- N/A (problemas acima resolvidos na própria entrada).

### Current Status
- Completed