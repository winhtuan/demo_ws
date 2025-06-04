package control;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/chat")
public class ChatEndPoint {

    // Lưu session để gửi broadcast
    private static final Set<Session> sessions = new CopyOnWriteArraySet<>();

    // Lưu username ứng với mỗi session
    private static final Map<Session, String> userNames = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("New session: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session senderSession) {

        // Nếu message bắt đầu bằng "LOGIN:", coi như client gửi username
        if (message.startsWith("LOGIN:")) {
            String username = message.substring("LOGIN:".length()).trim();
            userNames.put(senderSession, username);
            sendMessageToSession(senderSession, "System: Bạn đã đăng nhập với tên '" + username + "'");
            broadcast("System: " + username + " đã tham gia phòng chat", senderSession);
            return;
        }

        String senderName = userNames.get(senderSession);
        if (senderName == null) {
            sendMessageToSession(senderSession, "System: Bạn chưa đăng nhập. Vui lòng gửi LOGIN:yourname trước.");
            return;
        }

        // Gửi tin nhắn dạng: "username: message"
        String fullMessage = senderName + ": " + message;
        broadcast(fullMessage, null);
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        String username = userNames.remove(session);
        if (username != null) {
            broadcast("System: " + username + " đã rời phòng chat", null);
        }
        System.out.println("Closed session: " + session.getId());
    }

    private void sendMessageToSession(Session session, String message) {
        try {
            if (session.isOpen()) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcast(String message, Session exceptSession) {
        for (Session session : sessions) {
            if (!session.equals(exceptSession) && session.isOpen()) {
                sendMessageToSession(session, message);
            }
        }
    }
}
