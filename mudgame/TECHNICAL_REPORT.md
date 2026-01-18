<<<<<<< HEAD
# MUD武侠游戏网络版 - 技术报告

> **项目名称**：MUD武侠游戏网络版  
> **开发语言**：Java  
> **网络协议**：TCP/IP  
> **文档版本**：v1.0  
> **更新日期**：2025-01-08

---

## 目录

1. [系统概述](#1-系统概述)
2. [系统架构](#2-系统架构)
3. [核心数据结构](#3-核心数据结构)
4. [类设计与功能说明](#4-类设计与功能说明)
5. [关键流程分析](#5-关键流程分析)
6. [并发与同步机制](#6-并发与同步机制)
7. [总结](#7-总结)

---

## 1. 系统概述

### 1.1 项目背景

本项目是一款基于文本的多人在线角色扮演游戏（MUD，Multi-User Dungeon），采用经典的 Client-Server 架构，支持最多 5 个玩家同时在线。玩家可以在虚拟武侠世界中探索场景、与 NPC 对话、挑战怪物、与其他玩家 PK，并通过修炼台提升自身属性。

### 1.2 功能特性

| 功能模块 | 描述 |
|---------|------|
| 多人在线 | 支持最多 5 个客户端同时连接 |
| 角色系统 | 两种角色（萧言/莫），各有独特技能 |
| 场景系统 | 5 个场景，使用**图结构**管理场景连接 |
| 战斗系统 | PVE（打怪）和 PVP（玩家对战） |
| NPC 对话 | 选择题模式，答对获得经验 |
| 修炼台 | 共享资源，同时仅一人可用（互斥锁） |
| 存档系统 | JSON 格式持久化存储 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              系统架构图                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────────┐      TCP/IP       ┌──────────────────────────────┐  │
│   │   Client 1   │◄─────────────────►│                              │  │
│   └──────────────┘                   │                              │  │
│                                      │                              │  │
│   ┌──────────────┐      TCP/IP       │     NetworkGameServer        │  │
│   │   Client 2   │◄─────────────────►│                              │  │
│   └──────────────┘                   │  ┌────────────────────────┐  │  │
│                                      │  │ ClientHandler (线程池) │  │  │
│   ┌──────────────┐      TCP/IP       │  │  - handler1            │  │  │
│   │   Client 3   │◄─────────────────►│  │  - handler2            │  │  │
│   └──────────────┘                   │  │  - handler3...         │  │  │
│         ...                          │  └────────────────────────┘  │  │
│   ┌──────────────┐      TCP/IP       │                              │  │
│   │   Client N   │◄─────────────────►│  ┌────────────────────────┐  │  │
│   │  (max=5)     │                   │  │ 游戏实体管理            │  │  │
│   └──────────────┘                   │  │  - Figure (角色)       │  │  │
│                                      │  │  - Monster (怪物)      │  │  │
│                                      │  │  - Npc (NPC)           │  │  │
│                                      │  │  - Scene (场景)        │  │  │
│                                      │  └────────────────────────┘  │  │
│                                      │                              │  │
│                                      │  ┌────────────────────────┐  │  │
│                                      │  │ 核心管理器              │  │  │
│                                      │  │  - MapManager (地图)   │  │  │
│                                      │  │  - TrainingAltar (修炼)│  │  │
│                                      │  └────────────────────────┘  │  │
│                                      └──────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 层次 | 模块 | 职责 |
|------|------|------|
| 表示层 | NetworkGameClient | 用户交互、命令输入、结果展示 |
| 业务逻辑层 | ClientHandler | 命令解析、业务调度 |
| 业务逻辑层 | NetworkBattleSystem | 战斗逻辑（PVE/PVP） |
| 业务逻辑层 | TrainingAltar | 修炼台逻辑（共享资源管理） |
| 数据管理层 | MapManager | 地图/场景管理（图结构） |
| 数据管理层 | JsonUtil | 数据序列化/反序列化 |
| 实体层 | Figure, Monster, Npc, Skill, Scene | 游戏实体定义 |

---

## 3. 核心数据结构

### 3.1 数据结构总览

| 数据结构 | 使用位置 | 用途 |
|---------|---------|------|
| **图（Graph）** | MapManager | 场景连接关系（邻接表） |
| HashMap | NetworkGameServer, MapManager | 客户端映射、场景索引 |
| ConcurrentHashMap | NetworkGameServer.clients | 线程安全的客户端管理 |
| ArrayList | Figure.skills, Scene.availableActions | 技能列表、可用操作 |
| 二维数组 | MapManager.mapGrid | 地图坐标布局 |

### 3.2 图结构详解（重点）

#### 3.2.1 图的定义

在本项目中，**地图系统使用图（Graph）数据结构**来表示场景之间的连接关系：

- **顶点（Vertex）**：每个场景（SceneNode）
- **边（Edge）**：场景之间的通道（north/south/east/west 指针）
- **图类型**：**有向图**（从 A 能到 B，不代表从 B 能到 A，但本项目中大多为双向）

#### 3.2.2 图的实现方式：邻接表

```java
// MapManager.java 中的图结构实现

public class MapManager {
    // 顶点集合：使用 HashMap 存储所有场景节点
    // Key = 场景名称, Value = 场景节点
    private Map<String, SceneNode> sceneMap = new HashMap<>();
    
    // 场景节点定义（图的顶点）
    public static class SceneNode {
        public Scene scene;           // 场景数据
        public int x, y;              // 坐标（用于地图渲染）
        
        // 邻接关系（图的边）—— 使用指针/引用表示
        public SceneNode north;       // 北方相邻场景
        public SceneNode south;       // 南方相邻场景
        public SceneNode east;        // 东方相邻场景
        public SceneNode west;        // 西方相邻场景
    }
}
```

#### 3.2.3 图的可视化表示

```
                    ┌─────────┐
                    │ 新手村  │ (2,0)
                    └────┬────┘
                         │ south
                         ▼
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ 藏经阁  │◄───│武林广场 │───►│ 练功房  │
    │  (0,2)  │west│  (2,2)  │east│  (4,2)  │
    └─────────┘    └────┬────┘    └─────────┘
                        │ south
                        ▼
                   ┌─────────┐
                   │江湖客栈 │ (2,4)
                   └─────────┘

图例说明：
- 每个方框 = 图的顶点（SceneNode）
- 箭头 = 图的边（邻接关系）
- (x,y) = 节点坐标（用于地图渲染）
```

#### 3.2.4 图的核心操作

| 操作 | 方法 | 时间复杂度 | 说明 |
|------|------|-----------|------|
| 查找节点 | `getSceneNode(name)` | O(1) | HashMap 查找 |
| 移动（遍历边） | `move(current, direction)` | O(1) | 直接访问邻接指针 |
| 获取所有节点 | `getAllScenes()` | O(n) | 遍历 HashMap |
| 获取起始节点 | `getStartScene()` | O(1) | 返回"新手村"节点 |

#### 3.2.5 图结构的优势

1. **空间效率**：邻接表只存储实际存在的边，适合稀疏图
2. **查询效率**：O(1) 时间复杂度获取相邻场景
3. **扩展性强**：新增场景只需添加节点和边
4. **直观性**：north/south/east/west 语义清晰

---

## 4. 类设计与功能说明

### 4.1 类图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              类 图                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────┐         ┌──────────────────┐                     │
│  │ NetworkGameServer│◄────────│  ClientHandler   │                     │
│  │──────────────────│ 1    *  │──────────────────│                     │
│  │ -clients: Map    │         │ -socket: Socket  │                     │
│  │ -npcs: List      │         │ -player: Figure  │                     │
│  │ -mapManager      │         │ -currentScene    │                     │
│  │ -trainingAltar   │         │──────────────────│                     │
│  │──────────────────│         │ +run()           │                     │
│  │ +start()         │         │ +handleCommand() │                     │
│  │ +broadcastMsg()  │         │ +showMainMenu()  │                     │
│  └──────────────────┘         └────────┬─────────┘                     │
│           │                            │                                │
│           │ uses                       │ uses                           │
│           ▼                            ▼                                │
│  ┌──────────────────┐         ┌──────────────────┐                     │
│  │   MapManager     │         │NetworkBattleSystem│                    │
│  │──────────────────│         │──────────────────│                     │
│  │ -sceneMap: Map   │         │ +fight()         │                     │
│  │ -mapGrid[][]     │         │ +playerVsPlayer()│                     │
│  │──────────────────│         │ +useSkillPVP()   │                     │
│  │ +move()          │         └──────────────────┘                     │
│  │ +generateMap()   │                  │                                │
│  │ +getSceneNode()  │                  │ uses                           │
│  └────────┬─────────┘                  ▼                                │
│           │ contains          ┌──────────────────┐                     │
│           ▼                   │     Figure       │◄─────┐              │
│  ┌──────────────────┐         │──────────────────│      │ extends      │
│  │   SceneNode      │         │ -name, -grade    │      │              │
│  │──────────────────│         │ -attack, -defend │ ┌────┴─────┐        │
│  │ +scene: Scene    │         │ -skills: List    │ │ XiaoYan  │        │
│  │ +north,south,... │         │──────────────────│ │   Mo     │        │
│  │ +x, y            │         │ +takeDamage()    │ └──────────┘        │
│  └────────┬─────────┘         │ +gainExp()       │                     │
│           │ contains          │ +healToFull()    │                     │
│           ▼                   └──────────────────┘                     │
│  ┌──────────────────┐                                                  │
│  │     Scene        │         ┌──────────────────┐                     │
│  │──────────────────│         │    Monster       │                     │
│  │ -name            │         │──────────────────│                     │
│  │ -description     │         │ -name, -grade    │                     │
│  │ -availableActions│         │ -attack, -defend │                     │
│  │──────────────────│         │──────────────────│                     │
│  │ +addAction()     │         │ +takeDamage()    │                     │
│  │ +getMapDisplay() │         └──────────────────┘                     │
│  └──────────────────┘                                                  │
│                               ┌──────────────────┐                     │
│  ┌──────────────────┐         │      Npc         │                     │
│  │  TrainingAltar   │         │──────────────────│                     │
│  │──────────────────│         │ -question        │                     │
│  │ -isOccupied      │         │ -answer          │                     │
│  │ -currentUser     │         │ -options: List   │                     │
│  │──────────────────│         │──────────────────│                     │
│  │ +tryStartTrain() │         │ +checkAnswer()   │                     │
│  │ +endTraining()   │         └──────────────────┘                     │
│  │ +performTrain()  │                                                  │
│  └──────────────────┘         ┌──────────────────┐                     │
│                               │     Skill        │                     │
│  ┌──────────────────┐         │──────────────────│                     │
│  │    JsonUtil      │         │ -name            │                     │
│  │──────────────────│         │ -damageMultiplier│                     │
│  │ +figureToJson()  │         │──────────────────│                     │
│  │ +figureFromJson()│         │ +calculateDamage()│                    │
│  │ +loadNpcsFromFile│         └──────────────────┘                     │
│  └──────────────────┘                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 各类详细说明

#### 4.2.1 NetworkGameServer（服务器主类）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `clients` | ConcurrentHashMap<String, ClientHandler> | 在线玩家映射表 |
| `npcs` | List\<Npc\> | NPC 列表 |
| `mapManager` | MapManager | 地图管理器（图结构） |
| `trainingAltar` | TrainingAltar | 修炼台（共享资源） |
| `start()` | void | 启动服务器，监听端口 4444 |
| `registerClient()` | void | 注册新玩家 |
| `broadcastMessage()` | void | 全局广播消息 |
| `generateMonster()` | Monster | 生成随机怪物 |
| `getRandomNpc()` | Npc | 获取随机 NPC |

#### 4.2.2 ClientHandler（客户端处理器）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `socket` | Socket | 客户端连接 |
| `player` | Figure | 玩家角色实例 |
| `currentScene` | SceneNode | 当前所在场景节点 |
| `isInPvP` | boolean | 是否正在 PK |
| `isInBattle` | boolean | 是否正在打怪 |
| `run()` | void | 线程入口，处理客户端请求 |
| `handleCommand()` | void | 命令解析与分发 |
| `showMainMenu()` | void | 显示主菜单和地图 |
| `startBattle()` | void | 开始 PVE 战斗 |
| `talkToNpc()` | void | NPC 对话 |
| `changeScene()` | void | 场景切换（图遍历） |
| `useTrainingAltar()` | void | 使用修炼台 |

#### 4.2.3 MapManager（地图管理器 - 图结构核心）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `sceneMap` | HashMap<String, SceneNode> | **图的顶点集合** |
| `mapGrid` | SceneNode[][] | 坐标网格（渲染用） |
| `initializeMap()` | void | **构建图结构**（创建顶点、建立边） |
| `getSceneNode()` | SceneNode | O(1) 查找场景节点 |
| `move()` | SceneNode | **图的遍历**（沿边移动到相邻节点） |
| `generateMapDisplay()` | String | 生成 ASCII 地图 |
| `getAllScenes()` | Collection | 获取所有顶点 |

**SceneNode 内部类（图的顶点）**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `scene` | Scene | 场景数据 |
| `x, y` | int | 坐标位置 |
| `north` | SceneNode | **邻接指针**（北方相邻节点） |
| `south` | SceneNode | **邻接指针**（南方相邻节点） |
| `east` | SceneNode | **邻接指针**（东方相邻节点） |
| `west` | SceneNode | **邻接指针**（西方相邻节点） |

#### 4.2.4 TrainingAltar（修炼台 - 共享资源）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `isOccupied` | boolean | 是否被占用 |
| `currentUser` | String | 当前使用者 |
| `tryStartTraining()` | synchronized boolean | **互斥锁获取** |
| `endTraining()` | synchronized void | **释放锁** |
| `performTraining()` | void | 执行修炼流程 |

#### 4.2.5 Figure（角色基类）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `name, Grade, Exp` | String, int, int | 基本属性 |
| `attack, defend` | int | 攻防属性 |
| `LifeValue, MaxLifeValue` | int | 生命值 |
| `skills` | List\<Skill\> | 技能列表 |
| `takeDamage()` | void | 受到伤害 |
| `gainExp()` | void | 获得经验（可能触发升级） |
| `healToFull()` | void | 恢复满血 |
| `addAttack()` | void | 增加攻击力 |
| `addDefend()` | void | 增加防御力 |

#### 4.2.6 NetworkBattleSystem（战斗系统）

| 方法 | 说明 |
|------|------|
| `fight(player, monster, handler, server)` | PVE 战斗逻辑 |
| `playerVsPlayer(p1, p2, h1, h2, server)` | PVP 战斗逻辑 |
| `useSkillNetwork()` | 网络版技能使用 |
| `useSkillPVP()` | PVP 技能使用 |

#### 4.2.7 JsonUtil（JSON 工具类）

| 方法 | 说明 |
|------|------|
| `figureToJson(figure)` | 角色序列化为 JSON |
| `figureFromJson(json)` | JSON 反序列化为角色 |
| `loadNpcsFromFile(path)` | 从文件加载 NPC 列表 |

---

## 5. 关键流程分析

### 5.1 客户端连接流程

```
┌────────────────────────────────────────────────────────────────────────┐
│                        客户端连接时序图                                  │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Client              NetworkGameServer           ClientHandler         │
│    │                        │                          │               │
│    │  1. TCP连接请求         │                          │               │
│    │───────────────────────►│                          │               │
│    │                        │  2. accept()             │               │
│    │                        │  3. 创建ClientHandler    │               │
│    │                        │─────────────────────────►│               │
│    │                        │  4. new Thread().start() │               │
│    │                        │                          │               │
│    │  5. 发送欢迎信息        │◄─────────────────────────│               │
│    │◄───────────────────────│                          │               │
│    │                        │                          │               │
│    │  6. 选择角色类型(1/2)   │                          │               │
│    │───────────────────────►│─────────────────────────►│               │
│    │                        │                          │ 7. 创建角色   │
│    │                        │                          │    (XiaoYan   │
│    │                        │                          │     或 Mo)    │
│    │                        │                          │               │
│    │  8. 输入角色名          │                          │               │
│    │───────────────────────►│─────────────────────────►│               │
│    │                        │                          │ 9. 注册到     │
│    │                        │                          │    clients    │
│    │                        │                          │               │
│    │                        │ 10. 广播"XX加入游戏"     │               │
│    │                        │◄─────────────────────────│               │
│    │                        │                          │               │
│    │  11. 显示主菜单+地图    │                          │               │
│    │◄───────────────────────│◄─────────────────────────│               │
│    │                        │                          │               │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 场景移动流程（图遍历）

```
┌────────────────────────────────────────────────────────────────────────┐
│                        场景移动时序图                                    │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Client        ClientHandler         MapManager         SceneNode      │
│    │                 │                    │                  │         │
│    │  1. 输入"9"     │                    │                  │         │
│    │  (切换场景)     │                    │                  │         │
│    │────────────────►│                    │                  │         │
│    │                 │                    │                  │         │
│    │                 │  2. changeScene()  │                  │         │
│    │                 │                    │                  │         │
│    │  3. 显示可移动方向                   │                  │         │
│    │◄────────────────│                    │                  │         │
│    │                 │                    │                  │         │
│    │  4. 输入"n"     │                    │                  │         │
│    │  (向北移动)     │                    │                  │         │
│    │────────────────►│                    │                  │         │
│    │                 │                    │                  │         │
│    │                 │  5. move(current,  │                  │         │
│    │                 │       "n")         │                  │         │
│    │                 │───────────────────►│                  │         │
│    │                 │                    │                  │         │
│    │                 │                    │  6. return       │         │
│    │                 │                    │  current.north   │         │
│    │                 │                    │─────────────────►│         │
│    │                 │                    │◄─────────────────│         │
│    │                 │◄───────────────────│                  │         │
│    │                 │                    │                  │         │
│    │                 │  7. currentScene   │                  │         │
│    │                 │     = newScene     │                  │         │
│    │                 │                    │                  │         │
│    │  8. 显示新场景  │                    │                  │         │
│    │     信息+地图   │                    │                  │         │
│    │◄────────────────│                    │                  │         │
│    │                 │                    │                  │         │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.3 修炼台使用流程（互斥访问）

```
┌────────────────────────────────────────────────────────────────────────┐
│                      修炼台使用时序图（互斥锁）                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Player A      Player B       ClientHandler      TrainingAltar         │
│     │             │                │                   │               │
│     │  1. 输入"10"│                │                   │               │
│     │  (使用修炼台)                │                   │               │
│     │─────────────────────────────►│                   │               │
│     │             │                │                   │               │
│     │             │                │ 2. tryStartTrain()│               │
│     │             │                │  (synchronized)   │               │
│     │             │                │──────────────────►│               │
│     │             │                │                   │ 3. isOccupied │
│     │             │                │                   │    = true     │
│     │             │                │◄──────────────────│ return true   │
│     │             │                │                   │               │
│     │             │  4. 输入"10"   │                   │               │
│     │             │─────────────────────────────────────────────────►  │
│     │             │                │                   │               │
│     │             │                │ 5. tryStartTrain()│               │
│     │             │                │  (synchronized)   │               │
│     │             │                │──────────────────►│               │
│     │             │                │◄──────────────────│ return false  │
│     │             │                │                   │ (已被占用)    │
│     │             │                │                   │               │
│     │             │◄───────────────│ 6. 提示"修炼台   │               │
│     │             │                │    正在使用中"   │               │
│     │             │                │                   │               │
│     │  7. 修炼完成│                │                   │               │
│     │             │                │ 8. endTraining()  │               │
│     │             │                │──────────────────►│               │
│     │             │                │                   │ 9. isOccupied │
│     │             │                │                   │    = false    │
│     │             │                │                   │               │
│     │  10. 全局广播"修炼完成"      │                   │               │
│     │◄────────────────────────────►│                   │               │
│     │             │                │                   │               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 并发与同步机制

### 6.1 线程模型

```
┌─────────────────────────────────────────────────────────────────┐
│                         线程模型                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  主线程 (Main Thread)                                           │
│  └── NetworkGameServer.start()                                  │
│       └── while(true) { serverSocket.accept() }                 │
│            │                                                    │
│            ├── 创建 ClientHandler 线程 1                        │
│            ├── 创建 ClientHandler 线程 2                        │
│            ├── 创建 ClientHandler 线程 3                        │
│            ├── 创建 ClientHandler 线程 4                        │
│            └── 创建 ClientHandler 线程 5 (最多5个)              │
│                                                                 │
│  每个 ClientHandler 线程独立处理一个客户端的：                   │
│  - 命令接收与解析                                               │
│  - 战斗逻辑执行                                                 │
│  - 场景切换                                                     │
│  - 数据存取                                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 同步机制

| 资源 | 同步方式 | 说明 |
|------|---------|------|
| `clients` | ConcurrentHashMap | 线程安全的 Map，支持并发读写 |
| `TrainingAltar` | synchronized 方法 | 互斥锁，保证同一时间只有一个玩家使用 |
| `registerClient()` | synchronized 方法 | 防止并发注册冲突 |
| `removeClient()` | synchronized 方法 | 防止并发移除冲突 |

### 6.3 TrainingAltar 互斥锁实现

```java
public class TrainingAltar {
    private boolean isOccupied = false;
    private String currentUser = null;
    
    // synchronized 关键字保证原子性
    public synchronized boolean tryStartTraining(String playerName) {
        if (isOccupied) {
            return false;  // 已被占用，获取失败
        }
        isOccupied = true;
        currentUser = playerName;
        return true;  // 获取成功
    }
    
    public synchronized void endTraining() {
        isOccupied = false;
        currentUser = null;
    }
}
```

---

## 7. 总结

### 7.1 核心技术点

| 技术点 | 实现方式 |
|--------|---------|
| 网络通信 | Java Socket（TCP/IP） |
| 多线程 | Thread + ConcurrentHashMap |
| 数据结构 | **图（邻接表）**、HashMap、ArrayList |
| 互斥同步 | synchronized 关键字 |
| 数据持久化 | JSON（Gson 库） |
| 字符编码 | UTF-8 |

### 7.2 设计亮点

1. **图结构的应用**：使用邻接表实现场景连接，支持 O(1) 时间复杂度的场景移动
2. **共享资源管理**：修炼台使用 synchronized 实现互斥访问
3. **模块化设计**：清晰的职责划分，易于扩展
4. **场景限制系统**：不同场景有不同的可用操作，增加策略性

### 7.3 可扩展方向

- 增加更多场景（扩展图结构）
- 实现组队系统
- 添加物品/背包系统
- 引入数据库持久化
- 开发图形界面客户端

---

## 附录 A：文件清单

| 文件 | 说明 |
|------|------|
| `NetworkGameServer.java` | 服务器主程序 + ClientHandler |
| `NetworkGameClient.java` | 客户端程序 |
| `MapManager.java` | 地图管理器（图结构） |
| `TrainingAltar.java` | 修炼台（共享资源） |
| `NetworkBattleSystem.java` | 战斗系统 |
| `Figure.java` | 角色基类 |
| `XiaoYan.java` / `Mo.java` | 角色子类 |
| `Monster.java` | 怪物类 |
| `Npc.java` | NPC 类 |
| `Skill.java` | 技能类 |
| `Scene.java` | 场景类 |
| `JsonUtil.java` | JSON 工具类 |
| `npcs.json` | NPC 配置数据 |

---

*文档结束*

=======
# MUD武侠游戏网络版 - 技术报告

> **项目名称**：MUD武侠游戏网络版  
> **开发语言**：Java  
> **网络协议**：TCP/IP  
> **文档版本**：v1.0  
> **更新日期**：2025-01-08

---

## 目录

1. [系统概述](#1-系统概述)
2. [系统架构](#2-系统架构)
3. [核心数据结构](#3-核心数据结构)
4. [类设计与功能说明](#4-类设计与功能说明)
5. [关键流程分析](#5-关键流程分析)
6. [并发与同步机制](#6-并发与同步机制)
7. [总结](#7-总结)

---

## 1. 系统概述

### 1.1 项目背景

本项目是一款基于文本的多人在线角色扮演游戏（MUD，Multi-User Dungeon），采用经典的 Client-Server 架构，支持最多 5 个玩家同时在线。玩家可以在虚拟武侠世界中探索场景、与 NPC 对话、挑战怪物、与其他玩家 PK，并通过修炼台提升自身属性。

### 1.2 功能特性

| 功能模块 | 描述 |
|---------|------|
| 多人在线 | 支持最多 5 个客户端同时连接 |
| 角色系统 | 两种角色（萧言/莫），各有独特技能 |
| 场景系统 | 5 个场景，使用**图结构**管理场景连接 |
| 战斗系统 | PVE（打怪）和 PVP（玩家对战） |
| NPC 对话 | 选择题模式，答对获得经验 |
| 修炼台 | 共享资源，同时仅一人可用（互斥锁） |
| 存档系统 | JSON 格式持久化存储 |

---

## 2. 系统架构

### 2.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              系统架构图                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│   ┌──────────────┐      TCP/IP       ┌──────────────────────────────┐  │
│   │   Client 1   │◄─────────────────►│                              │  │
│   └──────────────┘                   │                              │  │
│                                      │                              │  │
│   ┌──────────────┐      TCP/IP       │     NetworkGameServer        │  │
│   │   Client 2   │◄─────────────────►│                              │  │
│   └──────────────┘                   │  ┌────────────────────────┐  │  │
│                                      │  │ ClientHandler (线程池) │  │  │
│   ┌──────────────┐      TCP/IP       │  │  - handler1            │  │  │
│   │   Client 3   │◄─────────────────►│  │  - handler2            │  │  │
│   └──────────────┘                   │  │  - handler3...         │  │  │
│         ...                          │  └────────────────────────┘  │  │
│   ┌──────────────┐      TCP/IP       │                              │  │
│   │   Client N   │◄─────────────────►│  ┌────────────────────────┐  │  │
│   │  (max=5)     │                   │  │ 游戏实体管理            │  │  │
│   └──────────────┘                   │  │  - Figure (角色)       │  │  │
│                                      │  │  - Monster (怪物)      │  │  │
│                                      │  │  - Npc (NPC)           │  │  │
│                                      │  │  - Scene (场景)        │  │  │
│                                      │  └────────────────────────┘  │  │
│                                      │                              │  │
│                                      │  ┌────────────────────────┐  │  │
│                                      │  │ 核心管理器              │  │  │
│                                      │  │  - MapManager (地图)   │  │  │
│                                      │  │  - TrainingAltar (修炼)│  │  │
│                                      │  └────────────────────────┘  │  │
│                                      └──────────────────────────────┘  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模块划分

| 层次 | 模块 | 职责 |
|------|------|------|
| 表示层 | NetworkGameClient | 用户交互、命令输入、结果展示 |
| 业务逻辑层 | ClientHandler | 命令解析、业务调度 |
| 业务逻辑层 | NetworkBattleSystem | 战斗逻辑（PVE/PVP） |
| 业务逻辑层 | TrainingAltar | 修炼台逻辑（共享资源管理） |
| 数据管理层 | MapManager | 地图/场景管理（图结构） |
| 数据管理层 | JsonUtil | 数据序列化/反序列化 |
| 实体层 | Figure, Monster, Npc, Skill, Scene | 游戏实体定义 |

---

## 3. 核心数据结构

### 3.1 数据结构总览

| 数据结构 | 使用位置 | 用途 |
|---------|---------|------|
| **图（Graph）** | MapManager | 场景连接关系（邻接表） |
| HashMap | NetworkGameServer, MapManager | 客户端映射、场景索引 |
| ConcurrentHashMap | NetworkGameServer.clients | 线程安全的客户端管理 |
| ArrayList | Figure.skills, Scene.availableActions | 技能列表、可用操作 |
| 二维数组 | MapManager.mapGrid | 地图坐标布局 |

### 3.2 图结构详解（重点）

#### 3.2.1 图的定义

在本项目中，**地图系统使用图（Graph）数据结构**来表示场景之间的连接关系：

- **顶点（Vertex）**：每个场景（SceneNode）
- **边（Edge）**：场景之间的通道（north/south/east/west 指针）
- **图类型**：**有向图**（从 A 能到 B，不代表从 B 能到 A，但本项目中大多为双向）

#### 3.2.2 图的实现方式：邻接表

```java
// MapManager.java 中的图结构实现

public class MapManager {
    // 顶点集合：使用 HashMap 存储所有场景节点
    // Key = 场景名称, Value = 场景节点
    private Map<String, SceneNode> sceneMap = new HashMap<>();
    
    // 场景节点定义（图的顶点）
    public static class SceneNode {
        public Scene scene;           // 场景数据
        public int x, y;              // 坐标（用于地图渲染）
        
        // 邻接关系（图的边）—— 使用指针/引用表示
        public SceneNode north;       // 北方相邻场景
        public SceneNode south;       // 南方相邻场景
        public SceneNode east;        // 东方相邻场景
        public SceneNode west;        // 西方相邻场景
    }
}
```

#### 3.2.3 图的可视化表示

```
                    ┌─────────┐
                    │ 新手村  │ (2,0)
                    └────┬────┘
                         │ south
                         ▼
    ┌─────────┐    ┌─────────┐    ┌─────────┐
    │ 藏经阁  │◄───│武林广场 │───►│ 练功房  │
    │  (0,2)  │west│  (2,2)  │east│  (4,2)  │
    └─────────┘    └────┬────┘    └─────────┘
                        │ south
                        ▼
                   ┌─────────┐
                   │江湖客栈 │ (2,4)
                   └─────────┘

图例说明：
- 每个方框 = 图的顶点（SceneNode）
- 箭头 = 图的边（邻接关系）
- (x,y) = 节点坐标（用于地图渲染）
```

#### 3.2.4 图的核心操作

| 操作 | 方法 | 时间复杂度 | 说明 |
|------|------|-----------|------|
| 查找节点 | `getSceneNode(name)` | O(1) | HashMap 查找 |
| 移动（遍历边） | `move(current, direction)` | O(1) | 直接访问邻接指针 |
| 获取所有节点 | `getAllScenes()` | O(n) | 遍历 HashMap |
| 获取起始节点 | `getStartScene()` | O(1) | 返回"新手村"节点 |

#### 3.2.5 图结构的优势

1. **空间效率**：邻接表只存储实际存在的边，适合稀疏图
2. **查询效率**：O(1) 时间复杂度获取相邻场景
3. **扩展性强**：新增场景只需添加节点和边
4. **直观性**：north/south/east/west 语义清晰

---

## 4. 类设计与功能说明

### 4.1 类图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              类 图                                       │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────────────┐         ┌──────────────────┐                     │
│  │ NetworkGameServer│◄────────│  ClientHandler   │                     │
│  │──────────────────│ 1    *  │──────────────────│                     │
│  │ -clients: Map    │         │ -socket: Socket  │                     │
│  │ -npcs: List      │         │ -player: Figure  │                     │
│  │ -mapManager      │         │ -currentScene    │                     │
│  │ -trainingAltar   │         │──────────────────│                     │
│  │──────────────────│         │ +run()           │                     │
│  │ +start()         │         │ +handleCommand() │                     │
│  │ +broadcastMsg()  │         │ +showMainMenu()  │                     │
│  └──────────────────┘         └────────┬─────────┘                     │
│           │                            │                                │
│           │ uses                       │ uses                           │
│           ▼                            ▼                                │
│  ┌──────────────────┐         ┌──────────────────┐                     │
│  │   MapManager     │         │NetworkBattleSystem│                    │
│  │──────────────────│         │──────────────────│                     │
│  │ -sceneMap: Map   │         │ +fight()         │                     │
│  │ -mapGrid[][]     │         │ +playerVsPlayer()│                     │
│  │──────────────────│         │ +useSkillPVP()   │                     │
│  │ +move()          │         └──────────────────┘                     │
│  │ +generateMap()   │                  │                                │
│  │ +getSceneNode()  │                  │ uses                           │
│  └────────┬─────────┘                  ▼                                │
│           │ contains          ┌──────────────────┐                     │
│           ▼                   │     Figure       │◄─────┐              │
│  ┌──────────────────┐         │──────────────────│      │ extends      │
│  │   SceneNode      │         │ -name, -grade    │      │              │
│  │──────────────────│         │ -attack, -defend │ ┌────┴─────┐        │
│  │ +scene: Scene    │         │ -skills: List    │ │ XiaoYan  │        │
│  │ +north,south,... │         │──────────────────│ │   Mo     │        │
│  │ +x, y            │         │ +takeDamage()    │ └──────────┘        │
│  └────────┬─────────┘         │ +gainExp()       │                     │
│           │ contains          │ +healToFull()    │                     │
│           ▼                   └──────────────────┘                     │
│  ┌──────────────────┐                                                  │
│  │     Scene        │         ┌──────────────────┐                     │
│  │──────────────────│         │    Monster       │                     │
│  │ -name            │         │──────────────────│                     │
│  │ -description     │         │ -name, -grade    │                     │
│  │ -availableActions│         │ -attack, -defend │                     │
│  │──────────────────│         │──────────────────│                     │
│  │ +addAction()     │         │ +takeDamage()    │                     │
│  │ +getMapDisplay() │         └──────────────────┘                     │
│  └──────────────────┘                                                  │
│                               ┌──────────────────┐                     │
│  ┌──────────────────┐         │      Npc         │                     │
│  │  TrainingAltar   │         │──────────────────│                     │
│  │──────────────────│         │ -question        │                     │
│  │ -isOccupied      │         │ -answer          │                     │
│  │ -currentUser     │         │ -options: List   │                     │
│  │──────────────────│         │──────────────────│                     │
│  │ +tryStartTrain() │         │ +checkAnswer()   │                     │
│  │ +endTraining()   │         └──────────────────┘                     │
│  │ +performTrain()  │                                                  │
│  └──────────────────┘         ┌──────────────────┐                     │
│                               │     Skill        │                     │
│  ┌──────────────────┐         │──────────────────│                     │
│  │    JsonUtil      │         │ -name            │                     │
│  │──────────────────│         │ -damageMultiplier│                     │
│  │ +figureToJson()  │         │──────────────────│                     │
│  │ +figureFromJson()│         │ +calculateDamage()│                    │
│  │ +loadNpcsFromFile│         └──────────────────┘                     │
│  └──────────────────┘                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### 4.2 各类详细说明

#### 4.2.1 NetworkGameServer（服务器主类）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `clients` | ConcurrentHashMap<String, ClientHandler> | 在线玩家映射表 |
| `npcs` | List\<Npc\> | NPC 列表 |
| `mapManager` | MapManager | 地图管理器（图结构） |
| `trainingAltar` | TrainingAltar | 修炼台（共享资源） |
| `start()` | void | 启动服务器，监听端口 4444 |
| `registerClient()` | void | 注册新玩家 |
| `broadcastMessage()` | void | 全局广播消息 |
| `generateMonster()` | Monster | 生成随机怪物 |
| `getRandomNpc()` | Npc | 获取随机 NPC |

#### 4.2.2 ClientHandler（客户端处理器）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `socket` | Socket | 客户端连接 |
| `player` | Figure | 玩家角色实例 |
| `currentScene` | SceneNode | 当前所在场景节点 |
| `isInPvP` | boolean | 是否正在 PK |
| `isInBattle` | boolean | 是否正在打怪 |
| `run()` | void | 线程入口，处理客户端请求 |
| `handleCommand()` | void | 命令解析与分发 |
| `showMainMenu()` | void | 显示主菜单和地图 |
| `startBattle()` | void | 开始 PVE 战斗 |
| `talkToNpc()` | void | NPC 对话 |
| `changeScene()` | void | 场景切换（图遍历） |
| `useTrainingAltar()` | void | 使用修炼台 |

#### 4.2.3 MapManager（地图管理器 - 图结构核心）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `sceneMap` | HashMap<String, SceneNode> | **图的顶点集合** |
| `mapGrid` | SceneNode[][] | 坐标网格（渲染用） |
| `initializeMap()` | void | **构建图结构**（创建顶点、建立边） |
| `getSceneNode()` | SceneNode | O(1) 查找场景节点 |
| `move()` | SceneNode | **图的遍历**（沿边移动到相邻节点） |
| `generateMapDisplay()` | String | 生成 ASCII 地图 |
| `getAllScenes()` | Collection | 获取所有顶点 |

**SceneNode 内部类（图的顶点）**：

| 属性 | 类型 | 说明 |
|------|------|------|
| `scene` | Scene | 场景数据 |
| `x, y` | int | 坐标位置 |
| `north` | SceneNode | **邻接指针**（北方相邻节点） |
| `south` | SceneNode | **邻接指针**（南方相邻节点） |
| `east` | SceneNode | **邻接指针**（东方相邻节点） |
| `west` | SceneNode | **邻接指针**（西方相邻节点） |

#### 4.2.4 TrainingAltar（修炼台 - 共享资源）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `isOccupied` | boolean | 是否被占用 |
| `currentUser` | String | 当前使用者 |
| `tryStartTraining()` | synchronized boolean | **互斥锁获取** |
| `endTraining()` | synchronized void | **释放锁** |
| `performTraining()` | void | 执行修炼流程 |

#### 4.2.5 Figure（角色基类）

| 属性/方法 | 类型 | 说明 |
|----------|------|------|
| `name, Grade, Exp` | String, int, int | 基本属性 |
| `attack, defend` | int | 攻防属性 |
| `LifeValue, MaxLifeValue` | int | 生命值 |
| `skills` | List\<Skill\> | 技能列表 |
| `takeDamage()` | void | 受到伤害 |
| `gainExp()` | void | 获得经验（可能触发升级） |
| `healToFull()` | void | 恢复满血 |
| `addAttack()` | void | 增加攻击力 |
| `addDefend()` | void | 增加防御力 |

#### 4.2.6 NetworkBattleSystem（战斗系统）

| 方法 | 说明 |
|------|------|
| `fight(player, monster, handler, server)` | PVE 战斗逻辑 |
| `playerVsPlayer(p1, p2, h1, h2, server)` | PVP 战斗逻辑 |
| `useSkillNetwork()` | 网络版技能使用 |
| `useSkillPVP()` | PVP 技能使用 |

#### 4.2.7 JsonUtil（JSON 工具类）

| 方法 | 说明 |
|------|------|
| `figureToJson(figure)` | 角色序列化为 JSON |
| `figureFromJson(json)` | JSON 反序列化为角色 |
| `loadNpcsFromFile(path)` | 从文件加载 NPC 列表 |

---

## 5. 关键流程分析

### 5.1 客户端连接流程

```
┌────────────────────────────────────────────────────────────────────────┐
│                        客户端连接时序图                                  │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Client              NetworkGameServer           ClientHandler         │
│    │                        │                          │               │
│    │  1. TCP连接请求         │                          │               │
│    │───────────────────────►│                          │               │
│    │                        │  2. accept()             │               │
│    │                        │  3. 创建ClientHandler    │               │
│    │                        │─────────────────────────►│               │
│    │                        │  4. new Thread().start() │               │
│    │                        │                          │               │
│    │  5. 发送欢迎信息        │◄─────────────────────────│               │
│    │◄───────────────────────│                          │               │
│    │                        │                          │               │
│    │  6. 选择角色类型(1/2)   │                          │               │
│    │───────────────────────►│─────────────────────────►│               │
│    │                        │                          │ 7. 创建角色   │
│    │                        │                          │    (XiaoYan   │
│    │                        │                          │     或 Mo)    │
│    │                        │                          │               │
│    │  8. 输入角色名          │                          │               │
│    │───────────────────────►│─────────────────────────►│               │
│    │                        │                          │ 9. 注册到     │
│    │                        │                          │    clients    │
│    │                        │                          │               │
│    │                        │ 10. 广播"XX加入游戏"     │               │
│    │                        │◄─────────────────────────│               │
│    │                        │                          │               │
│    │  11. 显示主菜单+地图    │                          │               │
│    │◄───────────────────────│◄─────────────────────────│               │
│    │                        │                          │               │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.2 场景移动流程（图遍历）

```
┌────────────────────────────────────────────────────────────────────────┐
│                        场景移动时序图                                    │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Client        ClientHandler         MapManager         SceneNode      │
│    │                 │                    │                  │         │
│    │  1. 输入"9"     │                    │                  │         │
│    │  (切换场景)     │                    │                  │         │
│    │────────────────►│                    │                  │         │
│    │                 │                    │                  │         │
│    │                 │  2. changeScene()  │                  │         │
│    │                 │                    │                  │         │
│    │  3. 显示可移动方向                   │                  │         │
│    │◄────────────────│                    │                  │         │
│    │                 │                    │                  │         │
│    │  4. 输入"n"     │                    │                  │         │
│    │  (向北移动)     │                    │                  │         │
│    │────────────────►│                    │                  │         │
│    │                 │                    │                  │         │
│    │                 │  5. move(current,  │                  │         │
│    │                 │       "n")         │                  │         │
│    │                 │───────────────────►│                  │         │
│    │                 │                    │                  │         │
│    │                 │                    │  6. return       │         │
│    │                 │                    │  current.north   │         │
│    │                 │                    │─────────────────►│         │
│    │                 │                    │◄─────────────────│         │
│    │                 │◄───────────────────│                  │         │
│    │                 │                    │                  │         │
│    │                 │  7. currentScene   │                  │         │
│    │                 │     = newScene     │                  │         │
│    │                 │                    │                  │         │
│    │  8. 显示新场景  │                    │                  │         │
│    │     信息+地图   │                    │                  │         │
│    │◄────────────────│                    │                  │         │
│    │                 │                    │                  │         │
└────────────────────────────────────────────────────────────────────────┘
```

### 5.3 修炼台使用流程（互斥访问）

```
┌────────────────────────────────────────────────────────────────────────┐
│                      修炼台使用时序图（互斥锁）                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  Player A      Player B       ClientHandler      TrainingAltar         │
│     │             │                │                   │               │
│     │  1. 输入"10"│                │                   │               │
│     │  (使用修炼台)                │                   │               │
│     │─────────────────────────────►│                   │               │
│     │             │                │                   │               │
│     │             │                │ 2. tryStartTrain()│               │
│     │             │                │  (synchronized)   │               │
│     │             │                │──────────────────►│               │
│     │             │                │                   │ 3. isOccupied │
│     │             │                │                   │    = true     │
│     │             │                │◄──────────────────│ return true   │
│     │             │                │                   │               │
│     │             │  4. 输入"10"   │                   │               │
│     │             │─────────────────────────────────────────────────►  │
│     │             │                │                   │               │
│     │             │                │ 5. tryStartTrain()│               │
│     │             │                │  (synchronized)   │               │
│     │             │                │──────────────────►│               │
│     │             │                │◄──────────────────│ return false  │
│     │             │                │                   │ (已被占用)    │
│     │             │                │                   │               │
│     │             │◄───────────────│ 6. 提示"修炼台   │               │
│     │             │                │    正在使用中"   │               │
│     │             │                │                   │               │
│     │  7. 修炼完成│                │                   │               │
│     │             │                │ 8. endTraining()  │               │
│     │             │                │──────────────────►│               │
│     │             │                │                   │ 9. isOccupied │
│     │             │                │                   │    = false    │
│     │             │                │                   │               │
│     │  10. 全局广播"修炼完成"      │                   │               │
│     │◄────────────────────────────►│                   │               │
│     │             │                │                   │               │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 6. 并发与同步机制

### 6.1 线程模型

```
┌─────────────────────────────────────────────────────────────────┐
│                         线程模型                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  主线程 (Main Thread)                                           │
│  └── NetworkGameServer.start()                                  │
│       └── while(true) { serverSocket.accept() }                 │
│            │                                                    │
│            ├── 创建 ClientHandler 线程 1                        │
│            ├── 创建 ClientHandler 线程 2                        │
│            ├── 创建 ClientHandler 线程 3                        │
│            ├── 创建 ClientHandler 线程 4                        │
│            └── 创建 ClientHandler 线程 5 (最多5个)              │
│                                                                 │
│  每个 ClientHandler 线程独立处理一个客户端的：                   │
│  - 命令接收与解析                                               │
│  - 战斗逻辑执行                                                 │
│  - 场景切换                                                     │
│  - 数据存取                                                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 6.2 同步机制

| 资源 | 同步方式 | 说明 |
|------|---------|------|
| `clients` | ConcurrentHashMap | 线程安全的 Map，支持并发读写 |
| `TrainingAltar` | synchronized 方法 | 互斥锁，保证同一时间只有一个玩家使用 |
| `registerClient()` | synchronized 方法 | 防止并发注册冲突 |
| `removeClient()` | synchronized 方法 | 防止并发移除冲突 |

### 6.3 TrainingAltar 互斥锁实现

```java
public class TrainingAltar {
    private boolean isOccupied = false;
    private String currentUser = null;
    
    // synchronized 关键字保证原子性
    public synchronized boolean tryStartTraining(String playerName) {
        if (isOccupied) {
            return false;  // 已被占用，获取失败
        }
        isOccupied = true;
        currentUser = playerName;
        return true;  // 获取成功
    }
    
    public synchronized void endTraining() {
        isOccupied = false;
        currentUser = null;
    }
}
```

---

## 7. 总结

### 7.1 核心技术点

| 技术点 | 实现方式 |
|--------|---------|
| 网络通信 | Java Socket（TCP/IP） |
| 多线程 | Thread + ConcurrentHashMap |
| 数据结构 | **图（邻接表）**、HashMap、ArrayList |
| 互斥同步 | synchronized 关键字 |
| 数据持久化 | JSON（Gson 库） |
| 字符编码 | UTF-8 |

### 7.2 设计亮点

1. **图结构的应用**：使用邻接表实现场景连接，支持 O(1) 时间复杂度的场景移动
2. **共享资源管理**：修炼台使用 synchronized 实现互斥访问
3. **模块化设计**：清晰的职责划分，易于扩展
4. **场景限制系统**：不同场景有不同的可用操作，增加策略性

### 7.3 可扩展方向

- 增加更多场景（扩展图结构）
- 实现组队系统
- 添加物品/背包系统
- 引入数据库持久化
- 开发图形界面客户端

---

## 附录 A：文件清单

| 文件 | 说明 |
|------|------|
| `NetworkGameServer.java` | 服务器主程序 + ClientHandler |
| `NetworkGameClient.java` | 客户端程序 |
| `MapManager.java` | 地图管理器（图结构） |
| `TrainingAltar.java` | 修炼台（共享资源） |
| `NetworkBattleSystem.java` | 战斗系统 |
| `Figure.java` | 角色基类 |
| `XiaoYan.java` / `Mo.java` | 角色子类 |
| `Monster.java` | 怪物类 |
| `Npc.java` | NPC 类 |
| `Skill.java` | 技能类 |
| `Scene.java` | 场景类 |
| `JsonUtil.java` | JSON 工具类 |
| `npcs.json` | NPC 配置数据 |

---

*文档结束*

>>>>>>> e1501ce6d55714bf6aecc1e18dd84acda821f7d9
