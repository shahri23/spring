@echo off
REM 🧪 Automated Test Script for SPI-PubSub Application (Windows Version)
REM This script runs the most important tests automatically

echo 🚀 Starting SPI-PubSub Application Tests...
echo ==========================================

set BASE_URL=http://localhost:8080
set PASS_COUNT=0
set FAIL_COUNT=0

echo Testing: Health Check...
curl -s %BASE_URL%/api/management/health > temp_response.txt
findstr /C:"\"status\":\"UP\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Health Check: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Health Check: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Configuration Status...
curl -s %BASE_URL%/api/management/config/status > temp_response.txt
findstr /C:"\"routing\":" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Configuration Status: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Configuration Status: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Enable PubSub...
curl -s -X POST "%BASE_URL%/api/management/config/pubsub/toggle?enabled=true" > temp_response.txt
findstr /C:"\"status\":\"success\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Enable PubSub: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Enable PubSub: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: PubSub Status...
curl -s %BASE_URL%/api/management/pubsub/status > temp_response.txt
findstr /C:"\"status\":\"active\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ PubSub Status: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ PubSub Status: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Router Status...
curl -s %BASE_URL%/api/router/status > temp_response.txt
findstr /C:"\"status\":\"UP\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Router Status: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Router Status: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Customer XML Transformation...
curl -s -X POST %BASE_URL%/api/transform -H "Content-Type: application/xml" -d "<customer><id>123</id><name>Test User</name></customer>" > temp_response.txt
findstr /C:"\"id\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Customer XML Transformation: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Customer XML Transformation: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Order XML Transformation...
curl -s -X POST %BASE_URL%/api/transform -H "Content-Type: application/xml" -d "<order><orderId>ORD-001</orderId><status>confirmed</status></order>" > temp_response.txt
findstr /C:"\"orderId\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Order XML Transformation: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Order XML Transformation: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Content Routing...
curl -s -X POST %BASE_URL%/api/router/route -H "Content-Type: application/json" -d "{\"type\":\"customer\",\"data\":\"test\"}" > temp_response.txt
findstr /C:"\"status\"" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Content Routing: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Content Routing: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: Configuration Export...
curl -s %BASE_URL%/api/management/config/export > temp_response.txt
findstr /C:"\"channels\":" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ Configuration Export: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ Configuration Export: FAIL
    set /a FAIL_COUNT+=1
)

echo Testing: 404 Error Handling...
curl -s %BASE_URL%/api/invalid-endpoint > temp_response.txt
findstr /C:"\"status\":404" temp_response.txt >nul
if %errorlevel%==0 (
    echo ✅ 404 Error Handling: PASS
    set /a PASS_COUNT+=1
) else (
    echo ❌ 404 Error Handling: FAIL
    set /a FAIL_COUNT+=1
)

REM Cleanup
del temp_response.txt

echo.
echo ==========================================
echo 🎯 Test Results Summary:
echo ✅ Passed: %PASS_COUNT% tests
echo ❌ Failed: %FAIL_COUNT% tests

if %FAIL_COUNT%==0 (
    echo 🎉 All tests passed! Your application is working perfectly!
) else (
    echo ⚠️  Some tests failed. Check the application logs and retry.
)

pause
