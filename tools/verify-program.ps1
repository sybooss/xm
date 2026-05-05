param(
    [switch]$WithSmoke,
    [switch]$WithBrowser,
    [switch]$WithRoleBrowser,
    [string]$FrontendUrl = "http://localhost:5173",
    [string]$BackendUrl = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$serverDir = Join-Path $root "server"
$webDir = Join-Path $root "web"
$summary = New-Object System.Collections.Generic.List[object]

function Add-Step($name, $ok, $detail = "") {
    $summary.Add([pscustomobject]@{
        step = $name
        ok = [bool]$ok
        detail = $detail
    }) | Out-Null
}

function Invoke-Step($name, [scriptblock]$block) {
    Write-Host ""
    Write-Host "==> $name"
    try {
        & $block
        Add-Step $name $true "passed"
    } catch {
        Add-Step $name $false $_.Exception.Message
        throw
    }
}

function Test-HttpReady($url) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 10
        return $response.StatusCode -ge 200 -and $response.StatusCode -lt 500
    } catch {
        return $false
    }
}

try {
    Invoke-Step "backend package" {
        Push-Location $serverDir
        try {
            mvn -q -DskipTests package
        } finally {
            Pop-Location
        }
    }

    Invoke-Step "frontend build" {
        Push-Location $webDir
        try {
            npm run build
        } finally {
            Pop-Location
        }
    }

    if ($WithSmoke) {
        if (-not (Test-HttpReady "$BackendUrl/system/status")) {
            throw "Backend is not reachable at $BackendUrl. Start the Spring Boot server before running -WithSmoke."
        }
        if (-not (Test-HttpReady $FrontendUrl)) {
            throw "Frontend is not reachable at $FrontendUrl. Start Vite before running browser or full smoke checks."
        }
        Invoke-Step "full API smoke" {
            & (Join-Path $root "tools\full-smoke-test.ps1")
        }
    }

    if ($WithBrowser) {
        if (-not (Test-HttpReady $FrontendUrl)) {
            throw "Frontend is not reachable at $FrontendUrl. Start Vite before running -WithBrowser."
        }
        Invoke-Step "browser smoke" {
            Push-Location $webDir
            try {
                npm run test:browser
            } finally {
                Pop-Location
            }
        }
    }

    if ($WithRoleBrowser) {
        if (-not (Test-HttpReady $FrontendUrl)) {
            throw "Frontend is not reachable at $FrontendUrl. Start Vite before running -WithRoleBrowser."
        }
        Invoke-Step "role browser smoke" {
            Push-Location $webDir
            try {
                npm run test:browser:roles
            } finally {
                Pop-Location
            }
        }
    }
} finally {
    Write-Host ""
    $summary | Format-Table -AutoSize
}

$failed = @($summary | Where-Object { $_.ok -ne $true })
if ($failed.Count -gt 0) {
    Write-Host "FAILED_COUNT=$($failed.Count)"
    exit 1
}

Write-Host "FAILED_COUNT=0"
