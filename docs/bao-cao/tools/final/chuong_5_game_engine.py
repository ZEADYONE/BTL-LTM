"""Chương 5 - Game Engine và đồng bộ thời gian thực (phần cá nhân C).

Sinh bởi docx_to_python.py từ bao_cao_nhom_bomberman-final.docx, sau đó có thể sửa tay.
Chạy lại docx_to_python.py sẽ ghi đè file này.
Cập nhật hình theo update.md (27/09/2026): vẽ lại sơ đồ ở mức phân tích chức năng (functional_diagrams.py), đưa lại Hình 5.3.
Chỉnh tay sau khi sinh (27/09/2026): đánh số lại bảng (5.5 → 5.3), bỏ đoạn chỉ có dấu chấm và hai câu ghi chú biên tập, thêm dấu chấm cuối câu.
"""
from __future__ import annotations

from report_common import (
    ASSET_DIR,
    SCREENSHOT_DIR,
    add_blank,
    add_body,
    add_bullet,
    add_figure,
    add_heading,
    add_table,
)


def write(doc):
    add_heading(doc, "CHƯƠNG 5. GAME ENGINE VÀ ĐỒNG BỘ THỜI GIAN THỰC", "CTDT-H1")
    add_heading(doc, "5.1 Phạm vi chức năng", "CTDT-H2")
    add_body(
        doc,
        "Phần Game Engine xử lý toàn bộ diễn biến trận đấu sau khi phòng chuyển sang trạng thái thi đấu. Hệ thống áp dụng mô hình Server có thẩm quyền (authoritative server): Client chỉ gửi ý định điều khiển gồm hướng di chuyển và yêu cầu đặt bom, còn Server kiểm tra, cập nhật bản đồ, người chơi, bom, vụ nổ và kết quả, sau đó gửi trạng thái hoàn chỉnh tới mọi người trong phòng.",
    )
    add_body(
        doc,
        "Phạm vi gồm tiếp nhận thao tác qua mạng, vòng lặp xử lý theo nhịp cố định cho từng phòng, luật di chuyển, đặt bom, nổ bom và nổ dây chuyền, xác định thắng hoặc hòa, xử lý người chơi mất kết nối giữa trận, đồng bộ trạng thái tới nhiều Client và phần thu nhận phím, hiển thị trận đấu ở Client. Bảng 5.1 liệt kê các chức năng đã triển khai.",
    )
    add_table(
        doc,
        "Bảng 5.1. Các chức năng thuộc phần Game Engine và đồng bộ thời gian thực",
        ["ID", "Chức năng"],
        [
            ["GE-01", "Khởi tạo trận cho phòng"],
            ["GE-02", "Tiếp nhận và kiểm tra thao tác"],
            ["GE-03", "Hàng đợi lệnh theo phòng"],
            ["GE-04", "Vòng lặp 20 tick/giây"],
            ["GE-05", "Di chuyển và va chạm"],
            ["GE-06", "Đặt bom"],
            ["GE-07", "Nổ bom, phá tường, nổ dây chuyền"],
            ["GE-08", "Loại người chơi, xác định kết quả"],
            ["GE-09", "Mất kết nối giữa trận"],
            ["GE-10", "Phát trạng thái theo phòng"],
            ["GE-11", "Kết thúc trận"],
            ["GE-12", "Thu phím và hiển thị trận"],
        ],
        [860, 8640],
    )
    add_heading(doc, "5.2 Thiết kế Game Engine", "CTDT-H2")
    add_body(
        doc,
        "Mỗi phòng đang thi đấu có một vùng trạng thái riêng gồm bản đồ, người chơi, bom, vụ nổ và trạng thái trận đấu. Nhờ đó, nhiều phòng có thể hoạt động đồng thời mà dữ liệu của các trận không bị trộn lẫn.",
    )
    add_body(
        doc,
        "Khi Client gửi thao tác, phần xử lý mạng không trực tiếp thay đổi trạng thái trò chơi. Thao tác được chuyển tới bộ xử lý của đúng phòng, sau đó Game Engine thực hiện kiểm tra và cập nhật trạng thái theo thứ tự.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-1-thanh-phan-game-engine.png",
        "Hình 5.1. Thành phần Game Engine",
        "Client gửi thao tác, Server chuyển tới bộ xử lý trận của đúng phòng; mỗi phòng có trạng thái trận riêng gồm bản đồ, người chơi, bom, vụ nổ; trạng thái mới được gửi về các Client trong phòng.",
    )
    add_body(
        doc,
        "Bản đồ 13 × 11 có tường cứng ở viền và tại các ô có cả chỉ số cột và hàng đều chẵn. Tường phá được sinh theo công thức cố định, sau đó vùng xung quanh bốn điểm xuất phát được dọn trống để người chơi có chỗ di chuyển ban đầu. Bảng 5.2 tổng hợp các thông số chính.",
    )
    add_table(
        doc,
        "Bảng 5.2. Thông số luật chơi",
        ["Thông số", "Giá trị"],
        [
            ["Kích thước bản đồ", "13 cột × 11 hàng"],
            ["Số người mỗi trận", "2 đến 4"],
            ["Điểm xuất phát", "(1,1), (11,1), (1,9), (11,9)"],
            ["Số bom đặt đồng thời", "1 mỗi người"],
            ["Tầm nổ", "2 ô mỗi hướng"],
            ["Thời gian chờ nổ", "3 giây"],
        ],
        [2900, 6420],
    )
    add_heading(doc, "5.3 Tiếp nhận thao tác người chơi", "CTDT-H2")
    add_body(doc, "Trong trận đấu, Client chủ yếu gửi hai loại thao tác:")
    add_bullet(doc, "di chuyển theo một hướng;")
    add_bullet(doc, "yêu cầu đặt bom.")
    add_body(
        doc,
        "Khi nhận yêu cầu, Server xác định người gửi, phòng mà người đó đang tham gia và trạng thái hiện tại của trận. Chỉ những thao tác hợp lệ mới được chuyển tới Game Engine xử lý.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-2-di-chuyen.png",
        "Hình 5.2. Xử lý thao tác di chuyển",
        "Người chơi nhấn phím, Client gửi hướng, Server kiểm tra thao tác và ô đích, cập nhật vị trí nếu hợp lệ và gửi trạng thái mới.",
    )
    add_heading(doc, "5.4 Vòng lặp xử lý và đồng bộ trạng thái", "CTDT-H2")
    add_body(doc, "Trong khi trận đấu diễn ra, Server xử lý trạng thái của từng phòng theo một nhịp cố định.")
    add_body(
        doc,
        "Ở mỗi chu kỳ, Server lần lượt xử lý các thao tác đang chờ, cập nhật các sự kiện phụ thuộc thời gian như bom phát nổ, vụ nổ kết thúc và người chơi bị loại, sau đó tạo trạng thái mới của trận. Hình 5.3 mô tả chu kỳ xử lý này.",
    )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-3-chu-ky-xu-ly-tran.png",
        "Hình 5.3. Chu kỳ xử lý trận",
        "Mỗi chu kỳ: tiếp nhận thao tác, xử lý di chuyển và đặt bom, cập nhật bom và vụ nổ, kiểm tra người bị loại và kết quả, gửi trạng thái mới rồi sang chu kỳ tiếp theo.",
    )
    add_heading(doc, "5.5 Di chuyển, đặt bom và nổ bom", "CTDT-H2")
    add_heading(doc, "5.5.1 Di chuyển và va chạm", "CTDT-H3")
    add_body(doc, "Khi Server nhận thao tác di chuyển, Game Engine xác định ô đích của người chơi.")
    add_body(
        doc,
        "Người chơi chỉ được di chuyển nếu ô đích nằm trong bản đồ và không bị chặn bởi vật cản, bom hoặc người chơi khác. Nếu điều kiện không hợp lệ, vị trí không thay đổi.",
    )
    add_body(doc, "Nhờ việc kiểm tra ở Server, các Client không thể tự quyết định vị trí của người chơi.")
    add_heading(doc, "5.5.2 Đặt bom", "CTDT-H3")
    add_body(
        doc,
        "Khi người chơi yêu cầu đặt bom, Server kiểm tra người chơi còn hoạt động và vị trí hiện tại có thể đặt bom hay không.",
    )
    add_body(
        doc,
        "Nếu hợp lệ, bom được tạo tại vị trí của người chơi và được Server quản lý cho tới thời điểm phát nổ.",
    )
    add_body(doc, "Khi bom nổ, trạng thái mới được gửi tới các Client để hiển thị vụ nổ.")
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-4-dat-bom.png",
        "Hình 5.4. Đặt bom và phát nổ",
        "Người chơi yêu cầu đặt bom, Server kiểm tra và tạo bom, đến thời gian nổ thì tính vùng ảnh hưởng, cập nhật trạng thái và gửi tới các Client.",
    )
    add_heading(doc, "5.5.3 Nổ bom và nổ dây chuyền", "CTDT-H3")
    add_body(doc, "Khi bom phát nổ, vùng ảnh hưởng lan theo các hướng từ vị trí đặt bom.")
    add_body(
        doc,
        "Vụ nổ bị chặn bởi tường cứng. Nếu gặp vật cản có thể phá hủy, vật cản bị phá và vùng nổ dừng tại đó. Nếu một quả bom khác nằm trong vùng ảnh hưởng, quả bom đó có thể được kích hoạt để tạo thành nổ dây chuyền.",
    )
    add_body(doc, "Người chơi nằm trong vùng nổ sẽ bị loại khỏi trận.")
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-5-lan-lua.png",
        "Hình 5.5. Lan lửa và nổ dây chuyền",
        "Lửa lan theo bốn hướng: gặp tường cứng thì dừng, gặp tường phá được thì phá và dừng, gặp bom khác thì kích hoạt bom đó, gặp người chơi thì người chơi bị loại.",
    )
    add_heading(doc, "5.6 Kết thúc trận và mất kết nối giữa trận", "CTDT-H2")
    add_body(doc, "Sau mỗi lần một người chơi bị loại, Server kiểm tra số người còn lại trong trận.")
    add_body(
        doc,
        "Nếu chỉ còn một người chơi còn sống, người đó được xác định là thắng và những người đã bị loại có kết quả thua. Nếu tất cả người chơi còn lại bị loại cùng thời điểm, kết quả của trận là hòa.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-6-ket-qua-tran.png",
        "Hình 5.6. Xác định kết quả trận",
        "Sau khi có người bị loại: còn hơn một người thì tiếp tục, còn một người thì người đó thắng, không còn ai thì hòa; sau đó gửi kết quả và kết thúc trận.",
    )
    add_body(
        doc,
        "Nếu người chơi mất kết nối hoặc rời phòng trong khi trận đang diễn ra, sự kiện đó được chuyển tới Game Engine để xử lý như một thay đổi của trận. Người chơi không còn tham gia được đánh dấu bị loại và Server tiếp tục kiểm tra kết quả.",
    )
    add_body(
        doc,
        "Khi trận kết thúc, Server gửi kết quả tới các Client trong phòng và chuyển dữ liệu cần lưu sang phần xử lý kết quả.",
    )
    add_blank(doc, )
    add_heading(doc, "5.7 Thu nhận thao tác và hiển thị phía Client", "CTDT-H2")
    add_body(
        doc,
        "Phía Client chịu trách nhiệm thu nhận thao tác bàn phím của người chơi và chuyển thành yêu cầu gửi tới Server.",
    )
    add_body(
        doc,
        "Các thao tác chính gồm di chuyển nhân vật và đặt bom. Client không tự quyết định kết quả của thao tác mà chờ trạng thái mới do Server gửi về.",
    )
    add_body(
        doc,
        "Khi nhận trạng thái trận, Client cập nhật vị trí người chơi, bản đồ, bom và vụ nổ rồi hiển thị trên màn hình. Một số xử lý hiển thị có thể được thực hiện ở Client để chuyển động trông mượt hơn, nhưng trạng thái chính thức vẫn do Server quyết định.",
    )
    add_blank(doc, )
    add_figure(
        doc,
        ASSET_DIR / "chuc-nang-5-7-xu-ly-client.png",
        "Hình 5.7. Xử lý hiển thị phía Client",
        "Người chơi nhập bàn phím, Client gửi thao tác, Server xử lý, Client nhận trạng thái, cập nhật bản đồ, người chơi, bom, vụ nổ và hiển thị khung hình mới.",
    )
    add_heading(doc, "5.8 Giao diện minh họa", "CTDT-H2")
    add_body(
        doc,
        "Các hình dưới đây minh họa màn hình trận đấu. Mọi đối tượng trên bản đồ đều được vẽ từ trạng thái Server gửi về, nên các Client trong cùng phòng quan sát cùng một diễn biến.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c5-game.png",
        "Hình 5.8. Màn hình trận đấu và HUD người chơi",
        "Màn hình trận đấu với bản đồ và thẻ người chơi.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c5-explosion.png",
        "Hình 5.9. Bom và vụ nổ",
        "Bom đang đếm giờ và vụ nổ lan theo bốn hướng.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c5-eliminated.png",
        "Hình 5.10. Người chơi bị loại",
        "Thông báo người chơi đã bị loại và tiếp tục theo dõi trận.",
    )
    add_figure(
        doc,
        SCREENSHOT_DIR / "c5-menu.png",
        "Hình 5.11. Menu trong trận",
        "Menu ESC với các nút RESUME, HELP, LEAVE MATCH.",
    )
    add_heading(doc, "5.9 Ánh xạ hình vẽ với chức năng", "CTDT-H2")
    add_body(
        doc,
        "Bảng 5.3 tổng hợp quan hệ giữa các hình vẽ, giao diện trong chương và chức năng của phần Game Engine.",
    )
    add_table(
        doc,
        "Bảng 5.3. Ánh xạ hình vẽ với chức năng phần Game Engine và đồng bộ thời gian thực",
        ["Mã hình", "Hình vẽ / giao diện", "Chức năng được minh họa"],
        [
            ["Hình 5.1", "Thành phần Game Engine", "Server quản lý trạng thái trận riêng cho từng phòng"],
            ["Hình 5.2", "Xử lý thao tác di chuyển", "Nhận thao tác di chuyển và cập nhật vị trí tại Server"],
            ["Hình 5.3", "Chu kỳ xử lý trận", "Xử lý trận theo chu kỳ cố định cho từng phòng"],
            ["Hình 5.4", "Đặt bom và phát nổ", "Đặt bom, hẹn giờ nổ và cập nhật trạng thái sau vụ nổ"],
            ["Hình 5.5", "Lan lửa và nổ dây chuyền", "Xử lý vụ nổ, phá tường, nổ dây chuyền và loại người chơi"],
            ["Hình 5.6", "Xác định kết quả trận", "Xác định thắng, hòa và kết thúc trận"],
            ["Hình 5.7", "Xử lý hiển thị phía Client", "Gửi thao tác và hiển thị trạng thái nhận từ Server"],
            ["Hình 5.8", "Màn hình trận đấu và HUD người chơi", "Hiển thị GAME_STATE: bản đồ, người chơi, trạng thái còn sống"],
            ["Hình 5.9", "Bom và vụ nổ", "Hiển thị bombs[], explosions[] và bản đồ sau khi phá tường"],
            ["Hình 5.10", "Người chơi bị loại", "Hiển thị players[].alive = false, chặn gửi thao tác"],
            ["Hình 5.11", "Menu trong trận", "Rời trận (LEAVE_ROOM) mà không tạm dừng Server"],
        ],
        [1100, 2600, 5530],
    )
    add_heading(doc, "5.10 Kết chương", "CTDT-H2")
    add_body(
        doc,
        "Chương 5 đã trình bày Game Engine theo mô hình Server có thẩm quyền: thao tác từ mạng được kiểm tra và xếp vào hàng đợi, mỗi phòng có một vòng lặp đơn luồng 20 tick/giây thực hiện toàn bộ thay đổi trạng thái, trạng thái đầy đủ được gửi 10 lần mỗi giây tới đúng các phiên của phòng. Các luật di chuyển, đặt bom, lan lửa, nổ dây chuyền, loại người chơi và xác định kết quả đều chạy trên Server, còn Client chỉ thu phím, nội suy và hiển thị. Kết quả trận được chuyển sang phần lưu trữ và hiển thị ở chương tiếp theo.",
    )
