/**
 * GOOGLE APPS SCRIPT FOR TELEGRAM BOT Webhook
 * 
 * Hướng dẫn thiết lập:
 * 1. Truy cập https://script.google.com và tạo một Dự án mới.
 * 2. Dán đoạn mã này vào file Code.gs của dự án.
 * 3. Tạo một Service Account trong Firebase Console (Cài đặt dự án -> Tài khoản dịch vụ -> Tạo mã khóa riêng mới).
 * 4. Tải file JSON chứa khóa về, sao chép client_email và private_key dán vào phần cấu hình bên dưới.
 * 5. Bấm "Triển khai" -> "Triển khai mới" -> Chọn loại "Ứng dụng web".
 *    - Thực thi dưới danh nghĩa: "Tôi" (chính bạn).
 *    - Ai có quyền truy cập: "Mọi người" (để Telegram có thể gửi dữ liệu qua POST).
 * 6. Copy URL Web App sau khi triển khai.
 * 7. Thiết lập Webhook cho Telegram Bot bằng cách truy cập URL trên trình duyệt:
 *    https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/setWebhook?url=<URL_WEB_APP_GOOGLE_APPS_SCRIPT>
 */

// === CẤU HÌNH HỆ THỐNG ===
const TELEGRAM_BOT_TOKEN = "8719184708:AAEHY0mfzeTyA7Zqu2S7W-Tu4ecYRZqbkjA";
const ADMIN_CHAT_ID = "5236653379";

// Thay thế thông tin Service Account từ Firebase của bạn vào đây:
const FIREBASE_PROJECT_ID = "canlua-3995f"; // Project ID của Firebase
const FIREBASE_CLIENT_EMAIL = "firebase-adminsdk-xxxxx@canluavn.iam.gserviceaccount.com"; // Điền email của service account
const FIREBASE_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----\n"; // Điền private key (bao gồm cả các ký tự \n)

/**
 * Hàm nhận request POST từ Telegram Webhook khi Admin thực hiện gửi tin nhắn hoặc trả lời (Reply).
 */
function doPost(e) {
  try {
    const update = JSON.parse(e.postData.contents);
    
    // Kiểm tra xem có tin nhắn phản hồi (Reply) từ Admin hay không
    if (update.message && update.message.reply_to_message) {
      const replyMessage = update.message;
      const originalMessage = replyMessage.reply_to_message;
      
      // Chỉ xử lý nếu tin nhắn xuất phát từ Admin và trong đúng chat ID cấu hình
      if (String(replyMessage.chat.id) === ADMIN_CHAT_ID) {
        const replyText = replyMessage.text;
        const originalText = originalMessage.text || "";
        
        // Trích xuất feedbackId từ tin nhắn gốc (ví dụ tin nhắn gốc có dòng: "Ref: feedback_xxxx")
        const feedbackIdMatch = originalText.match(/Ref:\s*([a-zA-Z0-9_-]+)/);
        
        if (feedbackIdMatch && feedbackIdMatch[1]) {
          const feedbackId = feedbackIdMatch[1];
          const replyTimestamp = Date.now();
          
          // Cập nhật câu trả lời lên Firestore
          const success = updateFirestoreFeedback(feedbackId, replyText, replyTimestamp);
          
          if (success) {
            // Phản hồi lại trên Telegram báo thành công
            sendTelegramMessage(ADMIN_CHAT_ID, `✅ Đã gửi phản hồi thành công tới user!\nNội dung: "${replyText}"`, replyMessage.message_id);
          } else {
            sendTelegramMessage(ADMIN_CHAT_ID, "❌ Lỗi: Không thể kết nối hoặc cập nhật Firestore.", replyMessage.message_id);
          }
        } else {
          sendTelegramMessage(ADMIN_CHAT_ID, "⚠️ Không tìm thấy mã tham chiếu (Ref: [feedbackId]) trong tin nhắn gốc để trả lời.", replyMessage.message_id);
        }
      }
    }
    
    return HtmlService.createHtmlOutput(JSON.stringify({ status: "success" }));
  } catch (error) {
    // Log lỗi để dễ dàng debug trên Apps Script Console
    console.error("Error in webhook: " + error.toString());
    return HtmlService.createHtmlOutput(JSON.stringify({ status: "error", message: error.toString() }));
  }
}

/**
 * Gửi tin nhắn Telegram từ Bot
 */
function sendTelegramMessage(chatId, text, replyToMessageId) {
  const url = `https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendMessage`;
  const payload = {
    chat_id: chatId,
    text: text
  };
  
  if (replyToMessageId) {
    payload.reply_to_message_id = replyToMessageId;
  }
  
  const options = {
    method: "post",
    contentType: "application/json",
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  };
  
  UrlFetchApp.fetch(url, options);
}

/**
 * Thực hiện cập nhật tài liệu phản hồi trên Firestore qua REST API bằng JWT OAuth2
 */
function updateFirestoreFeedback(feedbackId, replyText, replyTimestamp) {
  const accessToken = getGcpAccessToken();
  if (!accessToken) return false;
  
  const url = `https://firestore.googleapis.com/v1/projects/${FIREBASE_PROJECT_ID}/databases/(default)/documents/feedbacks/${feedbackId}?updateMask.fieldPaths=replyText&updateMask.fieldPaths=replyTimestamp&updateMask.fieldPaths=isReadByUser`;
  
  const payload = {
    fields: {
      replyText: { stringValue: replyText },
      replyTimestamp: { integerValue: String(replyTimestamp) },
      isReadByUser: { booleanValue: false } // Đánh dấu false để kích hoạt chấm đỏ trên app
    }
  };
  
  const options = {
    method: "patch",
    contentType: "application/json",
    headers: {
      Authorization: `Bearer ${accessToken}`
    },
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  };
  
  const response = UrlFetchApp.fetch(url, options);
  const respCode = response.getResponseCode();
  
  if (respCode >= 200 && respCode < 300) {
    return true;
  } else {
    console.error("Firestore Update Error: " + response.getContentText());
    return false;
  }
}

/**
 * Tạo GCP Access Token từ Service Account Key sử dụng JWT (không cần thư viện ngoài)
 */
function getGcpAccessToken() {
  const header = JSON.stringify({ alg: "RS256", typ: "JWT" });
  
  const now = Math.floor(Date.now() / 1000);
  const claim = JSON.stringify({
    iss: FIREBASE_CLIENT_EMAIL,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now
  });
  
  const base64Header = base64EncodeSafe(header);
  const base64Claim = base64EncodeSafe(claim);
  
  const signatureInput = `${base64Header}.${base64Claim}`;
  const signature = Utilities.computeRsaSha256Signature(signatureInput, FIREBASE_PRIVATE_KEY);
  const base64Signature = base64EncodeSafe(Utilities.newBlob(signature).getBytes());
  
  const jwt = `${signatureInput}.${base64Signature}`;
  
  const tokenUrl = "https://oauth2.googleapis.com/token";
  const payload = {
    grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
    assertion: jwt
  };
  
  const response = UrlFetchApp.fetch(tokenUrl, {
    method: "post",
    payload: payload,
    muteHttpExceptions: true
  });
  
  if (response.getResponseCode() === 200) {
    const data = JSON.parse(response.getContentText());
    return data.access_token;
  } else {
    console.error("OAuth Access Token Error: " + response.getContentText());
    return null;
  }
}

function base64EncodeSafe(input) {
  let encoded;
  if (typeof input === 'string') {
    encoded = Utilities.base64EncodeWebSafe(input, Utilities.Charset.UTF_8);
  } else {
    encoded = Utilities.base64EncodeWebSafe(input);
  }
  return encoded.replace(/=+$/, '');
}
