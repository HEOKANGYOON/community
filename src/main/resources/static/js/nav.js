/**
 * nav.js - 공통 네비게이션 + SSE 구독 + 알림 UI
 */

const API_BASE = '';
let unreadCount = 0;
let sseAbortController = null;

function getToken() {
    return localStorage.getItem('accessToken');
}

function authHeaders() {
    const token = getToken();
    const headers = { 'Content-Type': 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
}

function logout() {
    localStorage.removeItem('accessToken');
    if (sseAbortController) sseAbortController.abort();
    window.location.replace('/login.html');
}

// ────────────────────────────────────────────────
// 네비게이션 렌더링
// ────────────────────────────────────────────────
function renderNav() {
    const token = getToken();
    const nav = document.getElementById('nav-auth');
    if (!nav) return;

    if (token) {
        nav.innerHTML = `
            <div class="position-relative" id="notification-wrap">
                <button class="btn btn-sm btn-outline-secondary position-relative" id="bell-btn">
                    🔔
                    <span id="notif-badge"
                          class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger"
                          style="font-size:10px; display:none;">0</span>
                </button>
                <div id="notif-panel" style="
                    display:none;
                    position:absolute;
                    right:0;
                    top:calc(100% + 8px);
                    width:320px;
                    background:#fff;
                    border:1px solid #e9ecef;
                    border-radius:12px;
                    box-shadow:0 4px 16px rgba(0,0,0,0.1);
                    z-index:9999;
                    overflow:hidden;
                ">
                    <div style="padding:12px 16px; border-bottom:1px solid #f1f3f5; display:flex; justify-content:space-between; align-items:center;">
                        <span style="font-size:14px; font-weight:500;">알림</span>
                        <button id="mark-all-btn" style="font-size:12px; color:#6c757d; background:none; border:none; cursor:pointer;">모두 읽음</button>
                    </div>
                    <div id="notif-list" style="max-height:360px; overflow-y:auto;"></div>
                </div>
            </div>
            <button class="btn btn-sm btn-outline-secondary" id="logout-btn">로그아웃</button>
        `;

        document.getElementById('bell-btn').addEventListener('click', (e) => {
            e.stopPropagation();
            const panel = document.getElementById('notif-panel');
            const isOpen = panel.style.display === 'block';
            panel.style.display = isOpen ? 'none' : 'block';
            if (!isOpen) loadNotifications();
        });

        document.getElementById('logout-btn').addEventListener('click', logout);

        document.getElementById('mark-all-btn').addEventListener('click', markAllRead);

        document.addEventListener('click', (e) => {
            const wrap = document.getElementById('notification-wrap');
            if (wrap && !wrap.contains(e.target)) {
                const panel = document.getElementById('notif-panel');
                if (panel) panel.style.display = 'none';
            }
        });

        loadNotifications();
        subscribeSSE();

    } else {
        nav.innerHTML = `<a href="/login.html" class="btn btn-sm btn-dark">로그인</a>`;
    }
}

// ────────────────────────────────────────────────
// SSE 구독
// ────────────────────────────────────────────────
function subscribeSSE() {
    if (!getToken()) return;
    if (sseAbortController) sseAbortController.abort();
    sseAbortController = new AbortController();
    fetchSSE(sseAbortController.signal);
}

async function fetchSSE(signal) {
    try {
        const headers = { 'Authorization': `Bearer ${getToken()}` };
        const lastEventId = localStorage.getItem('lastEventId');
        if (lastEventId) headers['Last-Event-ID'] = lastEventId;

        const response = await fetch(`${API_BASE}/api/notifications/subscribe`, { headers, signal });
        if (!response.ok) return;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = '';

        while (true) {
            const { value, done } = await reader.read();
            if (done) break;

            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop();

            let eventId = '', dataLines = [];
            for (const line of lines) {
                if (line.startsWith('id:')) {
                    eventId = line.slice(3).trim();
                } else if (line.startsWith('data:')) {
                    dataLines.push(line.slice(5).trim());
                } else if (line === '') {
                    if (dataLines.length > 0) {
                        const data = dataLines.join('\n');
                        if (eventId) localStorage.setItem('lastEventId', eventId);
                        handleSSEEvent(data);
                    }
                    eventId = ''; dataLines = [];
                }
            }
        }
    } catch (e) {
        if (e.name !== 'AbortError' && getToken()) {
            setTimeout(() => subscribeSSE(), 3000);
        }
    }
}

function handleSSEEvent(data) {
    if (!data || data === 'connected') return;
    try {
        const notification = JSON.parse(data);
        unreadCount++;
        updateBadge();
        loadNotifications();
        showToast(notification);
    } catch (e) {}
}

// ────────────────────────────────────────────────
// 알림 목록 로드
// ────────────────────────────────────────────────
async function loadNotifications() {
    try {
        const res = await fetch(`${API_BASE}/api/notifications/notice`, {
            headers: authHeaders()
        });
        if (!res.ok) return;
        const json = await res.json();
        const notifications = json.data || [];
        unreadCount = notifications.filter(n => !n.isRead).length;
        updateBadge();
        renderNotifications(notifications);
    } catch (e) {}
}

function renderNotifications(notifications) {
    console.log('renderNotifications 호출, 개수:', notifications.length, notifications);


    const list = document.getElementById('notif-list');
    if (!list) return;

    if (!notifications.length) {
        list.innerHTML = '<div style="padding:24px; text-align:center; color:#adb5bd; font-size:13px;">알림이 없습니다.</div>';
        return;
    }

    list.innerHTML = notifications.map(n => `
        <div class="notif-item"
             data-id="${n.id}"
             data-target="${n.targetId || ''}"
             data-read="${n.isRead}"
             style="padding:12px 16px; border-bottom:1px solid #f8f9fa; background:${n.isRead ? '#fff' : '#f0f7ff'}; display:flex; justify-content:space-between; align-items:flex-start; gap:8px; cursor:pointer;">
            <div style="flex:1; pointer-events:none;">
                <div style="font-size:13px; color:#212529; margin-bottom:2px;">${escapeHtml(n.content)}</div>
                <div style="font-size:11px; color:#adb5bd;">${formatRelativeTime(n.createAt)} · ${notifTypeLabel(n.notificationType)}</div>
            </div>
            <button class="notif-delete-btn" data-id="${n.id}"
                    style="background:none; border:none; color:#adb5bd; font-size:18px; cursor:pointer; padding:0 2px; line-height:1; flex-shrink:0;">×</button>
        </div>
    `).join('');

    // 이벤트 위임
    list.onclick = async (e) => {
        // 삭제 버튼
        const deleteBtn = e.target.closest('.notif-delete-btn');
        if (deleteBtn) {
            e.stopPropagation();
            const id = deleteBtn.dataset.id;
            await fetch(`${API_BASE}/api/notifications/notice/${id}`, {
                method: 'DELETE', headers: authHeaders()
            });
            await loadNotifications();
            return;
        }

        // 알림 항목 클릭 → 읽음 처리
        const item = e.target.closest('.notif-item');
        if (item) {
            const id = item.dataset.id;
            const targetId = item.dataset.target;
            const isRead = item.dataset.read === 'true';

            if (!isRead) {
                await fetch(`${API_BASE}/api/notifications/notice/${id}`, {
                    method: 'PATCH', headers: authHeaders()
                });
                await loadNotifications();
            }

            if (targetId) {
                const boardId = new URLSearchParams(window.location.search).get('boardId');
                if (boardId) {
                    window.location.href = `/post.html?boardId=${boardId}&postId=${targetId}`;
                }
            }
        }
    };
}

async function markAllRead() {
    try {
        const res = await fetch(`${API_BASE}/api/notifications/notice`, { headers: authHeaders() });
        if (!res.ok) return;
        const json = await res.json();
        const unread = (json.data || []).filter(n => !n.isRead);
        await Promise.all(unread.map(n =>
            fetch(`${API_BASE}/api/notifications/notice/${n.id}`, {
                method: 'PATCH', headers: authHeaders()
            })
        ));
        await loadNotifications();
    } catch (e) {}
}

// ────────────────────────────────────────────────
// 뱃지 + 토스트
// ────────────────────────────────────────────────
function updateBadge() {
    const badge = document.getElementById('notif-badge');
    if (!badge) return;
    if (unreadCount > 0) {
        badge.style.display = 'inline-block';
        badge.textContent = unreadCount > 99 ? '99+' : unreadCount;
    } else {
        badge.style.display = 'none';
    }
}

function showToast(notification) {
    const toast = document.createElement('div');
    toast.style.cssText = 'position:fixed; bottom:24px; right:24px; z-index:99999; background:#212529; color:#fff; border-radius:10px; padding:12px 16px; font-size:13px; max-width:280px; box-shadow:0 4px 16px rgba(0,0,0,0.2);';
    toast.textContent = notification.content || '새 알림이 도착했습니다.';
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

// ────────────────────────────────────────────────
// 유틸
// ────────────────────────────────────────────────
function notifTypeLabel(type) {
    return { COMMENT: '새 댓글', REPLY: '새 대댓글', COMMENT_LIKE: '댓글 좋아요' }[type] || type;
}

function formatRelativeTime(dateStr) {
    const diff = Date.now() - new Date(dateStr).getTime();
    const min = Math.floor(diff / 60000);
    if (min < 1) return '방금 전';
    if (min < 60) return `${min}분 전`;
    const hour = Math.floor(min / 60);
    if (hour < 24) return `${hour}시간 전`;
    return `${Math.floor(hour / 24)}일 전`;
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

document.addEventListener('DOMContentLoaded', () => {
    renderNav();
});
