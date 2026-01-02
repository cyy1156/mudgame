package com.mudgame;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * MUD游戏网络服务器
 * 支持最多4个客户端同时连接
 */
public class NetworkGameServer {
    private static final int PORT = 4444;
    private static final int MAX_CLIENTS = 4;
    
    private ServerSocket serverSocket;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private List<Npc> npcs = new ArrayList<>();
    private Random random = new Random();
    
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
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(exclude)) {
                entry.getValue().sendMessage("[" + from + "] " + message);
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
            } else {
                player = new Mo();
                out.println("你选择了角色：莫");
            }
            player.name = playerName;
            
            server.registerClient(playerName, this);
            out.println("角色创建成功: " + playerName);
            sendStatus();
            showMainMenu();
            
            // 处理客户端消息
            String input;
            while (running && (input = in.readLine()) != null) {
                handleCommand(input);
            }
            
        } catch (IOException e) {
            System.out.println("客户端 " + playerName + " 断开连接: " + e.getMessage());
        } finally {
            if (playerName != null) {
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
            String[] parts = command.split(" ", 2);
            String cmd = parts[0].toLowerCase();
            String param = parts.length > 1 ? parts[1] : "";
            
            switch (cmd) {
                case "1":
                case "battle":
                case "fight":
                    startBattle();
                    break;
                case "2":
                case "npc":
                case "talk":
                    talkToNpc();
                    break;
                case "3":
                case "status":
                case "info":
                    sendStatus();
                    showMainMenu();
                    break;
                case "4":
                case "save":
                    saveGame();
                    showMainMenu();
                    break;
                case "5":
                case "load":
                    loadGame();
                    showMainMenu();
                    break;
                case "6":
                case "list":
                case "players":
                    listPlayers();
                    showMainMenu();
                    break;
                case "7":
                case "chat":
                    if (param.isEmpty()) {
                        out.println("用法: chat <消息>");
                    } else {
                        server.broadcastMessage(playerName, param, playerName);
                    }
                    showMainMenu();
                    break;
                case "8":
                case "pk":
                case "challenge":
                    if (param.isEmpty()) {
                        out.println("用法: pk <玩家名>");
                        showMainMenu();
                    } else {
                        challengePlayer(param);
                    }
                    break;
                case "accept":
                    if (pendingChallenge != null) {
                        acceptChallenge();
                    } else {
                        out.println("当前没有待处理的挑战");
                        showMainMenu();
                    }
                    break;
                case "reject":
                    if (pendingChallenge != null) {
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
                case "quit":
                case "exit":
                    out.println("再见！");
                    running = false;
                    break;
                default:
                    out.println("未知命令: " + command);
                    showMainMenu();
            }
        } catch (Exception e) {
            out.println("处理命令错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void showMainMenu() {
        out.println("\n===== 主菜单 =====");
        out.println("1. 打怪练级");
        out.println("2. NPC对话");
        out.println("3. 查看状态");
        out.println("4. 保存游戏");
        out.println("5. 加载游戏");
        out.println("6. 查看在线玩家");
        out.println("7. 聊天 (chat <消息>)");
        out.println("8. PK挑战 (pk <玩家名>)");
        out.println("9. 退出游戏");
        out.println("请选择(1-9):");
    }
    
    private void startBattle() {
        try {
            Monster monster = server.generateMonster();
            out.println("\n你遇到了怪物：" + monster.getName());
            out.println("等级: " + monster.getGrade() + ", 血量: " + monster.getLifeValue());
            out.println("战斗开始！");
            
            NetworkBattleSystem.fight(player, monster, this, server);
            
            showMainMenu();
        } catch (Exception e) {
            out.println("战斗错误: " + e.getMessage());
            e.printStackTrace();
            showMainMenu();
        }
    }
    
    private void talkToNpc() {
        try {
            Npc npc = server.getRandomNpc();
            out.println("\n你遇到 NPC：" + npc.getName());
            out.println(npc.getQuestion());
            out.println("请输入答案:");
            
            String answer = in.readLine();
            if (npc.checkAnswer(answer)) {
                out.println("回答正确！获得经验：" + npc.getExp());
                player.gainExp(npc.getExp());
                sendStatus();
            } else {
                out.println("回答错误，没有奖励。正确答案是: " + npc.getAnswer());
            }
            
            showMainMenu();
        } catch (IOException e) {
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
            out.println("游戏已保存！");
        } catch (IOException e) {
            out.println("保存失败：" + e.getMessage());
        }
    }
    
    private void loadGame() {
        try {
            String fileName = "src/com/mudgame/save_" + playerName + ".json";
            File file = new File(fileName);
            if (!file.exists()) {
                out.println("没有找到存档文件！");
                return;
            }
            
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }
            
            Figure loaded = JsonUtil.figureFromJson(json.toString());
            if (loaded != null) {
                player = loaded;
                player.name = playerName; // 保持当前玩家名
                out.println("存档加载成功！");
                sendStatus();
            } else {
                out.println("加载失败：数据格式错误");
            }
        } catch (IOException e) {
            out.println("加载失败：" + e.getMessage());
        }
    }
    
    private void listPlayers() {
        out.println("\n===== 在线玩家 =====");
        Map<String, ClientHandler> clients = server.getClients();
        int index = 1;
        for (String name : clients.keySet()) {
            ClientHandler handler = clients.get(name);
            if (handler != this) {
                out.println(index + ". " + name + " (可挑战)");
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
            out.println("玩家 " + targetName + " 不在线或无效！");
            showMainMenu();
            return;
        }
        
        out.println("向 " + targetName + " 发起挑战...");
        target.sendMessage(playerName + " 向你发起PK挑战！(输入 accept 接受，或 reject 拒绝)");
        target.setPendingChallenge(this);
        showMainMenu();
    }
    
    private ClientHandler pendingChallenge = null;
    
    public void setPendingChallenge(ClientHandler challenger) {
        this.pendingChallenge = challenger;
    }
    
    public ClientHandler getPendingChallenge() {
        return pendingChallenge;
    }
    
    public void acceptChallenge() {
        if (pendingChallenge != null) {
            out.println("你接受了 " + pendingChallenge.playerName + " 的挑战！");
            pendingChallenge.out.println(playerName + " 接受了你的挑战！");
            NetworkBattleSystem.playerVsPlayer(pendingChallenge.player, this.player, 
                                              pendingChallenge, this, server);
            pendingChallenge = null;
            this.pendingChallenge = null;
        }
    }
    
    public void sendMessage(String message) {
        out.println(message);
    }
    
    public BufferedReader getInput() {
        return in;
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

