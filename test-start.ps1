Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Story Weaver 启动脚本测试" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "1. 检查目录结构..." -ForegroundColor Yellow
if (Test-Path "front\vuetify-admin") {
    Write-Host "   ✅ 前端目录存在" -ForegroundColor Green
} else {
    Write-Host "   ❌ 前端目录不存在" -ForegroundColor Red
    exit 1
}

if (Test-Path "backend") {
    Write-Host "   ✅ 后端目录存在" -ForegroundColor Green
} else {
    Write-Host "   ❌ 后端目录不存在" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "2. 检查关键文件..." -ForegroundColor Yellow
if (Test-Path "backend\pom.xml") {
    Write-Host "   ✅ 后端 Maven 配置文件存在" -ForegroundColor Green
} else {
    Write-Host "   ❌ 后端 Maven 配置文件不存在" -ForegroundColor Red
}

if (Test-Path "front\vuetify-admin\package.json") {
    Write-Host "   ✅ 前端 package.json 存在" -ForegroundColor Green
} else {
    Write-Host "   ❌ 前端 package.json 不存在" -ForegroundColor Red
}

if (Test-Path "backend\src\main\resources\schema.sql") {
    Write-Host "   ✅ 数据库脚本存在" -ForegroundColor Green
} else {
    Write-Host "   ❌ 数据库脚本不存在" -ForegroundColor Red
}

Write-Host ""
Write-Host "3. 检查环境变量..." -ForegroundColor Yellow
$javaHome = $env:JAVA_HOME
if ($javaHome) {
    Write-Host "   JAVA_HOME: $javaHome" -ForegroundColor Gray
} else {
    Write-Host "   JAVA_HOME: 未设置" -ForegroundColor Gray
}

try {
    Get-Command java -ErrorAction Stop | Out-Null
    Write-Host "   ✅ Java 已安装" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Java 未安装" -ForegroundColor Red
}

try {
    Get-Command mvn -ErrorAction Stop | Out-Null
    Write-Host "   ✅ Maven 已安装" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Maven 未安装" -ForegroundColor Red
}

try {
    Get-Command node -ErrorAction Stop | Out-Null
    Write-Host "   ✅ Node.js 已安装" -ForegroundColor Green
} catch {
    Write-Host "   ❌ Node.js 未安装" -ForegroundColor Red
}

Write-Host ""
Write-Host "4. 检查前端依赖..." -ForegroundColor Yellow
if (Test-Path "front\vuetify-admin\node_modules") {
    Write-Host "   ✅ 前端依赖已安装" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ 前端依赖未安装，首次启动需要安装" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "5. 模拟启动流程..." -ForegroundColor Yellow
Write-Host "   a) 后端启动命令: mvn spring-boot:run" -ForegroundColor Gray
Write-Host "   b) 前端启动命令: npm run dev" -ForegroundColor Gray
Write-Host "   c) 数据库初始化: mysql -u root -p < backend\src\main\resources\schema.sql" -ForegroundColor Gray

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试完成！" -ForegroundColor Green
Write-Host ""
Write-Host "启动步骤:" -ForegroundColor Yellow
Write-Host "1. 确保 MySQL 服务已启动并执行数据库初始化" -ForegroundColor Gray
Write-Host "2. 运行 start-dev.bat 启动前后端服务" -ForegroundColor Gray
Write-Host "3. 访问 http://localhost:5173 使用 admin/admin123 登录" -ForegroundColor Gray
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Read-Host "按 Enter 键退出"