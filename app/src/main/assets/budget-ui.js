(function(){'use strict';
const $=id=>document.getElementById(id);
const routes={today:'overview',log:'records',progress:'budget-trends',plan:'settings'};
let currentRoute='today',active=true,refresh=()=>{},refreshRecords=()=>{};
const today=()=>{const date=new Date();return date.getFullYear()+'-'+String(date.getMonth()+1).padStart(2,'0')+'-'+String(date.getDate()).padStart(2,'0');};
const dialogReturnFocus=new Map();
function beforeDialogOpen(dialog){dialogReturnFocus.set(dialog.id,document.activeElement);}
function restoreDialogFocus(dialog){const source=dialogReturnFocus.get(dialog.id);dialogReturnFocus.delete(dialog.id);if(source&&source.isConnected&&source!==document.body&&!source.closest('[hidden]')&&!source.closest('dialog:not([open])'))source.focus({preventScroll:true});else if(active)$('budget-page-title').focus({preventScroll:true});}
const headings={today:['今天','每一笔，心里有数'],log:['收支记录','记下流向，随时核对'],progress:['收支趋势','按月份，看见变化'],plan:['预算计划','额度与选择，由你掌握']};
function syncHeading(){const heading=headings[currentRoute];$('budget-page-title').textContent=currentRoute==='today'?($('month').value===today().slice(0,7)?'本月概览':'月份概览'):heading[0];$('budget-kicker').textContent=heading[1];document.querySelector('.budget-month-control').hidden=currentRoute==='plan';$('budget-prev-month').disabled=$('month').value<='1900-01';$('budget-next-month').disabled=$('month').value>='2199-12';}
function moveMonth(offset){const value=$('month').value;if(!/^\d{4}-\d{2}$/.test(value))return;const [year,month]=value.split('-').map(Number),serial=year*12+month-1+offset;if(serial<1900*12||serial>2199*12+11)return;$('month').value=String(Math.floor(serial/12))+'-'+String(serial%12+1).padStart(2,'0');$('month').dispatchEvent(new Event('change',{bubbles:true}));syncHeading();}
function route(name,focus=true){
 if(!active||!routes[name])return false;
 currentRoute=name;
 document.querySelectorAll('.view').forEach(view=>view.hidden=view.id!==routes[name]);
 document.querySelectorAll('#budget-nav [data-budget-route]').forEach(button=>{const selected=button.dataset.budgetRoute===name;button.classList.toggle('selected',selected);if(selected)button.setAttribute('aria-current','page');else button.removeAttribute('aria-current');});
 document.body.dataset.budgetRoute=name;
 syncHeading();if(focus)$('budget-page-title').focus({preventScroll:true});
 window.scrollTo(0,0);return true;
}
function setMode(mode){onWellnessBack();active=mode==='budget';document.body.classList.toggle('budget-mode',active);$('budget-heading').hidden=!active;if(active){document.querySelectorAll('#budget-nav button').forEach(button=>button.hidden=false);route('today',false);}}
function init(options={}){refresh=options.refresh||refresh;for(const id of ['expense-dialog','review-dialog'])$(id).addEventListener('close',()=>restoreDialogFocus($(id)));$('budget-review-title').tabIndex=-1;$('wellness-settings').addEventListener('click',openSharedSettings);$('budget-settings-return').addEventListener('click',()=>window.handleBack());$('budget-edit-plan').addEventListener('click',()=>{$('budget-editor').open=true;const field=$('settings-fields').querySelector('input');if(field)field.focus();});$('settings-form').addEventListener('input',()=>{$('budget-plan-save-status').textContent='有尚未保存的修改。上方摘要仍显示已保存计划。';});refreshRecords=options.refreshRecords||refreshRecords;for(const id of ['budget-filter-type','budget-filter-day'])$(id).addEventListener('change',refreshRecords);$('budget-clear-filters').addEventListener('click',()=>{for(const id of ['filter','budget-filter-type','budget-filter-day'])$(id).value='';refreshRecords();});$('budget-day').value=today();$('budget-day').addEventListener('change',()=>{if(!BudgetModel.validDate($('budget-day').value)){$('budget-day').value=today();}if($('month').value!==$('budget-day').value.slice(0,7))$('month').value=$('budget-day').value.slice(0,7);syncHeading();refresh();});$('budget-reset-day').addEventListener('click',()=>{$('budget-day').value=today();$('budget-day').dispatchEvent(new Event('change',{bubbles:true}));});$('month').addEventListener('change',()=>{if($('budget-day').value.slice(0,7)!==$('month').value){$('budget-day').value=$('month').value===today().slice(0,7)?today():$('month').value+'-01';refresh();}});$('budget-prev-month').addEventListener('click',()=>moveMonth(-1));$('budget-next-month').addEventListener('click',()=>moveMonth(1));$('month').addEventListener('change',syncHeading);setMode('budget');}
function handleBack(){return active&&currentRoute!=='today'?route('today'):false;}
function syncPeriod(month){
 const day=$('budget-day');if(!BudgetModel.validDate(day.value)||!day.value.startsWith(month))day.value=month===today().slice(0,7)?today():month+'-01';
 const [year,monthNumber]=month.split('-').map(Number),last=new Date(Date.UTC(year,monthNumber,0)).getUTCDate();$('budget-filter-day').min=month+'-01';$('budget-filter-day').max=month+'-'+String(last).padStart(2,'0');syncHeading();
}
function filterRecords(rows,month){
 const day=$('budget-filter-day');if(day.value&&(!BudgetModel.validDate(day.value)||!day.value.startsWith(month)))day.value='';
 const category=$('filter').value,type=$('budget-filter-type').value;
 const result=rows.filter(row=>row.date.startsWith(month)&&(!category||row.category===category)&&(!type||row.type===type)&&(!day.value||row.date===day.value));
 const count=Number(!!category)+Number(!!type)+Number(!!day.value);$('budget-clear-filters').disabled=count===0;
 $('budget-filter-status').textContent=month+' · '+(count?count+' 项筛选 · ':'全部账目 · ')+result.length+' 笔';
 return result;
}
function node(tag,cls,text){const element=document.createElement(tag);if(cls)element.className=cls;if(text!==undefined)element.textContent=text;return element;}
function renderLedger({state,month,rows,open}){
 const container=$('record-list');container.replaceChildren();const cash=BudgetModel.totals({...state,expenses:rows},month);
 $('record-summary').textContent='当前筛选 · '+rows.length+' 笔 · 净支出 ¥'+money(cash.expense)+' · 收入 ¥'+money(cash.income)+' · 结余 ¥'+money(cash.net);
 if(!rows.length){const empty=node('div','budget-ledger-empty');empty.append(node('span','budget-empty-symbol','☷'),node('h3','','没有符合条件的账目'),node('p','quiet','试试清除筛选，或记下这一笔。'));const add=node('button','outline','＋ 记一笔');add.type='button';add.addEventListener('click',()=>open());empty.append(add);container.append(empty);return;}
 const names={expense:'支出',income:'收入',refund:'退款',transfer:'转账',repayment:'信用卡还款'},groups=new Map();
 for(const row of rows){if(!groups.has(row.date))groups.set(row.date,[]);groups.get(row.date).push(row);}
 for(const [day,entries]of groups){const section=node('section','budget-ledger-day');section.dataset.date=day;section.setAttribute('aria-label',day+' 账目');const heading=node('div','budget-ledger-day-heading'),sum=BudgetModel.totals({...state,expenses:entries},day);heading.append(node('h3','',day),node('p','','当前筛选 · '+entries.length+' 笔 · 净支出 ¥'+money(sum.expense)+' · 收入 ¥'+money(sum.income)));section.append(heading);
  for(const row of entries){const category=BudgetModel.CATEGORIES.find(item=>item.id===row.category),button=node('button','record budget-ledger-record'),icon=node('span','budget-ledger-symbol',row.type==='income'?'↙':row.type==='refund'?'↶':['transfer','repayment'].includes(row.type)?'⇄':category?category.icon:'−'),detail=node('span','details');button.type='button';button.dataset.recordId=row.id;button.dataset.type=row.type;
   detail.append(node('b','',(names[row.type]||'支出')+' · '+(category?category.name:row.category)),node('small','',row.note||row.source||'无备注'));if(row.note&&row.source)detail.append(node('small','',row.source));
   const amount=node('span','budget-ledger-amount'),prefix=['income','refund'].includes(row.type)?'+ ':row.type==='expense'?'− ':'';amount.append(node('strong','',prefix+'¥'+money(row.amount)));if(['transfer','repayment'].includes(row.type))amount.append(node('small','','不计收支'));else if(row.type==='refund')amount.append(node('small','','抵减支出'));
   button.append(icon,detail,amount);button.setAttribute('aria-label','编辑 '+day+' '+names[row.type]+' '+(category?category.name:row.category)+' '+money(row.amount)+' 元');button.addEventListener('click',()=>open(row));section.append(button);
  }container.append(section);
 }
}
function svgNode(tag,attrs,text){const element=document.createElementNS('http://www.w3.org/2000/svg',tag);for(const [key,value]of Object.entries(attrs||{}))element.setAttribute(key,value);if(text!==undefined)element.textContent=text;return element;}
function cashflowModel(state,month){
 const [year,monthNumber]=month.split('-').map(Number),days=new Date(Date.UTC(year,monthNumber,0)).getUTCDate();
 const rows=Array.from({length:days},(_,index)=>{const date=month+'-'+String(index+1).padStart(2,'0'),cash=BudgetModel.totals(state,date);return {date,day:index+1,...cash};});
 const min=Math.min(0,...rows.map(row=>row.expense)),max=Math.max(100,...rows.map(row=>Math.max(row.expense,row.income))),span=max-min;
 return {rows,min,max,zeroY:170-(0-min)/span*140,points:rows.map(row=>({...row,x:46+(row.day-1)/(days-1)*274,expenseY:170-(row.expense-min)/span*140,incomeY:170-(row.income-min)/span*140}))};
}
function axisMoney(value){const yuan=value/100,absolute=Math.abs(yuan);if(absolute>=100000000)return Number((yuan/100000000).toFixed(1))+'亿';if(absolute>=10000)return Number((yuan/10000).toFixed(1))+'万';return yuan.toLocaleString('zh-CN',{useGrouping:false,maximumFractionDigits:absolute<100?2:1});}
function renderCashflow(state,month){
 const container=$('budget-trend-content'),model=cashflowModel(state,month),rows=state.expenses.filter(row=>row.date.startsWith(month));container.replaceChildren();$('budget-trend-period').textContent=month;
 const cash=BudgetModel.totals(state,month);$('budget-trend-caption').textContent=rows.length+' 笔记录 · 月净支出 ¥'+money(cash.expense)+' · 月收入 ¥'+money(cash.income)+'。横轴为自然日，未记录日期记为 0，不代表实际没有收支。大额刻度用万/亿缩写，明细保留完整金额。';
 if(!rows.some(row=>['expense','income','refund'].includes(row.type))){container.append(node('div','budget-chart-empty','这个月还没有收入、支出或退款记录。'));return;}
 const svg=svgNode('svg',{viewBox:'0 0 360 216',role:'img','aria-labelledby':'budget-cashflow-title budget-cashflow-desc'});svg.append(svgNode('title',{id:'budget-cashflow-title'},month+' 每日净支出与收入'),svgNode('desc',{id:'budget-cashflow-desc'},'金额单位人民币元，净支出可因退款为负，纵轴包含零。'+model.rows.map(row=>row.date+'：净支出 '+money(row.expense)+' 元，收入 '+money(row.income)+' 元').join('；')));
 for(let index=0;index<3;index++){const value=model.max-(model.max-model.min)*index/2,y=30+index*70;svg.append(svgNode('line',{x1:46,y1:y,x2:320,y2:y,class:'budget-chart-grid'}),svgNode('text',{x:40,y:y+4,'text-anchor':'end',class:'budget-axis-label'},axisMoney(value)));}
 svg.append(svgNode('line',{x1:46,y1:model.zeroY,x2:320,y2:model.zeroY,class:'budget-chart-zero'}),svgNode('text',{x:6,y:14,class:'budget-axis-label'},'元'));
 for(const [key,cls]of [['expense','budget-expense-line'],['income','budget-income-line']]){svg.append(svgNode('polyline',{points:model.points.map(point=>point.x+','+point[key+'Y']).join(' '),class:cls,fill:'none'}));for(const point of model.points){const dot=svgNode('circle',{cx:point.x,cy:point[key+'Y'],r:2.5,class:cls+'-point','data-date':point.date,'data-value':point[key]});dot.append(svgNode('title',{},point.date+' '+(key==='expense'?'净支出':'收入')+' ¥'+money(point[key])));svg.append(dot);}}
 for(const day of [1,Math.ceil(model.rows.length/2),model.rows.length])svg.append(svgNode('text',{x:model.points[day-1].x,y:202,'text-anchor':'middle',class:'budget-axis-label'},day+' 日'));container.append(svg);
}
function categoryTrendModel(state,month){const rows=BudgetModel.CATEGORIES.map(category=>{const entries=state.expenses.filter(row=>row.date.startsWith(month)&&row.category===category.id),gross=entries.reduce((sum,row)=>sum+(row.type==='expense'?row.amount:0),0),refunds=entries.reduce((sum,row)=>sum+(row.type==='refund'?row.amount:0),0);return {...category,gross,refunds,net:gross-refunds};});const scale=Math.max(1,...rows.map(row=>Math.abs(row.net)));return rows.map(row=>({...row,width:Math.abs(row.net)/scale*50}));}
function renderCategoryTrends(state,month){const container=$('budget-category-chart');container.replaceChildren();for(const category of categoryTrendModel(state,month)){const button=node('button','budget-category-trend'),top=node('span','budget-category-trend-top'),bar=node('span','budget-signed-bar'),fill=node('i','');button.type='button';button.dataset.category=category.id;button.dataset.net=String(category.net);top.append(node('b','',category.name),node('strong','','¥'+money(category.net)));fill.style.width=category.width+'%';fill.style.left=category.net<0?50-category.width+'%':'50%';fill.dataset.sign=category.net<0?'negative':'positive';bar.append(fill);bar.setAttribute('aria-hidden','true');button.append(top,bar,node('small','','支出 ¥'+money(category.gross)+' · 退款 ¥'+money(category.refunds)));button.setAttribute('aria-label','查看 '+month+' '+category.name+'账目，净支出 '+money(category.net)+' 元');button.addEventListener('click',()=>{$('filter').value=category.id;$('budget-filter-type').value='';$('budget-filter-day').value='';refreshRecords();route('log');});container.append(button);}}
function afterInit(){const theme=$('theme-settings');if(theme)$('budget-appearance-target').append(theme);}
function openSharedSettings(){document.body.classList.add('shared-settings-from-food');$('budget-app-settings').open=true;$('budget-settings-return').hidden=false;$('budget-app-settings-title').focus({preventScroll:true});window.scrollTo(0,0);}
function onWellnessBack(){document.body.classList.remove('shared-settings-from-food');$('budget-settings-return').hidden=true;}
function planSaved(){$('budget-plan-save-status').textContent='预算计划已保存。历史账目保持原样。';}
function syncEntryType(){const descriptions={expense:'支出计入所选分类预算。',income:'收入单独统计，不会增加或抵扣支出预算。',refund:'退款抵减所选分类的净支出，不作为收入。',transfer:'转账只保留记录，不计收入、支出或预算。',repayment:'信用卡还款只保留记录，不再次计入支出。'};$('budget-entry-type-hint').textContent=descriptions[$('type').value]||'';}
const money=value=>(value/100).toLocaleString('zh-CN',{minimumFractionDigits:2,maximumFractionDigits:2});
function ringModel(spent,budget){const remaining=budget-spent;return {spent,budget,remaining,state:budget===0?'zero':remaining<0?'over':spent<0?'refund':remaining===0?'used':BudgetModel.level(spent,budget),progress:budget>0?Math.max(0,Math.min(1,remaining/budget)):null};}
function render({state,month}){
 renderCashflow(state,month);renderCategoryTrends(state,month);$('budget-plan-total').textContent='¥'+money(Object.values(state.settings.monthly).reduce((sum,value)=>sum+value,0));$('budget-plan-category-count').textContent=BudgetModel.CATEGORIES.length+' 类';$('budget-plan-daily-count').textContent=Object.values(state.settings.daily).filter(value=>value!==null).length+' 类';
 const day=$('budget-day').value||today(),dayCash=BudgetModel.totals(state,day);
 $('budget-day-heading').textContent=day===today()?'今日用度':'当日用度';$('budget-reset-day').hidden=day===today();
 $('budget-day-summary').textContent='净支出 ¥'+money(dayCash.expense)+' · 收入 ¥'+money(dayCash.income)+' · 日限额仅按这个自然日计算';
 const levels=BudgetModel.CATEGORIES.map(category=>BudgetModel.level(BudgetModel.total(state,month,category.id),state.settings.monthly[category.id]));
 const near=levels.filter(level=>level==='near').length,over=levels.filter(level=>level==='over').length;
 $('budget-category-status').textContent=over?over+' 项已超支'+(near?' · '+near+' 项近上限':''):near?near+' 项达到 80%':'达到 80% 提醒';
 const rows=state.expenses.filter(row=>row.date.startsWith(month));
 $('budget-refunds').textContent='¥'+money(rows.reduce((sum,row)=>sum+(row.type==='refund'?row.amount:0),0));
 $('budget-transfers').textContent='¥'+money(rows.reduce((sum,row)=>sum+(['transfer','repayment'].includes(row.type)?row.amount:0),0));
 const budget=Object.values(state.settings.monthly).reduce((sum,value)=>sum+value,0),model=ringModel(BudgetModel.total(state,month),budget),ring=$('budget-ring');
 Object.assign(ring.dataset,{state:model.state,month,spent:String(model.spent),budget:String(budget),remaining:String(model.remaining)});
 if(model.progress===null)delete ring.dataset.progress;else ring.dataset.progress=String(model.progress);
 $('budget-ring-fill').setAttribute('stroke-dasharray',(model.progress===null?0:model.progress*100)+' 100');$('budget-ring-fill').style.visibility=model.progress>0?'visible':'hidden';
 $('budget-ring-value').textContent=money(model.remaining);$('budget-ring-value').dataset.long=String(money(model.remaining).length>12);$('budget-ring-value').dataset.extraLong=String(money(model.remaining).length>17);
 const status=budget===0?'月预算为 0，不显示比例。仍可正常记录。':model.remaining<0?'已超出月预算 ¥'+money(-model.remaining)+'。负数表示超预算。':model.spent<0?'本月退款多于支出，剩余额度高于预算；圆环最多显示一整圈。':model.state==='near'?'净支出已达月预算的 80%，留意接下来的支出。':model.remaining===0?'已用完本月预算。':'剩余额度 = 月预算 − 净支出。';
 $('budget-ring-status').textContent=status;$('budget-ring-title').textContent=month+' 预算剩余 ¥'+money(model.remaining);$('budget-ring-description').textContent='月预算 ¥'+money(budget)+'；净支出 ¥'+money(model.spent)+'（支出减退款）。'+status;
}
window.BudgetUI={init,route,setMode,handleBack,render,ringModel,syncEntryType,filterRecords,renderLedger,cashflowModel,categoryTrendModel,axisMoney,planSaved,afterInit,onWellnessBack,beforeDialogOpen,syncPeriod,get currentRoute(){return currentRoute;},get selectedDay(){return $('budget-day').value||today();}};
})();
