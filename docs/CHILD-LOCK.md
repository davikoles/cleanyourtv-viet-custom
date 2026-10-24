# Khóa trẻ em

Tính năng chỉ hoạt động sau khi người dùng tự bật dịch vụ **Khóa trẻ em — CleanYourTV** trong Trợ năng và chọn hẹn giờ/khóa ngay. Cài APK chưa tự bật khóa.

## Lần đầu

1. Mở biểu tượng ổ khóa, nhập PIN đang dùng. Cài mới mặc định dùng `ddMMHH` theo giờ Việt Nam UTC+7 (24 giờ); ví dụ ngày 23 tháng 10 lúc 18h là `231018`.
2. Vào menu bật dịch vụ Trợ năng; quay lại menu khóa, nhập PIN lại. Nếu báo xung đột dịch vụ chặn phím, xử lý trước khi bật khóa; không tự tắt helper đang cần để điều khiển Home.
3. Chọn thử 1 phút, mở video có tiếng. Hết giờ kiểm tra màn đen và tiếng trên thiết bị thật.
4. Bấm và thả nhanh OK **hoặc** Mute 5 lần trong khoảng 3,5 giây; không trộn nút, không giữ phím. Nhập PIN để mở; kiểm tra âm thanh sau mở khóa.

Không khóa trong lúc cài/cập nhật APK. Giữ ADB và Home gốc khi thử lần đầu.

## Hai chế độ PIN

- Theo giờ: `ddMMHH`, đổi ở đầu giờ theo UTC+7. Đây là mã theo quy luật đoán được, không phải OTP bí mật.
- Cố định: tự đặt 6 số, nhập lại để xác nhận. Đổi chế độ/PIN phải xác thực bằng PIN hiện tại. Chỉ chế độ đang chọn mở được; mã theo giờ không là mã dự phòng khi chọn PIN cố định.
- Sai 5 lần phải đợi 60 giây. Menu cha mẹ cũng cần PIN và xác thực lại khi rời menu/để yên.
- PIN cố định lưu dưới dạng verifier PBKDF2 có salt, không lưu nguyên 6 số. Ghi nhớ PIN riêng trước khi dùng.

Hẹn giờ có các mốc 15/30/45/60/90/120 phút, thử 1 phút và khóa ngay. Dịch vụ lưu trạng thái để kiểm tra lại khi Android nối lại dịch vụ sau khởi động; không bảo đảm Android luôn giữ dịch vụ sau force-stop/cập nhật/thu hồi quyền.

## ADB cứu hộ

Thử lệnh trước lần khóa đầu, trên chính thiết bị bạn quản lý:

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
$serial = Read-Host 'ADB serial hoac IP:port'
& $adb -s $serial shell am broadcast --include-stopped-packages -a com.gathin.tvhome.action.RECOVER_CHILD_LOCK -n com.gathin.tvhome/.ChildLockRecoveryReceiver
```

Kết quả mong đợi: `Child lock cleared; box audio restore requested.` Lệnh hủy khóa/hẹn giờ và yêu cầu khôi phục mute trước đó; giữ nguyên PIN/chế độ, **không đặt lại PIN đã quên**. Caller được bảo vệ bằng WRITE_SECURE_SETTINGS; app thường không được gọi. Đừng để ADB mở cho người không được phép dùng.

## Giới hạn thực tế

Đây là lớp khóa trên box, không phải Device Owner, kiosk hoặc kiểm soát phần cứng TV. Nó chặn các phím Android thực sự gửi đến dịch vụ. IR/CEC/nút nguồn, menu hệ thống hoặc việc tắt dịch vụ có thể vượt lớp khóa. Mute của box không bảo đảm tắt tiếng HDMI passthrough/loa TV/AVR; nền đen không tắt đèn nền LCD. Phải thử bằng remote thật trên từng thiết bị trước khi dùng.
