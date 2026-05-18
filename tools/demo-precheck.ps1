param(
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "1234",
    [string]$Database = "test3",
    [string]$MysqlHost = "127.0.0.1",
    [int]$Port = 3306,
    [string]$BackendUrl = "http://localhost:8081",
    [string]$FrontendUrl = "http://localhost:5173",
    [switch]$RequireRunningServices
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$results = New-Object System.Collections.Generic.List[object]
$token = $null

function Add-Check($name, $ok, $detail = "", $required = $true) {
    $passed = $ok -eq $true
    $script:results.Add([pscustomobject]@{
        name = $name
        ok = $passed
        required = $required
        detail = [string]$detail
    }) | Out-Null
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
    return $null
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
        throw ($output | Out-String)
    }
    $line = $output | Where-Object { [string]$_ -match "\S" } | Select-Object -Last 1
    if ($null -eq $line) {
        return 0
    }
    return [int]([string]$line).Trim()
}

function Auth-Headers() {
    if ($script:token) {
        return @{ Authorization = "Bearer $script:token" }
    }
    return @{}
}

function Invoke-Api($method, $path, $body = $null) {
    $params = @{
        Uri = "$BackendUrl$path"
        Method = $method
        Headers = Auth-Headers
        TimeoutSec = 20
    }
    if ($null -ne $body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = $body | ConvertTo-Json -Depth 10
    }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

Write-Host "Demo precheck"
Write-Host "Root: $root"

$mysqlExe = Find-MysqlExe
Add-Check "mysql client available" ($null -ne $mysqlExe) $mysqlExe
if ($mysqlExe) {
    $checks = @(
        @{ Name = "OPS orders"; Sql = "select count(*) from demo_order where order_no like 'OPS2026%';"; Min = 24 },
        @{ Name = "OPS after-sales"; Sql = "select count(*) from after_sale_application where application_no like 'OPSASA%';"; Min = 12 },
        @{ Name = "Clean demo chat sessions"; Sql = "select count(*) from chat_session where session_no like 'OPSCS20260512000_';"; Min = 6 },
        @{ Name = "No extra demo chat sessions"; Sql = "select count(*) from chat_session where session_no not like 'OPSCS20260512000_';"; Max = 0 },
        @{ Name = "No dirty chat sessions"; Sql = "select count(*) from chat_session where lower(coalesce(title,'')) regexp 'browser|test|upload debug|large upload' or title like '%测试%' or title like '%网页咨询会话%' or title like '%????%';"; Max = 0 },
        @{ Name = "Each demo session has messages"; Sql = "select count(*) from (select s.id from chat_session s left join chat_message m on m.session_id=s.id where s.session_no like 'OPSCS20260512000_' group by s.id having count(m.id) < 2) t;"; Max = 0 },
        @{ Name = "OPS tickets"; Sql = "select count(*) from service_ticket where ticket_no like 'OPSTK%';"; Min = 6 },
        @{ Name = "Image risk sample"; Sql = "select count(*) from chat_message where file_url='/uploads/demo/ai-risk-evidence.png';"; Min = 1 },
        @{ Name = "No dirty chat messages"; Sql = "select count(*) from chat_message where lower(coalesce(content,'')) regexp 'browser|upload debug|large upload' or content like '%网页咨询会话%' or content like '%????%';"; Max = 0 },
        @{ Name = "No visible test knowledge docs"; Sql = "select count(*) from knowledge_doc where deleted=0 and (title='测试' or title like '%测试%' or lower(title) like '%browser%' or lower(title) like '%auto test%' or lower(title) like '%upload debug%');"; Max = 0 },
        @{ Name = "Image risk trace"; Sql = "select count(*) from process_trace where step_name='CHAT_IMAGE_RISK_SCAN' and step_status='SUCCESS';"; Min = 1 },
        @{ Name = "Evidence audit sample"; Sql = "select count(*) from evidence_audit where audit_no like 'OPSEAUD%';"; Min = 3 },
        @{ Name = "Risk assessment sample"; Sql = "select count(*) from after_sale_risk_assessment where assessment_no like 'OPSRISK%';"; Min = 3 },
        @{ Name = "Reply draft sample"; Sql = "select count(*) from reply_draft d join after_sale_application a on d.application_id=a.id where a.application_no like 'OPSASA%';"; Min = 5 },
        @{ Name = "Discarded draft sample"; Sql = "select count(*) from reply_draft d join after_sale_application a on d.application_id=a.id where a.application_no like 'OPSASA%' and d.status='DISCARDED';"; Min = 1 },
        @{ Name = "Product issue alert sample"; Sql = "select count(*) from product_issue_alert where alert_no like 'OPSPIA%';"; Min = 3 },
        @{ Name = "Demo customer reviews"; Sql = "select count(*) from service_review r join user_account u on r.user_id=u.id where u.username='demo_customer';"; Min = 3 },
        @{ Name = "Demo customer after-sales"; Sql = "select count(*) from after_sale_application a join user_account u on a.user_id=u.id where u.username='demo_customer';"; Min = 6 },
        @{ Name = "Demo customer tickets"; Sql = "select count(*) from service_ticket t join user_account u on t.user_id=u.id where u.username='demo_customer';"; Min = 6 },
        @{ Name = "Fallback or failed AI sample"; Sql = "select count(*) from ai_call_log where status in ('SKIPPED','FAILED');"; Min = 2 },
        @{ Name = "Evidence chain logs"; Sql = "select count(*) from after_sale_process_log where action in ('EVIDENCE_AUDIT','RISK_ASSESSMENT','PRODUCT_ISSUE_ALERT','GENERATE_REPLY_DRAFT','USE_REPLY_DRAFT','DISCARD_REPLY_DRAFT');"; Min = 10 }
    )
    foreach ($check in $checks) {
        try {
            $count = Invoke-MysqlScalar $mysqlExe $check.Sql
            if ($check.ContainsKey("Max")) {
                Add-Check $check.Name ($count -le $check.Max) "$count (max $($check.Max))"
            } else {
                Add-Check $check.Name ($count -ge $check.Min) "$count (min $($check.Min))"
            }
        } catch {
            Add-Check $check.Name $false $_.Exception.Message
        }
    }
}

foreach ($imageName in @("ai-risk-evidence.png", "air-fryer-scratch.png", "missing-filter.png", "watch-strap-wear.png")) {
    $imagePath = Join-Path $root "tmp\uploads\demo\$imageName"
    Add-Check "demo image exists: $imageName" (Test-Path $imagePath) $imagePath
}

try {
    $frontStatus = (Invoke-WebRequest -Uri "$FrontendUrl/" -UseBasicParsing -TimeoutSec 8).StatusCode
    Add-Check "frontend index" ($frontStatus -eq 200) "HTTP $frontStatus" $RequireRunningServices.IsPresent
} catch {
    Add-Check "frontend index" $false $_.Exception.Message $RequireRunningServices.IsPresent
}

try {
    $auth = Invoke-Api "Post" "/auth/login" @{ username = "admin"; password = "123456" }
    $script:token = $auth.token
    Add-Check "admin login" ($auth.role -eq "ADMIN" -and $null -ne $auth.token) "user=$($auth.username), role=$($auth.role)" $RequireRunningServices.IsPresent

    $status = Invoke-Api "Get" "/system/status"
    Add-Check "system status" ($status.database.status -eq "UP") "db=$($status.database.status), ai=$($status.ai.status)" $RequireRunningServices.IsPresent

    $afterSales = Invoke-Api "Get" "/admin/after-sales?page=1&pageSize=5&keyword=OPSASA202605120001"
    Add-Check "admin after-sales API" (($afterSales.total -ge 1) -or (($afterSales.rows | Measure-Object).Count -ge 1)) "total=$($afterSales.total)" $RequireRunningServices.IsPresent

    $tickets = Invoke-Api "Get" "/service-tickets?page=1&pageSize=5"
    Add-Check "service tickets API" (($tickets.total -ge 1) -or (($tickets.rows | Measure-Object).Count -ge 1)) "total=$($tickets.total)" $RequireRunningServices.IsPresent

    $sla = Invoke-Api "Get" "/admin/sla/tasks"
    Add-Check "SLA tasks API" (($sla | ConvertTo-Json -Depth 6).Length -gt 10) "loaded" $RequireRunningServices.IsPresent

    $productSummary = Invoke-Api "Get" "/admin/product-issue-insights/summary?days=7"
    Add-Check "product issue summary API" (($productSummary.openCount -ge 2) -and ($null -ne $productSummary.topAlert)) "open=$($productSummary.openCount)" $RequireRunningServices.IsPresent

    $profile = Invoke-Api "Get" "/admin/customers/1/profile"
    Add-Check "demo customer profile API" (($profile.reviewCount -ge 3) -and ($profile.averageRating -gt 0) -and (($profile.recentAfterSales | Measure-Object).Count -ge 1) -and (($profile.recentTickets | Measure-Object).Count -ge 1)) "reviews=$($profile.reviewCount), avg=$($profile.averageRating), afterSales=$(@($profile.recentAfterSales).Count), tickets=$(@($profile.recentTickets).Count)" $RequireRunningServices.IsPresent

    $logs = Invoke-Api "Get" "/log-diagnostics"
    Add-Check "log diagnostics API" ($null -ne $logs.ai -and $null -ne $logs.trace) "aiSamples=$($logs.ai.sampleSize)" $RequireRunningServices.IsPresent
} catch {
    Add-Check "backend API group" $false $_.Exception.Message $RequireRunningServices.IsPresent
}

$results | Format-Table -AutoSize
$failedRequired = @($results | Where-Object { $_.required -and -not $_.ok })
$failedOptional = @($results | Where-Object { -not $_.required -and -not $_.ok })

Write-Host "FAILED_REQUIRED=$($failedRequired.Count)"
Write-Host "FAILED_OPTIONAL=$($failedOptional.Count)"

if ($failedRequired.Count -gt 0) {
    exit 1
}
exit 0
