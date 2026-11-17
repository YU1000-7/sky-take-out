param(
    [string]$BaseUrl = 'http://127.0.0.1:18080',
    [string]$AdminUrl = 'http://127.0.0.1:8081',
    [string]$H5Url = 'http://127.0.0.1:8082'
)

$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'infra-common.ps1')

$base = $BaseUrl.TrimEnd('/')
$health = Invoke-RestMethod -Uri "$base/health" -TimeoutSec 10
if ($health.code -ne 1) {
    throw 'Backend health check failed.'
}

$loginBody = @{ username = 'admin'; password = '123456' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/admin/employee/login" `
    -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
if ($login.code -ne 1) {
    throw "Login failed: $($login.msg)"
}
$token = $login.data.token
$headers = @{ token = $token }
Write-Host 'PASS  login'

$page = Invoke-RestMethod -Uri "$base/admin/employee/page?page=1&pageSize=10" `
    -Headers $headers -TimeoutSec 10
if ($page.code -ne 1) {
    throw "Employee page failed: $($page.msg)"
}
Write-Host "PASS  employee page total=$($page.data.total)"

$suffix = Get-Date -Format 'HHmmss'
$username = "demo_$suffix"
$createBody = @{
    username = $username
    name = '联调员工'
    phone = '13800138002'
    sex = '男'
    idNumber = '110101199001011234'
} | ConvertTo-Json
$created = Invoke-RestMethod -Method Post -Uri "$base/admin/employee" `
    -Headers $headers -ContentType 'application/json' -Body $createBody -TimeoutSec 10
if ($created.code -ne 1) {
    throw "Create employee failed: $($created.msg)"
}
Write-Host "PASS  create employee $username"

$encodedName = [uri]::EscapeDataString('联调员工')
$search = Invoke-RestMethod -Uri "$base/admin/employee/page?page=1&pageSize=10&name=$encodedName" `
    -Headers $headers -TimeoutSec 10
$employee = $search.data.records | Where-Object { $_.username -eq $username } | Select-Object -First 1
if (-not $employee) {
    throw 'Created employee was not found.'
}
Write-Host "PASS  query employee id=$($employee.id)"

$updateBody = @{
    id = $employee.id
    username = $username
    name = '联调员工已更新'
    phone = '13900139000'
    sex = '男'
    idNumber = '110101199001011234'
} | ConvertTo-Json
$updated = Invoke-RestMethod -Method Put -Uri "$base/admin/employee" `
    -Headers $headers -ContentType 'application/json' -Body $updateBody -TimeoutSec 10
if ($updated.code -ne 1) {
    throw "Update employee failed: $($updated.msg)"
}
Write-Host 'PASS  update employee'

$disabled = Invoke-RestMethod -Method Put -Uri "$base/admin/employee/status/0?id=$($employee.id)" `
    -Headers $headers -TimeoutSec 10
if ($disabled.code -ne 1) {
    throw "Disable employee failed: $($disabled.msg)"
}
Write-Host 'PASS  disable employee'

$asset = Join-Path $RepoRoot 'assets\1107_上传联调示例_v1.svg'
$uploadRaw = & curl.exe -sS -X POST -H "token: $token" -F "file=@$asset" "$base/admin/common/upload"
if ($LASTEXITCODE -ne 0) {
    throw 'File upload request failed.'
}
$upload = $uploadRaw | ConvertFrom-Json
if ($upload.code -ne 1) {
    throw "Upload failed: $($upload.msg)"
}
$download = Invoke-WebRequest -Uri $upload.data -UseBasicParsing -TimeoutSec 10
if ($download.StatusCode -ne 200) {
    throw 'Uploaded file is not publicly readable.'
}
Write-Host "PASS  MinIO upload $($upload.data)"

$categories = (Invoke-RestMethod -Uri "$base/user/category/list?type=1" -TimeoutSec 10).data
if (@($categories).Count -lt 3) {
    throw 'Demo dish categories were not imported.'
}
$dishCategoryId = @($categories)[0].id
$dishes = (Invoke-RestMethod -Uri "$base/user/dish/list?categoryId=$dishCategoryId" -TimeoutSec 10).data
if (@($dishes).Count -lt 1) {
    throw 'Demo dishes were not imported.'
}
$dish = @($dishes)[0]
Write-Host "PASS  public menu category=$dishCategoryId dish=$($dish.id)"

$setmealCategories = (Invoke-RestMethod -Uri "$base/user/category/list?type=2" -TimeoutSec 10).data
if (@($setmealCategories).Count -lt 1) {
    throw 'Demo setmeal categories were not imported.'
}
$setmealCategoryId = @($setmealCategories)[0].id
$setmeals = (Invoke-RestMethod -Uri "$base/user/setmeal/list?categoryId=$setmealCategoryId" -TimeoutSec 10).data
if (@($setmeals).Count -lt 1) {
    throw 'Demo setmeals were not imported.'
}
$setmeal = @($setmeals)[0]
Write-Host "PASS  public setmeal category=$setmealCategoryId setmeal=$($setmeal.id)"

$customerPhone = '13800138000'
$customerLogin = Invoke-RestMethod -Method Post -Uri "$base/user/user/login" `
    -ContentType 'application/json' `
    -Body (@{ phone = $customerPhone } | ConvertTo-Json) `
    -TimeoutSec 10
if ($customerLogin.code -ne 1) {
    throw "Customer login failed: $($customerLogin.msg)"
}
$customerHeaders = @{ token = $customerLogin.data.token }
Write-Host 'PASS  customer login'

$addresses = (Invoke-RestMethod -Uri "$base/user/addressBook/list" `
        -Headers $customerHeaders -TimeoutSec 10).data
$address = @($addresses) | Select-Object -First 1
if (-not $address) {
    $addressBody = @{
        consignee = '联调客户'
        sex = '男'
        phone = $customerPhone
        provinceName = '江苏省'
        cityName = '苏州市'
        districtName = '姑苏区'
        detail = '观前街 1 号'
        label = '公司'
        isDefault = 1
    } | ConvertTo-Json
    $addressCreated = Invoke-RestMethod -Method Post -Uri "$base/user/addressBook" `
        -Headers $customerHeaders -ContentType 'application/json' -Body $addressBody -TimeoutSec 10
    if ($addressCreated.code -ne 1) {
        throw "Create address failed: $($addressCreated.msg)"
    }
    $addresses = (Invoke-RestMethod -Uri "$base/user/addressBook/list" `
            -Headers $customerHeaders -TimeoutSec 10).data
    $address = @($addresses) | Select-Object -First 1
}
if (-not $address) {
    throw 'Customer address was not created.'
}
Write-Host "PASS  customer address id=$($address.id)"

$clean = Invoke-RestMethod -Method Delete -Uri "$base/user/shoppingCart/clean" `
    -Headers $customerHeaders -TimeoutSec 10
if ($clean.code -ne 1) {
    throw "Clean cart failed: $($clean.msg)"
}
$dishCart = Invoke-RestMethod -Method Post -Uri "$base/user/shoppingCart" `
    -Headers $customerHeaders `
    -ContentType 'application/json' `
    -Body (@{ dishId = $dish.id; dishFlavor = '微辣' } | ConvertTo-Json) `
    -TimeoutSec 10
if ($dishCart.code -ne 1) {
    throw "Add dish to cart failed: $($dishCart.msg)"
}
$setmealCart = Invoke-RestMethod -Method Post -Uri "$base/user/shoppingCart" `
    -Headers $customerHeaders `
    -ContentType 'application/json' `
    -Body (@{ setmealId = $setmeal.id } | ConvertTo-Json) `
    -TimeoutSec 10
if ($setmealCart.code -ne 1) {
    throw "Add setmeal to cart failed: $($setmealCart.msg)"
}
$cart = (Invoke-RestMethod -Uri "$base/user/shoppingCart/list" `
        -Headers $customerHeaders -TimeoutSec 10).data
if (@($cart).Count -ne 2) {
    throw "Expected 2 cart items, got $(@($cart).Count)."
}
Write-Host 'PASS  cart contains dish and setmeal'

$submitBody = @{
    addressBookId = $address.id
    payMethod = 1
    remark = 'clean acceptance'
} | ConvertTo-Json
$submit = Invoke-RestMethod -Method Post -Uri "$base/user/order/submit" `
    -Headers $customerHeaders -ContentType 'application/json' -Body $submitBody -TimeoutSec 10
if ($submit.code -ne 1) {
    throw "Submit order failed: $($submit.msg)"
}
$orderId = $submit.data.orderId
$userDetail = (Invoke-RestMethod -Uri "$base/user/order/$orderId" `
        -Headers $customerHeaders -TimeoutSec 10).data
if (@($userDetail.orderDetailList).Count -ne 2) {
    throw 'Order should contain one dish and one setmeal detail.'
}
Write-Host "PASS  submit order id=$orderId"

foreach ($action in @('confirm', 'delivery', 'complete')) {
    $response = Invoke-RestMethod -Method Put -Uri "$base/admin/order/${action}?id=$orderId" `
        -Headers $headers -TimeoutSec 10
    if ($response.code -ne 1) {
        throw "Order action $action failed: $($response.msg)"
    }
}
$adminDetail = (Invoke-RestMethod -Uri "$base/admin/order/details/$orderId" `
        -Headers $headers -TimeoutSec 10).data
if ($adminDetail.order.status -ne 4 -or -not $adminDetail.order.deliveryTime) {
    throw 'Completed order state is incorrect.'
}
Write-Host 'PASS  merchant confirm, delivery and complete'

$report = (Invoke-RestMethod -Uri "$base/admin/report/turnover" `
        -Headers $headers -TimeoutSec 10).data
$top10 = (Invoke-RestMethod -Uri "$base/admin/report/top10" `
        -Headers $headers -TimeoutSec 10).data
if (@($report).Count -lt 1 -or @($top10).Count -lt 1) {
    throw 'Business report did not return expected data.'
}
Write-Host 'PASS  turnover and sales report'

Invoke-RestMethod -Method Post -Uri "$base/user/shoppingCart" `
    -Headers $customerHeaders `
    -ContentType 'application/json' `
    -Body (@{ setmealId = $setmeal.id } | ConvertTo-Json) `
    -TimeoutSec 10 | Out-Null
$cancelSubmit = Invoke-RestMethod -Method Post -Uri "$base/user/order/submit" `
    -Headers $customerHeaders -ContentType 'application/json' -Body $submitBody -TimeoutSec 10
if ($cancelSubmit.code -ne 1) {
    throw "Submit cancellation order failed: $($cancelSubmit.msg)"
}
$cancelOrderId = $cancelSubmit.data.orderId
$cancel = Invoke-RestMethod -Method Put -Uri "$base/user/order/cancel?id=$cancelOrderId" `
    -Headers $customerHeaders -TimeoutSec 10
if ($cancel.code -ne 1) {
    throw "Cancel order failed: $($cancel.msg)"
}
$cancelledDetail = (Invoke-RestMethod -Uri "$base/user/order/$cancelOrderId" `
        -Headers $customerHeaders -TimeoutSec 10).data
if ($cancelledDetail.order.status -ne 5 -or -not $cancelledDetail.order.cancelTime) {
    throw 'Cancelled order did not return status 5 and cancelTime.'
}
Write-Host "PASS  cancel order id=$cancelOrderId"

$adminPage = Invoke-WebRequest -Uri "$AdminUrl/" -UseBasicParsing -TimeoutSec 60
$h5Page = Invoke-WebRequest -Uri "$H5Url/" -UseBasicParsing -TimeoutSec 60
if ($adminPage.StatusCode -ne 200 -or $h5Page.StatusCode -ne 200) {
    throw 'Admin or H5 page did not return 200.'
}
Write-Host 'PASS  admin and H5 pages'

$logout = Invoke-RestMethod -Method Post -Uri "$base/admin/employee/logout" `
    -Headers $headers -TimeoutSec 10
if ($logout.code -ne 1) {
    throw "Logout failed: $($logout.msg)"
}
$blacklistVerified = $false
try {
    Invoke-RestMethod -Uri "$base/admin/employee/page?page=1&pageSize=10" `
        -Headers $headers -TimeoutSec 10 | Out-Null
} catch {
    $blacklistVerified = $true
}
if (-not $blacklistVerified) {
    throw 'Blacklisted token was still accepted.'
}
Write-Host 'PASS  logout and Redis blacklist'
Write-Host 'ALL CHECKS PASSED'
