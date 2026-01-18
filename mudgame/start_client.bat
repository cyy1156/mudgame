@echo off
chcp 65001 >nul
echo ========================================
echo MUD游戏客户端启动脚本
echo ========================================
echo.

if "%1"=="" (
    java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient
) else (
    if "%2"=="" (
        java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient %1
    ) else (
        java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient %1 %2
    )
)

pause

