package com.mudgame;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * MUD游戏网络服务器
 * 支持最多5个客户端同时连接
 */
public class NetworkGameServer {
    private static final int PORT = 4444;
    private static final int MAX_CLIENTS = 5;
    
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private List<Npc> npcs = new ArrayList<>();
    private List<Scene> scenes = new ArrayList<>();
    private Random random = new Random();
    private TrainingAltar trainingAltar = new TrainingAltar();  // 武林秘籍修炼台（共享资源）
    private MapManager mapManager = new MapManager();  // 地图管理器
    
    public static void main(String[] args) {
        // 设置控制台输出编码为UTF-8
        try {
            System.setProperty("file.encoding", "UTF-8");
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception e) {
            System.err.println("Warning: Failed to set UTF-8 encoding: " + e.getMessage());
        }
        NetworkGameServer server = new NetworkGameServer();
        server.start();
    }
    
    public void start() {
        try {
            // 加载NPC数据
            loadNpcs();
            // 初始化场景
            initializeScenes();
            
            serverSocket = new ServerSocket(PORT);/*代码里 new ServerSocket(PORT) 这个写法，默认会让服务器监听电脑上所有可用的网络接口，包括：
            内网 IP 对应的接口（比如 192.168.1.100）
            本地回环地址 127.0.0.1（仅本机访问用）*/
            System.out.println("====== MUD游戏服务器启动 ======");
            System.out.println("服务器监听端口: " + PORT);
            System.out.println("最大支持客户端数: " + MAX_CLIENTS);
            System.out.println("等待客户端连接...");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                
                if (clients.size() >= MAX_CLIENTS) {
                    PrintWriter out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
                    out.println("服务器已满，最多支持" + MAX_CLIENTS + "个玩家！");
                    clientSocket.close();
                    continue;
                }
                
                System.out.println("[客户端连接] 新客户端尝试连接，IP: " + clientSocket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("服务器错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadNpcs() {
        try {
            String npcFile = "src/com/mudgame/npcs.json";
            npcs = JsonUtil.loadNpcsFromFile(npcFile);
            System.out.println("已加载 " + npcs.size() + " 个NPC");
        } catch (Exception e) {
            System.out.println("加载NPC失败，使用默认NPC: " + e.getMessage());
            npcs.add(new Npc("书生", "武林中最强的是什么？", "心", 50));
        }
    }
    
    private void initializeScenes() {
        // 场景初始化已由MapManager完成
        // 这里只需要从MapManager获取场景列表
        for (MapManager.SceneNode node : mapManager.getAllScenes()) {
            scenes.add(node.scene);
        }
        System.out.println("已初始化 " + scenes.size() + " 个场景（地图系统已加载）");
    }
    
    public Scene getRandomScene() {
        if (scenes.isEmpty()) {
            return null;
        }
        return scenes.get(random.nextInt(scenes.size()));
    }
    
    public MapManager.SceneNode getRandomSceneNode() {
        List<MapManager.SceneNode> allNodes = new ArrayList<>(mapManager.getAllScenes());
        if (allNodes.isEmpty()) {
            return null;
        }
        return allNodes.get(random.nextInt(allNodes.size()));
    }
    
    public List<Scene> getScenes() {
        return scenes;
    }
    
    public synchronized void registerClient(String playerName, ClientHandler handler) {
        clients.put(playerName, handler);
        broadcastMessage("系统", playerName + " 加入了游戏！", playerName);
        sendPlayerList();
    }
    
    public synchronized void removeClient(String playerName) {
        clients.remove(playerName);
        broadcastMessage("系统", playerName + " 离开了游戏。", null);
        sendPlayerList();
    }
    
    public Map<String, ClientHandler> getClients() {
        return clients;
    }
    
    public List<Npc> getNpcs() {
        return npcs;
    }
    
    public void broadcastMessage(String from, String message, String exclude) {
        String timestamp = java.time.LocalTime.now().toString().substring(0, 8); // HH:mm:ss格式
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(exclude)) {
                entry.getValue().sendMessage("[" + timestamp + "] <" + from + "> " + message);
            } else {
                // 发送者自己也能看到消息（带标记）
                entry.getValue().sendMessage("[" + timestamp + "] <你> " + message);
            }
        }
    }
    
    private void sendPlayerList() {
        StringBuilder list = new StringBuilder("在线玩家: ");
        for (String name : clients.keySet()) {
            list.append(name).append(" ");
        }
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(list.toString());
        }
    }
    
    public ClientHandler getClientHandler(String playerName) {
        return clients.get(playerName);
    }
    
    public Monster generateMonster() {
        return new Monster();
    }
    
    public Npc getRandomNpc() {
        if (npcs.isEmpty()) {
            return new Npc("书生", "武林中最强的是什么？", "心", 50);
        }
        return npcs.get(random.nextInt(npcs.size()));
    }
    
    public TrainingAltar getTrainingAltar() {
        return trainingAltar;
    }
    
    public MapManager getMapManager() {
        return mapManager;
    }
}

/**
 * 客户端处理器
 */
class ClientHandler implements Runnable {
    private Socket socket;
    private NetworkGameServer server;
    private PrintWriter out;
    private BufferedReader in;
    private Figure player;
    private String playerName;
    private boolean running = true;
    private MapManager.SceneNode currentScene;  // 当前所在场景节点
    private BlockingQueue<String> inputQueue = new LinkedBlockingQueue<>(1);  // 输入队列（容量为1，只保留最新输入）
    
    public ClientHandler(Socket socket, NetworkGameServer server) {
        this.socket = socket;
        this.server = server;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            // 登录/注册
            out.println("欢迎来到MUD武侠世界！");
            out.println("请选择角色类型:");
            out.println("1. 萧言 - 出生于蛇岛，擅长火系技能");
            out.println("2. 莫 - 出生于云山，擅长风系技能");
            out.println("请选择(1-2):");
            
            String roleChoice = in.readLine();
            System.out.println("[客户端连接] 收到角色选择: " + (roleChoice != null ? roleChoice : "空"));
            int roleType = 1; // 默认选择萧言
            if (roleChoice != null && !roleChoice.trim().isEmpty()) {
                try {
                    roleType = Integer.parseInt(roleChoice.trim());
                    if (roleType < 1 || roleType > 2) {
                        roleType = 1; // 无效选择，默认萧言
                    }
                } catch (NumberFormatException e) {
                    roleType = 1; // 无效输入，默认萧言
                }
            }
            
            out.println("请输入角色名称:");
            playerName = in.readLine();
            System.out.println("[客户端连接] 收到角色名称: " + (playerName != null ? playerName : "空"));
            
            if (playerName == null || playerName.trim().isEmpty()) {
                playerName = "玩家" + System.currentTimeMillis();
            }
            playerName = playerName.trim();
            
            // 检查是否已有同名玩家
            if (server.getClients().containsKey(playerName)) {
                playerName = playerName + "_" + System.currentTimeMillis();
            }
            
            // 根据选择创建角色
            if (roleType == 1) {
                player = new XiaoYan();
                out.println("你选择了角色：萧言");
                System.out.println("[客户端连接] " + playerName + " 选择了角色：萧言");
            } else {
                player = new Mo();
                out.println("你选择了角色：莫");
                System.out.println("[客户端连接] " + playerName + " 选择了角色：莫");
            }
            player.name = playerName;
            
            server.registerClient(playerName, this);
            // 分配初始场景（新手村）
            currentScene = server.getMapManager().getStartScene();
            out.println("角色创建成功: " + playerName);
            System.out.println("[客户端连接] " + playerName + " 角色创建成功，已加入游戏，初始场景: " + (currentScene != null ? currentScene.scene.getName() : "无"));
            sendStatus();
            showMainMenu();
            
            // 启动输入读取线程
            System.out.println("[输入线程] " + playerName + " 准备启动输入读取线程...");
            Thread inputThread = new Thread(() -> {
                System.out.println("[输入线程] " + playerName + " 输入读取线程已启动，开始监听输入...");
                try {
            String input;
            while (running && (input = in.readLine()) != null) {
                        // 去掉首尾空白，统一格式
                        input = input.trim();
                        // 跳过空输入
                        if (input.isEmpty()) {
                            System.out.println("[输入线程] " + playerName + " 收到空输入，跳过");
                    continue;
                }

                        // 如果正在PK或战斗中，将输入放入队列供战斗系统读取
                        boolean inCombat = isInPvP || isInBattle;
                        System.out.println("[输入线程-检查] " + playerName + " 收到输入: " + input + " (isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ", inCombat: " + inCombat + ")");
                        
                        if (inCombat) {
                            // 使用容量为1的队列，自动丢弃旧输入，只保留最新输入
                            // 如果队列已满（有旧输入），poll()移除旧输入，然后offer()放入新输入
                            if (!inputQueue.offer(input)) {
                                // 队列已满，移除旧输入
                                String oldInput = inputQueue.poll();
                                // 放入新输入
                                inputQueue.offer(input);
                                System.out.println("[输入线程] " + playerName + " 队列已满，移除旧输入: " + oldInput + "，放入新输入: " + input);
                            }
                            System.out.println("[输入线程] " + playerName + " 在战斗模式下收到输入，已放入队列: " + input + 
                                " (队列大小: " + inputQueue.size() + ", isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ")");
                        } else {
                            // 不在战斗中，直接处理命令
                            System.out.println("[输入线程] " + playerName + " 在非战斗模式下处理命令: " + input + " (isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ")");
                handleCommand(input);
                        }
                    }
                    System.out.println("[输入线程] " + playerName + " 输入读取线程结束（连接已关闭）");
                } catch (IOException e) {
                    System.out.println("[输入线程] " + playerName + " 输入线程异常: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    running = false;
                    System.out.println("[输入线程] " + playerName + " 输入线程已停止，running = false");
                }
            });
            inputThread.setDaemon(true);
            inputThread.setName("InputThread-" + playerName); // 设置线程名称便于调试
            inputThread.start();
            System.out.println("[输入线程] " + playerName + " 输入读取线程已启动");
            
            // 主线程等待输入线程结束
            try {
                inputThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
        } catch (IOException e) {
            System.out.println("[客户端断开] " + (playerName != null ? playerName : "未知玩家") + " 断开连接: " + e.getMessage());
        } finally {
            if (playerName != null) {
                System.out.println("[客户端断开] " + playerName + " 已从服务器移除");
                server.removeClient(playerName);
            }
            closeConnection();
        }
    }
    
    private void handleCommand(String command) {
        try {
            if (command == null || command.trim().isEmpty()) {
                return;
            }
            
            command = command.trim();
            
            // 服务器端显示客户端操作日志
            System.out.println("[客户端操作] " + playerName + " 执行命令: " + command);
            
            // 如果正在PK中，将命令传递给PK系统处理
            if (isInPvP) {
                // PK中的命令由NetworkBattleSystem处理，这里只记录
                return;
            }
            
            // 如果正在打怪中，检查是否是战斗命令
            if (isInBattle) {
                // 战斗中的命令由NetworkBattleSystem处理，这里只记录
                return;
            }
            
            String[] parts = command.split(" ", 2);
            String cmd = parts[0].toLowerCase();
            String param = parts.length > 1 ? parts[1] : "";
            
            switch (cmd) {
                case "1":
                case "battle":
                case "fight":
                    // 检查当前场景是否允许打怪
                    if (currentScene == null || !currentScene.scene.getAvailableActions().contains("1")) {
                        out.println("当前场景不允许打怪！请切换到新手村、武林广场或练功房。");
                        showMainMenu();
                        return;
                    }
                    System.out.println("[客户端操作] " + playerName + " 在 " + currentScene.scene.getName() + " 开始打怪");
                    startBattle();
                    break;
                case "2":
                case "npc":
                case "talk":
                    // 检查当前场景是否允许NPC对话
                    if (currentScene == null || !currentScene.scene.getAvailableActions().contains("2")) {
                        out.println("当前场景没有NPC可以对话！请切换到有NPC的场景。");
                        showMainMenu();
                        return;
                    }
                    System.out.println("[客户端操作] " + playerName + " 在 " + currentScene.scene.getName() + " 与NPC对话");
                    talkToNpc();
                    break;
                case "3":
                case "status":
                case "info":
                    System.out.println("[客户端操作] " + playerName + " 查看状态");
                    sendStatus();
                    showMainMenu();
                    break;
                case "4":
                case "save":
                    System.out.println("[客户端操作] " + playerName + " 保存游戏");
                    saveGame();
                    showMainMenu();
                    break;
                case "5":
                case "load":
                    System.out.println("[客户端操作] " + playerName + " 加载游戏");
                    loadGame();
                    showMainMenu();
                    break;
                case "6":
                case "list":
                case "players":
                    System.out.println("[客户端操作] " + playerName + " 查看在线玩家列表");
                    listPlayers();
                    showMainMenu();
                    break;
                case "7":
                case "chat":
                    if (param.isEmpty()) {
                        out.println("用法: chat <消息>");
                        out.println("或者直接输入消息内容（不需要chat前缀）");
                        showMainMenu();
                    } else {
                        System.out.println("[客户端操作] " + playerName + " 发送聊天消息: " + param);
                        server.broadcastMessage(playerName, param, playerName);
                        out.println("消息已发送！");
                        showMainMenu();
                    }
                    break;
                case "8":
                case "pk":
                case "challenge":
                    // 检查当前场景是否允许PK
                    if (currentScene == null || !currentScene.scene.getAvailableActions().contains("8")) {
                        out.println("当前场景不允许PK！请切换到武林广场。");
                        showMainMenu();
                        return;
                    }
                    if (param.isEmpty()) {
                        out.println("用法: pk <玩家名>");
                        showMainMenu();
                    } else {
                        // 检查自己是否在PK中
                        if (isInPvP) {
                            out.println("你正在PK中，无法发起新的挑战！");
                            return;
                        }
                        System.out.println("[客户端操作] " + playerName + " 在 " + currentScene.scene.getName() + " 向 " + param + " 发起PK挑战");
                        challengePlayer(param);
                    }
                    break;
                case "accept":
                    if (pendingChallenge != null) {
                        System.out.println("[客户端操作] " + playerName + " 接受了 " + pendingChallenge.playerName + " 的PK挑战");
                        acceptChallenge();
                    } else {
                        out.println("当前没有待处理的挑战");
                        showMainMenu();
                    }
                    break;
                case "reject":
                    if (pendingChallenge != null) {
                        System.out.println("[客户端操作] " + playerName + " 拒绝了 " + pendingChallenge.playerName + " 的PK挑战");
                        out.println("你拒绝了 " + pendingChallenge.playerName + " 的挑战。");
                        pendingChallenge.out.println(playerName + " 拒绝了你的挑战。");
                        pendingChallenge = null;
                        showMainMenu();
                    } else {
                        out.println("当前没有待处理的挑战");
                        showMainMenu();
                    }
                    break;
                case "9":
                case "scene":
                case "change":
                    System.out.println("[客户端操作] " + playerName + " 切换场景");
                    changeScene();
                    showMainMenu();
                    break;
                case "10":
                case "train":
                case "altar":
                    System.out.println("[客户端操作] " + playerName + " 尝试使用修炼台");
                    useTrainingAltar();
                    break;
                case "0":
                case "quit":
                case "exit":
                    System.out.println("[客户端操作] " + playerName + " 退出游戏");
                    out.println("再见！");
                    running = false;
                    break;
                default:
                    // 如果不是已知命令，检查是否是聊天消息（不以数字开头的消息）
                    if (!cmd.matches("^\\d+$")) {
                        // 可能是聊天消息，直接发送
                        System.out.println("[客户端操作] " + playerName + " 发送聊天消息: " + command);
                        server.broadcastMessage(playerName, command, playerName);
                        out.println("消息已发送！");
                        showMainMenu();
                    } else {
                        System.out.println("[客户端操作] " + playerName + " 执行了未知命令: " + command);
                        out.println("未知命令: " + command);
                        showMainMenu();
                    }
            }
        } catch (Exception e) {
            out.println("处理命令错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showMainMenu() {
        // 如果正在PK或战斗中，不显示菜单
        if (isInPvP || isInBattle) {
            return;
        }
        
        // 显示美化地图
        if (currentScene != null) {
            out.println(server.getMapManager().generateMapDisplay(currentScene));
            out.println("场景描述: " + currentScene.scene.getDescription());
        } else {
            // 如果没有场景，分配初始场景
            currentScene = server.getMapManager().getStartScene();
            if (currentScene != null) {
                out.println(server.getMapManager().generateMapDisplay(currentScene));
                out.println("场景描述: " + currentScene.scene.getDescription());
            }
        }
        
        // 根据当前场景显示可用操作
        out.println("\n===== 主菜单 =====");
        out.println("【当前场景可用操作】");
        if (currentScene != null) {
            List<String> actions = currentScene.scene.getAvailableActions();
            if (actions.contains("1")) {
                out.println("1. 打怪练级 ✓");
            } else {
                out.println("1. 打怪练级 (当前场景不可用)");
            }
            if (actions.contains("2")) {
                out.println("2. NPC对话 ✓");
            } else {
                out.println("2. NPC对话 (当前场景不可用)");
            }
            if (actions.contains("8")) {
                out.println("8. PK挑战 (pk <玩家名>) ✓");
            }
        } else {
            out.println("1. 打怪练级");
            out.println("2. NPC对话");
            out.println("8. PK挑战 (pk <玩家名>)");
        }
        
        out.println("\n【通用操作】");
        out.println("3. 查看状态");
        out.println("4. 保存游戏");
        out.println("5. 加载游戏");
        out.println("6. 查看在线玩家");
        out.println("7. 聊天 (chat <消息> 或直接输入消息)");
        out.println("9. 切换场景");
        out.println("10. 武林秘籍修炼台 " + 
            (server.getTrainingAltar().isOccupied() ? 
                "[使用中：" + server.getTrainingAltar().getCurrentUser() + "]" : "[可用]"));
        out.println("0. 退出游戏");
        out.println("\n请选择操作:");
    }
    
    private void startBattle() {
        try {
            // 检查是否在PK中
            if (isInPvP) {
                out.println("你正在PK中，无法开始打怪！");
                return;
            }
            
            isInBattle = true;
            Monster monster = server.generateMonster();
            System.out.println("[战斗系统] " + playerName + " 遇到怪物: " + monster.getName() + " (等级: " + monster.getGrade() + ", 血量: " + monster.getLifeValue() + ")");
            out.println("\n你遇到了怪物：" + monster.getName());
            out.println("等级: " + monster.getGrade() + ", 血量: " + monster.getLifeValue());
            out.println("战斗开始！");
            
            // 将PVE战斗也放到独立线程中，保持架构一致性
            ClientHandler handler = this;
            Figure playerRef = this.player;
            Monster monsterRef = monster;
            
            Thread battleThread = new Thread(() -> {
                try {
                    System.out.println("[战斗系统] PVE战斗线程启动: " + handler.getPlayerName());
                    NetworkBattleSystem.fight(playerRef, monsterRef, handler);
                    System.out.println("[战斗系统] " + handler.getPlayerName() + " PVE战斗结束");
                } catch (Exception e) {
                    System.out.println("[战斗系统] PVE战斗线程异常: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    handler.setInBattle(false);
                    handler.showMainMenu();
                    System.out.println("[战斗系统] PVE战斗线程结束，已清除战斗状态");
                }
            });
            battleThread.setName("BattleThread-" + handler.getPlayerName());
            battleThread.start();
            
            // 注意：不要在这里清除isInBattle，因为战斗在独立线程中运行
            // 战斗状态会在战斗线程结束时清除
        } catch (Exception e) {
            isInBattle = false;
            System.out.println("[战斗系统] " + playerName + " 战斗发生错误: " + e.getMessage());
            out.println("战斗错误: " + e.getMessage());
            e.printStackTrace();
            showMainMenu();
        }
    }
    
    private void talkToNpc() {
        try {
            Npc npc = server.getRandomNpc();
            System.out.println("[NPC对话] " + playerName + " 遇到NPC: " + npc.getName() + ", 问题: " + npc.getQuestion());
            out.println("\n你遇到 NPC：" + npc.getName());
            if (npc.getDialogue() != null && !npc.getDialogue().isEmpty()) {
                out.println(npc.getDialogue());
            }
            out.println("\n" + npc.getQuestion());
            
            // 如果是选择题，显示选项
            if (npc.hasOptions()) {
                out.println("\n请选择答案:");
                List<String> options = npc.getOptions();
                for (int i = 0; i < options.size(); i++) {
                    out.println((i + 1) + ". " + options.get(i));
                }
                out.println("请输入选项编号(1-" + options.size() + "):");
            } else {
                out.println("请输入答案:");
            }
            
            String answer = in.readLine();
            System.out.println("[NPC对话] " + playerName + " 回答: " + (answer != null ? answer : "空"));
            if (npc.checkAnswer(answer)) {
                System.out.println("[NPC对话] " + playerName + " 回答正确，获得经验: " + npc.getExp());
                out.println("\n回答正确！获得经验：" + npc.getExp());
                player.gainExp(npc.getExp());
                sendStatus();
            } else {
                System.out.println("[NPC对话] " + playerName + " 回答错误，正确答案: " + npc.getAnswer());
                if (npc.hasOptions()) {
                    out.println("\n回答错误，没有奖励。");
                } else {
                    out.println("\n回答错误，没有奖励。正确答案是: " + npc.getAnswer());
                }
            }
            
            showMainMenu();
        } catch (IOException e) {
            System.out.println("[NPC对话] " + playerName + " NPC对话发生错误: " + e.getMessage());
            out.println("NPC对话错误: " + e.getMessage());
            showMainMenu();
        }
    }
    
    public void sendStatus() {
        out.println("\n===== 角色状态 =====");
        out.println("名字：" + player.getName());
        out.println("等级：" + player.getGrade());
        out.println("血量：" + player.getLifeValue() + "/" + player.getMaxLifeValue());
        out.println("攻击：" + player.getAttack());
        out.println("防御：" + player.getDefend());
        out.println("经验：" + player.getExp());
        out.println("技能：");
        for (Skill s : player.getSkills()) {
            out.println("- " + s.getName());
        }
    }
    
    private void saveGame() {
        try {
            // 确定角色类型
            String roleType = (player instanceof Mo) ? "Mo" : "XiaoYan";
            String json = JsonUtil.figureToJson(player, roleType);
            String fileName = "src/com/mudgame/save_" + playerName + ".json";
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                writer.print(json);
            }
            System.out.println("[存档系统] " + playerName + " 保存游戏成功，角色类型: " + roleType);
            out.println("游戏已保存！");
        } catch (IOException e) {
            System.out.println("[存档系统] " + playerName + " 保存游戏失败: " + e.getMessage());
            out.println("保存失败：" + e.getMessage());
        }
    }
    
    private void loadGame() {
        try {
            String fileName = "src/com/mudgame/save_" + playerName + ".json";
            File file = new File(fileName);
            if (!file.exists()) {
                System.out.println("[存档系统] " + playerName + " 尝试加载存档，但文件不存在: " + fileName);
                out.println("没有找到存档文件！");
                return;
            }
            
            System.out.println("[存档系统] " + playerName + " 开始加载存档: " + fileName);
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }
            
            Figure loaded = JsonUtil.figureFromJson(json.toString());
            if (loaded != null) {
                String loadedRoleType = (loaded instanceof Mo) ? "Mo" : "XiaoYan";
                System.out.println("[存档系统] " + playerName + " 加载存档成功，角色类型: " + loadedRoleType + ", 等级: " + loaded.getGrade());
                player = loaded;
                player.name = playerName; // 保持当前玩家名
                out.println("存档加载成功！");
                sendStatus();
            } else {
                System.out.println("[存档系统] " + playerName + " 加载存档失败：数据格式错误");
                out.println("加载失败：数据格式错误");
            }
        } catch (IOException e) {
            System.out.println("[存档系统] " + playerName + " 加载存档失败: " + e.getMessage());
            out.println("加载失败：" + e.getMessage());
        }
    }
    
    private void listPlayers() {
        System.out.println("[玩家列表] " + playerName + " 查看在线玩家列表");
        out.println("\n===== 在线玩家 =====");
        Map<String, ClientHandler> clients = server.getClients();
        int index = 1;
        for (String name : clients.keySet()) {
            ClientHandler handler = clients.get(name);
            if (handler != this) {
                String status = "";
                if (handler.isInPvP()) {
                    status = " [PK中]";
                } else if (handler.isInBattle()) {
                    status = " [战斗中]";
                } else {
                    status = " (可挑战)";
                }
                out.println(index + ". " + name + status);
                index++;
            }
        }
        if (index == 1) {
            out.println("当前只有你一人在线。");
        }
    }
    
    private void challengePlayer(String targetName) {
        ClientHandler target = server.getClientHandler(targetName);
        if (target == null || target == this) {
            System.out.println("[PK系统] " + playerName + " 向 " + targetName + " 发起挑战失败：玩家不在线或无效");
            out.println("玩家 " + targetName + " 不在线或无效！");
            showMainMenu();
            return;
        }
        
        // 检查目标玩家是否在PK中
        if (target.isInPvP()) {
            System.out.println("[PK系统] " + playerName + " 向 " + targetName + " 发起挑战失败：目标玩家正在PK中");
            out.println("玩家 " + targetName + " 正在PK中，无法接受挑战！");
            showMainMenu();
            return;
        }
        
        // 检查目标玩家是否正在打怪
        if (target.isInBattle()) {
            System.out.println("[PK系统] " + playerName + " 向 " + targetName + " 发起挑战失败：目标玩家正在战斗中");
            out.println("玩家 " + targetName + " 正在战斗中，无法接受挑战！");
            showMainMenu();
            return;
        }
        
        System.out.println("[PK系统] " + playerName + " 向 " + targetName + " 发起PK挑战");
        out.println("向 " + targetName + " 发起挑战...");
        target.sendMessage(playerName + " 向你发起PK挑战！(输入 accept 接受，或 reject 拒绝)");
        target.setPendingChallenge(this);
        showMainMenu();
    }
    
    private ClientHandler pendingChallenge = null;
    private volatile boolean isInPvP = false;  // 是否正在PK中
    private volatile boolean isInBattle = false;  // 是否正在打怪中
    
    public void setPendingChallenge(ClientHandler challenger) {
        this.pendingChallenge = challenger;
    }
    
    public ClientHandler getPendingChallenge() {
        return pendingChallenge;
    }
    
    public boolean isInPvP() {
        return isInPvP;
    }
    
    public void setInPvP(boolean inPvP) {
        this.isInPvP = inPvP;
    }
    
    public boolean isInBattle() {
        return isInBattle;
    }
    
    public void setInBattle(boolean inBattle) {
        this.isInBattle = inBattle;
    }
    
    public void acceptChallenge() {
        if (pendingChallenge != null) {
            // 检查自己是否在PK中或战斗中（被挑战者）
            if (isInPvP || isInBattle) {
                out.println("你正在战斗中，无法接受挑战！");
                pendingChallenge.out.println(playerName + " 正在战斗中，无法接受挑战。");
                pendingChallenge = null;
                showMainMenu();
                return;
            }
            
            // 检查挑战者是否在PK中或战斗中
            if (pendingChallenge.isInPvP() || pendingChallenge.isInBattle()) {
                out.println("挑战者正在战斗中，挑战已取消。");
                pendingChallenge = null;
                showMainMenu();
                return;
            }
            
            // 先清空两个玩家的输入队列，避免残留数据影响PK（特别是第二个玩家的"accept"命令）
            this.clearInputQueue();
            pendingChallenge.clearInputQueue();
            
            // 设置PK状态（必须在清空队列之后设置，确保后续输入进入队列）
            this.isInPvP = true;
            pendingChallenge.isInPvP = true;
            
            System.out.println("[PK系统] " + playerName + " 接受了 " + pendingChallenge.playerName + " 的PK挑战");
            System.out.println("[PK系统] 玩家1 (" + pendingChallenge.playerName + ") isInPvP: " + pendingChallenge.isInPvP);
            System.out.println("[PK系统] 玩家2 (" + playerName + ") isInPvP: " + this.isInPvP);
            
            out.println("你接受了 " + pendingChallenge.playerName + " 的挑战！");
            pendingChallenge.out.println(playerName + " 接受了你的挑战！");
            
            // 短暂延迟，确保输入线程状态已更新
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 关键修复：将PVP战斗逻辑从输入线程中分离，在独立线程中运行
            // 这样可以避免死锁：输入线程继续读取Socket，战斗逻辑在独立线程中等待队列输入
            ClientHandler handler1 = pendingChallenge;
            ClientHandler handler2 = this;
            Figure player1 = pendingChallenge.player;
            Figure player2 = this.player;
            
            Thread pvpThread = new Thread(() -> {
                try {
                    System.out.println("[PK系统] PVP战斗线程启动: " + handler1.getPlayerName() + " vs " + handler2.getPlayerName());
                    NetworkBattleSystem.playerVsPlayer(player1, player2, handler1, handler2, server);
                    System.out.println("[PK系统] " + handler2.getPlayerName() + " 与 " + handler1.getPlayerName() + " 的PVP战斗结束");
                } catch (Exception e) {
                    System.out.println("[PK系统] PVP战斗线程异常: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    // 确保两个玩家都回血（防止异常情况下没有回血）
                    try {
                        if (player1 != null && !player1.isAlive()) {
                            player1.healToFull();
                            System.out.println("[PK系统] 玩家1 (" + handler1.getPlayerName() + ") 已回血");
                        }
                        if (player2 != null && !player2.isAlive()) {
                            player2.healToFull();
                            System.out.println("[PK系统] 玩家2 (" + handler2.getPlayerName() + ") 已回血");
                        }
                    } catch (Exception e) {
                        System.out.println("[PK系统] 回血时发生异常: " + e.getMessage());
                    }
            
            // 清除PK状态
                    handler1.setInPvP(false);
                    handler2.setInPvP(false);
                    handler1.pendingChallenge = null;
                    handler2.pendingChallenge = null;
                    
                    // 确保两个玩家都显示菜单
                    try {
                        handler1.showMainMenu();
                        handler2.showMainMenu();
                        System.out.println("[PK系统] 已为两个玩家显示菜单");
                    } catch (Exception e) {
                        System.out.println("[PK系统] 显示菜单时发生异常: " + e.getMessage());
                    }
                    
                    System.out.println("[PK系统] PVP战斗线程结束，已清除PK状态");
                }
            });
            pvpThread.setName("PVPThread-" + handler1.getPlayerName() + "-vs-" + handler2.getPlayerName());
            pvpThread.start();
            
            // 注意：不要在这里清除PK状态，因为战斗在独立线程中运行
            // PK状态会在战斗线程结束时清除
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public BufferedReader getInput() {
        // 返回一个包装的BufferedReader，在战斗模式下从队列读取
        // 注意：这个方法在战斗开始时被调用一次，返回的BufferedReader会被战斗系统持续使用
        return new BufferedReader(new InputStreamReader(new InputStream() {
            @Override
            public int read() {
                // 实际不会被调用，readLine 已被重写
                return -1;
            }
        })) {
            @Override
            public String readLine() throws IOException {
                // 在战斗中，从队列读取（阻塞等待，直到有输入）
                // 注意：每次调用readLine()时都重新检查状态，而不是在创建时捕获
                boolean inCombat = isInPvP || isInBattle;
                System.out.println("[输入队列-readLine] " + playerName + " readLine()被调用 (isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ", inCombat: " + inCombat + ", 队列大小: " + inputQueue.size() + ")");
                
                if (inCombat) {
                    try {
                        System.out.println("[输入队列] " + playerName + " 等待从队列读取输入... (当前队列大小: " + inputQueue.size() + ", isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ")");
                        String line = inputQueue.take(); // 阻塞等待，直到队列中有输入
                        System.out.println("[输入队列] " + playerName + " 从队列取到输入: " + (line != null ? line : "null"));
                        
                        // 过滤空行
                        while (line != null && line.trim().isEmpty() && (isInPvP || isInBattle)) {
                            System.out.println("[输入队列] " + playerName + " 跳过空行，继续等待...");
                            line = inputQueue.take();
                        }
                        if (line != null) {
                            line = line.trim();
                        }
                        System.out.println("[输入队列] " + playerName + " 从队列读取成功: " + (line != null ? line : "null") + " (剩余队列大小: " + inputQueue.size() + ")");
                        return line;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.println("[输入队列] " + playerName + " 读取输入被中断");
                        throw new IOException("读取输入被中断", e);
                    }
                }
                // 不在战斗中，不应该调用这个方法（应该由主循环处理）
                System.out.println("[输入队列] " + playerName + " 警告：不在战斗模式下调用readLine() (isInPvP: " + isInPvP + ", isInBattle: " + isInBattle + ")");
                return null;
            }
        };
    }
    
    public PrintWriter getOutput() {
        return out;
    }
    
    public Figure getPlayer() {
        return player;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * 清空输入队列（用于PK开始时清理残留数据）
     */
    public void clearInputQueue() {
        inputQueue.clear();
        System.out.println("[输入队列] " + playerName + " 输入队列已清空");
    }
    
    private void changeScene() {
        if (currentScene == null) {
            currentScene = server.getMapManager().getStartScene();
        }
        
        out.println("\n════════════════════════════════════════════════════════");
        out.println("                    移动菜单                    ");
        out.println("════════════════════════════════════════════════════════");
        out.println("当前位置: " + currentScene.scene.getName());
        out.println("描述: " + currentScene.scene.getDescription());
        out.println("\n可移动方向:");
        
        List<String> availableDirections = new ArrayList<>();
        if (currentScene.north != null) {
            out.println("  ↑ 北 (n/north/上/北) → " + currentScene.north.scene.getName());
            availableDirections.add("n");
            availableDirections.add("north");
            availableDirections.add("上");
            availableDirections.add("北");
        }
        if (currentScene.south != null) {
            out.println("  ↓ 南 (s/south/下/南) → " + currentScene.south.scene.getName());
            availableDirections.add("s");
            availableDirections.add("south");
            availableDirections.add("下");
            availableDirections.add("南");
        }
        if (currentScene.east != null) {
            out.println("  → 东 (e/east/右/东) → " + currentScene.east.scene.getName());
            availableDirections.add("e");
            availableDirections.add("east");
            availableDirections.add("右");
            availableDirections.add("东");
        }
        if (currentScene.west != null) {
            out.println("  ← 西 (w/west/左/西) → " + currentScene.west.scene.getName());
            availableDirections.add("w");
            availableDirections.add("west");
            availableDirections.add("左");
            availableDirections.add("西");
        }
        
        if (availableDirections.isEmpty()) {
            out.println("  无可用方向");
            showMainMenu();
            return;
        }
        
        out.println("\n════════════════════════════════════════════════════════");
        out.println("输入方向移动，或输入 'list' 查看所有场景，或输入 'map' 查看地图:");
        
        try {
            String input = in.readLine();
            if (input == null || input.trim().isEmpty()) {
                showMainMenu();
                return;
            }
            
            input = input.trim().toLowerCase();
            
            // 显示所有场景列表
            if (input.equals("list")) {
                out.println(server.getMapManager().generateMapLegend());
                showMainMenu();
                return;
            }
            
            // 显示地图
            if (input.equals("map")) {
                out.println(server.getMapManager().generateMapDisplay(currentScene));
                showMainMenu();
                return;
            }
            
            // 尝试方向移动
            MapManager.SceneNode newScene = server.getMapManager().move(currentScene, input);
            
            if (newScene != null) {
                String oldSceneName = currentScene.scene.getName();
                currentScene = newScene;
                System.out.println("[场景切换] " + playerName + " 从 " + oldSceneName + " 移动到: " + currentScene.scene.getName());
                out.println("\n════════════════════════════════════════════════════════");
                out.println("你已到达【" + currentScene.scene.getName() + "】");
                out.println(currentScene.scene.getDescription());
                out.println("════════════════════════════════════════════════════════");
            } else {
                out.println("\n无效的方向！请输入: n/north/上/北, s/south/下/南, e/east/右/东, w/west/左/西");
                out.println("或输入 'list' 查看所有场景，'map' 查看地图");
            }
        } catch (Exception e) {
            out.println("\n移动失败: " + e.getMessage());
        }
    }
    
    private void useTrainingAltar() {
        TrainingAltar altar = server.getTrainingAltar();
        
        // 检查是否在战斗或PK中
        if (isInPvP || isInBattle) {
            out.println("你正在战斗中，无法使用修炼台！");
            showMainMenu();
            return;
        }
        
        // 尝试获取修炼台使用权
        if (altar.tryStartTraining(playerName)) {
            System.out.println("[修炼台] " + playerName + " 开始使用修炼台");
            try {
                // 执行修炼过程
                altar.performTraining(player, in, out, server, playerName);
            } finally {
                // 确保修炼结束后释放修炼台
                altar.endTraining();
                System.out.println("[修炼台] " + playerName + " 结束使用修炼台");
            }
            showMainMenu();
        } else {
            out.println("\n════════════════════════════════════════════════════════");
            out.println("修炼台正在被使用中！");
            out.println("当前使用者：" + altar.getCurrentUser());
            out.println("请稍后再试...");
            out.println("════════════════════════════════════════════════════════");
            System.out.println("[修炼台] " + playerName + " 尝试使用修炼台失败（已被占用：" + altar.getCurrentUser() + "）");
            showMainMenu();
        }
    }
    
    private void closeConnection() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

