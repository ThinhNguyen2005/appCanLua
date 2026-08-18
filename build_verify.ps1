Write-Host "=== STEP 1: Assemble Lite Debug ==="
.\gradlew.bat :app:assembleLiteDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "assembleLiteDebug FAILED with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "=== assembleLiteDebug SUCCESSFUL ==="

Write-Host "=== STEP 2: Assemble Full Debug ==="
.\gradlew.bat :app:assembleFullDebug
if ($LASTEXITCODE -ne 0) {
    Write-Error "assembleFullDebug FAILED with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "=== assembleFullDebug SUCCESSFUL ==="

Write-Host "=== STEP 3: Test Lite Debug Unit Test ==="
.\gradlew.bat :app:testLiteDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "testLiteDebugUnitTest FAILED with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "=== testLiteDebugUnitTest SUCCESSFUL ==="

Write-Host "=== STEP 4: Test Full Debug Unit Test ==="
.\gradlew.bat :app:testFullDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "testFullDebugUnitTest FAILED with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "=== testFullDebugUnitTest SUCCESSFUL ==="
