# MUD游戏客户端启动脚本 (PowerShell)
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "MUD游戏客户端启动脚本" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if ($args.Count -eq 0) {
    java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient
} elseif ($args.Count -eq 1) {
    java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient $args[0]
} else {
    java -Dfile.encoding=UTF-8 -Dconsole.encoding=UTF-8 -cp "out/production/USST;lib/gson-2.10.1.jar" com.mudgame.NetworkGameClient $args[0] $args[1]
}

Read-Host "按Enter键退出"

