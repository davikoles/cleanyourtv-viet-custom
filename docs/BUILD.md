# Build và cài đặt

## Công cụ

- JDK 17; không trỏ JAVA_HOME vào JBR/Java 25. Project dùng AGP 8.5.2 và Gradle wrapper 8.9 có checksum distribution.
- Android SDK Platform 34, Build-Tools 34.0.0, Platform-Tools.
- Windows: Android Studio có SDK Manager để cài các thành phần trên. Mac/Linux: cấu hình SDK tương đương.

Không commit `local.properties`, thư mục build, keystore hay khóa ADB. Repo đã có `.gitignore`.

## Windows

Tại gốc repo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Build.ps1
```

Có thể chỉ định đường dẫn JDK/SDK bằng `-JavaHome` và `-SdkRoot`. Script tự tìm Microsoft JDK 17 nếu JAVA_HOME chưa có; SDK mặc định ở `$env:LOCALAPPDATA\Android\Sdk`.

Nếu không dùng script:

```powershell
$env:JAVA_HOME = Read-Host 'Duong dan thu muc JDK 17'
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
Set-Location .\launcher
.\gradlew.bat testReleaseUnitTest lintRelease assembleRelease --no-daemon --console=plain
```

SDK nằm chỗ khác thì thay ANDROID_HOME bằng đường dẫn thật. Không cần commit đường dẫn máy vào source.

## macOS/Linux

Thiết lập JAVA_HOME trỏ JDK 17 và ANDROID_HOME trỏ Android SDK, sau đó:

```bash
cd launcher
sh ./gradlew testReleaseUnitTest lintRelease assembleRelease --no-daemon --console=plain
```

## Cài APK

Chỉ tiếp tục sau khi cả test, lint và assemble đều thành công. Bật tùy chọn nhà phát triển/gỡ lỗi trên TV, kết nối cùng mạng hoặc USB và chấp nhận yêu cầu ủy quyền. ADB qua mạng có thể dùng cổng khác hoặc yêu cầu pairing tùy firmware; dùng đúng địa chỉ TV hiển thị.

Tại gốc repo:

```powershell
$serial = Read-Host 'ADB serial hoac IP:port cua thiet bi'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Install.ps1 -Serial $serial
```

Script chỉ cài `-r` và mở Home mới. Khi đang khóa trẻ em, mở khóa và hủy hẹn giờ trước khi cập nhật.

## Ký ứng dụng

Release build hiện ký bằng debug keystore của máy build. Hai máy hoặc hai runner CI có thể tạo chữ ký khác nhau dù source giống nhau. APK của repo này có cùng package với launcher upstream; không thể cài song song mặc định.

Nếu đang dùng bản cũ trên box, build bằng đúng keystore đã dùng cho bản đó. Lỗi `INSTALL_FAILED_UPDATE_INCOMPATIBLE` không được giải quyết bằng tự xóa app/dữ liệu. Gói nguồn không chứa keystore của người cung cấp và không có APK đã ký bằng khóa của họ.

Để phát hành APK công khai lâu dài, maintainer cần chọn một release keystore riêng, giữ khóa ngoài repo, cấu hình signing và tăng versionCode cho các bản sau. Gói hiện tại chưa cấu hình release signing hoặc tự phát hành APK.

## Đặt làm Home

Trước tiên mở app từ Home gốc, thử di chuyển remote, mở app rồi Back về Home. Giữ bản gốc trong lần thử.

```powershell
$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$serial = Read-Host 'ADB serial hoac IP:port'
& $adb -s $serial shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
& $adb -s $serial shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

Phải thấy `com.gathin.tvhome/.MainActivity`, rồi bấm Home trên remote thật và khởi động lại kiểm tra. Nếu firmware vẫn trả Home gốc, dừng để tìm cách phù hợp thiết bị; hướng dẫn chung này không tự vô hiệu hóa launcher/setup wizard của hãng.

Nếu dùng helper remap phím, kiểm tra xung đột với dịch vụ khóa trẻ em trước khi hẹn giờ. Không thay cả Home và helper trong cùng một lần thử không giám sát.

## CI

Workflow GitHub Actions kèm theo chạy test, lint, assemble bằng JDK 17/SDK 34. Workflow chỉ kiểm tra build, không truy cập box, không ký bằng khóa cá nhân, không tạo GitHub Release hay tự đăng APK. Cần kiểm tra lần chạy thực tế sau khi push; workflow chưa được chạy trên tài khoản GitHub của người dùng trong lúc đóng gói.
