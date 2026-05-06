$ErrorActionPreference = "Stop"

$base = "http://localhost:8081"
$front = "http://localhost:5173"
$sub2api = "http://127.0.0.1:8080"
$stamp = Get-Date -Format "yyyyMMddHHmmss"
$authToken = $null
$results = New-Object System.Collections.Generic.List[object]
$created = @{
    categoryId = $null
    docId = $null
    orderId = $null
    afterSaleId = $null
    realAfterSaleId = $null
    realAfterSaleTicketId = $null
    realAfterSaleTicketSessionId = $null
    reviewOrderId = $null
    reviewAfterSaleId = $null
    sessionId = $null
    ticketId = $null
}
$otherCustomerToken = $null

function U($codes) {
    return -join ($codes | ForEach-Object { [char]$_ })
}

$chatQuestion = U @(0x8fd9,0x4e2a,0x8ba2,0x5355,0x80fd,0x4e0d,0x80fd,0x9000,0x8d27,0xff1f)
$followQuestion = U @(0x90a3,0x9000,0x6b3e,0x591a,0x4e45,0x5230,0x8d26,0xff1f)
$ticketQuestion = U @(0x5546,0x5bb6,0x4e00,0x76f4,0x4e0d,0x5904,0x7406,0x53ef,0x4ee5,0x8f6c,0x4eba,0x5de5,0x6295,0x8bc9,0x5417,0xff1f)
$accountExistsMessage = U @(0x8d26,0x53f7,0x5df2,0x5b58,0x5728)

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
    $headers = Auth-Headers
    $response = Invoke-RestMethod -Uri "$base$path" -Method Get -Headers $headers -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Post($path, $body) {
    $json = $body | ConvertTo-Json -Depth 12
    $headers = Auth-Headers
    $response = Invoke-RestMethod -Uri "$base$path" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 120
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Post-Raw($path, $body) {
    $json = $body | ConvertTo-Json -Depth 12
    $headers = Auth-Headers
    $response = Invoke-WebRequest -Uri "$base$path" -Method Post -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 120 -UseBasicParsing
    $stream = $response.RawContentStream
    $stream.Position = 0
    $bytes = New-Object byte[] $stream.Length
    [void]$stream.Read($bytes, 0, $bytes.Length)
    $content = [System.Text.Encoding]::UTF8.GetString($bytes)
    return $content | ConvertFrom-Json
}

function Api-Put($path, $body) {
    $json = $body | ConvertTo-Json -Depth 12
    $headers = Auth-Headers
    $response = Invoke-RestMethod -Uri "$base$path" -Method Put -Headers $headers -ContentType "application/json; charset=utf-8" -Body $json -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Delete($path) {
    $headers = Auth-Headers
    $response = Invoke-RestMethod -Uri "$base$path" -Method Delete -Headers $headers -TimeoutSec 60
    if ($response.code -ne 1) {
        throw "$path failed: $($response.msg)"
    }
    return $response.data
}

function Api-Get-Text($path) {
    $headers = Auth-Headers
    $response = Invoke-WebRequest -Uri "$base$path" -Method Get -Headers $headers -TimeoutSec 60 -UseBasicParsing
    return $response.Content
}

function Api-Get-Raw($path) {
    $headers = Auth-Headers
    $response = Invoke-WebRequest -Uri "$base$path" -Method Get -Headers $headers -TimeoutSec 60 -UseBasicParsing
    return $response.Content | ConvertFrom-Json
}

function Is-Rejected($response) {
    return $response.code -eq 0 -and -not [string]::IsNullOrWhiteSpace([string]$response.msg)
}

function Auth-Headers() {
    if ($script:authToken) {
        return @{ Authorization = "Bearer $script:authToken" }
    }
    return @{}
}

try {
    $frontStatus = (Invoke-WebRequest -Uri "$front/" -UseBasicParsing -TimeoutSec 20).StatusCode
    Add-Result "frontend index" ($frontStatus -eq 200) "HTTP $frontStatus"

    $subHealth = Invoke-RestMethod -Uri "$sub2api/health" -Method Get -TimeoutSec 20
    Add-Result "sub2api health" ($subHealth.status -eq "ok") $subHealth.status

    $auth = Api-Post "/auth/login" @{ username = "admin"; password = "123456" }
    $script:authToken = $auth.token
    $adminToken = $auth.token
    $adminUserId = $auth.userId
    Add-Result "auth login" ($auth.role -eq "ADMIN" -and $null -ne $auth.token) "user=$($auth.username), role=$($auth.role)"

    $status = Api-Get "/system/status"
    $aiEnabled = $status.ai.enabled -eq $true
    $aiReady = if ($aiEnabled) { $status.ai.status -eq "UP" } else { $status.ai.status -eq "SKIPPED" }
    Add-Result "system status" ($status.database.status -eq "UP" -and $aiReady) "db=$($status.database.status), ai=$($status.ai.status), enabled=$aiEnabled"

    $enums = Api-Get "/system/enums"
    Add-Result "system enums" ($enums.intentCodes.Count -ge 7) "intentCodes=$($enums.intentCodes.Count)"

    $me = Api-Get "/auth/me"
    Add-Result "auth me" ($me.username -eq "admin" -and $me.role -eq "ADMIN") "role=$($me.role)"

    $registerUsername = "auto_user_$stamp"
    $registerPassword = "auto123456"
    $registered = Api-Post "/auth/register" @{
        username = $registerUsername
        password = $registerPassword
        confirmPassword = $registerPassword
        displayName = "Auto User $stamp"
        phone = ""
    }
    Add-Result "auth register customer" ($registered.username -eq $registerUsername -and $registered.role -eq "CUSTOMER" -and $null -ne $registered.token) "user=$($registered.username), role=$($registered.role)"

    $duplicate = Api-Post-Raw "/auth/register" @{
        username = $registerUsername
        password = $registerPassword
        confirmPassword = $registerPassword
        displayName = "Duplicate User"
        phone = ""
    }
    Add-Result "auth register duplicate rejected" ($duplicate.code -eq 0 -and $duplicate.msg -eq $accountExistsMessage) $duplicate.msg

    $registeredLogin = Api-Post "/auth/login" @{ username = $registerUsername; password = $registerPassword }
    $customerToken = $registeredLogin.token
    $customerUserId = $registeredLogin.userId
    Add-Result "auth registered customer login" ($registeredLogin.username -eq $registerUsername -and $registeredLogin.role -eq "CUSTOMER") "role=$($registeredLogin.role)"

    $otherCustomerLogin = Api-Post "/auth/login" @{ username = "demo_customer"; password = "123456" }
    $otherCustomerToken = $otherCustomerLogin.token
    Add-Result "auth demo customer login for permission checks" ($otherCustomerLogin.role -eq "CUSTOMER" -and $otherCustomerLogin.userId -ne $customerUserId) "user=$($otherCustomerLogin.username),role=$($otherCustomerLogin.role)"

    $script:authToken = $customerToken
    $blockedAdminQueue = Api-Get-Raw "/admin/after-sales?page=1&pageSize=1"
    Add-Result "permission customer blocked admin after-sales" (Is-Rejected $blockedAdminQueue) $blockedAdminQueue.msg
    $blockedTicketQueue = Api-Get-Raw "/service-tickets?page=1&pageSize=1"
    Add-Result "permission customer blocked service ticket queue" (Is-Rejected $blockedTicketQueue) $blockedTicketQueue.msg

    $script:authToken = $adminToken
    $blockedCustomerCenter = Api-Get-Raw "/customer/after-sales?page=1&pageSize=1"
    Add-Result "permission admin blocked customer after-sales" (Is-Rejected $blockedCustomerCenter) $blockedCustomerCenter.msg

    $script:authToken = $adminToken

    $ai = Api-Post "/ai-tests" @{ prompt = "Reply exactly: AI smoke test success." }
    $aiTestOk = if ($aiEnabled) { $ai.status -eq "SUCCESS" -and $ai.used -eq $true } else { $ai.status -eq "SKIPPED" -and $ai.used -eq $false }
    Add-Result "ai-tests configured mode" $aiTestOk "status=$($ai.status), used=$($ai.used), enabled=$aiEnabled, latency=$($ai.latencyMs)"

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

    $realOrderNo = "REAL$stamp"
    Api-Post "/orders" @{
        orderNo = $realOrderNo
        userId = $customerUserId
        productName = "Real Flow Test Product $stamp"
        skuName = "Blue Standard"
        orderAmount = 268.80
        payStatus = "PAID"
        orderStatus = "SIGNED"
        logisticsStatus = "DELIVERED"
        afterSaleStatus = "NONE"
        paidAt = "2026-04-23T10:00:00"
        shippedAt = "2026-04-24T10:00:00"
        signedAt = "2026-04-25T10:00:00"
    } | Out-Null
    $realOrder = Api-Get "/orders/no/$realOrderNo"
    $realOrderId = $realOrder.id
    Add-Result "real after-sale order create" ($realOrder.userId -eq $customerUserId) "id=$realOrderId,user=$($realOrder.userId)"

    $script:authToken = $customerToken
    $realApplication = Api-Post "/customer/after-sales" @{
        orderId = $realOrderId
        serviceType = "RETURN"
        reasonCode = "QUALITY_PROBLEM"
        reasonText = "Auto real flow return reason"
        refundAmount = 188.80
    }
    $created.realAfterSaleId = $realApplication.id
    $customerAfterSales = Api-Get "/customer/after-sales?page=1&pageSize=10&keyword=$($realApplication.applicationNo)"
    $customerAfterSaleDetail = Api-Get "/customer/after-sales/$($created.realAfterSaleId)"
    $customerFlowOk = $realApplication.status -eq "SUBMITTED" -and
                      $customerAfterSales.total -ge 1 -and
                      @($customerAfterSaleDetail.processLogs).Count -eq 1 -and
                      $customerAfterSaleDetail.processLogs[0].action -eq "SUBMIT"
    Add-Result "real after-sale customer submit/timeline" $customerFlowOk "id=$($created.realAfterSaleId),status=$($realApplication.status),logs=$(@($customerAfterSaleDetail.processLogs).Count)"

    $script:authToken = $otherCustomerToken
    $blockedOtherCustomerDetail = Api-Get-Raw "/customer/after-sales/$($created.realAfterSaleId)"
    Add-Result "permission other customer blocked after-sale detail" (Is-Rejected $blockedOtherCustomerDetail) $blockedOtherCustomerDetail.msg
    $blockedOtherCustomerEvidence = Api-Post-Raw "/customer/after-sales/$($created.realAfterSaleId)/evidence" @{
        evidenceType = "TEXT"
        content = "Unauthorized evidence should be rejected"
    }
    Add-Result "permission other customer blocked evidence submit" (Is-Rejected $blockedOtherCustomerEvidence) $blockedOtherCustomerEvidence.msg

    $script:authToken = $adminToken
    $adminAfterSales = Api-Get "/admin/after-sales?page=1&pageSize=10&status=SUBMITTED&keyword=$($realApplication.applicationNo)"
    Add-Result "real after-sale admin queue" ($adminAfterSales.total -ge 1) "total=$($adminAfterSales.total)"

    $requestedEvidence = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/request-evidence" @{
        remark = "Auto admin requested logistics or issue evidence"
        assignedTo = $adminUserId
    }
    $requestEvidenceOk = $requestedEvidence.status -eq "NEED_MORE_EVIDENCE" -and
                         (@($requestedEvidence.processLogs | Where-Object { $_.action -eq "REQUEST_MORE_EVIDENCE" }).Count -ge 1)
    Add-Result "real after-sale admin request evidence" $requestEvidenceOk "status=$($requestedEvidence.status),logs=$(@($requestedEvidence.processLogs).Count)"

    $slaWaitingTasks = Api-Get "/admin/sla/tasks?page=1&pageSize=10&riskType=WAITING_CUSTOMER"
    $slaHasWaitingTask = $null -ne ($slaWaitingTasks.rows | Where-Object { $_.id -eq $created.realAfterSaleId } | Select-Object -First 1)
    Add-Result "real after-sale SLA waiting-customer queue" $slaHasWaitingTask "total=$($slaWaitingTasks.total)"

    $script:authToken = $customerToken
    $evidence = Api-Post "/customer/after-sales/$($created.realAfterSaleId)/evidence" @{
        evidenceType = "LOGISTICS_NO"
        content = "AUTO-LOGISTICS-$stamp"
    }
    $customerEvidenceDetail = Api-Get "/customer/after-sales/$($created.realAfterSaleId)"
    $evidenceOk = $evidence.evidenceType -eq "LOGISTICS_NO" -and
                  @($customerEvidenceDetail.evidences).Count -ge 1 -and
                  (@($customerEvidenceDetail.processLogs | Where-Object { $_.action -eq "SUPPLEMENT_EVIDENCE" }).Count -ge 1)
    Add-Result "real after-sale customer evidence chain" $evidenceOk "evidences=$(@($customerEvidenceDetail.evidences).Count),logs=$(@($customerEvidenceDetail.processLogs).Count)"

    $script:authToken = $adminToken
    $approvedApplication = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/approve" @{
        remark = "Auto admin approved real after-sale flow"
        approvedAmount = 188.80
        assignedTo = $adminUserId
    }
    $approvedDetail = Api-Get "/admin/after-sales/$($created.realAfterSaleId)"
    $approvedLogActions = @($approvedDetail.processLogs | ForEach-Object { $_.action })
    $approveFlowOk = $approvedApplication.status -eq "WAIT_BUYER_SEND" -and
                     $approvedDetail.approvedAmount -eq 188.80 -and
                     $approvedLogActions -contains "APPROVE" -and
                     @($approvedDetail.processLogs).Count -ge 2
    Add-Result "real after-sale admin approve/state-log" $approveFlowOk "status=$($approvedApplication.status),logs=$(@($approvedDetail.processLogs).Count)"

    $linkedTicket = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/tickets" @{
        remark = "Auto admin created linked service ticket for real after-sale flow"
    }
    $created.realAfterSaleTicketId = $linkedTicket.id
    $created.realAfterSaleTicketSessionId = $linkedTicket.sessionId
    $linkedAfterSaleDetail = Api-Get "/admin/after-sales/$($created.realAfterSaleId)"
    $createTicketOk = $linkedAfterSaleDetail.ticketId -eq $linkedTicket.id -and
                      $linkedAfterSaleDetail.ticketNo -eq $linkedTicket.ticketNo -and
                      (@($linkedAfterSaleDetail.processLogs | Where-Object { $_.action -eq "CREATE_TICKET" }).Count -ge 1)
    Add-Result "real after-sale linked ticket create/log" $createTicketOk "ticket=$($linkedTicket.ticketNo),status=$($linkedTicket.status)"

    $linkedTicketDetail = Api-Get "/service-tickets/$($created.realAfterSaleTicketId)"
    $linkedTicketDetail.status = "PROCESSING"
    Api-Put "/service-tickets/$($created.realAfterSaleTicketId)" $linkedTicketDetail | Out-Null
    $linkedTicketUpdated = Api-Get "/service-tickets/$($created.realAfterSaleTicketId)"
    $linkedTicketAfterSaleDetail = Api-Get "/admin/after-sales/$($created.realAfterSaleId)"
    $updateTicketOk = $linkedTicketUpdated.status -eq "PROCESSING" -and
                      (@($linkedTicketAfterSaleDetail.processLogs | Where-Object { $_.action -eq "UPDATE_TICKET" }).Count -ge 1)
    Add-Result "real after-sale linked ticket update/log" $updateTicketOk "status=$($linkedTicketUpdated.status),logs=$(@($linkedTicketAfterSaleDetail.processLogs).Count)"

    $replyDraft = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/reply-drafts" @{
        remark = "Auto admin requested AI copilot reply draft"
    }
    $draftList = Api-Get "/admin/after-sales/$($created.realAfterSaleId)/reply-drafts"
    $draftActions = @((Api-Get "/admin/after-sales/$($created.realAfterSaleId)").processLogs | ForEach-Object { $_.action })
    $draftCreateOk = $replyDraft.status -eq "DRAFT" -and
                     $replyDraft.draftContent.Length -gt 20 -and
                     @($draftList | Where-Object { $_.id -eq $replyDraft.id }).Count -eq 1 -and
                     $draftActions -contains "GENERATE_REPLY_DRAFT"
    Add-Result "real after-sale AI copilot draft create/log" $draftCreateOk "source=$($replyDraft.sourceType),ai=$($replyDraft.aiStatus),risk=$($replyDraft.riskLevel)"

    $usedDraft = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/reply-drafts/$($replyDraft.id)/use" @{
        remark = "Auto admin used AI copilot reply draft"
    }
    $usedDraftDetail = Api-Get "/admin/after-sales/$($created.realAfterSaleId)"
    $usedDraftOk = $usedDraft.status -eq "USED" -and
                   (@($usedDraftDetail.processLogs | Where-Object { $_.action -eq "USE_REPLY_DRAFT" }).Count -ge 1)
    Add-Result "real after-sale AI copilot draft use/log" $usedDraftOk "status=$($usedDraft.status)"

    $discardDraft = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/reply-drafts" @{
        remark = "Auto admin requested discardable AI copilot reply draft"
    }
    $discardedDraft = Api-Post "/admin/after-sales/$($created.realAfterSaleId)/reply-drafts/$($discardDraft.id)/discard" @{
        remark = "Auto admin discarded AI copilot reply draft"
    }
    $discardDraftDetail = Api-Get "/admin/after-sales/$($created.realAfterSaleId)"
    $discardDraftOk = $discardedDraft.status -eq "DISCARDED" -and
                      (@($discardDraftDetail.processLogs | Where-Object { $_.action -eq "DISCARD_REPLY_DRAFT" }).Count -ge 1)
    Add-Result "real after-sale AI copilot draft discard/log" $discardDraftOk "status=$($discardedDraft.status)"

    $reviewOrderNo = "REVIEWFLOW$stamp"
    Api-Post "/orders" @{
        orderNo = $reviewOrderNo
        userId = $customerUserId
        productName = "Review Flow Product $stamp"
        skuName = "Green Standard"
        orderAmount = 99.80
        payStatus = "PAID"
        orderStatus = "COMPLETED"
        logisticsStatus = "DELIVERED"
        afterSaleStatus = "NONE"
        paidAt = "2026-04-23T10:00:00"
        shippedAt = "2026-04-24T10:00:00"
        signedAt = "2026-04-25T10:00:00"
    } | Out-Null
    $reviewOrder = Api-Get "/orders/no/$reviewOrderNo"
    $created.reviewOrderId = $reviewOrder.id

    $script:authToken = $customerToken
    $reviewApplication = Api-Post "/customer/after-sales" @{
        orderId = $created.reviewOrderId
        serviceType = "REFUND"
        reasonCode = "QUALITY_PROBLEM"
        reasonText = "Auto completed review flow reason"
        refundAmount = 99.80
    }
    $created.reviewAfterSaleId = $reviewApplication.id
    $script:authToken = $adminToken
    $reviewApproved = Api-Post "/admin/after-sales/$($created.reviewAfterSaleId)/approve" @{
        remark = "Auto admin approved reviewable after-sale flow"
        approvedAmount = 99.80
        assignedTo = $adminUserId
    }
    $reviewCompleted = Api-Post "/admin/after-sales/$($created.reviewAfterSaleId)/complete" @{
        remark = "Auto admin completed reviewable after-sale flow"
    }
    Add-Result "real after-sale admin complete for review" ($reviewCompleted.status -eq "COMPLETED") "status=$($reviewCompleted.status)"

    $blockedAdminReview = Api-Post-Raw "/customer/after-sales/$($created.reviewAfterSaleId)/reviews" @{
        rating = 5
        tags = "should be rejected"
        comment = "Admin cannot submit a customer review"
    }
    Add-Result "permission admin blocked customer review submit" (Is-Rejected $blockedAdminReview) $blockedAdminReview.msg

    $script:authToken = $otherCustomerToken
    $blockedOtherCustomerReview = Api-Get-Raw "/customer/after-sales/$($created.reviewAfterSaleId)/reviews"
    Add-Result "permission other customer blocked review read" (Is-Rejected $blockedOtherCustomerReview) $blockedOtherCustomerReview.msg

    $script:authToken = $customerToken
    $review = Api-Post "/customer/after-sales/$($created.reviewAfterSaleId)/reviews" @{
        rating = 5
        tags = "响应快,处理清楚"
        comment = "Auto customer review for completed after-sale"
    }
    $reviewDetail = Api-Get "/customer/after-sales/$($created.reviewAfterSaleId)/reviews"
    $reviewOk = $review.rating -eq 5 -and $reviewDetail.comment -like "*Auto customer review*"
    Add-Result "real after-sale customer review submit/get" $reviewOk "rating=$($review.rating),tags=$($review.tags)"

    $script:authToken = $adminToken
    $profile = Api-Get "/admin/customers/$customerUserId/profile"
    $profileOk = $profile.customer.id -eq $customerUserId -and
                 $profile.reviewCount -ge 1 -and
                 $profile.averageRating -ge 5 -and
                 @($profile.reviews).Count -ge 1
    Add-Result "admin customer profile aggregates reviews" $profileOk "reviews=$($profile.reviewCount),avg=$($profile.averageRating),risk=$($profile.riskLevel)"

    $session = Api-Post "/chat-sessions" @{
        title = "Auto Test Session $stamp"
        orderNo = "DD202604290001"
        channel = "APP"
    }
    $created.sessionId = $session.id
    Add-Result "chat session create" ($null -ne $created.sessionId -and $session.channel -eq "APP") "id=$($created.sessionId), channel=$($session.channel)"

    $appSessions = Api-Get "/chat-sessions?page=1&pageSize=10&channel=APP"
    $hasAppSession = $null -ne ($appSessions.rows | Where-Object { $_.id -eq $created.sessionId } | Select-Object -First 1)
    Add-Result "chat session channel filter" $hasAppSession "channel=APP,total=$($appSessions.total)"

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
    $expectedSource = if ($aiEnabled) { "AI_ENHANCED" } else { "FALLBACK" }
    $expectedAiStatus = if ($aiEnabled) { "SUCCESS" } else { "SKIPPED" }
    $chatOk = $chat.intent.intentCode -eq "RETURN_APPLY" -and $chat.assistantMessage.sourceType -eq $expectedSource -and $chat.ai.status -eq $expectedAiStatus
    Add-Result "chat configured AI/fallback flow" $chatOk "intent=$($chat.intent.intentCode), source=$($chat.assistantMessage.sourceType), ai=$($chat.ai.status), enabled=$aiEnabled"

    $followChat = Api-Post "/chat-sessions/$($created.sessionId)/messages" @{
        content = $followQuestion
        orderNo = "DD202604290001"
        useAi = $true
    }
    $followOk = $followChat.context.followUp -eq $true -and $followChat.intent.intentCode -eq "REFUND_PROGRESS" -and $followChat.intent.method -eq "HYBRID"
    Add-Result "multi-turn follow-up context" $followOk "followUp=$($followChat.context.followUp), intent=$($followChat.intent.intentCode), method=$($followChat.intent.method)"

    $ticketChat = Api-Post "/chat-sessions/$($created.sessionId)/messages" @{
        content = $ticketQuestion
        orderNo = "DD202604290001"
        useAi = $true
    }
    $created.ticketId = $ticketChat.ticket.id
    $ticketOk = $ticketChat.ticket.needed -eq $true -and $null -ne $ticketChat.ticket.ticketNo -and $ticketChat.intent.intentCode -eq "COMPLAINT_TRANSFER"
    Add-Result "chat service ticket auto handoff" $ticketOk "ticket=$($ticketChat.ticket.ticketNo), priority=$($ticketChat.ticket.priority)"

    $ticketPage = Api-Get "/service-tickets?page=1&pageSize=10&status=PENDING"
    Add-Result "service ticket page" ($ticketPage.total -ge 1) "total=$($ticketPage.total)"

    $ticketDetail = Api-Get "/service-tickets/$($created.ticketId)"
    $ticketDetail.status = "PROCESSING"
    Api-Put "/service-tickets/$($created.ticketId)" $ticketDetail | Out-Null
    $ticketUpdated = Api-Get "/service-tickets/$($created.ticketId)"
    Add-Result "service ticket get/update" ($ticketUpdated.status -eq "PROCESSING") "status=$($ticketUpdated.status)"

    $sessionTickets = Api-Get "/chat-sessions/$($created.sessionId)/service-tickets"
    $sessionTicketCount = @($sessionTickets).Count
    Add-Result "session service tickets subresource" ($sessionTicketCount -ge 1) "count=$sessionTicketCount"

    $messages = Api-Get "/chat-sessions/$($created.sessionId)/messages"
    $messageCount = @($messages).Count
    Add-Result "chat messages list" ($messageCount -ge 6) "count=$messageCount"

    $traces = Api-Get "/chat-sessions/$($created.sessionId)/process-traces"
    $traceCount = @($traces).Count
    $hasContextTrace = $null -ne ($traces | Where-Object { $_.stepName -eq "CONTEXT_RESOLVE" } | Select-Object -First 1)
    Add-Result "process traces" ($traceCount -ge 7 -and $hasContextTrace) "count=$traceCount, contextTrace=$hasContextTrace"

    $aiLogs = Api-Get "/ai-call-logs?page=1&pageSize=10&status=SUCCESS"
    Add-Result "ai call logs" ($aiLogs.total -ge 1) "total=$($aiLogs.total)"

    $retrievalLogs = Api-Get "/retrieval-logs?page=1&pageSize=10&keyword=return"
    Add-Result "retrieval logs" ($retrievalLogs.total -ge 1) "total=$($retrievalLogs.total)"

    $diagnostics = Api-Get "/log-diagnostics"
    $diagnosticsOk = $diagnostics.ai.sampleSize -ge 1 -and $diagnostics.riskSignals.Count -ge 1 -and $diagnostics.actionItems.Count -ge 1
    Add-Result "log diagnostics health summary" $diagnosticsOk "health=$($diagnostics.ai.healthLevel), trend=$($diagnostics.ai.trendLabel), risks=$($diagnostics.riskSignals.Count)"

    $evidenceReport = Api-Get-Text "/chat-sessions/$($created.sessionId)/evidence-report"
    $evidenceOk = $evidenceReport.Contains($sessionDetail.sessionNo) -and
                  $evidenceReport.Contains("RETURN_APPLY") -and
                  $evidenceReport.Contains("SUCCESS") -and
                  $evidenceReport.Contains("BUSINESS_TOOL_CALLS") -and
                  $evidenceReport.Contains($ticketChat.ticket.ticketNo)
    Add-Result "chat evidence report export" $evidenceOk "length=$($evidenceReport.Length)"

    $operationInsights = Api-Get "/operation-insights"
    Add-Result "operation insights feature matrix" ($operationInsights.newFeatures.Count -ge 12) "newFeatures=$($operationInsights.newFeatures.Count)"
    Add-Result "operation insights intent radar" ($operationInsights.intentInsights.Count -ge 1) "intentInsights=$($operationInsights.intentInsights.Count)"
    Add-Result "operation insights action items" ($operationInsights.actionItems.Count -ge 4) "actionItems=$($operationInsights.actionItems.Count)"
    Add-Result "operation insights AI quality" ($operationInsights.aiInsights.Count -ge 4) "aiInsights=$($operationInsights.aiInsights.Count)"

    $featureClosures = Api-Get "/feature-closures"
    $closureCodes = @($featureClosures.closures | ForEach-Object { $_.code })
    $allClosed = (@($featureClosures.closures | Where-Object { $_.closedLoop -ne $true }).Count -eq 0)
    Add-Result "feature closure count" ($featureClosures.closures.Count -ge 14) "closures=$($featureClosures.closures.Count)"
    Add-Result "feature closure all closed" $allClosed "unclosed=$(@($featureClosures.closures | Where-Object { $_.closedLoop -ne $true }).Count)"
    Add-Result "feature closure SLA guard" ($closureCodes -contains "SLA_GUARD") "codes=$($closureCodes.Count)"
    Add-Result "feature closure evidence checker" ($closureCodes -contains "EVIDENCE_CHAIN_CHECKER") "codes=$($closureCodes.Count)"
    Add-Result "feature closure RAG review" ($closureCodes -contains "RAG_REVIEW_BOARD") "codes=$($closureCodes.Count)"
    Add-Result "feature closure demo references" ($featureClosures.demoSteps.Count -ge 6 -and $featureClosures.references.Count -ge 3) "steps=$($featureClosures.demoSteps.Count), refs=$($featureClosures.references.Count)"

    Api-Delete "/chat-sessions/$($created.sessionId)" | Out-Null
    $created.sessionId = $null
    $created.ticketId = $null
    Add-Result "chat session delete" $true "deleted"

    Api-Delete "/chat-sessions/$($created.realAfterSaleTicketSessionId)" | Out-Null
    $created.realAfterSaleTicketSessionId = $null
    $created.realAfterSaleTicketId = $null
    Add-Result "real after-sale linked ticket session delete" $true "deleted"

    Api-Delete "/after-sale-records/$($created.afterSaleId)" | Out-Null
    $created.afterSaleId = $null
    Add-Result "after-sale delete" $true "deleted"

    Api-Delete "/orders/$($created.orderId)" | Out-Null
    $created.orderId = $null
    Add-Result "order delete" $true "deleted"

    Api-Delete "/orders/$realOrderId" | Out-Null
    $created.realAfterSaleId = $null
    Add-Result "real after-sale order cascade delete" $true "deleted"

    Api-Delete "/orders/$($created.reviewOrderId)" | Out-Null
    $created.reviewOrderId = $null
    $created.reviewAfterSaleId = $null
    Add-Result "real after-sale review order cascade delete" $true "deleted"

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
        if ($created.ticketId) { Api-Delete "/service-tickets/$($created.ticketId)" | Out-Null }
    } catch {}
    try {
        if ($created.realAfterSaleTicketId) { Api-Delete "/service-tickets/$($created.realAfterSaleTicketId)" | Out-Null }
    } catch {}
    try {
        if ($created.realAfterSaleTicketSessionId) { Api-Delete "/chat-sessions/$($created.realAfterSaleTicketSessionId)" | Out-Null }
    } catch {}
    try {
        if ($created.afterSaleId) { Api-Delete "/after-sale-records/$($created.afterSaleId)" | Out-Null }
    } catch {}
    try {
        if ($created.reviewOrderId) { Api-Delete "/orders/$($created.reviewOrderId)" | Out-Null }
    } catch {}
    try {
        if ($realOrderId) { Api-Delete "/orders/$realOrderId" | Out-Null }
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
