let currentFilter = 'ALL';

async function fetchLogs() {
    try {
        const response = await fetch('/api/logs/private-view');
        const logs = await response.json();
        
        const logDisplay = document.getElementById('log-display');
        const clockDisplay = document.getElementById('clock-val');
        
        logDisplay.innerHTML = ''; // Clear display
        
        let maxClock = 0;

        logs.forEach(log => {
            // log format: [Time] [TYPE] [Lamport: X] Message
            const typeMatch = log.match(/\[([A-Z]+)\]/g);
            const type = typeMatch && typeMatch[1] ? typeMatch[1].replace('[', '').replace(']', '') : 'INFO';
            
            // Extract clock for display update
            const clockMatch = log.match(/\[Lamport: (\d+)\]/);
            if(clockMatch) {
                const clockVal = parseInt(clockMatch[1]);
                if(clockVal > maxClock) maxClock = clockVal;
            }

            // Apply filter
            if (currentFilter === 'ALL' || type === currentFilter) {
                const div = document.createElement('div');
                div.className = `log-entry log-${type}`;
                div.innerText = log;
                logDisplay.appendChild(div);
            }
        });

        clockDisplay.innerText = maxClock;
        // Tự động cuộn xuống cuối log
        logDisplay.scrollTop = logDisplay.scrollHeight;

    } catch (error) {
        console.error("Failed to fetch logs:", error);
    }
}

function filterLogs(filter) {
    currentFilter = filter;
    document.querySelectorAll('.btn-filter').forEach(btn => {
        btn.classList.toggle('active', btn.innerText === filter);
    });
    fetchLogs();
}

// Chạy fetch mỗi 2 giây
setInterval(fetchLogs, 2000);
fetchLogs(); // Chạy lần đầu ngay khi load