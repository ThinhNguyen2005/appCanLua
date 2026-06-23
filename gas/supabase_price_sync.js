/**
 * ============================================================
 * RICE PRICE SYNC — Google Apps Script → Supabase
 * ============================================================
 * Reads rice prices from Google Sheet → upserts into Supabase `rice_prices` table.
 * Replaces the old Firestore push flow.
 *
 * Setup:
 *   1. Open script.google.com → paste this file.
 *   2. Fill in CONFIG below.
 *   3. Vào Project Settings (biểu tượng bánh răng) → Tích chọn "Show 'appsscript.json' manifest file in editor".
 *   4. Thêm quyền vào "oauthScopes" trong file `appsscript.json`:
 *      "oauthScopes": [
 *        "https://www.googleapis.com/auth/spreadsheets",
 *        "https://www.googleapis.com/auth/script.external_request",
 *        "https://www.googleapis.com/auth/script.scriptapp"
 *      ]
 *   5. Run → setupTriggers → Authorize.
 *   6. Get SUPABASE_SERVICE_ROLE_KEY from Supabase Dashboard → Project Settings → API.
 *      WARNING: Never expose service_role key in client apps. GAS only.
 *
 * Sheet structure (row 2+ = data):
 * | A: Date | B: Variety | C: PriceMin | D: PriceMax | E: Trend | F: Source | G: Region | H: RiceType |
 */

// ─── CONFIG ─────────────────────────────────────────────────
var CONFIG = {
  SPREADSHEET_ID: '1OdzBJNS0c2jQcnt-W3bqMVj47bHgQGKYwgqGeIYcQxk',
  SHEET_NAME: 'Prices',
  DATA_START_ROW: 2,

  // Supabase project credentials
  SUPABASE_URL: 'https://wuongoybucznvfbuzzjh.supabase.co',
  SUPABASE_ANON_KEY: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Ind1b25nb3lidWN6bnZmYnV6empoIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzkyMDY4NjMsImV4cCI6MjA5NDc4Mjg2M30.laUhoFYWhf16p4D3Bqngf9xhPzI20YE_TNlG-W8CAMI',
  SUPABASE_GAS_KEY: 'gas_sync_secret_68593',

  TABLE: 'rice_prices',
};

// ─── COLUMNS ────────────────────────────────────────────────
var COL = {
  DATE: 0,      // A
  VARIETY: 1,   // B
  PRICE_MIN: 2, // C
  PRICE_MAX: 3, // D
  TREND: 4,     // E
  SOURCE: 5,    // F
  REGION: 6,    // G
  RICE_TYPE: 7  // H
};

// ─── MAIN: SYNC PRICES ──────────────────────────────────────
function syncPricesToSupabase() {
  var sheet = SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID)
    .getSheetByName(CONFIG.SHEET_NAME);

  if (!sheet) {
    Logger.log('ERROR: Sheet "' + CONFIG.SHEET_NAME + '" not found.');
    return;
  }

  var lastRow = sheet.getLastRow();
  if (lastRow < CONFIG.DATA_START_ROW) {
    Logger.log('No data rows found.');
    return;
  }

  var rows = sheet.getRange(CONFIG.DATA_START_ROW, 1, lastRow - CONFIG.DATA_START_ROW + 1, 8).getValues();
  var now = new Date().getTime();

  var prices = [];
  rows.forEach(function(row) {
    var variety = String(row[COL.VARIETY] || '').trim();
    if (!variety) return; // skip empty rows

    var priceMin = parseFloat(row[COL.PRICE_MIN]) || 0;
    var priceMax = parseFloat(row[COL.PRICE_MAX]) || 0;
    var trend = String(row[COL.TREND] || 'STABLE').trim().toUpperCase();
    var source = String(row[COL.SOURCE] || '').trim();
    var region = String(row[COL.REGION] || 'ĐBSCL').trim();
    var riceType = String(row[COL.RICE_TYPE] || 'lúa khô').trim();

    // Validate trend
    if (!['UP', 'DOWN', 'STABLE'].includes(trend)) trend = 'STABLE';

    // Use variety+region as deterministic ID (upsert key)
    var id = (variety + '_' + region)
      .toLowerCase()
      .replace(/\s+/g, '_')
      .replace(/[^a-z0-9_]/g, '');

    prices.push({
      id: id,
      variety: variety,
      price_min: priceMin,
      price_max: priceMax,
      price_avg7d: (priceMin + priceMax) / 2,
      region: region,
      updated_at: now,
      trend: trend,
      rice_type: riceType,
      source: source || 'Tổng hợp',
      active: true
    });
  });

  if (prices.length === 0) {
    Logger.log('No valid price rows to sync.');
    return;
  }

  // Upsert all prices in one batch call
  var result = supabaseUpsert(CONFIG.TABLE, prices);
  Logger.log('Synced ' + prices.length + ' prices. Status: ' + result.status);
}

// ─── SUPABASE REST CLIENT ────────────────────────────────────
/**
 * Upsert rows into a Supabase table via REST API.
 * Uses "Prefer: resolution=merge-duplicates" — insert or update by primary key.
 */
function supabaseUpsert(table, rows) {
  var url = CONFIG.SUPABASE_URL + '/rest/v1/' + table;
  var options = {
    method: 'post',
    headers: {
      'apikey': CONFIG.SUPABASE_ANON_KEY,
      'Authorization': 'Bearer ' + CONFIG.SUPABASE_ANON_KEY,
      'x-gas-key': CONFIG.SUPABASE_GAS_KEY,
      'Content-Type': 'application/json',
      'Prefer': 'resolution=merge-duplicates'
    },
    payload: JSON.stringify(rows),
    muteHttpExceptions: true
  };

  var response = UrlFetchApp.fetch(url, options);
  var status = response.getResponseCode();
  var body = response.getContentText();

  if (status >= 400) {
    Logger.log('Supabase error ' + status + ': ' + body.substring(0, 300));
  }

  return { status: status, body: body };
}

// ─── HELPERS ────────────────────────────────────────────────
/** Mark a variety as inactive (soft-delete). */
function deactivateVariety(variety, region) {
  var id = (variety + '_' + (region || 'ĐBSCL'))
    .toLowerCase()
    .replace(/\s+/g, '_')
    .replace(/[^a-z0-9_]/g, '');

  var url = CONFIG.SUPABASE_URL + '/rest/v1/' + CONFIG.TABLE + '?id=eq.' + encodeURIComponent(id);
  var options = {
    method: 'patch',
    headers: {
      'apikey': CONFIG.SUPABASE_ANON_KEY,
      'Authorization': 'Bearer ' + CONFIG.SUPABASE_ANON_KEY,
      'x-gas-key': CONFIG.SUPABASE_GAS_KEY,
      'Content-Type': 'application/json'
    },
    payload: JSON.stringify({ active: false, updated_at: new Date().getTime() }),
    muteHttpExceptions: true
  };

  var response = UrlFetchApp.fetch(url, options);
  Logger.log('Deactivate ' + id + ' → ' + response.getResponseCode());
}

/** Fetch current prices from Supabase (for verification). */
function listActivePrices() {
  var url = CONFIG.SUPABASE_URL + '/rest/v1/' + CONFIG.TABLE
    + '?active=eq.true&order=updated_at.desc&select=variety,price_min,price_max,trend,region';
  var options = {
    method: 'get',
    headers: {
      'apikey': CONFIG.SUPABASE_ANON_KEY,
      'Authorization': 'Bearer ' + CONFIG.SUPABASE_ANON_KEY
    },
    muteHttpExceptions: true
  };

  var response = UrlFetchApp.fetch(url, options);
  var prices = JSON.parse(response.getContentText());
  Logger.log('Active prices count: ' + prices.length);
  prices.forEach(function(p) {
    Logger.log(p.variety + ' [' + p.region + ']: ' + p.price_min + '-' + p.price_max + ' (' + p.trend + ')');
  });
}

// ─── TRIGGERS ───────────────────────────────────────────────
/** Run once to setup triggers. */
function setupTriggers() {
  // Remove existing triggers for this function
  ScriptApp.getProjectTriggers().forEach(function(t) {
    if (t.getHandlerFunction() === 'syncPricesToSupabase') {
      ScriptApp.deleteTrigger(t);
    }
  });

  // Sync every 30 minutes
  ScriptApp.newTrigger('syncPricesToSupabase')
    .timeBased()
    .everyMinutes(30)
    .create();

  // Also sync on spreadsheet edit
  ScriptApp.newTrigger('syncPricesToSupabase')
    .forSpreadsheet(SpreadsheetApp.openById(CONFIG.SPREADSHEET_ID))
    .onEdit()
    .create();

  Logger.log('Triggers created: every 30min + onEdit');
}
