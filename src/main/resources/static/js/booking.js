// Biến lưu trữ URL của server hiện tại đang được Client chọn
let currentServerApiUrl = '/api'; // Mặc định là gọi API của chính server đang chạy giao diện

// 1. Khi trang web tải xong, lấy danh sách các Server
document.addEventListener('DOMContentLoaded', function() {
    fetch('/api/servers/list')
        .then(response => response.json())
        .then(servers => {
            const select = document.getElementById('serverSelect');
            servers.forEach((serverUrl, index) => {
                const option = document.createElement('option');
                option.value = serverUrl.split(' ')[0]; // Lấy phần URL trước dấu cách
                option.text = serverUrl;
                select.appendChild(option);
            });
        });
});

// 2. Hàm xử lý khi Thầy thay đổi lựa chọn trong menu
function changeServer(newUrl) {
    currentServerApiUrl = newUrl + '/api';
    
    // Cập nhật hiển thị IP trên giao diện
    const ip = newUrl.replace('http://', '').replace(':8080', '');
    document.getElementById('currentIpDisplay').innerText = ip;
    
    // Thông báo cho Thầy
    const toast = document.getElementById('toast');
    toast.innerText = "🔌 Đã chuyển kết nối sang Trạm: " + ip;
    toast.style.display = "block";
    toast.style.background = "#007bff"; // Màu xanh dương thông báo
    setTimeout(() => { toast.style.display = "none"; }, 2500);
}

// 3. Hàm confirmBooking (Sửa lại để dùng currentServerApiUrl)
function confirmBooking() {
    const name = document.getElementById('passengerName').value;
    const flight = document.getElementById('flightId').value;
    const toast = document.getElementById('toast');

    if (!name) {
        alert("Vui lòng nhập tên khách hàng!");
        return;
    }

    // Hiển thị trạng thái đang xử lý
    toast.innerText = "🚀 Đang gửi yêu cầu đặt vé tới Trạm chọn...";
    toast.style.display = "block";
    toast.style.background = "#333";

    // Gửi yêu cầu đến API của Server ĐƯỢC CHỌN
    // SỬA DÒNG NÀY: Dùng backtick `` và biến currentServerApiUrl
    fetch(`${currentServerApiUrl}/booking?flightId=${flight}&userId=${encodeURIComponent(name)}`, {
        method: 'POST'
    })
    .then(response => response.text())
    .then(data => {
        toast.innerText = "✅ " + data;
        toast.style.background = "#28a745"; // Màu xanh thành công
        
        // Reset form sau 3 giây
        setTimeout(() => {
            toast.style.display = "none";
            document.getElementById('passengerName').value = "";
        }, 3000);
    })
    .catch(error => {
        toast.innerText = "❌ Lỗi: Không thể kết nối tới Trạm được chọn!";
        toast.style.background = "#dc3545"; // Màu đỏ lỗi
        console.error("Booking error:", error);
    });
}