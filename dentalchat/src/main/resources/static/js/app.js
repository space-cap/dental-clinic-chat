let stompClient = null;
let currentUsername = '';
let currentUserRole = '';
let currentRoomId = '';
let isConnected = false;

const statusDot = document.getElementById('statusDot');
const statusText = document.getElementById('statusText');
const roomInfo = document.getElementById('roomInfo');
const chatMessages = document.getElementById('chatMessages');
const userSetup = document.getElementById('userSetup');
const chatInput = document.getElementById('chatInput');
const usernameInput = document.getElementById('usernameInput');
const userRoleSelect = document.getElementById('userRoleSelect');
const roomIdInput = document.getElementById('roomIdInput');
const joinButton = document.getElementById('joinButton');
const messageInput = document.getElementById('messageInput');
const sendButton = document.getElementById('sendButton');

function connect() {
    const socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function (frame) {
        console.log('Connected: ' + frame);
        isConnected = true;
        updateConnectionStatus('연결됨', true);
        
        stompClient.subscribe('/topic/room/' + currentRoomId, function (messageOutput) {
            showMessage(JSON.parse(messageOutput.body));
        });
        
        joinRoom();
    }, function (error) {
        console.log('Connection error: ' + error);
        isConnected = false;
        updateConnectionStatus('연결 실패', false);
        setTimeout(connect, 5000);
    });
}

function disconnect() {
    if (stompClient !== null && isConnected) {
        stompClient.disconnect();
        isConnected = false;
        updateConnectionStatus('연결 해제됨', false);
    }
}

function joinRoom() {
    if (stompClient && isConnected) {
        const joinMessage = {
            sender: currentUsername,
            senderRole: currentUserRole,
            content: '',
            type: 'JOIN',
            roomId: currentRoomId
        };
        
        stompClient.send('/app/chat.joinRoom/' + currentRoomId, {}, JSON.stringify(joinMessage));
    }
}

function sendMessage() {
    const messageContent = messageInput.value.trim();
    if (messageContent === '') return;

    if (stompClient && isConnected) {
        const chatMessage = {
            sender: currentUsername,
            senderRole: currentUserRole,
            content: messageContent,
            type: 'CHAT',
            roomId: currentRoomId
        };

        stompClient.send('/app/chat.sendMessage/' + currentRoomId, {}, JSON.stringify(chatMessage));
        messageInput.value = '';
        adjustTextareaHeight();
    }
}

function showMessage(message) {
    const messageDiv = document.createElement('div');
    
    if (message.type === 'JOIN') {
        messageDiv.className = 'system-message';
        messageDiv.innerHTML = `
            <div class="message-bubble">
                ${message.content}
            </div>
        `;
    } else {
        const isOwn = message.sender === currentUsername;
        messageDiv.className = `message ${isOwn ? 'own' : ''}`;
        
        const roleClass = message.senderRole === 'CUSTOMER' ? 'customer' : 'admin';
        const senderInitial = message.sender.charAt(0).toUpperCase();
        const timestamp = new Date(message.timestamp).toLocaleTimeString('ko-KR', {
            hour: '2-digit',
            minute: '2-digit'
        });
        
        messageDiv.innerHTML = `
            <div class="message-avatar ${roleClass}">${senderInitial}</div>
            <div class="message-content">
                <div class="message-bubble">${escapeHtml(message.content)}</div>
                <div class="message-info">
                    <span class="message-sender">${escapeHtml(message.sender)}</span>
                    <span class="message-time">${timestamp}</span>
                </div>
            </div>
        `;
    }
    
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

function updateConnectionStatus(status, connected) {
    statusDot.className = `status-dot ${connected ? 'connected' : ''}`;
    statusText.textContent = status;
    sendButton.disabled = !connected;
    
    if (connected && currentRoomId) {
        roomInfo.textContent = `상담방: ${currentRoomId}`;
    } else {
        roomInfo.textContent = '';
    }
}

function adjustTextareaHeight() {
    messageInput.style.height = 'auto';
    messageInput.style.height = Math.min(messageInput.scrollHeight, 100) + 'px';
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function validateInput() {
    const username = usernameInput.value.trim();
    const roomId = roomIdInput.value.trim();
    
    if (username === '' || roomId === '') {
        alert('이름과 상담방 ID를 모두 입력해주세요.');
        return false;
    }
    
    if (username.length < 2) {
        alert('이름은 최소 2글자 이상 입력해주세요.');
        return false;
    }
    
    return true;
}

joinButton.addEventListener('click', function() {
    if (!validateInput()) return;
    
    currentUsername = usernameInput.value.trim();
    currentUserRole = userRoleSelect.value;
    currentRoomId = roomIdInput.value.trim();
    
    joinButton.disabled = true;
    joinButton.textContent = '연결 중...';
    
    userSetup.style.display = 'none';
    chatInput.style.display = 'flex';
    
    connect();
});

sendButton.addEventListener('click', sendMessage);

messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
    }
});

messageInput.addEventListener('input', adjustTextareaHeight);

window.addEventListener('beforeunload', function() {
    disconnect();
});

document.addEventListener('DOMContentLoaded', function() {
    adjustTextareaHeight();
    
    if (window.authenticatedUser && window.authenticatedUser.username) {
        usernameInput.value = window.authenticatedUser.username;
        usernameInput.disabled = true;
        
        if (window.authenticatedUser.isAdmin) {
            userRoleSelect.value = 'ADMIN';
        } else {
            userRoleSelect.value = 'CUSTOMER';
        }
        userRoleSelect.disabled = true;
        
        joinButton.textContent = '상담방 입장';
    }
});