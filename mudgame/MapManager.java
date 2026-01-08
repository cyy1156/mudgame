package com.mudgame;

import java.util.*;

/**
 * 地图管理器 - 管理场景的位置和连接关系
 */
public class MapManager {
    private Map<String, SceneNode> sceneMap = new HashMap<>();
    private SceneNode[][] mapGrid;  // 地图网格
    private int mapWidth = 5;
    private int mapHeight = 5;
    
    /**
     * 场景节点 - 包含场景信息和位置
     */
    public static class SceneNode {
        public Scene scene;
        public int x, y;  // 坐标
        public SceneNode north, south, east, west;  // 四个方向
        
        public SceneNode(Scene scene, int x, int y) {
            this.scene = scene;
            this.x = x;
            this.y = y;
        }
    }
    
    public MapManager() {
        mapGrid = new SceneNode[mapHeight][mapWidth];
        initializeMap();
    }
    
    /**
     * 初始化地图布局
     */
    private void initializeMap() {
        SceneNode scene1 = new SceneNode(new Scene("新手村", "宁静的村庄，适合新手修炼"), 2, 0);
        SceneNode scene2 = new SceneNode(new Scene("武林广场", "江湖人士聚集地，可以切磋武艺"), 2, 2);
        SceneNode scene3 = new SceneNode(new Scene("藏经阁", "存放武学秘籍的地方"), 0, 2);
        SceneNode scene4 = new SceneNode(new Scene("练功房", "专门用于修炼的场所"), 4, 2);
        SceneNode scene5 = new SceneNode(new Scene("江湖客栈", "休息和交流的地方"), 2, 4);
        
        // 设置场景可用操作
        scene1.scene.addAction("1");
        scene1.scene.addAction("2");
        
        scene2.scene.addAction("1");
        scene2.scene.addAction("2");
        scene2.scene.addAction("8");
        
        scene3.scene.addAction("2");
        scene3.scene.addAction("3");
        
        scene4.scene.addAction("1");
        scene4.scene.addAction("3");
        
        scene5.scene.addAction("2");
        scene5.scene.addAction("6");
        scene5.scene.addAction("7");
        
        // 建立连接关系
        scene1.south = scene2;
        scene2.north = scene1;
        scene2.west = scene3;
        scene2.east = scene4;
        scene2.south = scene5;
        scene3.east = scene2;
        scene4.west = scene2;
        scene5.north = scene2;
        
        // 存储到地图
        sceneMap.put("新手村", scene1);
        sceneMap.put("武林广场", scene2);
        sceneMap.put("藏经阁", scene3);
        sceneMap.put("练功房", scene4);
        sceneMap.put("江湖客栈", scene5);
        
        mapGrid[0][2] = scene1;
        mapGrid[2][2] = scene2;
        mapGrid[2][0] = scene3;
        mapGrid[2][4] = scene4;
        mapGrid[4][2] = scene5;
    }
    
    public SceneNode getSceneNode(String sceneName) {
        return sceneMap.get(sceneName);
    }
    
    public Collection<SceneNode> getAllScenes() {
        return sceneMap.values();
    }
    
    public SceneNode getStartScene() {
        return sceneMap.get("新手村");
    }
    
    /**
     * 生成美化地图显示（使用纯ASCII确保对齐）
     */
    public String generateMapDisplay(SceneNode currentScene) {
        StringBuilder map = new StringBuilder();
        
        String curr = currentScene != null ? currentScene.scene.getName() : "";
        
        map.append("\n");
        map.append("+------------------------------------------------------------+\n");
        map.append("|                     WU LIN DI TU                           |\n");
        map.append("|                    [ 武 林 地 图 ]                         |\n");
        map.append("+------------------------------------------------------------+\n");
        map.append("|                                                            |\n");
        
        // 新手村
        if (curr.equals("新手村")) {
            map.append("|                       [*新手村*]                           |\n");
        } else {
            map.append("|                        [新手村]                            |\n");
        }
        
        map.append("|                            |                               |\n");
        map.append("|                            v                               |\n");
        
        // 中间一行：藏经阁 - 武林广场 - 练功房
        map.append("|     ");
        if (curr.equals("藏经阁")) {
            map.append("[*藏经阁*]");
        } else {
            map.append(" [藏经阁] ");
        }
        map.append(" <---> ");
        if (curr.equals("武林广场")) {
            map.append("[*武林广场*]");
        } else {
            map.append(" [武林广场] ");
        }
        map.append(" <---> ");
        if (curr.equals("练功房")) {
            map.append("[*练功房*]");
        } else {
            map.append(" [练功房] ");
        }
        map.append("     |\n");
        
        map.append("|                            |                               |\n");
        map.append("|                            v                               |\n");
        
        // 江湖客栈
        if (curr.equals("江湖客栈")) {
            map.append("|                      [*江湖客栈*]                          |\n");
        } else {
            map.append("|                       [江湖客栈]                           |\n");
        }
        
        map.append("|                                                            |\n");
        map.append("+------------------------------------------------------------+\n");
        
        // 当前位置和可移动方向
        if (currentScene != null) {
            String posInfo = "| Location: " + currentScene.scene.getName();
            map.append(padTo60(posInfo)).append("|\n");
            
            StringBuilder dirs = new StringBuilder("| Move: ");
            if (currentScene.north != null) dirs.append("[N]").append(currentScene.north.scene.getName()).append(" ");
            if (currentScene.south != null) dirs.append("[S]").append(currentScene.south.scene.getName()).append(" ");
            if (currentScene.east != null) dirs.append("[E]").append(currentScene.east.scene.getName()).append(" ");
            if (currentScene.west != null) dirs.append("[W]").append(currentScene.west.scene.getName()).append(" ");
            map.append(padTo60(dirs.toString())).append("|\n");
        }
        
        map.append("+------------------------------------------------------------+\n");
        
        return map.toString();
    }
    
    /**
     * 填充字符串到60个字符宽度
     */
    private String padTo60(String str) {
        StringBuilder sb = new StringBuilder(str);
        // 计算需要填充的空格数（考虑中文字符）
        int width = 0;
        for (char c : str.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                width += 2;
            } else {
                width += 1;
            }
        }
        int need = 60 - width;
        for (int i = 0; i < need; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
    
    /**
     * 根据方向移动
     */
    public SceneNode move(SceneNode current, String direction) {
        if (current == null) return null;
        
        direction = direction.toLowerCase().trim();
        switch (direction) {
            case "n": case "north": case "上": case "北":
                return current.north;
            case "s": case "south": case "下": case "南":
                return current.south;
            case "e": case "east": case "右": case "东":
                return current.east;
            case "w": case "west": case "左": case "西":
                return current.west;
            default:
                return null;
        }
    }
    
    /**
     * 生成详细的地图说明
     */
    public String generateMapLegend() {
        StringBuilder legend = new StringBuilder();
        
        legend.append("\n");
        legend.append("+------------------------------------------------------------+\n");
        legend.append("|                     SCENE INFO                             |\n");
        legend.append("|                    [ 场景说明 ]                            |\n");
        legend.append("+------------------------------------------------------------+\n");
        
        String[][] info = {
            {"新手村", "宁静的村庄", "打怪,NPC"},
            {"藏经阁", "武学秘籍之地", "NPC对话"},
            {"武林广场", "江湖聚集地", "打怪,NPC,PK"},
            {"练功房", "修炼场所", "打怪"},
            {"江湖客栈", "休息交流", "NPC,聊天"}
        };
        
        for (String[] s : info) {
            String line = "| " + s[0] + " - " + s[1] + " [" + s[2] + "]";
            legend.append(padTo60(line)).append("|\n");
        }
        
        legend.append("+------------------------------------------------------------+\n");
        return legend.toString();
    }
}

