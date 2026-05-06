param(
    [switch]$RequireRunningServices,
    [switch]$RequireSub2api,
    [switch]$RequireMysql,
    [switch]$RequireCleanGit,
    [string]$BackendUrl = "http://localhost:8081",
    [string]$FrontendUrl = "http://localhost:5173",
    [string]$Sub2apiUrl = "http://127.0.0.1:8080",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "1234",
    [string]$Database = "test3"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$results = New-Object System.Collections.Generic.List[object]

function Add-Check($name, $ok, $detail = "", $required = $true) {
    $results.Add([pscustomobject]@{
        check = $name
        ok = [bool]$ok
        required = [bool]$required
        detail = $detail
    }) | Out-Null
}

function Test-Command($name) {
    $command = Get-Command $name -ErrorAction SilentlyContinue
    if ($command) {
        Add-Check "tool $name" $true $command.Source
        return $true
    }
    Add-Check "tool $name" $false "$name not found in PATH"
    return $false
}

function Test-Http($name, $url, $required = $true) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        Add-Check $name ($response.StatusCode -ge 200 -and $response.StatusCode -lt 500) "HTTP $($response.StatusCode)" $required
    } catch {
        Add-Check $name $false $_.Exception.Message $required
    }
}

function Invoke-Json($method, $url, $body = $null, $headers = @{}) {
    $params = @{
        Uri = $url
        Method = $method
        Headers = $headers
        TimeoutSec = 20
    }
    if ($null -ne $body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($body | ConvertTo-Json -Depth 8)
    }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 1) {
        throw "$url failed: $($response.msg)"
    }
    return $response.data
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

function Test-GitIgnored($path) {
    Push-Location $root
    try {
        & git check-ignore -q -- $path
        Add-Check "git ignores $path" ($LASTEXITCODE -eq 0) "check-ignore exit=$LASTEXITCODE"
    } finally {
        Pop-Location
    }
}

Push-Location $root
try {
    foreach ($path in @(
        "server\pom.xml",
        "web\package.json",
        "web\browser-smoke.mjs",
        "web\browser-role-smoke.mjs",
        "sql\schema.sql",
        "sql\seed.sql",
        "tools\full-smoke-test.ps1"
    )) {
        Add-Check "required file $path" (Test-Path (Join-Path $root $path)) $path
    }

    Test-Command "java" | Out-Null
    Test-Command "mvn" | Out-Null
    Test-Command "node" | Out-Null
    Test-Command "npm" | Out-Null
    Test-Command "git" | Out-Null

    foreach ($ignored in @(".env", "output", "tmp", "web/dist", "server/target", "web/node_modules")) {
        Test-GitIgnored $ignored
    }

    $trackedForbidden = @(& git ls-files -- ".env" "output" "tmp" "web/dist" "server/target" "node_modules" "web/node_modules")
    Add-Check "forbidden artifacts untracked" ($trackedForbidden.Count -eq 0) (($trackedForbidden -join ", "))

    if ($RequireCleanGit) {
        $dirty = @(& git status --short)
        Add-Check "git working tree clean" ($dirty.Count -eq 0) (($dirty -join " | "))
    }

    if ($RequireMysql) {
        $mysqlExe = Find-MysqlExe
        Add-Check "mysql client available" ($null -ne $mysqlExe) $mysqlExe
        if ($mysqlExe) {
            $query = "select 1; show tables like 'after_sale_application'; show tables like 'service_review';"
            $output = Get-Content -LiteralPath (Join-Path $root "sql\schema.sql") -Encoding UTF8 -TotalCount 1
            $previousPreference = $ErrorActionPreference
            $ErrorActionPreference = "Continue"
            try {
                $mysqlResult = $query | & $mysqlExe "-u$MysqlUser" "-p$MysqlPassword" "--default-character-set=utf8mb4" $Database 2>&1
                $mysqlExitCode = $LASTEXITCODE
            } finally {
                $ErrorActionPreference = $previousPreference
            }
            $mysqlText = $mysqlResult -join "`n"
            Add-Check "mysql schema reachable" ($mysqlExitCode -eq 0 -and $mysqlText -like "*after_sale_application*" -and $mysqlText -like "*service_review*") (($mysqlResult -join " ") + " " + $output)
        }
    }

    if ($RequireRunningServices) {
        Test-Http "backend health" "$BackendUrl/system/health"
        try {
            $login = Invoke-Json "Post" "$BackendUrl/auth/login" @{ username = "admin"; password = "123456" }
            $headers = @{ Authorization = "Bearer $($login.token)" }
            $status = Invoke-Json "Get" "$BackendUrl/system/status" $null $headers
            Add-Check "backend admin status" ($status.database.status -eq "UP") "db=$($status.database.status), ai=$($status.ai.status)"
        } catch {
            Add-Check "backend admin status" $false $_.Exception.Message
        }
        Test-Http "frontend index" "$FrontendUrl/"
    }

    if ($RequireSub2api) {
        try {
            $health = Invoke-RestMethod -Uri "$Sub2apiUrl/health" -Method Get -TimeoutSec 10
            Add-Check "sub2api health" ($health.status -eq "ok") "status=$($health.status)"
        } catch {
            Add-Check "sub2api health" $false $_.Exception.Message
        }
    } else {
        Test-Http "sub2api health optional" "$Sub2apiUrl/health" $false
    }
} finally {
    Pop-Location
}

$results | Format-Table -AutoSize
$failed = @($results | Where-Object { $_.required -and $_.ok -ne $true })
if ($failed.Count -gt 0) {
    Write-Host "FAILED_COUNT=$($failed.Count)"
    exit 1
}

Write-Host "FAILED_COUNT=0"
