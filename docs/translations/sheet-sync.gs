// Google Apps Script for the translations sheet. Paste into a STANDALONE project
// (script.google.com > New project), NOT into the sheet's Extensions > Apps Script:
// a script bound to the sheet is fully editable (token included) by anyone with edit
// access to the sheet. Set SYNC_TOKEN and SHEET_ID script properties, then
// Deploy > New deployment > Web app (Execute as: Me, Who has access: Anyone).
// See README.md in this directory.
//
// GET  ?token=...  returns the first sheet as CSV
// POST ?token=...  replaces the first sheet with the CSV in the request body, then
//                  inserts GOOGLETRANSLATE draft formulas into empty translation cells.
//                  Non-empty cells are never touched, so human translations are safe.
//                  Machine-drafted cells are tinted yellow; the tint survives after the
//                  next sync freezes the formula into plain text, so machine vs human
//                  authorship stays visible. Clear the tint when a human reviews a cell.

// Fallback column-name -> Google Translate code map, used only when a POST does not
// carry a ?langs= parameter (the sync workflow always sends one, generated from the
// registered languages in tt/translations.gradle.kts, so new languages need no edit here).
var LANGS = { Danish: 'da', German: 'de', Spanish: 'es', Italian: 'it', Portuguese: 'pt' };
var MT_COLOR = '#fff2cc';

function langsFrom_(e) {
  if (!e.parameter.langs) return LANGS;
  var out = {};
  e.parameter.langs.split(',').forEach(function (pair) {
    var i = pair.indexOf(':');
    if (i > 0) out[pair.slice(0, i)] = pair.slice(i + 1);
  });
  return out;
}

function doGet(e) {
  checkToken_(e);
  var rows = sheet_().getDataRange().getDisplayValues();
  var csv = rows.map(function (row, r) {
    return row.map(function (v, c) {
      return csvField_(r > 0 && c >= 3 ? sanitize_(v, row[2]) : v);
    }).join(',');
  }).join('\r\n');
  return ContentService.createTextOutput(csv).setMimeType(ContentService.MimeType.CSV);
}

function doPost(e) {
  checkToken_(e);
  var body = e.postData.contents.replace(/^\uFEFF/, '');
  var rows = Utilities.parseCsv(body);
  if (rows.length < 1 || rows[0][0] !== 'File') {
    throw new Error('CSV does not look like a translations export, refusing to overwrite sheet');
  }
  var width = rows[0].length;
  var padded = rows.map(function (row) {
    while (row.length < width) row.push('');
    return row.slice(0, width);
  });
  var langs = langsFrom_(e);
  var sheet = sheet_();
  var mtCells = machineTintedCells_(sheet); // remember authorship before wiping
  var oldRows = sheet.getLastRow();
  var oldCols = sheet.getLastColumn();
  sheet.clearContents();
  // User formatting (wrapping, bold headers, ...) is deliberately preserved. Fill
  // colors inside the data range stay reserved for the sync: setBackgrounds below
  // rewrites them all every run. Only strips the data no longer covers need clearing,
  // so stale tints cannot linger when the sheet shrinks.
  if (oldRows > padded.length) {
    sheet.getRange(padded.length + 1, 1, oldRows - padded.length, oldCols).clearFormat();
  }
  if (oldCols > width) {
    sheet.getRange(1, width + 1, Math.max(oldRows, padded.length), oldCols - width).clearFormat();
  }
  var range = sheet.getRange(1, 1, padded.length, width);
  range.setNumberFormat('@'); // plain text, so values like "1.1" don't become dates
  range.setValues(padded);

  // rebuild the machine-translated tints keyed by File/Key/language (not by cell
  // position, which shifts when rows are inserted), and pick the cells to draft
  var backgrounds = [];
  var drafts = [];
  for (var r = 0; r < padded.length; r++) {
    var rowBg = [];
    for (var c = 0; c < width; c++) {
      var bg = null;
      if (r > 0 && c >= 3 && langs[padded[0][c]]) {
        // resource paths (values like /textures/gui/...) are localized by hand, never drafted
        if (padded[r][c] === '' && padded[r][2] !== '' && padded[r][2].charAt(0) !== '/') {
          drafts.push([r, c]);
          bg = MT_COLOR;
        } else if (padded[r][c] !== '' && mtCells[cellKey_(padded[r][0], padded[r][1], padded[0][c])] === padded[r][c]) {
          // same text the draft had: still unreviewed. A differing value means the merge
          // chose a human edit from the code side, so the machine tint is cleared.
          bg = MT_COLOR;
        }
      }
      rowBg.push(bg);
    }
    backgrounds.push(rowBg);
  }
  range.setBackgrounds(backgrounds);
  drafts.forEach(function (rc) {
    sheet.getRange(rc[0] + 1, rc[1] + 1)
      .setNumberFormat('General')
      .setFormula('=GOOGLETRANSLATE($C' + (rc[0] + 1) + ', "en", "' + langs[padded[0][rc[1]]] + '")');
  });
  return ContentService.createTextOutput('OK ' + (padded.length - 1) + ' rows, ' + drafts.length + ' drafts');
}

// File/Key/language -> current text of every cell tinted as machine-translated
function machineTintedCells_(sheet) {
  var out = {};
  var dataRange = sheet.getDataRange();
  var rows = dataRange.getDisplayValues();
  if (rows.length < 2 || rows[0][0] !== 'File') return out;
  var bgs = dataRange.getBackgrounds();
  for (var r = 1; r < rows.length; r++) {
    for (var c = 3; c < rows[0].length; c++) {
      if (bgs[r][c].toLowerCase() === MT_COLOR) {
        out[cellKey_(rows[r][0], rows[r][1], rows[0][c])] = rows[r][c];
      }
    }
  }
  return out;
}

function cellKey_(file, key, language) {
  return file + '\u0000' + key + '\u0000' + language;
}

// Keep half-computed or broken cells out of the game: still-loading or errored
// formulas, and machine translations that mangled {0}-style format placeholders,
// are exported as empty (= untranslated, falls back to English at runtime).
function sanitize_(v, english) {
  if (!v || v === 'Loading...' || v.charAt(0) === '#') return '';
  if (placeholders_(v) !== placeholders_(english)) return '';
  return v;
}

function placeholders_(s) {
  return (String(s).match(/\{\d+\}/g) || []).sort().join(',');
}

function sheet_() {
  var id = PropertiesService.getScriptProperties().getProperty('SHEET_ID');
  if (!id) throw new Error('Set the SHEET_ID script property to the spreadsheet id');
  return SpreadsheetApp.openById(id).getSheets()[0];
}

function csvField_(v) {
  var s = String(v);
  return /[",\n\r]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s;
}

function checkToken_(e) {
  var expected = PropertiesService.getScriptProperties().getProperty('SYNC_TOKEN');
  if (!expected || !e.parameter.token || e.parameter.token !== expected) {
    throw new Error('Invalid or missing token');
  }
}
