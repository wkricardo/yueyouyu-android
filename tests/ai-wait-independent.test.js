'use strict';
const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const {JSDOM} = require('jsdom');
const assets = path.resolve(__dirname, '../app/src/main/assets');
const M = require(path.join(assets, 'model.js'));
const H = require(path.join(assets, 'health-trend.js'));
const SOURCES = ['statement_image', 'budget_text', 'food_image', 'food_camera', 'food_text', 'health_history'];

function setup(fakeTimers = false) {
  const dom = new JSDOM(fs.readFileSync(path.join(assets, 'index.html'), 'utf8'), {url: 'https://app.local/', runScripts: 'outside-only'});
  const w = dom.window, requests = [], cancelled = [], tokens = new Map(), observers = [];
  const OriginalObserver = w.MutationObserver, RealDate = w.Date;
  w.Date = class extends RealDate { constructor(...a) { super(...(a.length ? a : ['2026-10-15T12:00:00Z'])); } static now() { return new RealDate('2026-10-15T12:00:00Z').getTime(); } };
  w.MutationObserver = class extends OriginalObserver { constructor(fn) { super(fn); observers.push(this); } };
  w.scrollTo = () => {}; w.alert = () => {}; w.confirm = () => true; w.setInterval = () => 0;
  let virtualNow = 0, timerSequence = 0; const timers = new Map(), timerHistory = [];
  if (fakeTimers) {
    w.setTimeout = (fn, delay = 0) => { const id = ++timerSequence, task = {id, fn, at: virtualNow + Number(delay), delay: Number(delay)}; timers.set(id, task); timerHistory.push(task); return id; };
    w.clearTimeout = id => timers.delete(id);
  }
  function tick(milliseconds) {
    const end = virtualNow + milliseconds;
    for (;;) { const next = [...timers.values()].filter(task => task.at <= end).sort((a, b) => a.at - b.at || a.id - b.id)[0]; if (!next) break; virtualNow = next.at; timers.delete(next.id); next.fn(); }
    virtualNow = end;
  }
  w.HTMLDialogElement.prototype.showModal = function() { this.open = true; };
  w.HTMLDialogElement.prototype.close = function() { this.open = false; this.dispatchEvent(new w.Event('close')); };
  w.Android = {
    notify() {}, clearAiRetry() {}, clearAiSession() { tokens.clear(); },
    requestAi(raw) { requests.push(JSON.parse(raw)); },
    cancelAi(attempt) { cancelled.push(attempt); },
    consumeAiEvent(token, id, kind) { const entry = tokens.get(token); if (!entry || entry.id !== id || entry.kind !== kind) return false; tokens.delete(token); return true; },
    isAiResultCurrent() { return true; }
  };
  let state = M.defaultState();
  state = M.addWeight(state, {id: 'synthetic-one', date: '2026-10-01', kg: 70});
  state = M.addWeight(state, {id: 'synthetic-two', date: '2026-10-15', kg: 69.8});
  w.localStorage.setItem(M.KEY, JSON.stringify(state));
  for (const script of w.document.querySelectorAll('script[src]')) w.eval(fs.readFileSync(path.join(assets, script.getAttribute('src')), 'utf8'));
  const $ = id => w.document.getElementById(id);
  let sequence = 0;
  function event(request, kind, extra = {}) {
    const n = requests.indexOf(request) + 1;
    const value = {version: request.version, mode: request.mode, source: request.source, revision: request.revision,
      clientAttempt: request.clientAttempt, sessionId: 'independent-wait-session', requestId: 'independent-request-' + n,
      generation: n, event: kind, deliveryToken: 'independent-delivery-' + (++sequence), ...extra};
    tokens.set(value.deliveryToken, {id: value.requestId, kind: value.event});
    w.receiveAiEvent(JSON.stringify(value));
    return value;
  }
  function start(source) {
    const mode = source.startsWith('budget') || source === 'statement_image' ? 'budget' : 'food';
    $('mode-' + mode).click();
    if (source === 'health_history') {
      w.WellnessUI.route('progress'); $('health-analyze').click(); $('health-send').click();
    } else if (source.endsWith('_text')) {
      $('ai-text-' + mode).click(); $('ai-compose-text').value = mode === 'budget' ? '午饭人民币32元' : '一碗米饭200克';
      $('ai-compose-text').dispatchEvent(new w.Event('input', {bubbles: true}));
      $('ai-compose-form').dispatchEvent(new w.Event('submit', {cancelable: true, bubbles: true}));
    } else {
      $(({statement_image: 'recognize', food_image: 'choose-food', food_camera: 'capture-food'})[source]).click();
    }
    assert.equal(requests.at(-1).source, source);
    return requests.at(-1);
  }
  return {w, $, requests, cancelled, event, start, tick, timerHistory, raw: () => w.localStorage.getItem(M.KEY), close() { observers.forEach(o => o.disconnect()); w.close(); }};
}
function success(request) {
  if (request.source === 'health_history') {
    const summary = JSON.parse(request.text);
    return JSON.stringify({assessment: H.stats(summary).weeklyKg === null ? 'insufficient_data' : 'descriptive_only', takeaways: H.allowedTakeaways(summary), nextSteps: ['balanced_meals']});
  }
  if (request.mode === 'food') return JSON.stringify({name: '合成米饭', portionGrams: 200, caloriesLow: 200, caloriesHigh: 300, confidence: 'low', note: ''});
  return JSON.stringify({records: [{date: '2026-10-15', type: 'expense', category: 'meals', amount: '32.00', currency: 'CNY', note: '合成午饭'}]});
}
function sending(e, request) {
  e.event(request, 'started');
  e.event(request, 'phase', {phase: 'consent'});
  e.event(request, 'phase', {phase: 'sending'});
  assert.equal(e.$('ai-task-status').dataset.phase, 'sending');
  assert.equal(e.$('ai-task-cancel').hidden, false);
}
for (const source of SOURCES) {
  test('independent exact owned success exits waiting: ' + source, async () => {
    const e = setup();
    try {
      const before = e.raw(), request = e.start(source); sending(e, request);
      e.event(request, 'result', {data: success(request)});
      await Promise.resolve();
      assert.equal(e.$('ai-task-status').dataset.phase, 'ready');
      assert.equal(e.$('ai-task-cancel').hidden, true);
      assert.equal(e.$('ai-task-dismiss').hidden, false);
      if (source === 'health_history') assert.equal(e.$('health-result').hidden, false);
      else assert.equal(e.$(request.mode === 'food' ? 'food-dialog' : 'review-dialog').open, true);
      assert.equal(e.raw(), before, 'receiving AI output must not persist records');
    } finally { e.close(); }
  });
  test('independent timeout exits waiting and explicit retry owns a new attempt: ' + source, () => {
    const e = setup();
    try {
      const before = e.raw(), request = e.start(source); sending(e, request);
      const failure = e.event(request, 'error', {error: '合成整体超时', errorCode: 'timeout', retryable: true});
      assert.equal(e.$('ai-task-status').dataset.phase, 'error');
      assert.equal(e.$('ai-task-cancel').hidden, true);
      assert.equal(e.$('ai-retry').hidden, false);
      assert.equal(e.requests.length, 1, 'timeout must not automatically send again');
      e.$('ai-retry').click();
      assert.equal(e.requests.length, 2);
      const retry = e.requests[1];
      assert.equal(retry.retryOf, failure.requestId);
      assert.notEqual(retry.clientAttempt, request.clientAttempt);
      if (request.text !== undefined) assert.equal(retry.text, request.text);
      sending(e, retry);
      e.event(request, 'result', {data: success(request)});
      e.event(request, 'error', {error: '合成迟到失败', errorCode: 'timeout', retryable: true});
      assert.equal(e.$('ai-task-status').dataset.phase, 'sending', 'old terminal callback must not stop the newer attempt');
      e.event(retry, 'result', {data: success(retry)});
      assert.equal(e.$('ai-task-status').dataset.phase, 'ready');
      assert.equal(e.raw(), before);
    } finally { e.close(); }
  });
  test('independent cancel exits waiting and rejects late completion: ' + source, () => {
    const e = setup();
    try {
      const before = e.raw(), request = e.start(source); sending(e, request);
      e.$('ai-task-cancel').click();
      assert.deepEqual(e.cancelled, [request.clientAttempt]);
      assert.equal(e.$('ai-task-status').dataset.phase, 'cancelled');
      e.event(request, 'result', {data: success(request)});
      assert.equal(e.$('ai-task-status').dataset.phase, 'cancelled');
      assert.equal(e.$('food-dialog').open, false);
      assert.equal(e.$('review-dialog').open, false);
      assert.equal(e.$('health-result').hidden, true);
      assert.equal(e.raw(), before);
    } finally { e.close(); }
  });
}

for (const source of SOURCES) {
  test('independent missing native acknowledgement has bounded wait and allows fresh input: ' + source, () => {
    const e = setup(true);
    try {
      const before = e.raw(), request = e.start(source);
      e.tick(9999); assert.equal(e.$('ai-task-status').dataset.phase, 'preparing');
      e.tick(1); assert.equal(e.$('ai-task-status').dataset.phase, 'error');
      assert.deepEqual(e.cancelled, [request.clientAttempt]);
      assert.equal(e.requests.length, 1); assert.equal(e.$('ai-retry').hidden, true);
      e.event(request, 'started'); e.event(request, 'result', {data: success(request)});
      assert.equal(e.$('ai-task-status').dataset.phase, 'error');
      const fresh = e.start(source); assert.equal(e.requests.length, 2);
      sending(e, fresh); e.event(fresh, 'result', {data: success(fresh)});
      assert.equal(e.$('ai-task-status').dataset.phase, 'ready'); assert.equal(e.raw(), before);
    } finally { e.close(); }
  });
  test('independent missing terminal callback expires despite duplicate sending phases: ' + source, () => {
    const e = setup(true);
    try {
      const before = e.raw(), request = e.start(source); sending(e, request);
      e.tick(129999); assert.equal(e.$('ai-task-status').dataset.phase, 'sending');
      e.event(request, 'phase', {phase: 'sending'}); e.tick(1);
      assert.equal(e.$('ai-task-status').dataset.phase, 'error');
      assert.deepEqual(e.cancelled, [request.clientAttempt]);
      assert.equal(e.requests.length, 1); assert.equal(e.$('ai-retry').hidden, true);
      e.event(request, 'result', {data: success(request)});
      assert.equal(e.$('ai-task-status').dataset.phase, 'error');
      assert.equal(e.$('food-dialog').open, false); assert.equal(e.$('review-dialog').open, false);
      assert.equal(e.$('health-result').hidden, true); assert.equal(e.raw(), before);
    } finally { e.close(); }
  });
  test('independent consent remains user paced and a valid result clears the watchdog: ' + source, () => {
    const e = setup(true);
    try {
      const before = e.raw(), request = e.start(source);
      e.event(request, 'started'); e.event(request, 'phase', {phase: 'consent'});
      e.tick(300000); assert.equal(e.$('ai-task-status').dataset.phase, 'consent'); assert.deepEqual(e.cancelled, []);
      e.event(request, 'phase', {phase: 'sending'}); e.tick(1000);
      e.event(request, 'result', {data: success(request)}); e.tick(300000);
      assert.equal(e.$('ai-task-status').dataset.phase, 'ready'); assert.deepEqual(e.cancelled, []);
      assert.equal(e.requests.length, 1); assert.equal(e.raw(), before);
    } finally { e.close(); }
  });
}
test('independent cleared old timer cannot cancel a newer owner even if delivered late', () => {
  const e = setup(true);
  try {
    const old = e.start('food_text'); sending(e, old);
    const stale = e.timerHistory.find(task => task.delay === 130000);
    e.$('ai-task-cancel').click(); const fresh = e.start('food_text'); sending(e, fresh);
    stale.fn(); assert.equal(e.$('ai-task-status').dataset.phase, 'sending');
    assert.deepEqual(e.cancelled, [old.clientAttempt]);
    e.event(fresh, 'result', {data: success(fresh)}); assert.equal(e.$('ai-task-status').dataset.phase, 'ready');
  } finally { e.close(); }
});
