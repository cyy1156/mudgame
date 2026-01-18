<<<<<<< HEAD
# 中文乱码问题解决方案

## 问题说明

在Windows系统上运行Java程序时，如果控制台编码不是UTF-8，可能会出现中文乱码问题。

## 解决方案

### 方法一：使用启动脚本（推荐）

项目根目录提供了以下启动脚本，已自动配置UTF-8编码：

- `start_server.bat` - Windows CMD启动服务器
- `start_server.ps1` - PowerShell启动服务器
- `start_client.bat` - Windows CMD启动客户端
- `start_client.ps1` - PowerShell启动客户端

直接双击运行或命令行执行即可，无需额外配置。

### 方法二：手动设置编码

#### Windows CMD

1. 设置代码页为UTF-8：
```cmd
chcp 65001
```

2. 启动程序时添加编码参数：
```cmd
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

#### Windows PowerShell

1. 设置控制台编码：
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

2. 启动程序时添加编码参数：
```powershell
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

#### Linux/Mac

通常不需要特殊设置，如果遇到乱码，可以设置环境变量：
```bash
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
java -Dfile.encoding=UTF-8 -cp "out/production/USST:lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

## 代码层面的改进

代码已经进行了以下改进以支持UTF-8：

1. **网络通信使用UTF-8编码**
   - `InputStreamReader` 和 `OutputStreamWriter` 都指定了 `"UTF-8"` 编码
   - 确保服务器和客户端之间的数据传输使用UTF-8

2. **控制台输出使用UTF-8编码**
   - 在 `main` 方法中设置了 `System.setOut` 和 `System.setErr` 使用UTF-8编码
   - 设置了系统属性 `file.encoding=UTF-8`

## 验证编码设置

运行程序后，如果看到正确的中文显示（如"欢迎来到MUD武侠世界！"），说明编码设置成功。

如果仍然出现乱码，请检查：
1. 是否使用了启动脚本
2. 命令行窗口的字体是否支持中文（建议使用"新宋体"或"Consolas"）
3. 是否添加了 `-Dfile.encoding=UTF-8` 参数

## 常见问题

**Q: 为什么使用启动脚本后还是乱码？**
A: 可能是命令行窗口的字体不支持中文，请尝试更换字体。

**Q: 可以永久设置CMD编码为UTF-8吗？**
A: 可以，但建议使用启动脚本，因为它会在每次启动时自动设置。

**Q: IDE中运行正常，但命令行乱码？**
A: IDE通常自动处理编码，命令行需要手动设置。使用启动脚本可以解决这个问题。

=======
# 中文乱码问题解决方案

## 问题说明

在Windows系统上运行Java程序时，如果控制台编码不是UTF-8，可能会出现中文乱码问题。

## 解决方案

### 方法一：使用启动脚本（推荐）

项目根目录提供了以下启动脚本，已自动配置UTF-8编码：

- `start_server.bat` - Windows CMD启动服务器
- `start_server.ps1` - PowerShell启动服务器
- `start_client.bat` - Windows CMD启动客户端
- `start_client.ps1` - PowerShell启动客户端

直接双击运行或命令行执行即可，无需额外配置。

### 方法二：手动设置编码

#### Windows CMD

1. 设置代码页为UTF-8：
```cmd
chcp 65001
```

2. 启动程序时添加编码参数：
```cmd
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

#### Windows PowerShell

1. 设置控制台编码：
```powershell
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

2. 启动程序时添加编码参数：
```powershell
java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

#### Linux/Mac

通常不需要特殊设置，如果遇到乱码，可以设置环境变量：
```bash
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8
java -Dfile.encoding=UTF-8 -cp "out/production/USST:lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer
```

## 代码层面的改进

代码已经进行了以下改进以支持UTF-8：

1. **网络通信使用UTF-8编码**
   - `InputStreamReader` 和 `OutputStreamWriter` 都指定了 `"UTF-8"` 编码
   - 确保服务器和客户端之间的数据传输使用UTF-8

2. **控制台输出使用UTF-8编码**
   - 在 `main` 方法中设置了 `System.setOut` 和 `System.setErr` 使用UTF-8编码
   - 设置了系统属性 `file.encoding=UTF-8`

## 验证编码设置

运行程序后，如果看到正确的中文显示（如"欢迎来到MUD武侠世界！"），说明编码设置成功。

如果仍然出现乱码，请检查：
1. 是否使用了启动脚本
2. 命令行窗口的字体是否支持中文（建议使用"新宋体"或"Consolas"）
3. 是否添加了 `-Dfile.encoding=UTF-8` 参数

## 常见问题

**Q: 为什么使用启动脚本后还是乱码？**
A: 可能是命令行窗口的字体不支持中文，请尝试更换字体。

**Q: 可以永久设置CMD编码为UTF-8吗？**
A: 可以，但建议使用启动脚本，因为它会在每次启动时自动设置。

**Q: IDE中运行正常，但命令行乱码？**
A: IDE通常自动处理编码，命令行需要手动设置。使用启动脚本可以解决这个问题。

>>>>>>> e1501ce6d55714bf6aecc1e18dd84acda821f7d9
