/**
 * =====================================
 * CanLua News Crawler — Google Apps Script
 * =====================================
 * Mục đích: Cào bài báo nông nghiệp từ các RSS nguồn Việt Nam,
 *   phân loại theo topic, rồi đẩy lên Firestore collection "news_articles".
 *   App Android chỉ cần ĐỌC từ Firestore — không cần RSS, HTML parsing, hay Coil.
 *
 * Setup:
 *   1. Mở script.google.com → New project → Paste toàn bộ file này.
 *   2. Vào Project Settings → điền FIRESTORE_PROJECT_ID (lấy từ Firebase Console).
 *   3. Thêm OAuth scope trong appsscript.json:
 *      "oauthScopes": ["https://www.googleapis.com/auth/datastore", "https://www.googleapis.com/auth/script.external_request"]
 *   4. Chạy thử: Run → crawlAndPushNews → Authorize permissions → Kiểm tra Firestore.
 *   5. Thiết lập Trigger: Triggers (icon đồng hồ) → Add Trigger:
 *      Function: crawlAndPushNews | Event: Time-driven | Every 4 hours
 *
 * Schema Firestore "news_articles/{id}":
 *   id          String  (SHA-256 của link — dùng để dedup)
 *   title       String
 *   description String  (plain text, đã strip HTML, max 240 chars)
 *   link        String
 *   source      String  (tên nguồn hiển thị)
 *   thumbnail   String? (URL ảnh hoặc null)
 *   publishedAt Number  (Unix ms)
 *   topic       String  (RICE | GRAIN | WEATHER | MARKET)
 *   cachedAt    Number  (Unix ms — lúc GAS push lên)
 */

// ─── CẤU HÌNH ──────────────────────────────────────────────────────────────
var FIRESTORE_PROJECT_ID = "YOUR_FIREBASE_PROJECT_ID"; // ← Thay bằng Project ID của bạn
var COLLECTION = "news_articles";
var MAX_AGE_DAYS = 14;       // Bài cũ hơn 14 ngày sẽ bị bỏ qua
var MAX_ARTICLES_PER_RUN = 80; // Giới hạn để tránh timeout GAS 6 phút

// ─── DANH SÁCH NGUỒN RSS ───────────────────────────────────────────────────
var RSS_SOURCES = [
  {
    url: "https://news.google.com/rss/search?q=%22gi%C3%A1+l%C3%BAa%22+OR+%22thu+mua+l%C3%BAa%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Tin Lúa Gạo",
    defaultTopic: "RICE"
  },
  {
    url: "https://news.google.com/rss/search?q=%22n%C3%B4ng+nghi%E1%BB%87p%22+OR+%22n%C3%B4ng+d%C3%A2n%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Tin Nông Nghiệp",
    defaultTopic: "MARKET"
  },
  {
    url: "https://news.google.com/rss/search?q=%22xu%E1%BA%A5t+kh%E1%BA%A9u+g%E1%BA%A1o%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Xuất Khẩu Gạo",
    defaultTopic: "GRAIN"
  },
  {
    url: "https://news.google.com/rss/search?q=%22%C4%90BSCL%22+OR+%22%C4%91%E1%BB%93ng+b%E1%BA%B1ng+s%C3%B4ng+C%E1%BB%ADu+Long%22+n%C3%B4ng&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Tin ĐBSCL",
    defaultTopic: "MARKET"
  },
  {
    url: "https://vnexpress.net/rss/kinh-doanh.rss",
    displayName: "VnExpress Kinh Doanh",
    defaultTopic: "MARKET"
  },
  {
    url: "https://tuoitre.vn/rss/kinh-doanh.rss",
    displayName: "Tuổi Trẻ Kinh Doanh",
    defaultTopic: "MARKET"
  },
  {
    url: "https://danviet.vn/rss/nha-nong-552.rss",
    displayName: "Dân Việt Nhà Nông",
    defaultTopic: "RICE"
  },
  {
    url: "https://vov.vn/rss/kinh-te-83.rss",
    displayName: "VOV Kinh Tế",
    defaultTopic: "MARKET"
  }
];

// ─── ENTRY POINT ───────────────────────────────────────────────────────────
/**
 * Hàm chính — được trigger tự động mỗi 4 giờ.
 * Chạy tất cả RSS sources, dedup, classify topic, push lên Firestore.
 */
function crawlAndPushNews() {
  var now = new Date().getTime();
  var cutoffMs = now - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000);
  var allArticles = [];
  var seenIds = {};

  // ── Fetch song song từng nguồn (GAS không có true async, nhưng UrlFetchApp.fetchAll() thì có)
  var requests = RSS_SOURCES.map(function(src) {
    return { url: src.url, muteHttpExceptions: true };
  });

  var responses;
  try {
    responses = UrlFetchApp.fetchAll(requests); // Fetch song song, tiết kiệm thời gian
  } catch (e) {
    Logger.log("fetchAll error: " + e);
    return;
  }

  RSS_SOURCES.forEach(function(src, idx) {
    var resp = responses[idx];
    if (!resp || resp.getResponseCode() !== 200) {
      Logger.log("Source " + src.displayName + " failed: " + (resp ? resp.getResponseCode() : "null"));
      return;
    }
    var articles = parseRss(resp.getContentText(), src, now, cutoffMs);
    articles.forEach(function(a) {
      if (!seenIds[a.id]) {
        seenIds[a.id] = true;
        allArticles.push(a);
      }
    });
  });

  // Sắp xếp mới nhất trước, giới hạn số lượng để tránh timeout
  allArticles.sort(function(a, b) { return b.publishedAt - a.publishedAt; });
  var toWrite = allArticles.slice(0, MAX_ARTICLES_PER_RUN);

  Logger.log("Crawled " + allArticles.length + " unique articles, pushing " + toWrite.length + " to Firestore...");

  // Push lên Firestore dùng batch REST API
  var pushed = 0;
  var failed = 0;
  toWrite.forEach(function(article) {
    var ok = upsertToFirestore(article);
    if (ok) pushed++; else failed++;
  });

  Logger.log("Done. Pushed: " + pushed + " | Failed: " + failed);
}

// ─── RSS PARSER ────────────────────────────────────────────────────────────
/**
 * Parse XML RSS content và trả về list NewsArticle objects.
 * Sử dụng XmlService.parse() — chính xác hơn regex.
 */
function parseRss(xmlContent, src, now, cutoffMs) {
  var articles = [];
  try {
    var doc = XmlService.parse(xmlContent);
    var root = doc.getRootElement();

    // Hỗ trợ cả RSS 2.0 (<rss><channel><item>) và Atom (<feed><entry>)
    var ns = root.getNamespace();
    var items = [];

    // RSS 2.0
    var channels = root.getChildren("channel");
    if (channels.length > 0) {
      items = channels[0].getChildren("item");
    }
    // Atom feed
    if (items.length === 0) {
      items = root.getChildren("entry", ns);
    }

    items.forEach(function(item) {
      try {
        var article = parseItem(item, src, now, cutoffMs);
        if (article) articles.push(article);
      } catch (e) {
        // Skip malformed items silently
      }
    });
  } catch (e) {
    Logger.log("parseRss error for " + src.displayName + ": " + e);
  }
  return articles;
}

/**
 * Parse 1 <item> hoặc <entry> thành NewsArticle object.
 */
function parseItem(item, src, now, cutoffMs) {
  // Title
  var title = getText(item, "title");
  if (!title || title.trim() === "") return null;

  // Link — thử nhiều cách
  var link = getText(item, "link");
  if (!link || link.trim() === "") {
    // Atom: <link href="..."/>
    var linkEl = item.getChild("link");
    if (linkEl) link = linkEl.getAttribute("href") ? linkEl.getAttribute("href").getValue() : "";
  }
  if (!link || link.trim() === "") return null;

  // Dedupe ID = SHA-256-like hash. GAS không có crypto native, dùng
  // Utilities.computeDigest để tính MD5 (đủ để dedup)
  var idBytes = Utilities.computeDigest(Utilities.DigestAlgorithm.MD5, link);
  var id = idBytes.map(function(b) {
    return ("0" + (b & 0xFF).toString(16)).slice(-2);
  }).join("");

  // Publication date
  var pubDateStr = getText(item, "pubDate") || getText(item, "published") || getText(item, "updated");
  var publishedAt = pubDateStr ? new Date(pubDateStr).getTime() : now;
  if (isNaN(publishedAt)) publishedAt = now;
  if (publishedAt < cutoffMs) return null; // Bài quá cũ, bỏ qua

  // Description — strip HTML tags, giới hạn 240 ký tự
  var rawDesc = getText(item, "description") || getText(item, "summary") || "";
  var description = stripHtml(rawDesc, 240);

  // Thumbnail — thử media:thumbnail, enclosure, hoặc img trong description
  var thumbnail = extractThumbnail(item, rawDesc);

  // Topic classification
  var topic = classifyTopic(title + " " + description, src.defaultTopic);

  return {
    id: id,
    title: title.trim(),
    description: description,
    link: link.trim(),
    source: src.displayName,
    thumbnail: thumbnail,
    publishedAt: publishedAt,
    topic: topic,
    cachedAt: now
  };
}

// ─── HELPERS ───────────────────────────────────────────────────────────────

/** Lấy text content của child element theo tên. */
function getText(element, childName) {
  var child = element.getChild(childName);
  if (!child) {
    // Thử với namespaces thông dụng
    var mediaNs = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    child = element.getChild(childName, mediaNs);
  }
  return child ? child.getText() : "";
}

/** Strip HTML tags và cắt ngắn text. */
function stripHtml(html, maxChars) {
  if (!html) return "";
  // Xóa CDATA wrapper
  var text = html.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1");
  // Xóa tất cả HTML tags
  text = text.replace(/<[^>]+>/g, " ");
  // Decode HTML entities thông dụng
  text = text.replace(/&amp;/g, "&")
             .replace(/&lt;/g, "<")
             .replace(/&gt;/g, ">")
             .replace(/&quot;/g, '"')
             .replace(/&#39;/g, "'")
             .replace(/&nbsp;/g, " ");
  // Chuẩn hóa khoảng trắng
  text = text.replace(/\s+/g, " ").trim();
  // Cắt ngắn
  if (maxChars > 0 && text.length > maxChars) {
    text = text.substring(0, maxChars).replace(/\s+\S*$/, "") + "…";
  }
  return text;
}

/** Trích xuất URL ảnh thumbnail từ item RSS. */
function extractThumbnail(item, rawDesc) {
  // 1. <media:thumbnail url="..."/>
  try {
    var mediaNs = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    var thumb = item.getChild("thumbnail", mediaNs);
    if (thumb) {
      var url = thumb.getAttribute("url");
      if (url) return url.getValue();
    }
  } catch (e) {}

  // 2. <enclosure type="image/*" url="..."/>
  try {
    var enclosure = item.getChild("enclosure");
    if (enclosure) {
      var type = enclosure.getAttribute("type");
      var url = enclosure.getAttribute("url");
      if (type && url && type.getValue().startsWith("image")) {
        return url.getValue();
      }
    }
  } catch (e) {}

  // 3. <img src="..."> trong description CDATA
  if (rawDesc) {
    var imgMatch = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
    if (imgMatch && imgMatch[1] && imgMatch[1].startsWith("http")) {
      return imgMatch[1];
    }
  }

  return null;
}

/** Phân loại topic từ text — mirror logic từ NewsSource.kt của Android app. */
function classifyTopic(text, fallback) {
  var lower = text.toLowerCase();

  // Thời tiết / thiên tai
  if (/thời tiết|mưa|bão|nắng nóng|hạn hán|lũ|xâm nhập mặn|ngập mặn|triều cường/.test(lower)) {
    return "WEATHER";
  }
  // Xuất khẩu gạo
  if (/xuất khẩu|giá gạo|gạo xuất|gạo việt|nhập khẩu gạo|hợp đồng gạo|đơn hàng gạo/.test(lower)) {
    return "GRAIN";
  }
  // Lúa canh tác
  if (/giá lúa|thu mua lúa|lúa st25|lúa st24|vụ đông xuân|vụ hè thu|vụ thu đông|vụ mùa|trồng lúa|ruộng lúa|giống lúa/.test(lower)) {
    return "RICE";
  }
  // Thị trường nói chung
  if (/thị trường|nông sản|nông nghiệp|nông dân|đbscl|đồng bằng sông cửu long|hợp tác xã|khuyến nông|phân bón/.test(lower)) {
    return "MARKET";
  }

  return fallback;
}

// ─── FIRESTORE REST API ────────────────────────────────────────────────────
/**
 * Upsert 1 article lên Firestore dùng REST PATCH (create hoặc update).
 * Không cần Firebase SDK — dùng OAuth token từ GAS session.
 */
function upsertToFirestore(article) {
  var token = ScriptApp.getOAuthToken();
  var docPath = COLLECTION + "/" + article.id;
  var url = "https://firestore.googleapis.com/v1/projects/" + FIRESTORE_PROJECT_ID
    + "/databases/(default)/documents/" + docPath;

  var body = {
    fields: {
      id:          { stringValue: article.id },
      title:       { stringValue: article.title },
      description: { stringValue: article.description },
      link:        { stringValue: article.link },
      source:      { stringValue: article.source },
      thumbnail:   article.thumbnail
                    ? { stringValue: article.thumbnail }
                    : { nullValue: null },
      publishedAt: { integerValue: String(article.publishedAt) },
      topic:       { stringValue: article.topic },
      cachedAt:    { integerValue: String(article.cachedAt) }
    }
  };

  try {
    var resp = UrlFetchApp.fetch(url, {
      method: "PATCH",
      contentType: "application/json",
      headers: { "Authorization": "Bearer " + token },
      payload: JSON.stringify(body),
      muteHttpExceptions: true
    });
    var code = resp.getResponseCode();
    if (code === 200 || code === 201) return true;
    Logger.log("Firestore PATCH failed for " + article.id + ": " + code + " " + resp.getContentText().substring(0, 200));
    return false;
  } catch (e) {
    Logger.log("Firestore fetch error: " + e);
    return false;
  }
}

/**
 * Dọn bài báo cũ > 14 ngày khỏi Firestore.
 * Gọi thủ công hoặc thêm trigger "Weekly" riêng.
 */
function cleanOldArticles() {
  var token = ScriptApp.getOAuthToken();
  var cutoffMs = new Date().getTime() - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000);
  var url = "https://firestore.googleapis.com/v1/projects/" + FIRESTORE_PROJECT_ID
    + "/databases/(default)/documents:runQuery";

  // Query articles cũ hơn cutoff
  var query = {
    structuredQuery: {
      from: [{ collectionId: COLLECTION }],
      where: {
        fieldFilter: {
          field: { fieldPath: "publishedAt" },
          op: "LESS_THAN",
          value: { integerValue: String(cutoffMs) }
        }
      },
      limit: 200
    }
  };

  try {
    var resp = UrlFetchApp.fetch(url, {
      method: "POST",
      contentType: "application/json",
      headers: { "Authorization": "Bearer " + token },
      payload: JSON.stringify(query),
      muteHttpExceptions: true
    });
    var results = JSON.parse(resp.getContentText());
    var deleted = 0;
    results.forEach(function(r) {
      if (!r.document) return;
      var docUrl = "https://firestore.googleapis.com/v1/" + r.document.name;
      UrlFetchApp.fetch(docUrl, {
        method: "DELETE",
        headers: { "Authorization": "Bearer " + token },
        muteHttpExceptions: true
      });
      deleted++;
    });
    Logger.log("Cleaned " + deleted + " old articles from Firestore.");
  } catch (e) {
    Logger.log("cleanOldArticles error: " + e);
  }
}
