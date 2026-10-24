# Kiểm tra bản đóng gói

## Đầu vào và phạm vi

Đầu vào là `launcher.zip` do người dùng cung cấp sau khi họ xác nhận build/cài và khởi động lại box thành công. Gói chia sẻ chỉ xuất mã nguồn, Gradle wrapper, tài liệu, license và script độc lập. Không dùng source từ bản vá cũ để ghi đè bản cuối của người dùng.

Các thay đổi đóng gói được liệt kê trong NOTICE.md. Bản có ảnh bổ sung 4 JPEG, lớp cài ảnh đi kèm, 4 kiểm tra sao chép và mặc định 6 cột/dock/cinematic theo cấu hình được cung cấp. Không thay cách lưu tên/PIN, mặc định khóa, package hoặc version APK. Ảnh chọn sẵn chỉ áp dụng cho lần cài mới.

## Kiểm chứng kỹ thuật

Kết quả thực hiện trong quá trình đóng gói được ghi trong `docs/VALIDATION-RESULTS.txt`. Các kiểm tra bao gồm nguồn Java với Android API 34/AndroidX, tài nguyên qua AAPT2, bộ JUnit gốc, cấu trúc Gradle và loại trừ file riêng/build. Đây không thay thế toàn bộ Android Gradle Plugin/lint/R8 hoặc kiểm tra remote trên máy thật.

Người cung cấp đã báo BUILD SUCCESSFUL với test/lint/assemble trên Windows trước khi gửi ZIP. Môi trường đóng gói không cài lại APK vào box của họ. Script PowerShell và workflow GitHub được chuẩn bị/kiểm tra nội dung, chưa được thực thi trên Windows hoặc tài khoản GitHub người dùng trong lúc đóng gói.

## Kiểm tra lại sau khi clone

Chạy `scripts/Build.ps1` hoặc Gradle `testReleaseUnitTest lintRelease assembleRelease`. Không bỏ qua test/lint khi lỗi. Giữ bản gốc và thử Home/Back/remote, hình nền, thời tiết, hẹn khóa 1 phút, PIN, âm thanh và khởi động lại trên mỗi firmware đích.

## Tệp không mang theo

Không có build/cache, local.properties, keystore/khóa ADB, APK/AAB, backup nguồn, địa chỉ box riêng, tên người dùng, video gia đình hoặc dữ liệu SharedPreferences trên box. Bốn hình nền được cung cấp rõ ràng cho bản chia sẻ được giữ nguyên trong assets; có bảng checksum để đối chiếu. Các biểu tượng/banner và font upstream vẫn được giữ.
