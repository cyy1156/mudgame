## 类与函数说明（最终版）

本文件按模块解释每个类的**职责、关键函数、使用的数据结构**。  
重点：地图系统使用“图（Graph）”结构表达场景连接关系。

### 1) 数据结构总览（你用到了哪些“结构”）
- **图（Graph）/邻接结构（地图）**：`MapManager.SceneNode` 用 `north/south/east/west` 指针表达邻接关系（等价于有向图/无向图的邻接表表示）。
- **映射 Map**：
  - `ConcurrentHashMap<String, ClientHandler>`：在线玩家名 → 连接处理器（线程安全）。
  - `HashMap<String, MapManager.SceneNode>`：场景名 → 地图节点（快速定位）。
  - `HashMap<String, Scene>`：本地单机版 `SceneManager` 用来注册/查找场景。
- **线性表 List**：
  - `List<Npc>`：NPC 池（随机抽取）。
  - `List<Scene>`：场景列表（给部分逻辑或展示使用）。
  - `List<Skill>`：角色技能列表。
  - `List<String>`：场景可用操作（命令白名单）、NPC 选项列表。
- **二维数组（网格坐标）**：`SceneNode[][] mapGrid` 保存地图坐标布局（用于生成地图显示）。
- **随机数 Random**：怪物生成、NPC随机、修炼奖励随机。
- **IO 流**：
  - `BufferedReader`：读网络/读文件。
  - `PrintWriter`：写网络/写文件。
- **Socket**：`ServerSocket` 监听，`Socket` 连接。
- **JSON（Gson）**：序列化/反序列化角色和 NPC 配置。

---

## 2) 网络版核心类（推荐重点讲）

### `src/com/mudgame/NetworkGameServer.java`
**职责**：服务器主程序；监听连接；维护全局数据（在线玩家、NPC、场景、地图、修炼台）；对外提供广播、随机NPC/怪物等能力。  
**关键字段/结构**：
- `ConcurrentHashMap<String, ClientHandler> clients`：在线玩家表（线程安全 Map）。
- `List<Npc> npcs`：NPC 池。
- `List<Scene> scenes`：场景列表（由 `MapManager` 初始化后收集）。
- `MapManager mapManager`：地图系统（图结构）。
- `TrainingAltar trainingAltar`：修炼台（共享资源/互斥）。
**关键函数**：
- `start()`：启动服务器，`accept()` 循环接入新客户端并创建 `ClientHandler` 线程。
- `loadNpcs()`：从 `npcs.json` 加载 NPC（失败则用默认 NPC）。
- `initializeScenes()`：从 `mapManager.getAllScenes()` 收集场景。
- `registerClient(...) / removeClient(...)`：加入/离开时更新 `clients` 并广播。
- `broadcastMessage(from, message, exclude)`：全服广播（带时间戳）。
- `generateMonster()`：生成随机怪物。
- `getRandomNpc()`：随机 NPC（从 `npcs` 列表中抽取）。
- `getMapManager()` / `getTrainingAltar()`：向 `ClientHandler` 提供地图/修炼台引用。

### `ClientHandler`（在 `NetworkGameServer.java` 内部类）
**职责**：一个客户端连接对应一个处理器线程；解析命令并调用业务逻辑；维护该玩家会话状态（是否战斗/PK，当前场景等）。  
**关键字段/结构**：
- `BufferedReader in` / `PrintWriter out`：网络 IO。
- `Figure player`：玩家实体（服务器端权威状态）。
- `MapManager.SceneNode currentScene`：当前位置（图节点）。
- `pendingChallenge`、`isInPvP`、`isInBattle`：会话状态。
**关键函数（按“功能块”讲就够）**：
- `run()`：登录流程（选角色→输入名→分配起始场景），随后读循环 `handleCommand()`。
- `handleCommand(String)`：命令路由；对打怪/NPC/PK 做**场景白名单校验**。
- `showMainMenu()`：输出地图（`mapManager.generateMapDisplay(currentScene)`）+ 可用操作菜单。
- **PVE**：`startBattle()` → `NetworkBattleSystem.fight(...)`。
- **NPC**：`talkToNpc()`：展示对话/选项→读取答案→校验→发经验。
- **存档**：`saveGame()`（写 `save_<玩家名>.json`）、`loadGame()`（读并恢复 `Figure`）。
- **在线与聊天**：`listPlayers()`、广播聊天。
- **PK**：`challengePlayer()`、`acceptChallenge()` → `NetworkBattleSystem.playerVsPlayer(...)`。
- **移动**：`changeScene()`：方向移动（调用 `mapManager.move(...)`），支持 `map`/`list`。
- **修炼台**：`useTrainingAltar()`：尝试获取互斥资源→执行修炼→释放资源。

### `src/com/mudgame/NetworkGameClient.java`
**职责**：纯客户端；建立 TCP 连接；读线程打印服务端消息；主线程读取用户输入并发送。  
**关键函数**：
- `connect(serverAddress, port)`：连接服务器 + 启动读线程 + 循环发送用户命令。

### `src/com/mudgame/NetworkBattleSystem.java`
**职责**：网络战斗系统（PVE/PVP）；从 `ClientHandler` 的输入流读取战斗指令；输出战斗过程。  
**关键函数**：
- `fight(Figure, Monster, ClientHandler, NetworkGameServer)`：PVE 回合制；仅接受 11/12。
- `playerVsPlayer(Figure, Figure, ClientHandler, ClientHandler, NetworkGameServer)`：PVP 回合制；状态锁定与结算。
**关键点**：使用自定义异常 `MissCannarinException` 限制输入范围，避免协议混乱。

### `src/com/mudgame/JsonUtil.java`
**职责**：JSON 序列化/反序列化工具（Gson）；负责把 `Figure` / NPC 配置转换为对象或 JSON。  
**关键函数**：
- `figureToJson(Figure)` / `figureToJson(Figure, roleType)`：角色 → JSON（通过 DTO 适配字段）。
- `figureFromJson(String)`：JSON → `Figure`（根据 `roleType` 构造 `Mo` 或 `XiaoYan`，恢复技能列表）。
- `loadNpcsFromFile(String)`：读 `npcs.json` → `List<Npc>`（支持 options 选择题）。

### `src/com/mudgame/TrainingAltar.java`
**职责**：修炼台共享资源（互斥访问）；答题→修炼→随机奖励；全服广播。  
**关键函数**：
- `tryStartTraining(playerName)`：`synchronized` 互斥获取。
- `endTraining()`：释放占用。
- `isOccupied()` / `getCurrentUser()`：状态查询。
- `performTraining(player, in, out, server, playerName)`：问答、模拟修炼、随机奖励（攻击/防御/经验升级）并广播。

### `src/com/mudgame/MapManager.java`
**职责**：地图/场景关系管理与地图渲染。  
**“图结构”说明（重点）**：
- 每个场景是一个**图节点**：`SceneNode`。
- 节点通过 `north/south/east/west` 指向相邻节点，相当于**邻接表（Adjacency List）**的一种实现。
- 通过 `sceneMap(name->node)` 实现 O(1) 查找节点；通过 `mapGrid[y][x]` 保存坐标布局用于渲染。
**关键函数**：
- `getStartScene()`：起始场景节点（新手村）。
- `getAllScenes()`：返回所有节点集合（服务器初始化场景列表使用）。
- `generateMapDisplay(currentScene)`：输出地图（ASCII），并标记当前位置与方向。
- `move(current, direction)`：根据方向（n/s/e/w 或 上下左右）返回相邻节点。
- `generateMapLegend()`：输出“场景说明”列表。

---

## 3) 游戏实体类（服务器端权威数据）

### `src/com/mudgame/Figure.java`
**职责**：角色基类（HP、等级、攻防、经验、技能列表）。  
**关键数据结构**：`List<Skill> skills`。  
**关键函数**：
- `takeDamage(...)` / `takeDamageNetwork(...)`：受伤结算（含最小伤害 1）。
- `gainExp(int)`：加经验，满 100 自动升级（私有 `Upgrade()`）。
- `healToFull()`：战斗后回血。
- `learnSkill(Skill)`：学习技能。
- `addAttack(int)` / `addDefend(int)`：修炼台加成用。
- 一系列 getter：`getAttack/getDefend/getGrade/...`

### `src/com/mudgame/XiaoYan.java`、`src/com/mudgame/Mo.java`
**职责**：两种角色的派生类；初始化角色故事与初始技能。  
**关键函数**：
- `initialise()`：根据等级（10/20）学习新技能（目前只在构造时触发，升级后不会自动补发技能）。
> 备注：`Mo.java` 源码存在编码乱码，但 class 文件可用；如需完全修复可统一转 UTF-8。

### `src/com/mudgame/Monster.java`
**职责**：怪物实体；构造时随机生成怪物类型、等级与属性。  
**关键函数**：
- `Monster()`：随机怪物名 → 推导等级区间 → 随机属性（攻防血/经验）。
- `takeDamage(...)` / `takeDamageNetwork(...)`：受伤。
- getter：`getName/getGrade/getAttack/getDefend/getExp/getLifeValue`。

### `src/com/mudgame/Npc.java`
**职责**：NPC 实体；支持“选项题/文字题”两种答题方式。  
**关键数据结构**：`List<String> options`。  
**关键函数**：
- `checkAnswer(input)`：如果输入数字则映射到 `options`；否则直接字符串比对。
- `hasOptions()`：是否为选择题。
- getter：`getName/getQuestion/getAnswer/getExp/getDialogue/getOptions`。

### `src/com/mudgame/Skill.java`
**职责**：技能（名称、伤害倍率、描述）。  
**关键函数**：
- `calculateDamage(playerAttack)`：伤害 = 攻击 × 倍率。
- getter：`getName/getDamageMultiplier/getDescription`。

### `src/com/mudgame/Scene.java`
**职责**：场景数据（名称、描述、允许的操作集合）。  
**关键数据结构**：`List<String> availableActions`（命令白名单，如 "1","2","8"...）。  
**关键函数**：
- `addAction(String)`：声明该场景允许的命令。
- `getAvailableActions()`：给 `ClientHandler` 做场景限制校验。

---

## 4) 本地单机版（与网络版并存，但网络版不依赖）
这些类用于 `Game.java` 入口的单机交互，和网络版不是同一套流程：
- `Game.java`：单机入口，菜单循环。
- `SceneManager.java`：`Map<String, Scene>` 注册与进入场景。
- `BattleSystem.java`：单机战斗系统（Scanner 输入）。
- `BattleScene.java` / `NpcScene.java`：单机场景实现（重写 `enter(...)`）。
- `FileHandler.java`：单机 txt 存档（固定路径 `save.txt`）。


