/**
 * =====================================
 * CanLua News Crawler — Google Apps Script v2.2 (Ultimate Hybrid)
 * =====================================
 * Cào bài báo nông nghiệp từ các RSS nguồn Việt Nam,
 * phân loại theo topic, trích xuất mô tả + ảnh từ trang gốc cho nguồn Google News,
 * lọc trùng qua Supabase trước khi Enrich để tiết kiệm hạn ngạch mạng & thời gian chạy.
 *
 * Setup:
 *   1. Mở script.google.com → New project → Paste toàn bộ file này.
 *   2. Điền SUPABASE_URL và SUPABASE_SERVICE_ROLE_KEY dưới cấu hình.
 *   3. Vào Project Settings (biểu tượng bánh răng) → Tích chọn "Show 'appsscript.json' manifest file in editor".
 *   4. Quay lại bộ soạn thảo (Editor) → Mở file `appsscript.json` và bổ sung cấu hình `oauthScopes` như sau:
 *      "oauthScopes": [
 *        "https://www.googleapis.com/auth/script.external_request",
 *        "https://www.googleapis.com/auth/script.scriptapp"
 *      ]
 *   5. Chọn hàm "setupTriggers" → Nhấn "Run" → Cấp quyền (Authorize) khi được hỏi.
 */

// ─── CẤU HÌNH ──────────────────────────────────────────────────────────────
var SUPABASE_URL = "https://wuongoybucznvfbuzzjh.supabase.co";
var SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind1b25nb3lidWN6bnZmYnV6empoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkyMDY4NjMsImV4cCI6MjA5NDc4Mjg2M30.laUhoFYWhf16p4D3Bqngf9xhPzI20YE_TNlG-W8CAMI"; // public anon key
var SUPABASE_GAS_KEY = "gas_sync_secret_68593"; // secret key for RLS bypass write
var TABLE = "news_articles";
var MAX_AGE_DAYS = 14;        // Bài cũ hơn 14 ngày sẽ bị bỏ qua
var MAX_ARTICLES_PER_RUN = 50; // Giới hạn số bài đẩy lên Supabase mỗi lần chạy

// ─── DANH SÁCH NGUỒN RSS ───────────────────────────────────────────────────
// Phân loại:
//   - PRIMARY: Báo trực tiếp — có mô tả + ảnh đầy đủ trong RSS
//   - SECONDARY: Google News search — cần enrich từ trang gốc
var RSS_SOURCES = [
  {
    url: "https://vnexpress.net/rss/kinh-doanh.rss",
    displayName: "VnExpress Kinh Doanh",
    defaultTopic: "MARKET",
    priority: "PRIMARY"
  },
  {
    url: "https://tuoitre.vn/rss/kinh-doanh.rss",
    displayName: "Tuổi Trẻ Kinh Doanh",
    defaultTopic: "MARKET",
    priority: "PRIMARY"
  },
  {
    url: "https://nongnghiepmoitruong.vn/nong-nghiep.rss",
    displayName: "Nông Nghiệp Môi Trường",
    defaultTopic: "RICE",
    priority: "PRIMARY"
  },
  {
    url: "https://vietnamnet.vn/rss/kinh-doanh.rss",
    displayName: "Vietnamnet Kinh Doanh",
    defaultTopic: "MARKET",
    priority: "PRIMARY"
  },
  {
    url: "https://danviet.vn/rss/nha-nong-552.rss",
    displayName: "Dân Việt Nhà Nông",
    defaultTopic: "RICE",
    priority: "PRIMARY"
  },
  {
    url: "https://cafef.vn/rss/kinh-doanh.rss",
    displayName: "CafeF Kinh Doanh",
    defaultTopic: "MARKET",
    priority: "PRIMARY"
  },
  {
    url: "https://nhandan.vn/rss/kinh-te.rss",
    displayName: "Nhân Dân Kinh Tế",
    defaultTopic: "MARKET",
    priority: "PRIMARY"
  },
  {
    url: "https://nongnghiep.vn/rss/home.rss",
    displayName: "Nông Nghiệp Việt Nam",
    defaultTopic: "RICE",
    priority: "PRIMARY"
  },
  {
    url: "https://news.google.com/rss/search?q=%22gi%C3%A1+l%C3%BAa%22+OR+%22thu+mua+l%C3%BAa%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Tin Lúa Gạo",
    defaultTopic: "RICE",
    priority: "SECONDARY"
  },
  {
    url: "https://news.google.com/rss/search?q=%22th%E1%BB%9Di+ti%E1%BA%BFt%22+%22l%C3%BAa%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Thời Tiết Nông Nghiệp",
    defaultTopic: "WEATHER",
    priority: "SECONDARY"
  },
  {
    url: "https://news.google.com/rss/search?q=%22xu%E1%BA%A5t+kh%E1%BA%A9u+g%E1%BA%A1o%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Xuất Khẩu Gạo",
    defaultTopic: "GRAIN",
    priority: "SECONDARY"
  },
  {
    url: "https://news.google.com/rss/search?q=%22%C4%90BSCL%22+OR+%22%C4%91%E1%BB%93ng+b%E1%BA%B1ng+s%C3%B4ng+C%E1%BB%ADu+Long%22&hl=vi&gl=VN&ceid=VN:vi",
    displayName: "Tin ĐBSCL",
    defaultTopic: "MARKET",
    priority: "SECONDARY"
  }
];

// ─── ENTRY POINT ───────────────────────────────────────────────────────────
function crawlAndPushNews() {
  var now = new Date().getTime();
  var cutoffMs = now - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000);
  var allArticles = [];
  var seenIds = {};

  // ── Bước 1: Lấy danh sách id đã có trong Supabase (L3 dedup)
  var existingIds = fetchExistingIds();
  Logger.log("Existing articles in Supabase: " + Object.keys(existingIds).length);

  // ── Bước 2: Fetch tất cả RSS song song
  var requests = RSS_SOURCES.map(function(src) {
    return { url: src.url, muteHttpExceptions: true };
  });
  var responses;
  try {
    responses = UrlFetchApp.fetchAll(requests);
  } catch (e) {
    Logger.log("fetchAll error: " + e);
    return;
  }

  // ── Bước 3: Parse + lọc trùng sơ bộ trong RAM (L1 + L2)
  var successCount = 0;
  var failCount = 0;
  RSS_SOURCES.forEach(function(src, idx) {
    var resp = responses[idx];
    if (!resp || resp.getResponseCode() !== 200) {
      Logger.log("Source " + src.displayName + " failed: " + (resp ? resp.getResponseCode() : "null"));
      failCount++;
      return;
    }
    successCount++;
    var articles = parseRss(resp.getContentText(), src, now, cutoffMs);
    articles.forEach(function(a) {
      if (!seenIds[a.id]) {
        seenIds[a.id] = true;
        allArticles.push(a);
      }
    });
  });

  Logger.log("Sources OK: " + successCount + " | Failed: " + failCount);
  Logger.log("Total unique articles in RAM: " + allArticles.length);

  // ── Bước 4: Lọc bỏ bài đã có sẵn trong Supabase trước khi Enrich ─────────
  var newArticles = allArticles.filter(function(a) {
    return !existingIds[a.id];
  });

  newArticles.sort(function(a, b) { return b.publishedAt - a.publishedAt; });
  var toWrite = newArticles.slice(0, MAX_ARTICLES_PER_RUN);

  Logger.log("New articles to write: " + toWrite.length);

  // ── Bước 5: Enrich descriptions + thumbnails cho SECONDARY sources thực sự mới ─────────
  var secondaryToEnrich = toWrite.filter(function(a) { return a.needsEnrich; });
  Logger.log("Articles needing decode & enrich (SECONDARY & NEW): " + secondaryToEnrich.length);

  // Giải mã link Google News trước
  secondaryToEnrich.forEach(function(article) {
    var decodedUrl = decodeGoogleNewsUrl(article.link);
    if (decodedUrl && decodedUrl !== article.link) {
      Logger.log("Decoded Google News URL: " + article.title + " -> " + decodedUrl);
      article.link = decodedUrl;
      article.id = computeMd5(decodedUrl);
    }
  });

  // Lọc lại toWrite để loại bỏ bài sau khi giải mã có ID trùng với bài đã tồn tại trong Supabase
  toWrite = toWrite.filter(function(article) {
    if (existingIds[article.id]) {
      Logger.log("Skipped duplicate article after decode: " + article.title);
      return false;
    }
    return true;
  });

  // Lấy lại danh sách cần enrich thực sự (chỉ những bài không bị loại bỏ)
  var secondaryToEnrichFiltered = toWrite.filter(function(a) { return a.needsEnrich; });

  // Enrich song song theo batch 5 sử dụng link đã giải mã
  for (var i = 0; i < secondaryToEnrichFiltered.length; i += 5) {
    var batch = secondaryToEnrichFiltered.slice(i, i + 5);
    var enrichRequests = batch.map(function(a) {
      return { url: a.link, muteHttpExceptions: true, followRedirects: true };
    });
    try {
      var enrichResponses = UrlFetchApp.fetchAll(enrichRequests);
      batch.forEach(function(article, j) {
        var enriched = enrichArticleFromPage(article, enrichResponses[j]);
        article.description = enriched.description;
        article.thumbnail = enriched.thumbnail;
      });
    } catch (e) {
      Logger.log("Enrich batch error at " + i + ": " + e);
    }
  }

  // ── Bước 6: Chỉ push bài thực sự mới lên Supabase ─────────────────────
  var pushed = 0;
  var failed = 0;
  var skipped = 0;
  toWrite.forEach(function(article) {
    // Lớp bảo vệ cuối: đảm bảo description hợp lệ trước khi ghi DB
    var sanitized = sanitizeArticleForSupabase(article);
    if (sanitized.dropped) {
      Logger.log("Skipped article (no usable description): " + article.title);
      skipped++;
      return;
    }
    article.description = sanitized.description;
    var ok = insertToSupabase(article);
    if (ok) pushed++; else failed++;
  });
  Logger.log("Done. Pushed: " + pushed + " | Failed: " + failed + " | Skipped: " + skipped);
}

// ─── SUPABASE: LẤY DANH SÁCH ID ĐÃ CÓ (L3) ───────────────────────────────
function fetchExistingIds() {
  var url = SUPABASE_URL + "/rest/v1/" + TABLE + "?select=id";
  var options = {
    method: "get",
    headers: {
      'apikey': SUPABASE_ANON_KEY,
      'Authorization': 'Bearer ' + SUPABASE_ANON_KEY
    },
    muteHttpExceptions: true
  };
  var existing = {};
  try {
    var resp = UrlFetchApp.fetch(url, options);
    var results = JSON.parse(resp.getContentText());
    if (Array.isArray(results)) {
      results.forEach(function(r) {
        if (r.id) {
          existing[r.id] = true;
        }
      });
    }
  } catch (e) {
    Logger.log("fetchExistingIds error: " + e);
  }
  return existing;
}

// ─── RSS PARSER ────────────────────────────────────────────────────────────
/** Parse XML RSS content, tự động fallback sang Regex parser nếu XML bị lỗi cấu trúc. */
function parseRss(xmlContent, src, now, cutoffMs) {
  try {
    var doc = XmlService.parse(xmlContent);
    var root = doc.getRootElement();
    var ns = root.getNamespace();
    var items = [];

    var channels = root.getChildren("channel");
    if (channels.length > 0) {
      items = channels[0].getChildren("item");
    }
    if (items.length === 0) {
      items = root.getChildren("entry", ns);
    }

    var itemsToParse = items.slice(0, 20);
    var articles = [];

    itemsToParse.forEach(function(item) {
      try {
        var article = parseItem(item, src, now, cutoffMs);
        if (article) articles.push(article);
      } catch (e) {
        // Bỏ qua lỗi từng bài
      }
    });
    return articles;
  } catch (e) {
    // FALLBACK: Nếu báo trả về XML lỗi (như Dân Việt, Nông Nghiệp VN), dùng Regex để parse
    Logger.log("XML parse failed for " + src.displayName + " (" + e.message + "). Falling back to Regex parser...");
    return parseRssRegex(xmlContent, src, now, cutoffMs);
  }
}

function parseItem(item, src, now, cutoffMs) {
  var title = getText(item, "title");
  if (!title || title.trim() === "") return null;

  var link = getText(item, "link");
  if (!link || link.trim() === "") {
    var linkEl = item.getChild("link");
    if (linkEl) {
      var hrefAttr = linkEl.getAttribute("href");
      if (hrefAttr) link = hrefAttr.getValue();
    }
  }
  if (!link || link.trim() === "") return null;
  link = link.trim();

  // ID = MD5 của link
  var id = computeMd5(link);

  var pubDateStr = getText(item, "pubDate") || getText(item, "published") || getText(item, "updated");
  var publishedAt = pubDateStr ? new Date(pubDateStr).getTime() : now;
  if (isNaN(publishedAt)) publishedAt = now;
  if (publishedAt < cutoffMs) return null;

  var rawDesc = getText(item, "description")
    || getText(item, "summary")
    || getText(item, "content:encoded")
    || getText(item, "content")
    || "";
  var description = stripHtml(rawDesc, 300);
  description = cleanDescription(description, title);

  var thumbnail = extractThumbnail(item, rawDesc);
  var topic = classifyTopic(title + " " + description, src.defaultTopic);
  var needsEnrich = (src.priority === "SECONDARY");

  return {
    id: id,
    title: title.trim(),
    description: description,
    link: link,
    source: src.displayName,
    thumbnail: thumbnail,
    publishedAt: publishedAt,
    topic: topic,
    cachedAt: now,
    needsEnrich: needsEnrich
  };
}

/** Regex-based Parser - cứu cánh khi XML của trang báo bị lỗi syntax */
function parseRssRegex(xmlContent, src, now, cutoffMs) {
  var articles = [];
  try {
    var items = xmlContent.match(/<(item|entry)>[\s\S]*?<\/\1>/gi);
    if (!items) return articles;

    var itemsToParse = items.slice(0, 20);

    itemsToParse.forEach(function(itemXml) {
      try {
        var titleM = itemXml.match(/<title[^>]*>([\s\S]*?)<\/title>/i);
        var title = titleM ? titleM[1] : "";
        title = stripCdataAndHtml(title);
        if (!title || title.trim() === "") return;

        var linkM = itemXml.match(/<link[^>]*>([\s\S]*?)<\/link>/i);
        var link = "";
        if (linkM) {
          link = linkM[1].trim();
          if (link.indexOf("<") === 0 || link === "") {
            var hrefM = itemXml.match(/<link[^>]+href=["']([^"']+)["']/i);
            if (hrefM) link = hrefM[1];
          }
        }
        if (!link || link.trim() === "") return;
        link = link.trim();

        var id = computeMd5(link);

        var pubM = itemXml.match(/<(pubDate|published|updated)[^>]*>([\s\S]*?)<\/\1>/i);
        var pubDateStr = pubM ? pubM[2] : "";
        var publishedAt = pubDateStr ? new Date(pubDateStr).getTime() : now;
        if (isNaN(publishedAt)) publishedAt = now;
        if (publishedAt < cutoffMs) return;

        var descM = itemXml.match(/<(description|summary|content|content:encoded)[^>]*>([\s\S]*?)<\/\1>/i);
        var rawDesc = descM ? descM[2] : "";
        var description = stripHtml(rawDesc, 300);
        description = cleanDescription(description, title);

        var thumbnail = null;
        var mediaM = itemXml.match(/<(media:content|media:thumbnail|enclosure)[^>]+url=["']([^"']+)["']/i);
        if (mediaM) {
          thumbnail = ensureHttps(mediaM[2]);
        }
        if (!thumbnail && rawDesc) {
          var imgM = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
          if (imgM) thumbnail = ensureHttps(imgM[1]);
        }

        var topic = classifyTopic(title + " " + description, src.defaultTopic);
        var needsEnrich = (src.priority === "SECONDARY");

        articles.push({
          id: id,
          title: title.trim(),
          description: description,
          link: link,
          source: src.displayName,
          thumbnail: thumbnail,
          publishedAt: publishedAt,
          topic: topic,
          cachedAt: now,
          needsEnrich: needsEnrich
        });
      } catch (e) {}
    });
  } catch (e) {
    Logger.log("parseRssRegex error: " + e);
  }
  return articles;
}

function stripCdataAndHtml(str) {
  if (!str) return "";
  var text = str.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1");
  return text.replace(/<[^>]+>/g, " ").trim();
}

// ─── HELPERS ───────────────────────────────────────────────────────────────
function getText(element, childName) {
  var child = element.getChild(childName);
  if (!child) {
    var mediaNs = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    child = element.getChild(childName, mediaNs);
    if (!child) {
      var contentNs = XmlService.getNamespace("content", "http://purl.org/rss/1.0/modules/content/");
      child = element.getChild(childName, contentNs);
    }
  }
  return child ? child.getText() : "";
}

function stripHtml(html, maxChars) {
  if (!html) return "";
  var text = html.replace(/<!\[CDATA\[([\s\S]*?)\]\]>/g, "$1");
  text = text.replace(/<[^>]+>/g, " ");
  text = text.replace(/&amp;/g, "&")
             .replace(/&lt;/g, "<")
             .replace(/&gt;/g, ">")
             .replace(/&quot;/g, '"')
             .replace(/&#39;/g, "'")
             .replace(/&nbsp;/g, " ")
             .replace(/&mdash;/g, "—")
             .replace(/&ndash;/g, "–")
             .replace(/&hellip;/g, "…")
             .replace(/&#(\d+);/g, function(m, code) { return String.fromCharCode(parseInt(code, 10)); });
  text = text.replace(/[\r\n\t]+/g, " ").replace(/\s{2,}/g, " ").trim();
  if (maxChars > 0 && text.length > maxChars) {
    text = text.substring(0, maxChars).replace(/\s+\S*$/, "") + "…";
  }
  return text;
}

function extractThumbnail(item, rawDesc) {
  try {
    var mediaNs = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    var mediaContent = item.getChild("content", mediaNs);
    if (mediaContent) {
      var url = mediaContent.getAttribute("url");
      if (url) return ensureHttps(url.getValue());
    }
  } catch (e) {}

  try {
    var mediaNs = XmlService.getNamespace("media", "http://search.yahoo.com/mrss/");
    var thumb = item.getChild("thumbnail", mediaNs);
    if (thumb) {
      var url = thumb.getAttribute("url");
      if (url) return ensureHttps(url.getValue());
    }
  } catch (e) {}

  try {
    var enclosure = item.getChild("enclosure");
    if (enclosure) {
      var type = enclosure.getAttribute("type");
      var url = enclosure.getAttribute("url");
      if (type && url && type.getValue().toLowerCase().indexOf("image") !== -1) {
        return ensureHttps(url.getValue());
      }
    }
  } catch (e) {}

  if (rawDesc) {
    var imgMatch = rawDesc.match(/<img[^>]+src=["']([^"']+)["']/i);
    if (imgMatch && imgMatch[1]) {
      return ensureHttps(imgMatch[1]);
    }
  }

  return null;
}

function enrichArticleFromPage(article, resp) {
  var result = {
    description: article.description,
    thumbnail: article.thumbnail
  };

  if (!resp || resp.getResponseCode() !== 200) return result;

  try {
    var html = resp.getContentText();

    // ── 1. Trích xuất og:image ────────────────────────────────
    if (!result.thumbnail) {
      var ogMatch = html.match(/<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']/i);
      if (ogMatch && ogMatch[1]) {
        result.thumbnail = ensureHttps(ogMatch[1]);
      }
      if (!result.thumbnail) {
        var ogMatch2 = html.match(/<meta[^>]+content=["']([^"']+)["'][^>]+property=["']og:image["']/i);
        if (ogMatch2 && ogMatch2[1]) {
          result.thumbnail = ensureHttps(ogMatch2[1]);
        }
      }
      if (!result.thumbnail) {
        var twitterMatch = html.match(/<meta[^>]+name=["']twitter:image["'][^>]+content=["']([^"']+)["']/i);
        if (twitterMatch && twitterMatch[1]) {
          result.thumbnail = ensureHttps(twitterMatch[1]);
        }
      }
      if (!result.thumbnail) {
        var imgMatches = html.match(/<img[^>]+src=["']([^"']+)["']/gi);
        if (imgMatches) {
          for (var i = 0; i < imgMatches.length; i++) {
            var srcMatch = imgMatches[i].match(/src=["']([^"']+)["']/i);
            if (srcMatch && srcMatch[1] && srcMatch[1].length > 50) {
              var url = ensureHttps(srcMatch[1]);
              if (url && !url.match(/\.(gif|png|svg|ico|banner|ads?|tracking|pixel|logo)/i)) {
                result.thumbnail = url;
                break;
              }
            }
          }
        }
      }
    }

    // ── 2. Trích xuất description từ meta tag ─────────────────
    if (!result.description || result.description.length < 30) {
      var descMatch = html.match(/<meta[^>]+name=["']description["'][^>]+content=["']([^"']+)["']/i);
      if (descMatch && descMatch[1]) {
        var metaDesc = stripHtml(descMatch[1], 300).trim();
        if (metaDesc.length > 20) {
          result.description = metaDesc;
        }
      }
      if (!result.description || result.description.length < 30) {
        var ogDescMatch = html.match(/<meta[^>]+property=["']og:description["'][^>]+content=["']([^"']+)["']/i);
        if (ogDescMatch && ogDescMatch[1]) {
          var ogDesc = stripHtml(ogDescMatch[1], 300).trim();
          if (ogDesc.length > 20) {
            result.description = ogDesc;
          }
        }
      }
    }

    // ── 2b. Loại bỏ Google News boilerplate nếu meta description cũng chứa nó ─
    if (result.description) {
      result.description = cleanDescription(result.description, article.title);
    }

    // ── 3. Trích xuất đoạn văn đầu tiên từ article body ──────
    if (!result.description || result.description.length < 50) {
      var bodyMatch = html.match(/<article[^>]*>([\s\S]{200,2000})<\/article>/i);
      if (bodyMatch) {
        var paraMatch = bodyMatch[1].match(/<p[^>]*>([\s\S]{50,500})<\/p>/i);
        if (paraMatch) {
          var extracted = stripHtml(paraMatch[1], 300).trim();
          if (extracted.length > 40) {
            result.description = extracted;
          }
        }
      }
    }

    // ── 4. Fallback cuối cùng: tạo description từ title + source ─
    if (!result.description || result.description.length < 30) {
      result.description = generateFallbackDescription(article.title, article.source, article.topic);
    }

  } catch (e) {
    Logger.log("Enrich error for " + article.link + ": " + e);
  }

  return result;
}

/**
 * Tạo description dự phòng khi cả RSS lẫn trang gốc đều không cung cấp được mô tả hữu ích.
 * Ưu tiên lấy phần "tóm tắt ngắn" nằm sau dấu ":" trong tiêu đề — đây là pattern phổ biến của báo VN.
 * @param {string} title   - Tiêu đề bài viết.
 * @param {string} source  - Tên nguồn (Vd: "Tạp chí Doanh nghiệp và Hội nhập").
 * @param {string} topic   - Chủ đề (RICE / GRAIN / WEATHER / MARKET).
 * @returns {string} Description rỗng nếu không tạo được gì hữu ích.
 */
function generateFallbackDescription(title, source, topic) {
  if (!title) return "";

  // Pattern phổ biến: "Giá lúa gạo hôm nay 9/6/2026: Giá lúa gạo tiếp tục ổn định, FAO ghi nhận ..."
  // -> lấy phần sau dấu ":" đầu tiên làm summary
  var colonIdx = title.indexOf(":");
  if (colonIdx > 0 && colonIdx < title.length - 10) {
    var afterColon = title.substring(colonIdx + 1).trim();
    if (afterColon.length >= 30) {
      // Bỏ tiền tố "Giá lúa gạo hôm nay DD/MM/YYYY" nếu lặp lại
      afterColon = afterColon.replace(/^(giá lúa gạo hôm nay|giá lúa hôm nay|giá gạo hôm nay|cập nhật|tin mới)\s*[:\-—]?\s*/i, "").trim();
      if (afterColon.length >= 25) {
        return capitalizeFirst(afterColon) + ".";
      }
    }
  }

  // Fallback 2: dựng từ topic + source
  var topicLabel = {
    "RICE":    "tin tức về lúa",
    "GRAIN":   "tin tức về gạo/xuất khẩu",
    "WEATHER": "tin tức thời tiết nông nghiệp",
    "MARKET":  "tin tức thị trường nông sản"
  }[topic] || "tin tức nông nghiệp";

  if (source) {
    return "Bài viết từ " + source + " về " + topicLabel + ".";
  }
  return "Cập nhật " + topicLabel + ".";
}

function capitalizeFirst(str) {
  if (!str) return "";
  return str.charAt(0).toUpperCase() + str.substring(1);
}

function ensureHttps(url) {
  if (!url) return null;
  var trimmed = url.trim();
  var lower = trimmed.toLowerCase();
  if (lower.indexOf("data:") === 0) return null;
  if (lower.indexOf("javascript:") === 0) return null;
  if (lower.indexOf("blob:") === 0) return null;
  if (lower.indexOf("https://") === 0) return trimmed;
  if (lower.indexOf("http://") === 0) return "https://" + trimmed.substring(7);
  if (trimmed.indexOf("//") === 0) return "https:" + trimmed;
  return trimmed;
}

/**
 * Chuẩn hóa description cho bài viết:
 *  - Bóc tách HTML entities + CDATA + tags đã làm ở stripHtml trước đó.
 *  - Phát hiện & loại bỏ chuỗi boilerplate của Google News (luôn xuất hiện trong RSS Google News).
 *  - Loại bỏ các mẫu boilerplate khác hay xuất hiện (link rút gọn, "Xem thêm:", tên nguồn lặp lại,...).
 *  - Nếu description chỉ chứa phần tiêu đề (trùng lặp / lặp lại) -> trả về "" để kích hoạt enrich từ trang gốc.
 */
function cleanDescription(description, title) {
  if (!description) return "";

  var cleanDesc = description
    .replace(/Comprehensive up-to-date news coverage, aggregated from sources all over the world by Google News\.?/gi, "")
    .replace(/Comprehensive up-to-date news coverage\.?/gi, "")
    .replace(/To read this and other full stories, subscribe to the[\s\S]*?newsletter\.?/gi, "")
    .replace(/Subscribe to our[\s\S]{0,80}newsletter\.?/gi, "")
    .replace(/Xem thêm[:\s]*/gi, " ")
    .replace(/Đọc thêm[:\s]*/gi, " ")
    .replace(/The post[\s\S]{0,150}appeared first on[\s\S]*?\./gi, "")
    .replace(/Source link[\s\S]{0,80}/gi, "")
    .replace(/&nbsp;/g, " ")
    .replace(/&hellip;/g, "…")
    .replace(/\.{3,}/g, "…")
    .replace(/[\s\u00A0]{2,}/g, " ")
    .trim();

  // Cắt dấu phân cách đầu/cuối (chỉ còn "- " hoặc "—" mà description bị cắt ngang)
  cleanDesc = cleanDesc.replace(/^[\s\-\—\|\:]+/, "").replace(/[\s\-\—\|\:]+$/, "").trim();

  if (!cleanDesc) return "";
  if (!title) return cleanDesc;

  var cleanTitle = title.trim();

  var normDesc = normalizeVietnamese(cleanDesc.toLowerCase());
  var normTitle = normalizeVietnamese(cleanTitle.toLowerCase());

  if (normDesc === normTitle) {
    return "";
  }

  if (normDesc.indexOf(normTitle) === 0) {
    var extraChars = normDesc.length - normTitle.length;
    if (extraChars < 60) {
      return "";
    }
    // Bỏ phần title lặp lại ở đầu, lấy phần còn lại
    cleanDesc = cleanDesc.substring(cleanTitle.length).replace(/^[\s\-\—\|\:]+/, "").trim();
    normDesc = normalizeVietnamese(cleanDesc.toLowerCase());
    if (normDesc.length < 30) {
      return cleanDesc;
    }
  }

  if (cleanDesc.length < 25 && normDesc !== normTitle) {
    return cleanDesc;
  }

  return cleanDesc;
}

function normalizeVietnamese(str) {
  if (!str) return "";
  var normalized = str
    .replace(/[àáảãạăằắẳẵặâầấẩẫậ]/g, "a")
    .replace(/[èéẻẽẹêềếểễệ]/g, "e")
    .replace(/[ìíỉĩị]/g, "i")
    .replace(/[òóỏõọôồốổỗộơờớởỡợ]/g, "o")
    .replace(/[ùúủũụưừứửữự]/g, "u")
    .replace(/[ỳýỷỹỵ]/g, "y")
    .replace(/[đ]/g, "d")
    .replace(/[^a-z0-9\s]/g, "")
    .replace(/\s+/g, " ").trim();
  return normalized;
}

function classifyTopic(text, fallback) {
  if (!text) return fallback;
  var lower = text.toLowerCase();
  if (/thời tiết|mưa|bão|nắng nóng|hạn hán|lũ|xâm nhập mặn|ngập mặn|triều cường|dự báo thời tiết|thiên tai|lũ lụt/.test(lower)) {
    return "WEATHER";
  }
  if (/xuất khẩu|giá gạo|gạo xuất|gạo việt|nhập khẩu gạo|hợp đồng gạo|đơn hàng gạo|ph Philippines|indica|jasmine|om 5451|st 21|st 24/.test(lower)) {
    return "GRAIN";
  }
  if (/giá lúa|thu mua lúa|lúa st25|lúa st24|vụ đông xuân|vụ hè thu|vụ thu đông|vụ mùa|trồng lúa|ruộng lúa|giống lúa|cấy lúa|gặt lúa|nefarious|lúa|cây lúa/.test(lower)) {
    return "RICE";
  }
  if (/thị trường|nông sản|nông nghiệp|nông dân|đbscl|đồng bằng sông cửu long|hợp tác xã|khuyến nông|phân bón|giống cây|máy nông nghiệp|bảo hiểm nông nghiệp/.test(lower)) {
    return "MARKET";
  }
  return fallback;
}

// ─── SUPABASE REST API ────────────────────────────────────────────────────
function insertToSupabase(article) {
  var url = SUPABASE_URL + "/rest/v1/" + TABLE;
  var payload = {
    id:          article.id,
    title:       article.title,
    description: article.description || "",
    link:        article.link,
    source:      article.source,
    thumbnail:   article.thumbnail,
    published_at: article.publishedAt,
    topic:       article.topic,
    cached_at:    article.cachedAt
  };

  try {
    var resp = UrlFetchApp.fetch(url, {
      method:             "POST",
      contentType:        "application/json",
      headers:            {
        'apikey': SUPABASE_ANON_KEY,
        'Authorization': 'Bearer ' + SUPABASE_ANON_KEY,
        'x-gas-key': SUPABASE_GAS_KEY,
        'Prefer': 'resolution=merge-duplicates'
      },
      payload:            JSON.stringify(payload),
      muteHttpExceptions: true
    });
    var code = resp.getResponseCode();
    if (code === 200 || code === 201 || code === 204) return true;
    Logger.log("Supabase INSERT failed for " + article.id + ": " + code + " - " + resp.getContentText());
    return false;
  } catch (e) {
    Logger.log("Supabase fetch error: " + e);
    return false;
  }
}

/**
 * Sanitize lần cuối trước khi ghi Supabase — đảm bảo tuyệt đối không có
 * description rỗng / chỉ là boilerplate Google News / quá ngắn.
 * Trả về object { description, dropped } — dropped = true nếu bài này nên
 * được bỏ qua hoàn toàn vì description vẫn không thể tạo được.
 */
function sanitizeArticleForSupabase(article) {
  var desc = (article.description || "").trim();

  // Phát hiện & loại bỏ boilerplate Google News (trong trường hợp còn sót)
  var stripped = desc.replace(/Comprehensive up-to-date news coverage, aggregated from sources all over the world by Google News\.?/gi, "")
                     .replace(/Comprehensive up-to-date news coverage\.?/gi, "")
                     .trim();
  if (stripped.length === 0) desc = "";

  // Nếu description quá ngắn hoặc vẫn rỗng -> tạo fallback từ title + source
  if (desc.length < 30) {
    desc = generateFallbackDescription(article.title, article.source, article.topic);
  }

  // Bỏ qua bài viết nếu ngay cả fallback cũng không tạo được
  // (bài Google News không có title, hoặc title không có nội dung hữu ích)
  if (!desc || desc.length < 10) {
    return { description: "", dropped: true };
  }

  return { description: desc, dropped: false };
}

function cleanOldArticles() {
  var cutoffMs = new Date().getTime() - (MAX_AGE_DAYS * 24 * 60 * 60 * 1000);
  var url = SUPABASE_URL + "/rest/v1/" + TABLE + "?published_at=lt." + cutoffMs;
  var options = {
    method: "delete",
    headers: {
      'apikey': SUPABASE_ANON_KEY,
      'Authorization': 'Bearer ' + SUPABASE_ANON_KEY,
      'x-gas-key': SUPABASE_GAS_KEY
    },
    muteHttpExceptions: true
  };
  try {
    var resp = UrlFetchApp.fetch(url, options);
    Logger.log("Cleaned old articles. Response: " + resp.getResponseCode());
  } catch (e) {
    Logger.log("cleanOldArticles error: " + e);
  }
}

// ─── TRIGGER SETUP ─────────────────────────────────────────────────────────
function setupTriggers() {
  ScriptApp.getProjectTriggers().forEach(function(t) { ScriptApp.deleteTrigger(t); });

  ScriptApp.newTrigger("crawlAndPushNews")
    .timeBased().everyHours(4).create();

  ScriptApp.newTrigger("cleanOldArticles")
    .timeBased().onWeekDay(ScriptApp.WeekDay.SUNDAY).atHour(2).create();

  Logger.log("✅ Triggers đã được tạo:");
  Logger.log("   • crawlAndPushNews  → mỗi 4 giờ");
}

// ─── HELPERS: GOOGLE NEWS DECODER & MD5 ────────────────────────────────────
function computeMd5(str) {
  var idBytes = Utilities.computeDigest(Utilities.DigestAlgorithm.MD5, str);
  return idBytes.map(function(b) {
    return ("0" + (b & 0xFF).toString(16)).slice(-2);
  }).join("");
}

function decodeGoogleNewsUrl(sourceUrl) {
  try {
    var parts = sourceUrl.split("/");
    var lastPart = parts[parts.length - 1];
    var base64 = lastPart.split("?")[0];
    
    if (sourceUrl.indexOf("news.google.com") === -1 || base64.length < 50) {
      return sourceUrl;
    }

    // Bước 1: Lấy signature và timestamp
    var pageUrl = "https://news.google.com/articles/" + base64;
    var response = UrlFetchApp.fetch(pageUrl, {
      muteHttpExceptions: true,
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
        "Cookie": "CONSENT=YES+shp.gws.20211108-0-0.vi+FX+999; SOCS=MCU.vi.VN+CS.2a3b4c"
      }
    });
    
    var code = response.getResponseCode();
    var html = response.getContentText();
    Logger.log("[GAS Decode] Fetch articles pageUrl: " + pageUrl + " | Code: " + code + " | HTML Length: " + html.length);

    var sgMatch = html.match(/data-n-a-sg=["']([^"']+)["']/i);
    var tsMatch = html.match(/data-n-a-ts=["']([^"']+)["']/i);
    
    var signature = sgMatch ? sgMatch[1] : null;
    var timestamp = tsMatch ? tsMatch[1] : null;
    
    if (!signature || !timestamp) {
      Logger.log("[GAS Decode] Fallback to RSS articles URL...");
      pageUrl = "https://news.google.com/rss/articles/" + base64;
      response = UrlFetchApp.fetch(pageUrl, {
        muteHttpExceptions: true,
        headers: {
          "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36",
          "Cookie": "CONSENT=YES+shp.gws.20211108-0-0.vi+FX+999; SOCS=MCU.vi.VN+CS.2a3b4c"
        }
      });
      code = response.getResponseCode();
      html = response.getContentText();
      Logger.log("[GAS Decode] Fetch RSS pageUrl: " + pageUrl + " | Code: " + code + " | HTML Length: " + html.length);
      
      sgMatch = html.match(/data-n-a-sg=["']([^"']+)["']/i);
      tsMatch = html.match(/data-n-a-ts=["']([^"']+)["']/i);
      signature = sgMatch ? sgMatch[1] : null;
      timestamp = tsMatch ? tsMatch[1] : null;
    }
    
    Logger.log("[GAS Decode] Extracted signature: " + signature + " | timestamp: " + timestamp);
    
    if (!signature || !timestamp) {
      return sourceUrl;
    }

    // Bước 2: batchexecute
    var rpcUrl = "https://news.google.com/_/DotsSplashUi/data/batchexecute";
    var payload = ["Fbv4je", '["garturlreq",[["X","X",["X","X"],null,null,1,1,"US:en",null,1,null,null,null,null,null,0,1],"X","X",1,[1,1,1],1,1,null,0,0,null,0],"' + base64 + '",' + timestamp + ',"' + signature + '"]'];
    var fReq = [[payload]];
    
    var options = {
      method: "POST",
      contentType: "application/x-www-form-urlencoded;charset=UTF-8",
      payload: "f.req=" + encodeURIComponent(JSON.stringify(fReq)),
      muteHttpExceptions: true,
      headers: {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36"
      }
    };
    
    var rpcResponse = UrlFetchApp.fetch(rpcUrl, options);
    var rpcCode = rpcResponse.getResponseCode();
    Logger.log("[GAS Decode] batchexecute response code: " + rpcCode);
    if (rpcCode !== 200) {
      return sourceUrl;
    }
    
    var text = rpcResponse.getContentText();
    Logger.log("[GAS Decode] batchexecute response text length: " + text.length);
    var lines = text.split("\n\n");
    if (lines.length < 2) {
      Logger.log("[GAS Decode] batchexecute lines length < 2");
      return sourceUrl;
    }
    
    var parsed = JSON.parse(lines[1]);
    if (parsed && parsed.length > 0 && parsed[0][2]) {
      var decodedJson = JSON.parse(parsed[0][2]);
      if (decodedJson && decodedJson[1]) {
        Logger.log("[GAS Decode] Successfully decoded: " + decodedJson[1]);
        return decodedJson[1];
      }
    }
    Logger.log("[GAS Decode] Failed to find decoded URL in batchexecute response!");
    return sourceUrl;
  } catch (e) {
    Logger.log("decodeGoogleNewsUrl error: " + e);
    return sourceUrl;
  }
}

