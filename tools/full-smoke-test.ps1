$ErrorActionPreference = "Stop"

$base = "http://localhost:8081"
$front = "http://localhost:5173"
$sub2api = "http://127.0.0.1:8080"
$stamp = Get-Date -Format "yyyyMMddHHmmss"
$results = New-Object System.Collections.Generic.List[object]
$created = @{
    categoryId = $null
    docId = $null
    orderId = $null
    afterSaleId = $null
    sessionId = $null
}

function U($codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

$chatQuestion = U @(0x8fd9,0x4e2a,0x8ba2,0x5355,0x80fd,0x4e0d,0x80fd,0x9000,0x8d27,0xff1f)

function Add-Result($name, $ok, $detail = "") {
    $passed = $false
    if ($ok -eq $true) {
        $passed = $true
    }
    $script:results.Add([pscustomobject]@{
        name = $name
        ok = $passed
        detail = $detail
    }) | Out-Null
}

function Api-Get($path) {
    $response = Invoke-RestMethod -Uri "$base$path" -Method Get -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Post($path, $body) {
    $json = $body | ConvertTo-Json -Depth 12
    $response = Invoke-RestMethod -Uri "$base$path" -Method Post -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 120
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Put($path, $body) {
    $json = $body | ConvertTo-Json -Depth 12
    $response = Invoke-RestMethod -Uri "$base$path" -Method Put -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Delete($path) {
    $response = Invoke-RestMethod -Uri "$base$path" -Method Delete -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

try {
    $frontStatus = (Invoke-WebRequest -Uri "$front/" -UseBasicParsing -TimeoutSec 20).StatusCode
    Add-Result "frontend index" ($frontStatus -eq 200) "HTTP $frontStatus"

    $subHealth = Invoke-RestMethod -Uri "$sub2api/health" -Method Get -TimeoutSec 20
    Add-Result "sub2api health" ($subHealth.status -eq "ok") $subHealth.status

    $status = Api-Get "/system/status"
    Add-Result "system status" ($status.database.status -eq "UP" -and $status.ai.status -eq "UP") "db=$($status.database.status), ai=$($status.ai.status)"

    $enums = Api-Get "/system/enums"
    Add-Result "system enums" ($enums.intentCodes.Count -ge 7) "intentCodes=$($enums.intentCodes.Count)"

    $ai = Api-Post "/ai-tests" @{ prompt = "Reply exactly: AI smoke test success." }
    Add-Result "ai-tests real model" ($ai.status -eq "SUCCESS" -and $ai.used -eq $true) "status=$($ai.status), used=$($ai.used), latency=$($ai.latencyMs)"

    $categoryCode = "AUTO_TEST_$stamp"
    Api-Post "/knowledge-categories" @{
        parentId = 0
        categoryCode = $categoryCode
        categoryName = "Auto Test Category $stamp"
        sortOrder = 999
        enabled = 1
    } | Out-Null
    $categories = Api-Get "/knowledge-categories"
    $category = $categories | Where-Object { $_.categoryCode -eq $categoryCode } | Select-Object -First 1
    $created.categoryId = $category.id
    Add-Result "knowledge category create/list" ($null -ne $created.categoryId) "id=$($created.categoryId)"

    $category.categoryName = "Auto Test Category Updated $stamp"
    Api-Put "/knowledge-categories/$($created.categoryId)" $category | Out-Null
    $categoryUpdated = Api-Get "/knowledge-categories/$($created.categoryId)"
    Add-Result "knowledge category get/update" ($categoryUpdated.categoryName -like "*Updated*") $categoryUpdated.categoryName

    $docTitle = "Auto Test Return Rule $stamp"
    Api-Post "/knowledge-docs" @{
        categoryId = $created.categoryId
        title = $docTitle
        docType = "FAQ"
        intentCode = "RETURN_APPLY"
        scenario = "auto-test"
        question = "auto test return question"
        answer = "auto test return answer"
        content = "Auto test return rule content. Return is usually allowed within seven days after signing if resale is not affected."
        keywords = "auto,test,return"
        priority = 88
        status = "ENABLED"
    } | Out-Null
    $docPage = Api-Get ("/knowledge-docs?page=1&pageSize=10&keyword=" + [uri]::EscapeDataString($docTitle))
    $doc = $docPage.rows | Where-Object { $_.title -eq $docTitle } | Select-Object -First 1
    $created.docId = $doc.id
    Add-Result "knowledge doc create/page" ($null -ne $created.docId) "id=$($created.docId)"

    $docDetail = Api-Get "/knowledge-docs/$($created.docId)"
    $docDetail.answer = "auto test return answer updated"
    Api-Put "/knowledge-docs/$($created.docId)" $docDetail | Out-Null
    $docUpdated = Api-Get "/knowledge-docs/$($created.docId)"
    Add-Result "knowledge doc get/update" ($docUpdated.answer -like "*updated*") $docUpdated.answer

    $hits = Api-Get "/knowledge-docs/search?query=auto%20test%20return%20question&intentCode=RETURN_APPLY&limit=5"
    $hitCount = @($hits).Count
    Add-Result "knowledge search" ($hitCount -ge 1) "hits=$hitCount"

    $orderNo = "AUTO$stamp"
    Api-Post "/orders" @{
        orderNo = $orderNo
        userId = 1
        productName = "Auto Test Product $stamp"
        skuName = "Black Standard"
        orderAmount = 199.90
        payStatus = "PAID"
        orderStatus = "SIGNED"
        logisticsStatus = "DELIVERED"
        afterSaleStatus = "NONE"
        paidAt = "2026-04-20T10:00:00"
        shippedAt = "2026-04-21T10:00:00"
        signedAt = "2026-04-22T10:00:00"
    } | Out-Null
    $order = Api-Get "/orders/no/$orderNo"
    $created.orderId = $order.id
    Add-Result "order create/getByNo" ($order.orderNo -eq $orderNo) "id=$($created.orderId)"

    $order.productName = "Auto Test Product Updated $stamp"
    Api-Put "/orders/$($created.orderId)" $order | Out-Null
    $orderUpdated = Api-Get "/orders/$($created.orderId)"
    Add-Result "order get/update" ($orderUpdated.productName -like "*Updated*") $orderUpdated.productName

    $orderPage = Api-Get "/orders?page=1&pageSize=10&keyword=$orderNo"
    Add-Result "order page/search" ($orderPage.total -ge 1) "total=$($orderPage.total)"

    Api-Post "/after-sale-records" @{
        orderId = $created.orderId
        serviceType = "RETURN"
        reason = "auto test return reason"
        status = "APPLIED"
        refundAmount = 199.90
        remark = "auto test after-sale record"
    } | Out-Null
    $afterPage = Api-Get "/after-sale-records?page=1&pageSize=10&orderId=$($created.orderId)"
    $after = $afterPage.rows | Select-Object -First 1
    $created.afterSaleId = $after.id
    Add-Result "after-sale create/page" ($null -ne $created.afterSaleId) "id=$($created.afterSaleId)"

    $after.reason = "auto test after-sale reason updated"
    Api-Put "/after-sale-records/$($created.afterSaleId)" $after | Out-Null
    $afterUpdated = Api-Get "/after-sale-records/$($created.afterSaleId)"
    Add-Result "after-sale get/update" ($afterUpdated.reason -like "*updated*") $afterUpdated.reason

    $orderAfterSales = Api-Get "/orders/$($created.orderId)/after-sale-records"
    $orderAfterSaleCount = @($orderAfterSales).Count
    Add-Result "order after-sale subresource" ($orderAfterSaleCount -ge 1) "count=$orderAfterSaleCount"

    $session = Api-Post "/chat-sessions" @{
        title = "Auto Test Session $stamp"
        orderNo = "DD202604290001"
        channel = "WEB"
    }
    $created.sessionId = $session.id
    Add-Result "chat session create" ($null -ne $created.sessionId) "id=$($created.sessionId)"

    $sessionDetail = Api-Get "/chat-sessions/$($created.sessionId)"
    Add-Result "chat session detail" ($sessionDetail.id -eq $created.sessionId) "sessionNo=$($sessionDetail.sessionNo)"

    Api-Put "/chat-sessions/$($created.sessionId)" @{
        title = "Auto Test Session Updated $stamp"
        status = "ACTIVE"
        channel = "WEB"
        orderNo = "DD202604290001"
    } | Out-Null
    $sessionUpdated = Api-Get "/chat-sessions/$($created.sessionId)"
    Add-Result "chat session update" ($sessionUpdated.title -like "*Updated*") $sessionUpdated.title

    $chat = Api-Post "/chat-sessions/$($created.sessionId)/messages" @{
        content = $chatQuestion
        orderNo = "DD202604290001"
        useAi = $true
    }
    $chatOk = $chat.intent.intentCode -eq "RETURN_APPLY" -and $chat.assistantMessage.sourceType -eq "AI_ENHANCED" -and $chat.ai.status -eq "SUCCESS"
    Add-Result "chat AI enhanced flow" $chatOk "intent=$($chat.intent.intentCode), source=$($chat.assistantMessage.sourceType), ai=$($chat.ai.status)"

    $messages = Api-Get "/chat-sessions/$($created.sessionId)/messages"
    $messageCount = @($messages).Count
    Add-Result "chat messages list" ($messageCount -ge 2) "count=$messageCount"

    $traces = Api-Get "/chat-sessions/$($created.sessionId)/process-traces"
    $traceCount = @($traces).Count
    Add-Result "process traces" ($traceCount -ge 5) "count=$traceCount"

    $aiLogs = Api-Get "/ai-call-logs?page=1&pageSize=10&status=SUCCESS"
    Add-Result "ai call logs" ($aiLogs.total -ge 1) "total=$($aiLogs.total)"

    $retrievalLogs = Api-Get "/retrieval-logs?page=1&pageSize=10&keyword=return"
    Add-Result "retrieval logs" ($retrievalLogs.total -ge 1) "total=$($retrievalLogs.total)"

    Api-Delete "/chat-sessions/$($created.sessionId)" | Out-Null
    $created.sessionId = $null
    Add-Result "chat session delete" $true "deleted"

    Api-Delete "/after-sale-records/$($created.afterSaleId)" | Out-Null
    $created.afterSaleId = $null
    Add-Result "after-sale delete" $true "deleted"

    Api-Delete "/orders/$($created.orderId)" | Out-Null
    $created.orderId = $null
    Add-Result "order delete" $true "deleted"

    Api-Delete "/knowledge-docs/$($created.docId)" | Out-Null
    $created.docId = $null
    Add-Result "knowledge doc delete" $true "deleted"

    Api-Delete "/knowledge-categories/$($created.categoryId)" | Out-Null
    $created.categoryId = $null
    Add-Result "knowledge category delete" $true "deleted"
}
catch {
    Add-Result "test runner exception" $false $_.Exception.Message
}
finally {
    try {
        if ($created.sessionId) { Api-Delete "/chat-sessions/$($created.sessionId)" | Out-Null }
    } catch {}
    try {
        if ($created.afterSaleId) { Api-Delete "/after-sale-records/$($created.afterSaleId)" | Out-Null }
    } catch {}
    try {
        if ($created.orderId) { Api-Delete "/orders/$($created.orderId)" | Out-Null }
    } catch {}
    try {
        if ($created.docId) { Api-Delete "/knowledge-docs/$($created.docId)" | Out-Null }
    } catch {}
    try {
        if ($created.categoryId) { Api-Delete "/knowledge-categories/$($created.categoryId)" | Out-Null }
    } catch {}
}

$failed = @($results | Where-Object { $_.ok -ne $true })
$results | Format-Table -AutoSize

if ($failed.Count -gt 0) {
    Write-Host "FAILED_COUNT=$($failed.Count)"
    exit 1
}

Write-Host "FAILED_COUNT=0"
