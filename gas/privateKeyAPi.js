// =========================================================================
// GOOGLE APPS SCRIPT BACKEND PROXY FOR CANLUA APP
// =========================================================================

// Cấu hình Model mặc định cho OpenRouter.
// Chỉ dùng `openrouter/free` — router tự chọn model free còn available.
// Lý do: model này ổn định hơn so với các model `:free` cụ thể (chúng hay bị OpenRouter
// retire không báo trước), và router tự động route sang provider còn hoạt động.
var MODEL = "openrouter/free";

// Hàm Helper để chuẩn hóa định dạng trả về JSON
function jsonResponse(data) {
  var output = JSON.stringify(data);
  return ContentService.createTextOutput(output)
                       .setMimeType(ContentService.MimeType.JSON);
}

// ─── ĐIỂM TIẾP NHẬN YÊU CẦU GET (doGet) ──────────────────────────────────
// Thích hợp cho việc đọc dữ liệu nhẹ nhàng như Thời tiết
function doGet(e) {
  try {
    var params = e.parameter;
    var scriptProperties = PropertiesService.getScriptProperties();
    var secretToken = scriptProperties.getProperty('APP_HANDSHAKE_TOKEN');
    
    // 1. Kiểm tra mã bảo mật Handshake (Bắt buộc)
    if (!secretToken) {
      return jsonResponse({ error: "Chưa cấu hình APP_HANDSHAKE_TOKEN trên GAS." });
    }
    if (params.token !== secretToken) {
      return jsonResponse({ error: "Unauthorized. Token handshake không đúng." });
    }
    
    var action = params.action;
    
    // ACTION: Lấy thời tiết từ OpenWeather (Trả về trực tiếp JSON của OpenWeather)
    if (action === 'weather') {
      var lat = params.lat;
      var lon = params.lon;
      if (!lat || !lon) {
        return jsonResponse({ error: "Thiếu tọa độ lat hoặc lon." });
      }
      
      var openWeatherKey = scriptProperties.getProperty('OPENWEATHER_API_KEY');
      if (!openWeatherKey) {
        return jsonResponse({ error: "Chưa cấu hình API Key OpenWeather." });
      }
      
      var url = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + openWeatherKey + "&units=metric&lang=vi";
      var response = UrlFetchApp.fetch(url, { muteHttpExceptions: true });
      var result = JSON.parse(response.getContentText());
      return jsonResponse(result);
    }
    
    // ACTION: Gửi tin nhắn Telegram ngắn qua URL (GET)
    if (action === 'telegram') {
      var message = params.message;
      if (!message) {
        return jsonResponse({ error: "Thiếu nội dung message." });
      }
      var res = sendTelegramMessage(message);
      return jsonResponse(res);
    }
    
    return jsonResponse({ error: "Action GET không được hỗ trợ." });
    
  } catch (error) {
    return jsonResponse({ error: error.toString() });
  }
}

// ─── ĐIỂM TIẾP NHẬN YÊU CẦU POST (doPost) ────────────────────────────────
// Thích hợp cho việc truyền dữ liệu phức tạp/lớn như AI prompt, tin nhắn Telegram dài
function doPost(e) {
  try {
    var scriptProperties = PropertiesService.getScriptProperties();
    var secretToken = scriptProperties.getProperty('APP_HANDSHAKE_TOKEN');
    
    // Phân tích dữ liệu JSON gửi lên từ App
    var postData = JSON.parse(e.postData.contents);

    // 1. Kiểm tra mã bảo mật Handshake (Bắt buộc)
    if (!secretToken || postData.token !== secretToken) {
      return jsonResponse({ error: "Unauthorized. Token handshake không đúng." });
    }

    var action = postData.action;
    // M-DEBUG: log keys của payload để xác định app gửi field nào. Có thể gỡ sau khi fix xong.
    var payloadKeys = Object.keys(postData || {});
    Logger.log("[GAS Proxy] Received doPost. Action: " + action + " | Keys: [" + payloadKeys.join(", ") + "] | Raw contents (first 800): " + e.postData.contents.substring(0, 800));
    
    // ACTION: Gửi tin nhắn Telegram dài qua Body (POST)
    if (action === 'telegram') {
      var message = postData.message;
      if (!message) {
        return jsonResponse({ error: "Thiếu nội dung message." });
      }
      var res = sendTelegramMessage(message);
      return jsonResponse(res);
    }
    
    // ACTION: Gọi AI qua OpenRouter (Trả về trực tiếp JSON của OpenRouter)
    if (action === 'openrouter') {
      var openRouterKey = scriptProperties.getProperty('OPENROUTER_API_KEY');
      if (!openRouterKey) {
        Logger.log("[GAS Proxy] Error: OPENROUTER_API_KEY is missing in script properties!");
        return jsonResponse({ error: "Chưa cấu hình API Key OpenRouter." });
      }

      // M-17: Chỉ dùng 1 model duy nhất `openrouter/free` (router tự chọn provider còn sống).
      // Nếu model này fail 405/404 trong tương lai, cần đổi sang model khác tại đây.
      var currentModel = postData.model || MODEL;
      Logger.log("[GAS Proxy] Received openrouter request. Model: " + currentModel + " | Messages: " + (postData.messages ? postData.messages.length : 0));

      var openRouterPayload = {
        model: currentModel,
        messages: postData.messages
      };

      var url = "https://openrouter.ai/api/v1/chat/completions";
      var start = new Date().getTime();
      var response;
      try {
        response = UrlFetchApp.fetch(url, {
          method: "POST",
          headers: {
            "Authorization": "Bearer " + openRouterKey,
            "Content-Type": "application/json",
            "HTTP-Referer": "https://github.com/ThinhNguyen2005/appCanLua",
            "X-Title": "App Can Lua"
          },
          payload: JSON.stringify(openRouterPayload),
          muteHttpExceptions: true
        });
      } catch (e) {
        Logger.log("[GAS Proxy] UrlFetch exception: " + e);
        return jsonResponse({
          error: {
            code: -1,
            message: "Không kết nối được OpenRouter: " + e.toString()
          }
        });
      }
      var end = new Date().getTime();
      var code = response.getResponseCode();
      var text = response.getContentText();
      Logger.log("[GAS Proxy] OpenRouter response code: " + code + " | time: " + (end - start) + "ms | bodyLen: " + text.length);

      if (code === 200) {
        // M-18: Tách chỉ phần cần thiết từ OpenRouter trước khi trả cho client.
        // Lý do: body OpenRouter đầy đủ chứa reasoning_details, usage, model, fingerprint...
        // dễ dài 5-15 KB. GAS echo endpoint đôi khi trả 405 khi response body lớn
        // (ghi nhận: AI trả lời dài 500 tokens → body ~12KB, GAS 405 với status thật
        // là 200 từ OpenRouter). Strip xuống còn ~1-2 KB để tránh bug này.
        try {
          var or = JSON.parse(text);
          var content = (or.choices && or.choices[0] && or.choices[0].message)
            ? or.choices[0].message.content
            : "";
          var finishReason = (or.choices && or.choices[0])
            ? (or.choices[0].finish_reason || or.choices[0].native_finish_reason || "stop")
            : "stop";
          return jsonResponse({
            ok: true,
            content: content || "",
            model: or.model || currentModel,
            finishReason: finishReason
          });
        } catch (e) {
          Logger.log("[GAS Proxy] 200 OK but body is not valid JSON: " + text.substring(0, 300));
          return jsonResponse({ error: "OpenRouter trả 200 nhưng body không phải JSON.", raw: text.substring(0, 500) });
        }
      }

      // M-17: Ghi log body lỗi để debug — rất quan trọng vì OpenRouter thường
      // trả 405/404 với error message chi tiết trong body. Trước đây code chỉ
      // log code + length nên không biết lý do thật.
      Logger.log("[GAS Proxy] Non-200 body from OpenRouter: " + text.substring(0, 500));

      var parsed;
      try { parsed = JSON.parse(text); } catch (e) { parsed = { error: { message: text.substring(0, 300) } }; }
      return jsonResponse({
        error: {
          code: code,
          message: "OpenRouter trả " + code + " cho model '" + currentModel + "': " + (parsed.error ? parsed.error.message : "no message"),
          hint: code === 405
            ? "Model 'openrouter/free' có thể tạm thời không route được provider nào. Thử lại sau vài phút."
            : (code === 404 ? "Model không tồn tại trên OpenRouter. Có thể cần đổi sang model khác." : null)
        }
      });
    }
    
    return jsonResponse({ error: "Action POST không được hỗ trợ." });
    
  } catch (error) {
    Logger.log("[GAS Proxy] Error in doPost: " + error.toString());
    return jsonResponse({ error: error.toString() });
  }
}

// ─── HÀM GỬI TELEGRAM DÙNG CHUNG ──────────────────────────────────────────
function sendTelegramMessage(message) {
  var scriptProperties = PropertiesService.getScriptProperties();
  var botToken = scriptProperties.getProperty('TELEGRAM_BOT_TOKEN');
  var chatId = scriptProperties.getProperty('TELEGRAM_ADMIN_CHAT_ID');
  
  if (!botToken || !chatId) {
    return { error: "Chưa cấu hình thông tin bot Telegram hoặc Admin Chat ID." };
  }
  
  var url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
  var payload = {
    chat_id: chatId,
    text: message,
    parse_mode: "HTML"
  };
  
  var response = UrlFetchApp.fetch(url, {
    method: "POST",
    contentType: "application/json",
    payload: JSON.stringify(payload),
    muteHttpExceptions: true
  });
  
  var code = response.getResponseCode();
  return { code: code, success: code === 200, response: JSON.parse(response.getContentText()) };
}

// ─── HÀM KIỂM TRA KẾT NỐI OPENROUTER (Chạy thủ công từ Editor để debug) ───────
// Test model `openrouter/free` xem còn hoạt động không.
// Nếu 405/404, cần đổi sang model khác tại biến MODEL.
function testOpenRouter() {
  var scriptProperties = PropertiesService.getScriptProperties();
  var openRouterKey = scriptProperties.getProperty('OPENROUTER_API_KEY');
  var handshakeToken = scriptProperties.getProperty('APP_HANDSHAKE_TOKEN');

  Logger.log("[GAS Test] APP_HANDSHAKE_TOKEN configured: " + (handshakeToken ? "YES (length: " + handshakeToken.length + ")" : "NO"));
  Logger.log("[GAS Test] OPENROUTER_API_KEY configured: " + (openRouterKey ? "YES (length: " + openRouterKey.length + ")" : "NO"));

  if (!openRouterKey) {
    Logger.log("[GAS Test] Lỗi: Chưa cấu hình OPENROUTER_API_KEY trong Script Properties.");
    return;
  }

  var payload = {
    model: MODEL,
    messages: [{ role: "user", content: "Xin chào" }]
  };

  var url = "https://openrouter.ai/api/v1/chat/completions";
  try {
    Logger.log("[GAS Test] Bắt đầu gọi OpenRouter với model: " + MODEL);
    var start = new Date().getTime();
    var response = UrlFetchApp.fetch(url, {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + openRouterKey,
        "Content-Type": "application/json",
        "HTTP-Referer": "https://github.com/ThinhNguyen2005/appCanLua",
        "X-Title": "App Can Lua"
      },
      payload: JSON.stringify(payload),
      muteHttpExceptions: true
    });
    var end = new Date().getTime();

    Logger.log("[GAS Test] Phản hồi từ OpenRouter:");
    Logger.log("- Mã trạng thái (Status Code): " + response.getResponseCode());
    Logger.log("- Thời gian thực thi (Response Time): " + (end - start) + "ms");
    Logger.log("- Nội dung phản hồi (Body): " + response.getContentText());
  } catch (e) {
    Logger.log("[GAS Test] Gặp lỗi ngoại lệ (Exception): " + e.toString());
  }
}
