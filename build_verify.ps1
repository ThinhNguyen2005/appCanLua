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

Write-Host "=== STEP 3: Test Debug Unit Test ==="
.\gradlew.bat :app:testDebugUnitTest
if ($LASTEXITCODE -ne 0) {
    Write-Error "testDebugUnitTest FAILED with code $LASTEXITCODE"
    exit $LASTEXITCODE
}
Write-Host "=== testDebugUnitTest SUCCESSFUL ==="
