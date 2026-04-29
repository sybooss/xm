$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$logs = Join-Path $root "output\logs"
$jar = Join-Path $root "server\target\returns-assistant-0.0.1-SNAPSHOT.jar"
$pidFile = Join-Path $logs "server.pid"
$outLog = Join-Path $logs "server.out.log"
$errLog = Join-Path $logs "server.err.log"
$envFile = Join-Path $root ".env"

New-Item -ItemType Directory -Force -Path $logs | Out-Null

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith("#")) {
            return
        }
        $index = $line.IndexOf("=")
        if ($index -le 0) {
            return
        }
        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim().Trim('"').Trim("'")
        if ($name -and -not [Environment]::GetEnvironmentVariable($name, "Process")) {
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }
}

if (Test-Path $pidFile) {
    $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($oldPid) {
        Stop-Process -Id ([int]$oldPid) -Force -ErrorAction SilentlyContinue
    }
}

if (-not $env:OPENAI_API_KEY) {
    throw "OPENAI_API_KEY is not set. Copy .env.example to .env and fill your OpenAI-compatible API key first."
}

if (-not $env:AI_ENABLED) { $env:AI_ENABLED = "true" }
if (-not $env:OPENAI_BASE_URL) { $env:OPENAI_BASE_URL = "http://127.0.0.1:8080/v1" }
if (-not $env:OPENAI_MODEL) { $env:OPENAI_MODEL = "gpt-4o-mini" }
if (-not $env:OPENAI_TIMEOUT_SECONDS) { $env:OPENAI_TIMEOUT_SECONDS = "30" }
if (-not $env:OPENAI_MAX_RETRIES) { $env:OPENAI_MAX_RETRIES = "1" }

$process = Start-Process `
    -FilePath "java" `
    -ArgumentList "-jar", $jar `
    -WorkingDirectory $root `
    -RedirectStandardOutput $outLog `
    -RedirectStandardError $errLog `
    -WindowStyle Hidden `
    -PassThru

Set-Content -Path $pidFile -Value $process.Id
Write-Host "Spring Boot started with sub2api. PID=$($process.Id), URL=http://localhost:8081"
