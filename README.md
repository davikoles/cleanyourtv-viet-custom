# CleanYourTV Việt — Custom Launcher

Giao diện Home tùy biến cho **Android TV, Google TV và Android TV Box**, gọn gàng, hỗ trợ tiếng Việt và dễ sử dụng bằng remote.

Dự án dựa trên [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv), bổ sung lịch âm, thời tiết, tên chủ sở hữu, hình nền và khóa trẻ em. Đây là bản tùy biến độc lập, không phải bản phát hành chính thức của tác giả gốc.

## Tính năng nổi bật

- **Lời chào theo tên:** mỗi TV tự nhập tên chủ sở hữu, ví dụ `XIN CHÀO, AN`.
- **Đồng hồ và lịch:** giờ 24h, ngày dương tiếng Việt, lịch âm kèm can chi; header dùng giờ Việt Nam UTC+7.
- **Thời tiết:** vị trí ước lượng theo IP mạng, tự cập nhật khoảng 15 phút khi Home đang mở.
- **Dock gọn 6 ô:** chọn app, đổi thứ tự và thay số cột bằng remote.
- **Thanh nút kính:** Nguồn vào → Khóa trẻ em → Wi-Fi/mạng → Cài đặt.
- **4 hình nền có sẵn:** hai ảnh cực quang, CR7 và Avengers; không cần chép ảnh qua ADB.
- **Nền giữ màu:** không đổi màu toàn hình nền khi chuyển app; viền focus vẫn theo cấu hình.
- **Khóa trẻ em:** hẹn giờ xem, lớp phủ đen, yêu cầu tạm dừng/tắt tiếng; PIN cố định hoặc PIN theo giờ.

Gói chỉ chứa **launcher và hình nền**, không kèm ứng dụng xem phim, tài khoản đăng nhập hoặc PIN của người phát triển.

## Chuẩn bị

| Yêu cầu | Chi tiết |
|---|---|
| Thiết bị | Android TV/Google TV/box từ Android 8.0, API 26 trở lên |
| Điều khiển | Remote có phím điều hướng, OK và Back |
| Cài qua mạng | PC và TV phải liên lạc được trong cùng mạng; PC dùng dây LAN vẫn được |
| Nếu tự build | Windows, JDK 17 và Android SDK Platform 34/Build-Tools 34.0.0/Platform-Tools |

Giữ Home gốc trong lần thử đầu. Không phải mọi firmware hoặc remote đều có hành vi giống nhau.

## 1. Lấy file APK

### Nếu đã có APK trong Releases

Vào mục **Releases → Assets** của repo, tải file có đuôi `.apk` do maintainer phát hành. File `Source code (zip)` hoặc `Code → Download ZIP` là mã nguồn, không phải APK để mở trực tiếp trên TV.

Có thể chép APK vào USB, mở bằng trình quản lý file trên TV và cho phép trình đó cài ứng dụng khi Android yêu cầu. Hoặc dùng ADB theo phần 2 bên dưới. Nếu chưa có APK trong Releases thì cần tự build; workflow hiện tại không tự xuất bản APK.

### Tự build trên Windows

1. Trên trang repo, chọn **Code → Download ZIP**, giải nén. Tìm thư mục có `README.md`, `launcher`, `scripts`, `docs` và `LICENSE`.
2. Cài [JDK 17](https://learn.microsoft.com/en-us/java/openjdk/download) và [Android Studio](https://developer.android.com/studio).
3. Trong SDK Manager, cài **Android SDK Platform 34**, **Build-Tools 34.0.0** và **Platform-Tools**. Bật Show Package Details nếu cần chọn đúng phiên bản.
4. Mở PowerShell tại thư mục gốc project và chạy:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Build.ps1
```

Lần đầu cần Internet để tải dependency. `Bypass` chỉ áp dụng cho tiến trình này, không đổi chính sách toàn máy. Script tự tìm Microsoft JDK 17 nếu JAVA_HOME chưa có; nếu dùng JDK ở nơi khác, xem [BUILD.md](docs/BUILD.md).

Chờ **BUILD SUCCESSFUL**. APK sẽ nằm tại:

```text
launcher\app\build\outputs\apk\release\app-release.apk
```

Không dùng Java 25 để build project này. Nếu build lỗi, chưa cài APK.

## 2. Cài lên TV bằng ADB trên Windows

### Bật gỡ lỗi và xem IP

Trong phần Giới thiệu trên TV, bấm **Bản dựng/Build** khoảng 7 lần để bật Tùy chọn nhà phát triển. Bật **USB debugging/Gỡ lỗi USB** hoặc gỡ lỗi qua mạng nếu firmware hỗ trợ. Tên menu có thể khác theo hãng.

Xem IP của TV trong cài đặt mạng. Nhiều box dùng cổng `5555`; firmware khác có thể yêu cầu ghép đôi hoặc dùng cổng riêng. Dùng đúng địa chỉ thiết bị hiển thị.

### Kết nối

Mở PowerShell. Nếu tự build, đứng ở thư mục gốc project. Khai báo:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$serial = Read-Host 'Nhap IP:port ADB cua TV, hoac USB serial'
```

Nếu SDK nằm ở nơi khác, sửa `$adb` thành đường dẫn thật. Với kết nối mạng, chạy:

```powershell
& $adb connect $serial
```

Chọn **Cho phép** khi TV hỏi quyền gỡ lỗi, rồi kiểm tra:

```powershell
& $adb devices
```

Thiết bị cần hiện **device**. Nếu thấy unauthorized thì xử lý yêu cầu trên TV trước. Kết nối USB không cần lệnh `connect`.

### Cài và mở launcher

Nếu tự build tại project này:

```powershell
$apk = Join-Path (Get-Location) 'launcher\app\build\outputs\apk\release\app-release.apk'
Test-Path $apk
```

Nếu tải APK từ Releases, dùng thay thế:

```powershell
$apk = Read-Host 'Nhap duong dan day du toi file APK da tai'
Test-Path $apk
```

Chỉ khi kết quả là **True**, và TV đã mở khóa/hủy hẹn giờ khóa, mới cài:

```powershell
& $adb -s $serial install -r $apk
```

Chờ **Success**, rồi mở:

```powershell
& $adb -s $serial shell am start -n com.gathin.tvhome/.MainActivity
```

Khi mở cửa sổ PowerShell mới, cần khai báo lại `$adb`, `$serial` và `$apk`. Nếu lỗi khác chữ ký, không tự gỡ app/xóa dữ liệu; đọc phần ký ứng dụng trong [BUILD.md](docs/BUILD.md).

## 3. Thiết lập giao diện

**Tên chủ sở hữu:** nhập tên khi được hỏi. Có thể đổi tại **giữ OK ở bánh răng → Cài đặt launcher → Tên chủ sở hữu**.

| Thao tác trên biểu tượng bánh răng | Kết quả |
|---|---|
| Bấm OK một lần | Mở cài đặt hệ thống TV/box |
| Giữ OK đến khi menu xuất hiện | Chọn Cài đặt launcher, Chỉnh sửa dock hoặc Sắp xếp ứng dụng |

**Hình nền:** vào **Cài đặt launcher → Hình nền → chọn ảnh → Back về Home**. Bốn ảnh nằm sẵn trong APK. Cài mới chưa có cấu hình sẽ dùng CR7; cài đè giữ lựa chọn cũ nên đôi khi cần chọn ảnh một lần. Không cần cài lại ứng dụng.

**Dock:** cài mới mặc định 6 cột. Thứ tự package tham chiếu là SmartTube → Netflix → CloudStream → Kodi → FPT Play → iMedia. Không có APK của các app này trong gói; thiếu app thì launcher có thể lấp ô bằng app đang có. Chỉnh lại trong menu dock để dùng các app bạn thích.

**Thời tiết và lịch âm:** giữ Internet và đồng hồ hệ thống đúng giờ. Vị trí theo IP có thể lệch do nhà mạng/VPN. Chọn dòng thời tiết để xem chi tiết/làm mới. Lịch âm cập nhật qua ngày khi Home đang mở hoặc được mở lại.

## 4. Dùng khóa trẻ em

Tính năng chưa tự bật khi cài APK. **Thử mốc 1 phút và mở khóa bằng remote thật trước khi dùng.**

1. Chọn icon ổ khóa, nhập PIN hiện tại. Cài mới dùng **ddMMHH** theo giờ Việt Nam: ngày + tháng + giờ 24h, đủ 6 số, không có năm.
2. Ví dụ **24/10 lúc 13h → `241013`**. Đây chỉ là ví dụ; phải dùng ngày/giờ lúc nhập.
3. Trong menu khóa, chọn bật dịch vụ ở Trợ năng, bật **Khóa trẻ em — CleanYourTV**, quay lại menu và nhập PIN lại.
4. Chọn thử 1 phút hoặc hẹn 15/30/45/60/90/120 phút. Hết giờ, app phủ màn đen và yêu cầu tạm dừng/tắt tiếng.
5. Bấm và thả nhanh **OK 5 lần hoặc Mute 5 lần** để hiện bàn phím, rồi nhập PIN. Không trộn hai nút hoặc chỉ giữ phím.

Có thể chọn **PIN cố định 6 số** trong menu. Đổi PIN/chế độ cần xác thực bằng PIN hiện tại; PIN mới phải nhập hai lần. Khi dùng PIN cố định, mã theo giờ không mở được. Sai 5 lần phải đợi 60 giây.

Mã theo giờ đoán được nếu biết quy luật. Lớp khóa không chặn mọi nút IR/CEC/menu hệ thống và không bảo đảm mute được HDMI/passthrough. Xem [giới hạn và ADB cứu hộ](docs/CHILD-LOCK.md). Giữ ADB trong lần thử đầu.

## 5. Đặt làm Home mặc định

Sau khi thử di chuyển, mở app và Back về Home ổn định, chọn launcher trong phần ứng dụng Home mặc định của TV nếu có. Có thể thử qua ADB:

```powershell
& $adb -s $serial shell cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity
& $adb -s $serial shell cmd package resolve-activity --brief --user 0 -a android.intent.action.MAIN -c android.intent.category.HOME
```

Kết quả cần có `com.gathin.tvhome/.MainActivity`. Bấm **Home trên remote thật**, rồi khởi động lại kiểm tra. Nếu vẫn về Home gốc, firmware chưa chấp nhận thay đổi; không tự tắt launcher/setup wizard theo hướng dẫn dành cho một dòng TV khác.

Sau khi kiểm tra mọi thứ, tắt gỡ lỗi/thu hồi ủy quyền ADB nếu không còn dùng.

## Lỗi thường gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Biểu thức sau `&` không hợp lệ | Khai báo lại biến `$adb` trong cửa sổ PowerShell hiện tại |
| `failed to stat` hoặc không thấy APK | Kiểm tra thư mục đang đứng và `Test-Path`; tự build phải có BUILD SUCCESSFUL trước |
| `Unsupported class file major version` | Chọn JDK 17 cho JAVA_HOME rồi build lại |
| `unauthorized` hoặc không kết nối | Kiểm tra ủy quyền trên TV, đúng IP/cổng và liên lạc mạng |
| Cài đè bị lỗi chữ ký | Dùng keystore/bản phát hành tương thích; không tự xóa dữ liệu |
| PIN theo giờ không đúng | Kiểm tra ddMMHH, giờ Việt Nam 24h, chế độ PIN và thời gian khóa nhập sai |
| Không tự hiện hình nền khi cài đè | Chọn ảnh trong Cài đặt launcher; dữ liệu cũ được giữ theo thiết kế |

## Ghi nguồn và giấy phép

Giữ ghi nhận [Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv). Mã gốc dùng MIT; font Outfit dùng SIL OFL 1.1. Lịch âm chuyển thể từ Hồ Ngọc Đức có điều kiện cá nhân/phi thương mại, nên không ghi toàn bộ bản ghép là MIT hoặc tự do thương mại. Bốn hình nền có quyền riêng, không được cấp lại MIT theo giấy phép mã nguồn. Xem [LICENSE](LICENSE), [NOTICE.md](NOTICE.md) và [thông báo hình nền](docs/WALLPAPERS.md).

Package hiện là `com.gathin.tvhome`; release build dùng debug signing như project gốc, chưa có hệ thống ký/phát hành APK ổn định cho cộng đồng. APK từ các máy build khác nhau có thể không cập nhật lẫn nhau. Maintainer cần chuẩn bị release signing trước khi phát hành APK lâu dài; hướng dẫn tại [BUILD.md](docs/BUILD.md).

## Tài liệu thêm

- [Build và ký ứng dụng](docs/BUILD.md)
- [Tùy chỉnh giao diện](docs/USAGE.md)
- [Khóa trẻ em và cứu hộ](docs/CHILD-LOCK.md)
- [Dữ liệu và quyền](docs/PRIVACY.md)
- [Phạm vi kiểm chứng](docs/VALIDATION.md): bản có ảnh đã qua 204 kiểm tra JVM và biên dịch Java/tài nguyên; vẫn cần thử trên firmware đích.
- [Tải mã nguồn ZIP từ GitHub](https://docs.github.com/en/repositories/working-with-files/using-files/downloading-source-code-archives)
- [ADB chính thức](https://developer.android.com/tools/adb)
