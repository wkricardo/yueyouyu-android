/* Shared, dependency-free selector for every static and dynamically added select. */
(function () {
  'use strict';
  if (window.SelectSheet) return;
  const entries = new Map();
  let sheet, list, title, active = null, cursor = -1, observer;
  function name(select) {
    if (select.getAttribute('aria-label')) return select.getAttribute('aria-label');
    const labelled = select.getAttribute('aria-labelledby');
    if (labelled) return labelled.split(/\s+/).map(id => document.getElementById(id)?.textContent || '').join(' ').trim();
    return Array.from(select.labels || []).map(label => {
      const copy = label.cloneNode(true);
      copy.querySelectorAll('select,button,input,.quiet').forEach(node => node.remove());
      return copy.textContent.trim();
    }).join(' ') || '选择选项';
  }
  function unavailable(option) { return option.disabled || option.parentElement?.disabled || option.hidden; }
  function available(select) { return Array.from(select.options).some(option => !unavailable(option)); }
  // Native value assignment intentionally emits no event. Mirror it without changing that contract.
  function mirrorProperty(prototype, property, owner) {
    const descriptor = Object.getOwnPropertyDescriptor(prototype, property);
    if (!descriptor?.set) return;
    Object.defineProperty(prototype, property, { ...descriptor,
      set(value) { descriptor.set.call(this, value); const select = owner(this); if (select && entries.has(select)) refresh(select); }
    });
  }
  mirrorProperty(HTMLSelectElement.prototype, 'value', select => select);
  mirrorProperty(HTMLSelectElement.prototype, 'selectedIndex', select => select);
  mirrorProperty(HTMLOptionElement.prototype, 'selected', option => option.closest('select'));
  function refresh(select) {
    if (!select) { entries.forEach((_, item) => refresh(item)); return; }
    const entry = entries.get(select);
    if (!entry) return;
    if (!select.isConnected) { if (active === select) close(); entry.button.remove(); entries.delete(select); return; }
    const text = select.options[select.selectedIndex]?.label || '请选择';
    if (entry.value.textContent !== text) entry.value.textContent = text;
    entry.button.setAttribute('aria-label', name(select) + '：' + text);
    entry.button.disabled = select.matches(':disabled') || !available(select);
    entry.button.hidden = select.hidden;
    if (active === select) {
      if (entry.button.disabled || select.hidden) close();
      else render();
    }
  }
  function enhance(select) {
    if (entries.has(select)) return;
    const button = document.createElement('button'), value = document.createElement('span'), arrow = document.createElement('span');
    button.type = 'button'; button.className = 'select-trigger';
    button.setAttribute('aria-haspopup', 'dialog'); button.setAttribute('aria-expanded', 'false');
    button.setAttribute('aria-controls', 'select-sheet');
    value.className = 'select-value'; arrow.className = 'select-chevron'; arrow.textContent = '⌄'; arrow.setAttribute('aria-hidden', 'true');
    button.append(value, arrow); select.after(button);
    select.classList.add('select-native'); select.setAttribute('aria-hidden', 'true'); select.tabIndex = -1;
    entries.set(select, {button, value});
    button.addEventListener('click', event => { event.preventDefault(); open(select); });
    select.addEventListener('click', event => event.preventDefault());
    select.addEventListener('input', () => refresh(select)); select.addEventListener('change', () => refresh(select));
    select.addEventListener('invalid', event => { event.preventDefault(); button.focus(); button.setAttribute('aria-invalid', 'true'); });
    select.addEventListener('change', () => button.removeAttribute('aria-invalid'));
    refresh(select);
  }
  function move(index, direction) {
    if (!active) return;
    const options = Array.from(active.options);
    while (index >= 0 && index < options.length && unavailable(options[index])) index += direction;
    if (index >= 0 && index < options.length) { cursor = index; markCursor(); }
  }
  function markCursor() {
    list.querySelectorAll('[role="option"]').forEach(row => row.classList.toggle('select-cursor', Number(row.dataset.index) === cursor));
    const row = list.querySelector('[data-index="' + cursor + '"]');
    if (row) { list.setAttribute('aria-activedescendant', row.id); row.scrollIntoView?.({block:'nearest'}); }
    else list.removeAttribute('aria-activedescendant');
  }
  function render() {
    if (!active) return;
    title.textContent = name(active);
    list.replaceChildren();
    Array.from(active.options).forEach((option, index) => {
      if (option.hidden) return;
      const row = document.createElement('div'), text = document.createElement('span'), check = document.createElement('span');
      row.className = 'select-option'; row.id = 'select-sheet-option-' + index; row.dataset.index = String(index);
      row.setAttribute('role', 'option'); row.setAttribute('aria-selected', String(option.selected));
      row.setAttribute('aria-disabled', String(!!unavailable(option)));
      text.textContent = option.label; check.textContent = option.selected ? '✓' : ''; check.setAttribute('aria-hidden', 'true');
      row.append(text, check); list.append(row);
    });
    if (cursor < 0 || !active.options[cursor] || unavailable(active.options[cursor])) {
      cursor = active.selectedIndex;
      if (cursor < 0 || unavailable(active.options[cursor])) cursor = Array.from(active.options).findIndex(option => !unavailable(option));
    }
    markCursor();
  }
  function open(select) {
    refresh(select);
    const entry = entries.get(select);
    if (!entry || entry.button.disabled) return;
    if (active) close();
    active = select; cursor = select.selectedIndex;
    entry.button.setAttribute('aria-expanded', 'true');
    render(); sheet.showModal(); list.focus(); markCursor();
  }
  function restore() {
    if (!active) return;
    const entry = entries.get(active); active = null; cursor = -1;
    if (entry) { entry.button.setAttribute('aria-expanded', 'false'); if (entry.button.isConnected && !entry.button.disabled) entry.button.focus(); }
  }
  function close() { if (!active) return false; sheet.close(); restore(); return true; }
  function choose(index) {
    if (!active || !active.options[index] || unavailable(active.options[index])) return;
    const select = active, changed = select.selectedIndex !== index;
    close();
    if (changed) {
      select.selectedIndex = index;
      select.dispatchEvent(new Event('input', {bubbles:true}));
      select.dispatchEvent(new Event('change', {bubbles:true}));
    }
    refresh(select);
  }
  function init() {
    if (sheet) { document.querySelectorAll('select').forEach(enhance); refresh(); return; }
    sheet = document.createElement('dialog'); sheet.id = 'select-sheet'; sheet.className = 'select-sheet';
    sheet.setAttribute('aria-labelledby', 'select-sheet-title'); sheet.setAttribute('aria-modal', 'true');
    sheet.innerHTML = '<div class="select-sheet-handle" aria-hidden="true"></div><div class="select-sheet-heading"><h2 id="select-sheet-title"></h2><button class="select-sheet-cancel" type="button">取消</button></div><div class="select-options" role="listbox" tabindex="0" aria-labelledby="select-sheet-title"></div>';
    document.body.append(sheet); list = sheet.querySelector('.select-options'); title = sheet.querySelector('h2');
    sheet.querySelector('button').addEventListener('click', close);
    sheet.addEventListener('cancel', event => { event.preventDefault(); close(); });
    sheet.addEventListener('close', () => { if (!sheet.open) restore(); });
    sheet.addEventListener('click', event => { if (event.target === sheet) { const bounds = sheet.getBoundingClientRect(); if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) close(); } });
    list.addEventListener('click', event => { const row = event.target.closest('[data-index]'); if (row) choose(Number(row.dataset.index)); });
    sheet.addEventListener('keydown', event => {
      if (!active) return;
      if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close(); return; }
      if (event.key === 'Tab') { event.preventDefault(); const cancel = sheet.querySelector('button'); (document.activeElement === list ? cancel : list).focus(); return; }
      if (event.target !== list) return;
      if (['ArrowDown','ArrowUp','Home','End','Enter',' '].includes(event.key)) event.preventDefault();
      if (event.key === 'ArrowDown') move(cursor + 1, 1);
      if (event.key === 'ArrowUp') move(cursor - 1, -1);
      if (event.key === 'Home') move(0, 1);
      if (event.key === 'End') move(active.options.length - 1, -1);
      if (event.key === 'Enter' || event.key === ' ') choose(cursor);
    });
    document.querySelectorAll('select').forEach(enhance);
    document.addEventListener('reset', () => queueMicrotask(() => refresh()));
    observer = new MutationObserver(records => {
      const changed = new Set();
      for (const record of records) {
        if (sheet.contains(record.target) || record.target.closest?.('.select-trigger')) continue;
        const select = (record.target.nodeType === 1 ? record.target : record.target.parentElement)?.closest('select'); if (select) changed.add(select);
        if (record.type === 'childList') record.addedNodes.forEach(node => {
          if (node.nodeType !== 1) return;
          if (node.matches('select')) enhance(node);
          node.querySelectorAll('select').forEach(enhance);
        });
        if (record.type === 'attributes' && record.target.matches?.('fieldset')) record.target.querySelectorAll('select').forEach(item => changed.add(item));
      }
      entries.forEach((_, select) => { if (!select.isConnected) changed.add(select); });
      changed.forEach(select => refresh(select));
    });
    observer.observe(document.body, {subtree:true, childList:true, characterData:true, attributes:true, attributeFilter:['disabled','selected','label','value','hidden','aria-label','aria-labelledby']});
  }
  window.SelectSheet = {init, refresh, close, isOpen:() => !!active};
  init();
})();
