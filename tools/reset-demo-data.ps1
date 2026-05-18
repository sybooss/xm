param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "1234",
    [string]$Database = "test3",
    [string]$MysqlHost = "127.0.0.1",
    [int]$Port = 3306
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")

function U($codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

function Find-MysqlExe() {
    $bundled = "D:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
    if (Test-Path $bundled) {
        return $bundled
    }
    $command = Get-Command "mysql" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    throw "mysql.exe not found. Add MySQL bin to PATH or install MySQL client."
}

function Mysql-BaseArgs() {
    return @(
        "--default-character-set=utf8mb4",
        "--protocol=tcp",
        "-h$MysqlHost",
        "-P$Port",
        "-u$MysqlUser",
        "--password=$MysqlPassword"
    )
}

function Invoke-MysqlFile($mysqlExe, $filePath, $tempDir) {
    $fullPath = (Resolve-Path $filePath).Path
    $tempFile = Join-Path $tempDir ([System.IO.Path]::GetFileName($fullPath))
    Copy-Item -LiteralPath $fullPath -Destination $tempFile -Force
    $sourcePath = $tempFile.Replace("\", "/")
    Write-Host "[SQL] SOURCE $([System.IO.Path]::GetFileName($fullPath))"
    $args = Mysql-BaseArgs
    $args += "--execute=SOURCE $sourcePath"
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $mysqlExe @args 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        $text = ($output | Out-String).Trim()
        throw "MySQL failed while executing $fullPath`n$text"
    }
}

function Invoke-MysqlScalar($mysqlExe, $sql) {
    $args = Mysql-BaseArgs
    $args += @("--batch", "--skip-column-names", "--execute=$sql", $Database)
    $previousPreference = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & $mysqlExe @args 2>&1
        $exitCode = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
    if ($exitCode -ne 0) {
        $text = ($output | Out-String).Trim()
        throw "MySQL query failed: $sql`n$text"
    }
    $line = $output | Where-Object { [string]$_ -match "\S" } | Select-Object -Last 1
    if ($null -eq $line) {
        return 0
    }
    return [int]([string]$line).Trim()
}

function New-DemoRiskImage($filePath) {
    Add-Type -AssemblyName System.Drawing
    $dir = Split-Path -Parent $filePath
    New-Item -ItemType Directory -Force -Path $dir | Out-Null

    $bitmap = New-Object System.Drawing.Bitmap 900, 600
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $graphics.Clear([System.Drawing.Color]::FromArgb(246, 248, 250))

        $tableBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(222, 226, 230))
        $graphics.FillRectangle($tableBrush, 0, 420, 900, 180)

        $cardBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::White)
        $graphics.FillRectangle($cardBrush, 70, 55, 760, 430)

        $borderPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(180, 184, 190)), 3
        $graphics.DrawRectangle($borderPen, 70, 55, 760, 430)

        $earbudBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(32, 36, 42))
        $graphics.FillEllipse($earbudBrush, 210, 190, 120, 120)
        $graphics.FillEllipse($earbudBrush, 520, 190, 120, 120)
        $stemPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(32, 36, 42)), 26
        $graphics.DrawLine($stemPen, 285, 290, 330, 385)
        $graphics.DrawLine($stemPen, 575, 290, 540, 385)

        $signalPen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(220, 38, 38)), 5
        $graphics.DrawArc($signalPen, 360, 175, 160, 160, 205, 130)
        $graphics.DrawArc($signalPen, 335, 150, 210, 210, 205, 130)
        $graphics.DrawLine($signalPen, 425, 250, 475, 300)
        $graphics.DrawLine($signalPen, 475, 250, 425, 300)

        $fontTitle = New-Object System.Drawing.Font "Microsoft YaHei", 28, ([System.Drawing.FontStyle]::Bold)
        $fontMeta = New-Object System.Drawing.Font "Arial", 18, ([System.Drawing.FontStyle]::Regular)
        $fontWatermark = New-Object System.Drawing.Font "Microsoft YaHei", 30, ([System.Drawing.FontStyle]::Bold)
        $textBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(26, 32, 44))
        $mutedBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(90, 98, 110))
        $watermarkBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(225, 220, 38, 38))
        $graphics.DrawString((U @(0x6f14,0x793a,0x552e,0x540e,0x51ed,0x8bc1)), $fontTitle, $textBrush, 110, 85)
        $graphics.DrawString("Order OPS202605120002 / Earbud disconnect", $fontMeta, $mutedBrush, 110, 140)
        $graphics.DrawString("AI GENERATED DEMO", $fontMeta, $watermarkBrush, 600, 90)
        $graphics.DrawString((U @(0x8c46,0x5305,0x0041,0x0049,0x751f,0x6210)), $fontWatermark, $watermarkBrush, 610, 520)
    } finally {
        $graphics.Dispose()
    }
    try {
        $bitmap.Save($filePath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $bitmap.Dispose()
    }
}

$mysqlExe = Find-MysqlExe
$tempDir = Join-Path $env:TEMP ("returns-demo-reset-" + $PID + "-" + (Get-Date -Format "yyyyMMddHHmmss"))
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null

Write-Host "Demo database reset"
Write-Host "Root: $root"
Write-Host "MySQL: $mysqlExe"
Write-Host "Database: $Database on ${MysqlHost}:$Port"

$demoDir = Join-Path $root "tmp\uploads\demo"
$riskImage = Join-Path $demoDir "ai-risk-evidence.png"
New-DemoRiskImage $riskImage
$namedRiskImage = Join-Path $demoDir ((U @(0x8c46,0x5305,0x0041,0x0049,0x751f,0x6210,0x002d,0x552e,0x540e,0x51ed,0x8bc1,0x6f14,0x793a)) + ".png")
Copy-Item -LiteralPath $riskImage -Destination $namedRiskImage -Force
Write-Host "[IMAGE] $riskImage"
Write-Host "[IMAGE] $namedRiskImage"

$sqlFiles = @(
    "sql\schema.sql",
    "sql\seed.sql",
    "sql\demo-usage-data.sql"
)
foreach ($relativePath in $sqlFiles) {
    Invoke-MysqlFile $mysqlExe (Join-Path $root $relativePath) $tempDir
}

$checks = @(
    @{ Name = "OPS orders"; Sql = "select count(*) from demo_order where order_no like 'OPS2026%';"; Min = 24 },
    @{ Name = "OPS after-sales"; Sql = "select count(*) from after_sale_application where application_no like 'OPSASA%';"; Min = 12 },
    @{ Name = "OPS tickets"; Sql = "select count(*) from service_ticket where ticket_no like 'OPSTK%';"; Min = 8 },
    @{ Name = "Image risk message"; Sql = "select count(*) from chat_message where file_url='/uploads/demo/ai-risk-evidence.png';"; Min = 1 },
    @{ Name = "Image risk trace"; Sql = "select count(*) from process_trace where step_name='CHAT_IMAGE_RISK_SCAN';"; Min = 1 },
    @{ Name = "Evidence audits"; Sql = "select count(*) from evidence_audit where audit_no like 'OPSEAUD%';"; Min = 3 },
    @{ Name = "Risk assessments"; Sql = "select count(*) from after_sale_risk_assessment where assessment_no like 'OPSRISK%';"; Min = 3 },
    @{ Name = "Reply drafts"; Sql = "select count(*) from reply_draft d join after_sale_application a on d.application_id=a.id where a.application_no like 'OPSASA%';"; Min = 5 },
    @{ Name = "Product alerts"; Sql = "select count(*) from product_issue_alert where alert_no like 'OPSPIA%';"; Min = 3 },
    @{ Name = "Fallback or failed AI logs"; Sql = "select count(*) from ai_call_log where status in ('SKIPPED','FAILED');"; Min = 2 }
)

$failed = 0
foreach ($check in $checks) {
    $count = Invoke-MysqlScalar $mysqlExe $check.Sql
    $ok = $count -ge $check.Min
    if (-not $ok) { $failed++ }
    $status = if ($ok) { "PASS" } else { "FAIL" }
    Write-Host ("[{0}] {1}: {2} (min {3})" -f $status, $check.Name, $count, $check.Min)
}

if ($failed -gt 0) {
    throw "Demo reset finished with $failed failed data checks."
}

Write-Host "Demo database reset finished. You can now run tools\demo-precheck.ps1."
