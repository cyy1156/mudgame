# MUD游戏网络版使用说明

## 概述
这是一个基于TCP协议的多人在线MUD武侠游戏，支持最多4个客户端同时连接。

## 功能特性
1. **多人在线游戏**：支持最多4个玩家同时在线
2. **角色系统**：创建角色、升级、学习技能
3. **打怪升级**：与随机生成的怪物战斗获得经验
4. **NPC对话**：与NPC对话回答问题获得经验奖励（NPC数据存储在npcs.json中）
5. **玩家交互**：查看在线玩家列表、聊天
6. **玩家PK**：向其他在线玩家发起挑战进行PK战斗
7. **存档系统**：使用JSON格式保存和加载角色数据

## 文件说明

### 服务器端
- `NetworkGameServer.java` - 游戏服务器主程序
- `NetworkBattleSystem.java` - 网络版战斗系统（支持PVE和PVP）
- `JsonUtil.java` - JSON工具类，用于读写角色和NPC数据
- `npcs.json` - NPC对话数据配置文件

### 客户端
- `NetworkGameClient.java` - 游戏客户端程序

### 其他核心文件
- `Figure.java` - 角色基类
- `Monster.java` - 怪物类
- `Npc.java` - NPC类
- `Skill.java` - 技能类
- `XiaoYan.java` - 玩家角色类

## 使用方法

### 1. 编译项目

首先编译所有Java文件（包含Gson库）：
```bash
javac -encoding UTF-8 -cp "lib/gson-2.10.1.jar" -d out/production/USST src/com/mudgame/NetworkGameServer.java src/com/mudgame/NetworkGameClient.java src/com/mudgame/NetworkBattleSystem.java src/com/mudgame/JsonUtil.java src/com/mudgame/Figure.java src/com/mudgame/XiaoYan.java src/com/mudgame/Skill.java src/com/mudgame/Npc.java src/com/mudgame/Monster.java
```

### 2. 启动服务器

**方法一：使用启动脚本（推荐，解决中文乱码）**

**Windows CMD:**
```bash
start_server.bat
```

**Windows PowerShell:**
```bash
.\start_server.ps1
```

**方法二：手动启动**
```bash
# Windows
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer

# Linux/Mac
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST:lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

服务器默认监听端口：**8888**

### 3. 启动客户端

在不同的终端或机器上运行客户端（最多4个）：

**方法一：使用启动脚本（推荐，解决中文乱码）**

**Windows CMD:**
```bash
# 连接本地服务器
start_client.bat

# 连接指定服务器
start_client.bat <服务器地址> <端口>
```

**Windows PowerShell:**
```bash
# 连接本地服务器
.\start_client.ps1

# 连接指定服务器
.\start_client.ps1 <服务器地址> <端口>
```

**方法二：手动启动**
```bash
# Windows - 连接本地服务器
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient

# Windows - 连接指定服务器
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient <服务器地址> <端口>

# Linux/Mac
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST:lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient <服务器地址> <端口>
```

**注意：** 使用启动脚本可以自动设置UTF-8编码，解决中文乱码问题。如果手动启动，请确保添加 `-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8` 参数。

### 3. 游戏操作

连接成功后，客户端会提示输入角色名称，然后进入游戏主菜单：

```
===== 主菜单 =====
1. 打怪练级
2. NPC对话
3. 查看状态
4. 保存游戏
5. 加载游戏
6. 查看在线玩家
7. 聊天 (chat <消息>)
8. PK挑战 (pk <玩家名>)
9. 退出游戏
请选择(1-9):
```

**命令说明：**
- 输入 `1` 或 `battle`：开始打怪
- 输入 `2` 或 `npc`：与NPC对话
- 输入 `3` 或 `status`：查看角色状态
- 输入 `4` 或 `save`：保存游戏（JSON格式）
- 输入 `5` 或 `load`：加载游戏
- 输入 `6` 或 `list`：查看在线玩家列表
- 输入 `7 chat <消息>`：向所有在线玩家发送消息
- 输入 `8 pk <玩家名>`：向指定玩家发起PK挑战
  - 被挑战的玩家输入 `accept` 接受挑战
  - 被挑战的玩家输入 `reject` 拒绝挑战
- 输入 `9` 或 `quit`：退出游戏

## 数据存储

### NPC数据（npcs.json）
NPC对话数据存储在 `src/com/mudgame/npcs.json` 文件中，格式如下：
```json
[
  {
    "id": 1,
    "name": "书生",
    "question": "武林中最强的是什么？",
    "answer": "心",
    "exp": 50,
    "dialogue": "年轻人，你可知武学的真谛？"
  }
]
```

### 角色存档
每个玩家的存档保存在 `src/com/mudgame/save_<玩家名>.json` 文件中，使用JSON格式存储角色数据。

## 网络协议

- **协议**：TCP
- **默认端口**：8888
- **通信方式**：文本命令（一行一条命令）
- **编码**：UTF-8

## 注意事项

1. 确保服务器先启动，再启动客户端
2. 最多支持4个客户端同时连接
3. PK战斗是回合制的，需要双方玩家依次操作
4. 游戏数据保存在服务器端的JSON文件中
5. 如果服务器关闭，所有客户端连接会断开
6. **中文乱码问题**：使用提供的启动脚本（`.bat` 或 `.ps1`）可以自动设置UTF-8编码，解决中文显示乱码问题
7. 如果使用命令行手动启动，请确保：
   - 添加 `-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8` 参数
   - 在CMD中运行 `chcp 65001` 设置代码页为UTF-8
   - 在PowerShell中设置 `[Console]::OutputEncoding = [System.Text.Encoding]::UTF8`

## 扩展功能

可以修改 `npcs.json` 文件来添加更多NPC，或修改游戏逻辑来添加新功能。


