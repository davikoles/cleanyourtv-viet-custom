# Giao diện, dock và hình nền

## Tên và đồng hồ

Lần đầu mở, nhập tên chủ sở hữu hoặc chọn Để sau. Mỗi TV lưu tên riêng; APK không chứa tên/PIN của người phát triển. Giữ OK tại bánh răng → Cài đặt launcher → Tên chủ sở hữu để đổi tên.

Header dùng tiếng Việt và múi giờ Việt Nam: đồng hồ 24 giờ, ngày dương, âm lịch/can chi. Lịch âm cập nhật theo ngày khi Home được mở lại và qua nhịp kiểm tra định kỳ. Giờ hệ thống phải đúng. Cỡ chữ hệ thống quá lớn hoặc tên/địa điểm quá dài có thể khiến nội dung hiện dấu ba chấm.

## Bốn nút góc phải

Nguồn vào, khóa trẻ em, mạng và cài đặt nằm trong cùng một nền kính. Nguồn vào có thể không liệt kê gì trên box không có tuner/HDMI-in. Biểu tượng mạng có thể hiện Ethernet khi dùng dây LAN. Bấm ngắn bánh răng mở cài đặt box; giữ OK mở menu chỉnh launcher/dock/app.

## Dock

Chọn số cột trong cài đặt launcher; muốn 6 ô dock thì chọn 6 cột. Chỉnh sửa dock và sắp xếp ứng dụng trong menu giữ OK ở bánh răng. Các ứng dụng trong mẫu `examples/six-apps/config.json` phải được cài riêng và chỉ là ví dụ nhận diện package, không phải ứng dụng được phân phối kèm hoặc khuyến nghị nội dung.

Source mặc định dùng 6 cột, dock kính và thứ tự 6 package giống cấu hình được cung cấp. Gói không kèm các ứng dụng đó hoặc tài khoản/dữ liệu của chúng. Mẫu JSON không tự được đẩy vào box; không cần đẩy để dùng các mặc định mới. Theme JSON hoặc tùy chọn đang lưu có ưu tiên riêng. Nếu đẩy config.json mới, cơ chế theme upstream có thể đặt lại lựa chọn cột/hình nền trên thiết bị; sao lưu trước khi thay theme.

Nền đổi màu theo app đã bị tắt trong bản tùy biến. Ô app được thu gọn bằng hàm riêng, giữ nguyên hàm fit gốc để các kiểm tra layout vẫn có ý nghĩa. Viền focus theo app còn phụ thuộc cấu hình `app_glow`/`cinematic_home`.

## Thêm hình nền

Gói đã có sẵn hai ảnh cực quang, CR7 và Avengers. Khi mở launcher lần đầu, app đưa các ảnh đi kèm vào bộ sưu tập riêng để trình chọn hiện có đọc được. Chỉ chọn CR7 làm mặc định khi chưa có theme, ảnh hay tùy chọn cũ; không thay hình nền đã chọn của bản đang dùng. Chọn ảnh khác tại **giữ OK ở bánh răng → Cài đặt launcher → Hình nền**. File đi kèm bị thiếu sẽ được bổ sung lại khi tạo Home; file ảnh và lựa chọn của người dùng không bị ghi đè.

Các lệnh sau chỉ cần khi muốn thêm ảnh ngoài bộ đi kèm.

Chuẩn bị ảnh ngang 16:9, JPG nhẹ, khoảng 1920×1080 phù hợp TV Full HD. Ảnh lớn hơn vẫn có thể đọc nhưng tốn tài nguyên; ảnh dọc sẽ bị cắt khi phủ màn hình ngang. Không dùng tên tệp có dấu/khoảng trắng trong URI. Không chỉ đổi đuôi PNG thành JPG.

Ví dụ thêm một ảnh bằng PowerShell, không cần build lại:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$serial = Read-Host 'ADB serial hoac IP:port'
$photo = Read-Host 'Duong dan day du toi anh JPG'
& $adb -s $serial push $photo /data/local/tmp/custom-wallpaper.jpg
if ($LASTEXITCODE -ne 0) { throw 'ADB push failed' }
& $adb -s $serial shell 'content write --uri content://com.gathin.tvhome.theme/wallpapers/custom-wallpaper.jpg < /data/local/tmp/custom-wallpaper.jpg'
if ($LASTEXITCODE -ne 0) { throw 'Wallpaper write failed' }
```

Lặp lại bằng tên khác như `mountains.jpg`, `ocean.jpg` để thêm nhiều lựa chọn. Provider này chỉ cho ADB/caller có quyền hệ thống thích hợp ghi ảnh; không cần chép ảnh vào thư mục source launcher. Không dùng pipeline PowerShell để truyền byte ảnh trực tiếp.

Sau đó giữ OK tại bánh răng → Cài đặt launcher → Hình nền → chọn ảnh → Back về Home. Nếu đang mở danh sách thì thoát và vào lại. Ảnh thêm thủ công lưu trên box, không tự đi theo APK; bốn ảnh đi kèm thì nằm sẵn trong APK và source.
