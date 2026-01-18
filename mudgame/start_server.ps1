# MUD游戏服务器启动脚本 (PowerShell)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8"

Write-Host "========================================" -ForegroundColor Green
Write-Host "MUD游戏服务器启动脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameServer

Read-Host "按Enter键退出"

