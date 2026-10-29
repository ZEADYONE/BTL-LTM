# KẾ HOẠCH THIẾT KẾ VÀ XÂY DỰNG WEBSITE GIỚI THIỆU BOMBERMAN ONLINE

> **Trạng thái:** Đã triển khai và smoke test local (2026-10-02). Còn chờ: chốt DDNS/hostname thật, thông tin nhóm, kiểm thử trên máy Windows không có JDK và qua mạng ngoài LAN.  
> **Mục tiêu:** Tạo website tĩnh, responsive, dùng đúng ngôn ngữ thiết kế của JavaFX client; cung cấp bản tải Windows và hướng dẫn người chơi kết nối tới game server. Website chạy cùng MySQL và TCP game server bằng Docker Compose trên máy chủ tại nhà hoặc VPS.

---

## 0. Căn cứ và quyết định chính

### 0.1 Nguồn giao diện hiện có

- Design token và component JavaFX: `client-fx/src/main/resources/css/game-theme.css`.
- SVG nhân vật, vật phẩm, icon và background: `client-fx/src/main/resources/assets/svg/`.
- Font Lilita One và Nunito: `client-fx/src/main/resources/fonts/`.
- Icon ứng dụng: `client-fx/src/main/resources/assets/app_icon.png`.
- Gói tải hiện tại `BombermanClient.zip` chứa client libGDX cũ, **không dùng làm bản tải chính** của website.
- Client JavaFX mới được đóng gói từ task `:client-fx:packageApp`; phải ZIP toàn bộ thư mục `client-fx/build/dist/BombermanOnline/`.

### 0.2 Quyết định kiến trúc

| Mã | Quyết định | Lý do |
|---|---|---|
| Q1 | Website là HTML/CSS/JavaScript thuần, không dùng framework frontend. | Trang đơn giản, tải nhanh, không cần Node.js hoặc pipeline phức tạp trên server. |
| Q2 | Dùng các SVG, font và màu hiện có làm nguồn nhận diện duy nhất. | Website và desktop client giữ cùng phong cách hình ảnh. |
| Q3 | Chuyển JavaFX CSS sang CSS trình duyệt, không sao chép nguyên các thuộc tính `-fx-*`. | JavaFX CSS không tương thích trực tiếp với trình duyệt. |
| Q4 | Website được phục vụ bằng một container web riêng trong `docker-compose.yml`. | Có thể khởi động, dừng và cập nhật cùng game server. |
| Q5 | Game tiếp tục dùng TCP `8081`; website dùng HTTP/HTTPS `80/443`. | Website không làm proxy cho giao thức TCP game. |
| Q6 | Bản tải chính là JavaFX Windows app image có runtime đi kèm. | Người chơi không phải tự cài JDK; giao diện đúng phiên bản hiện tại. |
| Q7 | Không hiển thị trạng thái server thời gian thực trong MVP. | Trình duyệt không thể kiểm tra raw TCP trực tiếp; tính năng này cần backend phụ. |
| Q8 | Không tự động mở app/chọn server bằng custom URI trong MVP. | Cần đăng ký protocol trên Windows và thay đổi bộ cài; hướng dẫn + nút sao chép đủ cho bản đầu. |

### 0.3 Điều chỉnh khi triển khai

| Điểm trong kế hoạch | Thực tế trong mã | Cách xử lý |
|---|---|---|
| Bước “nhập `hostname:port`” | Popup **SERVER ADDRESS** có hai ô riêng HOST và PORT, mở bằng nút bánh răng ở góc dưới trái màn hình đăng nhập. | Website có nút sao chép riêng HOST, PORT và cả `host:port` (để gửi bạn bè); hướng dẫn ghi đúng tên nút. |
| Tiêu đề dùng Lilita One | Lilita One không có glyph tiếng Việt (chỉ có â, ê, ô). | Lilita One chỉ dùng cho logo và nhãn ASCII; tiêu đề/nút tiếng Việt dùng Nunito Black. |
| Sửa root `.dockerignore` | Root ignore loại `client-fx/src` và `*.zip`, cần giữ cho image server. | Dùng `website/Dockerfile.dockerignore` (allowlist, context ~1 MB); root ignore thêm `downloads`, `website`. |
| ZIP nằm trong image | ZIP ~65 MB, đổi mỗi lần build, vượt mức GitHub cảnh báo. | `downloads/` mount chỉ đọc vào container và nằm trong `.gitignore`; cập nhật ZIP không cần build lại image. |
| `BOMBERMAN_DOWNLOAD_VERSION` trong `.env` | Dễ lệch với file thật. | `packageZip` sinh `release.json` (version, size, SHA-256); website đọc file này. |
| Cache immutable theo version | Build lại cùng version cho checksum khác. | `/downloads/` dùng `no-cache` (revalidate bằng ETag). |
| `website` `depends_on` server healthy | Server lỗi sẽ kéo website xuống theo. | Website không phụ thuộc server. |
| Nút copy dùng Clipboard API | Clipboard API chỉ có trên HTTPS/localhost, website chạy HTTP qua DDNS. | Có fallback `execCommand('copy')`, đã thử cả hai trường hợp. |
| Cảnh báo HTTP (rủi ro) | Chrome/Edge cảnh báo “insecure download” khi tải ZIP qua HTTP (đã thấy khi test). | Hiển thị checksum SHA-256; HTTPS vẫn là bước tiếp theo. (Phần hỏi đáp đã được bỏ theo yêu cầu.) |
| `packageApp` thiếu input | Task không khai báo jar client làm input nên có thể bị UP-TO-DATE sau khi sửa code. | Thêm `inputs.dir(jpackageInput)`. |

---

## 1. Phạm vi sản phẩm

### 1.1 Nội dung bắt buộc của landing page

1. Hero giới thiệu **Bomberman Online Mini**.
2. Nút **Tải bản Windows** và thông tin phiên bản/dung lượng.
3. Địa chỉ máy chủ dạng `hostname:port` kèm nút **Sao chép**.
4. Hướng dẫn cài đặt theo từng bước:
   - tải file ZIP;
   - giải nén toàn bộ thư mục;
   - chạy `BombermanOnline.exe`;
   - mở **Server Address**;
   - nhập địa chỉ server;
   - đăng ký/đăng nhập và tham gia phòng.
5. Giới thiệu gameplay: tạo phòng, sẵn sàng, đặt bom, bảng xếp hạng và lịch sử trận.
6. Yêu cầu hệ thống và lưu ý Windows SmartScreen.
7. Thông tin checksum SHA-256 của file tải.
8. Footer ghi tên đồ án, công nghệ và liên kết mã nguồn nếu được phép công khai.

### 1.2 Ngoài phạm vi MVP

- Tài khoản web hoặc đăng nhập trên website.
- Bảng xếp hạng đọc trực tiếp từ MySQL.
- Trang quản trị server.
- Cập nhật client tự động.
- Thanh toán, quảng cáo hoặc thu thập dữ liệu người dùng.
- Web client chơi trực tiếp trong trình duyệt.

---

## 2. Cấu trúc trang dự kiến

```text
Header
├── Logo / tên game
├── Giới thiệu
├── Cách chơi
├── Hướng dẫn cài đặt
└── Tải game

Hero
├── Tên Bomberman Online Mini
├── Mô tả ngắn
├── CTA "Tải bản Windows"
├── CTA "Xem hướng dẫn"
└── Minh họa bomber + bomb + flame

Server card
├── Trạng thái cấu hình: "Server chính"
├── hostname:port
├── Nút sao chép
└── Ghi chú kết nối từ Internet

Feature cards
├── Chơi 2–4 người
├── Nhiều phòng đồng thời
├── Authoritative server
└── Ranking & history

Install guide
├── Bước 1: Tải ZIP
├── Bước 2: Giải nén
├── Bước 3: Chạy EXE
├── Bước 4: Chọn Server Address
└── Bước 5: Nhập hostname:port

Download card
├── Phiên bản
├── Hệ điều hành
├── Dung lượng
├── SHA-256
└── Nút tải

FAQ / troubleshooting
└── SmartScreen, firewall, timeout, sai địa chỉ server

Footer
```

---

## 3. Hệ thiết kế web

### 3.1 Design token chuyển từ JavaFX

| Token web | Giá trị gốc | Mục đích |
|---|---:|---|
| `--color-outline` | `#2A1F3D` | Viền đậm, chữ chính |
| `--color-bg-outer` | `#2B2140` | Nền ngoài |
| `--color-frame-center` | `#FBB36B` | Trung tâm nền cam |
| `--color-frame-edge` | `#E8762C` | Rìa nền cam |
| `--color-purple` | `#6C45C9` | Panel chính |
| `--color-purple-dark` | `#563299` | Header/đổ bóng panel |
| `--color-panel-dark` | `#3A2D57` | Card tối |
| `--color-cream` | `#FFF1E6` | Card sáng |
| `--color-gold` | `#FFC93C` | CTA và điểm nhấn |
| `--color-green` | `#38A83A` | Trạng thái tốt |
| `--color-red` | `#E23C3C` | Cảnh báo/lỗi |

### 3.2 Typography

- Tiêu đề/logo: **Lilita One**.
- Nội dung và nút: **Nunito ExtraBold/Bold**.
- Khai báo bằng `@font-face`, phục vụ font từ chính website, không phụ thuộc Google Fonts.
- Có fallback `Arial, sans-serif` nếu font tải lỗi.

### 3.3 Component chính

- Nút nổi 3 lớp: outline tối, cạnh màu và gradient mặt nút.
- Panel bo góc có viền `--color-outline` và shadow cứng giống JavaFX.
- Server address dùng card tím, icon gear/player và nút copy màu vàng.
- Step card có số thứ tự, icon SVG và đường nối trên desktop.
- Background dùng `pattern_bomb.svg` với opacity thấp.
- Chỉ dùng animation nhẹ: hover nút, floating bomber và spark; tôn trọng `prefers-reduced-motion`.

### 3.4 Responsive và accessibility

- Breakpoint mục tiêu: `360px`, `768px`, `1024px`, `1440px`.
- Không xuất hiện horizontal scroll ở màn hình 360px.
- Nút và link có vùng bấm tối thiểu 44px.
- Có focus ring rõ ràng cho bàn phím.
- SVG trang trí dùng `aria-hidden="true"`; ảnh có ý nghĩa phải có `alt`.
- Màu chữ/nền đạt tương phản dễ đọc; không chỉ dùng màu để truyền đạt trạng thái.

---

## 4. Cấu trúc mã nguồn dự kiến

```text
website/
├── Dockerfile
├── nginx.conf
├── index.html
├── css/
│   └── site.css
├── js/
│   └── site.js
└── assets/
    └── screenshots/                 ảnh chụp app thật, nếu sử dụng

downloads/
├── BombermanOnline-Windows.zip
└── BombermanOnline-Windows.zip.sha256
```

SVG và font giữ tại `client-fx/src/main/resources/` làm nguồn chuẩn. Website image sẽ
copy các thư mục cần thiết từ root build context; không sửa trực tiếp asset gốc. Cần
điều chỉnh `.dockerignore` để website Dockerfile đọc được `assets/svg` và `fonts`.

---

## 5. Kế hoạch đóng gói bản tải

### 5.1 Luồng build

```text
:client-fx:packageApp
        ↓
client-fx/build/dist/BombermanOnline/
        ↓ ZIP toàn bộ thư mục
downloads/BombermanOnline-Windows.zip
        ↓ SHA-256
downloads/BombermanOnline-Windows.zip.sha256
        ↓
Website download card
```

### 5.2 Yêu cầu đối với file ZIP

- Khi giải nén phải có `BombermanOnline.exe`, thư mục `app/` và `runtime/`.
- Không phát hành riêng file `.exe`.
- Tên ZIP có thể thêm version, ví dụ `BombermanOnline-0.1.0-Windows.zip`.
- Website phải hiển thị đúng version, dung lượng và checksum của artifact đang phục vụ.
- Không tiếp tục dùng `BombermanClient.zip` cũ cho nút tải chính.

### 5.3 Cấu hình server trong client

MVP giữ luồng nhập server tại màn hình đăng nhập. Website hiển thị và cho sao chép
`BOMBERMAN_PUBLIC_HOST:BOMBERMAN_TCP_PORT`. Sau MVP có thể bổ sung bước build để đặt
DDNS mặc định trong `client.properties`, nhưng người chơi vẫn phải có quyền sửa địa chỉ.

---

## 6. Tích hợp Docker Compose

### 6.1 Service dự kiến

```yaml
website:
  build:
    context: .
    dockerfile: website/Dockerfile
  restart: unless-stopped
  depends_on:
    server:
      condition: service_healthy
  ports:
    - "${BOMBERMAN_WEB_PORT:-80}:80"
```

Biến bổ sung vào `env.template`:

```dotenv
BOMBERMAN_WEB_PORT=80
BOMBERMAN_PUBLIC_HOST=game.example.com
BOMBERMAN_DOWNLOAD_VERSION=0.1.0
```

Nếu nội dung hostname/version phải thay đổi theo `.env`, chọn một trong hai cách:

1. Sinh `config.js` khi container khởi động bằng template của Nginx; hoặc
2. Dùng placeholder trong `index.html` và `envsubst` ở entrypoint.

Ưu tiên cách 1 để HTML có thể cache lâu còn `config.js` luôn cập nhật theo môi trường.

### 6.2 Port và router

| Dịch vụ | Cổng host | Có forward router? |
|---|---:|---|
| Website HTTP | `80` | Có |
| Website HTTPS | `443` | Có khi cấu hình TLS |
| Bomberman TCP | `8081` | Có |
| MySQL | `127.0.0.1:3306` | Không |

### 6.3 DNS gợi ý

- `www.example.com` hoặc `bomberman.example.com`: website.
- `game.example.com`: game server TCP.
- Hai hostname có thể cùng trỏ về một public IP/DDNS.
- Nếu dùng Cloudflare DNS, website có thể bật proxy; hostname game raw TCP phải để
  **DNS only** nếu không dùng Spectrum/Tunnel chuyên dụng.

---

## 7. Các giai đoạn thực hiện

### Giai đoạn 1 — Chuẩn bị nội dung và artifact

- [ ] Chốt hostname và port public của game server.
- [ ] Chốt tên nhóm, thông tin footer và liên kết repository.
- [x] Build JavaFX app image mới.
- [x] ZIP đầy đủ app image và sinh SHA-256 (`:client-fx:packageZip`).
- [ ] Kiểm tra ZIP trên một máy Windows không có JDK.
- [x] Chụp 3–5 ảnh app thật: đăng nhập, sảnh, phòng chờ, gameplay, ranking (lấy từ `docs/bao-cao/screenshots`, chuyển WebP).

### Giai đoạn 2 — Dựng design system web

- [x] Khai báo CSS variables từ bảng màu JavaFX.
- [x] Khai báo bốn font local bằng `@font-face`.
- [x] Chuyển nút, panel, badge và card sang CSS web.
- [x] Dùng `pattern_bomb.svg` làm background.
- [x] Tạo hero composition từ `bomber_full.svg`, `bomb.svg`, `spark.svg`.
- [x] Xác nhận giao diện desktop và mobile (ảnh chụp headless 360/768/1024/1440px).

### Giai đoạn 3 — Xây dựng landing page

- [x] Tạo semantic HTML cho header, main sections và footer.
- [x] Thêm navigation cuộn tới section.
- [x] Thêm nút download với thuộc tính `download`.
- [x] Thêm nút copy server address và phản hồi “Đã sao chép”.
- [x] Thêm install guide và troubleshooting.
- [x] Thêm version, size, checksum từ cấu hình build/deploy (`downloads/release.json`).
- [x] Thêm Open Graph metadata, favicon và title/description.

### Giai đoạn 4 — Container và Compose

- [x] Tạo `website/Dockerfile` dựa trên Nginx Alpine (`nginx:1.30-alpine`).
- [x] Tạo `website/nginx/default.conf.template`, MIME type đúng cho ZIP/SVG/TTF và cache header phù hợp.
- [x] Thêm service `website` vào Compose.
- [x] Thêm biến web vào `env.template`.
- [x] Điều chỉnh ignore file để website build đọc SVG/font nhưng không gửi file thừa (`website/Dockerfile.dockerignore`).
- [x] Cập nhật README với lệnh build, URL website và cấu hình router.

### Giai đoạn 5 — Kiểm thử và phát hành

- [x] Chạy `docker compose config`.
- [x] Build toàn bộ image bằng `docker compose build`.
- [x] Xác nhận MySQL, server và website cùng healthy.
- [x] Kiểm tra HTTP status của `/`, asset SVG/font và file ZIP.
- [x] Tải ZIP qua website, giải nén và chạy app (máy dev; cửa sổ client mở bình thường).
- [ ] Kết nối JavaFX client tới DDNS thật qua mạng ngoài LAN.
- [x] Kiểm tra giao diện ở 360/768/1440px.
- [x] Kiểm tra keyboard navigation, focus và reduced motion.
- [x] Kiểm tra checksum file tải.
- [x] Tắt/xóa container smoke test, không xóa volume dữ liệu thật.

---

## 8. Tiêu chí nghiệm thu

Website được xem là hoàn thành khi thỏa tất cả điều kiện:

1. Trang tải được bằng domain/DDNS từ mạng ngoài.
2. Giao diện nhận biết rõ là cùng sản phẩm với JavaFX client.
3. Không có JavaFX CSS `-fx-*` trong CSS web.
4. Không có asset lỗi, font lỗi hoặc horizontal overflow trên mobile.
5. Nút copy trả về đúng `hostname:port` đang cấu hình.
6. Nút download trả về HTTP 200 và đúng file JavaFX ZIP mới.
7. ZIP giải nén có `BombermanOnline.exe`, `app/` và `runtime/`.
8. Client từ ZIP kết nối được tới server bằng địa chỉ hướng dẫn trên website.
9. MySQL không được public ra LAN/Internet.
10. `docker compose up -d --build` khởi động thành công cả ba service.
11. Các service có restart policy và healthcheck phù hợp.
12. README đủ để một thành viên khác tự deploy lại từ đầu.

---

## 9. Rủi ro và biện pháp

| Rủi ro | Ảnh hưởng | Biện pháp |
|---|---|---|
| Phát hành nhầm ZIP libGDX cũ | Người dùng thấy giao diện/sản phẩm không đúng | Pipeline chỉ lấy output từ `:client-fx:packageApp`; kiểm tra cấu trúc ZIP trước deploy. |
| Sao chép nguyên JavaFX CSS | Website không hiển thị đúng | Chuyển token/component sang CSS chuẩn, kiểm tra bằng trình duyệt. |
| DDNS đổi IP chậm | Website/game tạm thời không truy cập được | Chạy DDNS updater trên router/server; giảm TTL nếu nhà cung cấp cho phép. |
| ISP dùng CGNAT | Port forwarding không hoạt động | Kiểm tra WAN IP trước; xin public IPv4 hoặc dùng tunnel/VPS. |
| Website HTTP bị cảnh báo | Giảm độ tin cậy khi tải EXE/ZIP | Bổ sung HTTPS bằng Caddy/reverse proxy sau khi domain hoạt động. |
| Windows SmartScreen cảnh báo | Người dùng ngại chạy app chưa ký số | Viết hướng dẫn rõ, hiển thị checksum; cân nhắc code signing về sau. |
| TCP game chưa có TLS | Credential có thể bị đọc trên đường truyền | Chỉ dùng tài khoản game riêng trong demo; lập kế hoạch TLS/VPN riêng trước khi public rộng. |
| File ZIP lớn và cache sai version | Người dùng tải bản cũ | Gắn version vào tên file, cache immutable theo version và cập nhật `config.js`. |
| Xóa nhầm volume khi deploy | Mất tài khoản/lịch sử trận | Không dùng `down -v` trong quy trình update; backup trước migration. |

---

## 10. Thứ tự ưu tiên triển khai

1. Đóng gói và xác minh JavaFX ZIP mới.
2. Chốt hostname/port public.
3. Dựng design tokens và hero responsive.
4. Hoàn thiện nội dung hướng dẫn và download card.
5. Tích hợp container website vào Compose.
6. Kiểm thử tải–giải nén–chạy–kết nối end-to-end.
7. Đưa lên DDNS/public IP.
8. Bổ sung HTTPS và tối ưu cache sau khi luồng chính ổn định.

---

## 11. Đầu ra cuối cùng

- Source website tĩnh trong `website/`.
- Container web chạy cùng Compose.
- JavaFX Windows ZIP mới và file SHA-256 trong `downloads/`.
- Landing page responsive dùng đúng font, màu và SVG của game.
- Hướng dẫn triển khai/cập nhật trong README.
- Biên bản kiểm thử gồm URL, version, checksum và kết quả kết nối từ mạng ngoài.
