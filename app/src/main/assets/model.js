(function(root,factory){const api=factory();if(typeof module==='object'&&module.exports)module.exports=api;else root.BudgetModel=api;})(typeof globalThis!=='undefined'?globalThis:this,function(){
'use strict';
const CATEGORIES=[{id:'rent',name:'固定房租',icon:'⌂',budget:2500,color:'#879af0'},{id:'transport',name:'交通出行',icon:'↗',budget:1000,color:'#63a9c9'},{id:'meals',name:'日常吃饭',icon:'◒',budget:1500,color:'#4ca58e'},{id:'dining',name:'品质餐饮',icon:'✦',budget:1600,color:'#dca566'},{id:'fun',name:'娱乐 / 电子',icon:'◇',budget:2000,color:'#b290c8'}];
const TYPES=['expense','income','refund','transfer','repayment'];
const ids=CATEGORIES.map(x=>x.id);const KEY='slow-budget-v1';
function defaultState(){return {version:4,settings:{statementYear:2026,incomeLabels:['工资','奖金','兼职','其他收入'],monthly:Object.fromEntries(CATEGORIES.map(c=>[c.id,c.budget*100])),daily:Object.fromEntries(ids.map(id=>[id,id==='meals'?5000:null]))},expenses:[],nutrition:[],weights:[],favorites:[]};}
function validDate(s){if(typeof s!=='string'||!/^\d{4}-\d{2}-\d{2}$/.test(s)||s<'1900-01-01'||s>'2199-12-31')return false;const d=new Date(s+'T12:00:00Z');return !isNaN(d)&&d.toISOString().slice(0,10)===s;}
// Dates from OCR are calendar dates: preserve the displayed day, never shift by timezone.
function normalizeDate(value,referenceYear){
 if(typeof value!=='string'||!value.trim())throw Error('日期为空，请填写 YYYY-MM-DD');
 const raw=value.trim();let m=raw.match(/^(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})(?:(?:T| )([01]\d|2[0-3]):([0-5]\d)(?::([0-5]\d)(?:\.\d+)?)?(?:Z|[+-](?:[01]\d|2[0-3]):?[0-5]\d)?)?$/)||raw.match(/^(\d{4})年\s*(\d{1,2})月\s*(\d{1,2})日?$/);
 if(!m){const short=raw.match(/^(\d{1,2})[-/.](\d{1,2})$/)||raw.match(/^(\d{1,2})月\s*(\d{1,2})日?$/);if(short){if(!/^(19|20|21)\d{2}$/.test(String(referenceYear||'')))throw Error('日期缺少年份，请选择账单年份，或填写完整日期');m=[raw,String(referenceYear),short[1],short[2]];}else throw Error('日期格式无法识别，请填写 YYYY-MM-DD');}
 const result=m[1]+'-'+m[2].padStart(2,'0')+'-'+m[3].padStart(2,'0');
 if(!validDate(result))throw Error('日期不存在或超出 1900–2199 年，请核对年月日');return result;
}
function cents(value){const s=String(value).trim();if(!/^\d+(?:\.\d{1,2})?$/.test(s))throw Error('请输入最多两位小数的有效金额');const n=Math.round(Number(s)*100);if(!Number.isSafeInteger(n)||n>10000000000)throw Error('金额过大');return n;}
function validMoney(n,zero){return Number.isSafeInteger(n)&&n>=(zero?0:1)&&n<=10000000000;}
function shortText(value,max,required){if(typeof value!=='string'||value.length>max||(required&&!value.trim()))throw Error('文字内容无效或过长');return value;}
function record(e,legacy){
 if(!e||typeof e!=='object')throw Error('账目内容为空');
 const type=legacy?'expense':(e.type||'expense');
 if(typeof e.id!=='string'||!e.id||e.id.length>100)throw Error('账目编号无效');
 if(!TYPES.includes(type))throw Error('请选择有效的交易类型');
 if(!validDate(e.date))throw Error('日期无效，请填写真实的 YYYY-MM-DD 日期');
 if(!validMoney(e.amount,false))throw Error('金额需大于 0，且不超过 100000000 元');
 if(typeof e.category!=='string'||!e.category.trim()||e.category.length>40)throw Error('分类为空或过长，请选择分类');
 const category=e.category;if((type==='expense'||type==='refund')&&!ids.includes(category))throw Error('请选择有效的支出分类');
 if(typeof e.note!=='string'||e.note.length>200)throw Error('备注需为文字，最多 200 字');
 const source=legacy?'':e.source===undefined?'':e.source;if(typeof source!=='string'||source.length>80)throw Error('来源需为文字，最多 80 字');
 return {id:e.id,date:e.date,type,category,amount:e.amount,note:e.note,source};
}
function validate(raw){if(!raw||![1,2,3,4].includes(raw.version)||!raw.settings||!Array.isArray(raw.expenses)||raw.expenses.length>100000)throw Error('备份格式不正确');const s=defaultState();if(raw.settings.statementYear!==undefined){if(!Number.isInteger(raw.settings.statementYear)||raw.settings.statementYear<1900||raw.settings.statementYear>2199)throw Error('账单年份需为 1900–2199');s.settings.statementYear=raw.settings.statementYear;}for(const id of ids){const m=raw.settings.monthly&&raw.settings.monthly[id],d=raw.settings.daily&&raw.settings.daily[id];if(!validMoney(m,true)||(d!==null&&!validMoney(d,false)))throw Error('预算设置无效');s.settings.monthly[id]=m;s.settings.daily[id]=d;}if(raw.version>=2){const labels=raw.settings.incomeLabels;if(!Array.isArray(labels)||!labels.length||labels.length>30)throw Error('收入分类需为 1 至 30 项');s.settings.incomeLabels=labels.map(x=>shortText(x,40,true).trim());if(new Set(s.settings.incomeLabels).size!==labels.length)throw Error('收入分类不能重复');}const seen=new Set();s.expenses=raw.expenses.map(e=>{const clean=record(e,raw.version===1);if(seen.has(clean.id))throw Error('账目编号重复');seen.add(clean.id);return clean;});if(raw.version>=3){s.nutrition=cleanList(raw.nutrition,cleanNutrition);s.weights=cleanList(raw.weights,cleanWeight);}if(raw.version>=4){s.favorites=cleanList(raw.favorites,cleanFavorite);const names=new Set();for(const f of s.favorites){const name=favoriteNameKey(f.name);if(names.has(name))throw Error('常用食物名称重复，请更名');names.add(name);}}return s;}
function parse(text){if(typeof text!=='string'||text.length>25000000)throw Error('备份文件过大或无效');return validate(JSON.parse(text));}
function load(storage){const raw=storage.getItem(KEY);return raw===null?defaultState():parse(raw);}
function save(storage,state){const encoded=JSON.stringify(validate(state)),prior=storage.getItem(KEY);if(prior!==null){let old;try{old=JSON.parse(prior);}catch(_){old={};}if(old.version===1||old.version===2){if(storage.getItem(KEY+'-before-v3')===null)storage.setItem(KEY+'-before-v3',prior);}if([1,2,3].includes(old.version)&&storage.getItem(KEY+'-before-v4')===null)storage.setItem(KEY+'-before-v4',prior);}storage.setItem(KEY,encoded);}
function add(state,expense){const next=validate({...state,expenses:[...state.expenses,expense]});return next;}
function update(state,id,expense){if(!state.expenses.some(e=>e.id===id))throw Error('找不到账目');return validate({...state,expenses:state.expenses.map(e=>e.id===id?{...expense,id}:e)});}
function remove(state,id){return validate({...state,expenses:state.expenses.filter(e=>e.id!==id)});}
function total(state,period,category){return state.expenses.reduce((n,e)=>n+((e.date.startsWith(period)&&(!category||e.category===category))?(e.type==='refund'?-e.amount:(!e.type||e.type==='expense')?e.amount:0):0),0);}
function totals(state,period){const expense=total(state,period),income=state.expenses.reduce((n,e)=>n+(e.date.startsWith(period)&&e.type==='income'?e.amount:0),0);return {expense,income,net:income-expense};}
function duplicateKey(e){return [e.date,e.amount,e.type||'expense',String(e.note||'').toLowerCase().replace(/\s+/g,'').trim()].join('|');}
function coarseKey(e){return [e.date,e.amount,e.type||'expense'].join('|');}
function yearlessDate(value){return typeof value==='string'&&(/^(\d{1,2})[-/.](\d{1,2})$/.test(value.trim())||/^(\d{1,2})月\s*(\d{1,2})日?$/.test(value.trim()));}
function reviewRecognition(input,state,referenceYear,initial){
 if(referenceYear===undefined)referenceYear=state.settings.statementYear===undefined?2026:state.settings.statementYear;
 if(typeof input==='string'){if(input.length>2000000)throw Error('识别结果过大');input=JSON.parse(input);}
 const list=Array.isArray(input)?input:input&&input.records;
 if(!Array.isArray(list)||list.length>300||!list.length)throw Error('一次请识别 1 至 300 笔账目');
 const known=new Set(state.expenses.map(duplicateKey)),possible=new Set(state.expenses.map(coarseKey));
 return list.map((raw,i)=>{
 raw=raw&&typeof raw==='object'?raw:{};
 // Only initial ingestion may fill an empty OCR date; later clears are deliberate edits.
 if(initial&&(raw.date===undefined||raw.date===null||typeof raw.date==='string'&&!raw.date.trim())&&typeof raw.rawDate==='string')raw={...raw,date:raw.rawDate};
 const row={date:typeof raw.date==='string'?raw.date:'',rawDate:typeof raw.rawDate==='string'?raw.rawDate:typeof raw.date==='string'?raw.date:'',type:TYPES.includes(raw.type)?raw.type:'',category:typeof raw.category==='string'?raw.category:'',amount:typeof raw.amount==='number'||typeof raw.amount==='string'?String(raw.amount):'',note:typeof raw.note==='string'?raw.note:typeof raw.merchant==='string'?raw.merchant:'',source:typeof raw.source==='string'?raw.source:''};
 let error='',duplicate=false;const errors=[];
 // Keep inference only while the date still equals our last generated value.
 // A changed full date or an intentional clear becomes a manual override.
 const inferredSource=!initial&&yearlessDate(raw.inferredDateSource)&&row.date===raw.inferredDateValue?raw.inferredDateSource:yearlessDate(row.date)?row.date:null;
 if(inferredSource!==null)row.date=inferredSource;
 try{row.date=normalizeDate(row.date,referenceYear);}catch(e){errors.push(e.message);}
 if(inferredSource!==null){row.inferredDateSource=inferredSource;row.inferredDateValue=row.date;}
 if(!row.type)errors.push('请选择交易类型');
 if(!row.category.trim())errors.push('请选择分类');else if((row.type==='expense'||row.type==='refund')&&!ids.includes(row.category))errors.push('请选择有效的支出分类');else if(row.category.length>40)errors.push('分类最多 40 字');
 let amount;try{amount=cents(row.amount);if(!amount)throw Error('金额需大于 0');}catch(e){errors.push(e.message);}
 if(raw.note!=null&&typeof raw.note!=='string'||row.note.length>200)errors.push('备注需为文字，最多 200 字');
 if(raw.source!=null&&typeof raw.source!=='string'||row.source.length>80)errors.push('来源需为文字，最多 80 字');
 if(!errors.length){const e=record({...row,id:'review-'+i,amount},false),key=duplicateKey(e);duplicate=known.has(key)||possible.has(coarseKey(e));known.add(key);possible.add(coarseKey(e));}
 error=errors.join('；');return {...row,duplicate,error,selected:!error&&!duplicate};
 });
}
function importReviewed(state,rows){
 if(!Array.isArray(rows)||rows.length>300)throw Error('待导入账目无效');
 const selected=rows.filter(x=>x&&x.selected);if(!selected.length)return validate(state);
 const checked=reviewRecognition(selected,state);const invalid=checked.find(r=>r.error);if(invalid)throw Error(invalid.error);
 const stamp=Date.now().toString(36)+'-'+Math.random().toString(36).slice(2,12);
 const added=checked.map((row,i)=>({...row,id:'import-'+stamp+'-'+i,amount:cents(row.amount)}));
 return validate({...state,expenses:[...state.expenses,...added]});
}
function level(amount,limit){if(limit===null)return 'off';if(amount>limit)return 'over';if(limit>0&&amount*5>=limit*4)return 'near';return 'ok';}
function crossings(before,after,date,category){const result=[];for(const scope of ['monthly','daily']){const period=scope==='monthly'?date.slice(0,7):date;const limit=after.settings[scope][category];if(limit===undefined)continue;const old=level(total(before,period,category),limit),now=level(total(after,period,category),limit);const rank={off:0,ok:0,near:1,over:2};if(rank[now]>rank[old])result.push({scope,level:now,amount:total(after,period,category),limit});}const month=date.slice(0,7),budget=Object.values(after.settings.monthly).reduce((a,b)=>a+b,0),old=level(total(before,month),budget),now=level(total(after,month),budget),rank={ok:0,near:1,over:2};if(rank[now]>rank[old])result.push({scope:'total',level:now,amount:total(after,month),limit:budget});return result;}

// Food and weight calculations follow the audited offline Harmony model.
function finiteRange(n,min,max){if(typeof n!=='number'||!Number.isFinite(n)||n<min||n>max)throw Error('数值超出有效范围');return n;}
function cleanList(list,clean){if(!Array.isArray(list)||list.length>100000)throw Error('记录列表无效');const seen=new Set();return list.map(r=>{const row=clean(r);if(seen.has(row.id))throw Error('记录编号重复');seen.add(row.id);return row;});}
function cleanNutrition(r){if(!r||!validDate(r.date))throw Error('食物日期无效');const low=finiteRange(r.lowKcal,0,1000000),high=finiteRange(r.highKcal,low,1000000);return {id:shortText(r.id,100,true),date:r.date,name:shortText(r.name,100,true).trim(),grams:finiteRange(r.grams,0.01,1000000),kcal:finiteRange(r.kcal,low,high),lowKcal:low,highKcal:high,note:shortText(r.note,200,false)};}

// Favorites are independent value objects; historical meals never reference them.
function favoriteNameKey(name){return shortText(name,100,true).normalize('NFKC').toLocaleLowerCase('en-US').replace(/\s+/g,'');}
function cleanFavorite(r){if(!r||typeof r!=='object')throw Error('常用食物无效');const low=finiteRange(r.lowKcal,0,1000000),high=finiteRange(r.highKcal,low,1000000);return {id:shortText(r.id,100,true),name:shortText(r.name,100,true).trim(),grams:finiteRange(r.grams,0.01,1000000),lowKcal:low,highKcal:high,note:shortText(r.note===undefined?'':r.note,200,false)};}
function findFavorite(state,name,exceptId){const key=favoriteNameKey(name);return state.favorites.find(r=>r.id!==exceptId&&favoriteNameKey(r.name)===key)||null;}
const addFavorite=(s,r)=>mutateList(s,'favorites',r),updateFavorite=(s,id,r)=>mutateList(s,'favorites',r,id),removeFavorite=(s,id)=>mutateList(s,'favorites',null,id,true);
function favoriteFromNutrition(r,id){const food=cleanNutrition(r);return cleanFavorite({...food,id});}
function scaleFavorite(favorite,grams){const base=cleanFavorite(favorite);finiteRange(grams,0.01,1000000);const lowKcal=finiteRange(base.lowKcal*grams/base.grams,0,1000000),highKcal=finiteRange(base.highKcal*grams/base.grams,lowKcal,1000000);return {grams,lowKcal,highKcal};}

function cleanWeight(r){if(!r||!validDate(r.date))throw Error('体重日期无效');return {id:shortText(r.id,100,true),date:r.date,kg:finiteRange(r.kg,0.1,1000)};}
function energyKcal(energy,unit,basis,consumed,portionGrams){finiteRange(energy,0,1000000);finiteRange(consumed,0,1000000);if(!['kJ','kcal'].includes(unit))throw Error('请选择 kJ 或 kcal');if(!['per100g','perportion'].includes(basis))throw Error('请选择每 100 克或每份');let portions=consumed;if(portionGrams!==undefined){finiteRange(portionGrams,0.01,1000000);if(basis==='perportion')portions=consumed/portionGrams;}return finiteRange((unit==='kJ'?energy/4.184:energy)*(basis==='per100g'?consumed/100:portions),0,1000000);}
function mutateList(state,key,row,id,remove){const next=validate(state);if(id&&!next[key].some(r=>r.id===id))throw Error('找不到记录');next[key]=id?(remove?next[key].filter(r=>r.id!==id):next[key].map(r=>r.id===id?{...row,id}:r)):[...next[key],row];return validate(next);}
const addNutrition=(s,r)=>mutateList(s,'nutrition',r),updateNutrition=(s,id,r)=>mutateList(s,'nutrition',r,id),removeNutrition=(s,id)=>mutateList(s,'nutrition',null,id,true);
const addWeight=(s,r)=>mutateList(s,'weights',r),updateWeight=(s,id,r)=>mutateList(s,'weights',r,id),removeWeight=(s,id)=>mutateList(s,'weights',null,id,true);
function nutritionTotal(state,date){return state.nutrition.filter(r=>r.date===date).reduce((a,r)=>({lowKcal:a.lowKcal+r.lowKcal,highKcal:a.highKcal+r.highKcal}),{lowKcal:0,highKcal:0});}
function weightTrend(state){const rows=[...state.weights].sort((a,b)=>a.date.localeCompare(b.date)||a.id.localeCompare(b.id));if(!rows.length)return null;return {first:rows[0],latest:rows[rows.length-1],change:rows.length<2?null:Math.round((rows[rows.length-1].kg-rows[0].kg)*1000)/1000,count:rows.length};}
function reviewFood(input){if(typeof input==='string'){if(input.length>20000)throw Error('识别结果过大');input=JSON.parse(input);}if(!input||typeof input!=='object')throw Error('识别结果无效');return {name:shortText(input.name,100,true).trim(),portionGrams:finiteRange(input.portionGrams,0.01,1000000),caloriesLow:finiteRange(input.caloriesLow,0,1000000),caloriesHigh:finiteRange(input.caloriesHigh,input.caloriesLow,1000000),confidence:shortText(input.confidence===undefined?'未知':String(input.confidence),80,false),note:shortText(input.note||'',200,false)};}

return {cleanFavorite,favoriteNameKey,findFavorite,addFavorite,updateFavorite,removeFavorite,favoriteFromNutrition,scaleFavorite,energyKcal,addNutrition,updateNutrition,removeNutrition,addWeight,updateWeight,removeWeight,nutritionTotal,weightTrend,reviewFood,CATEGORIES,TYPES,totals,reviewRecognition,importReviewed,duplicateKey,KEY,defaultState,validDate,normalizeDate,cents,validate,parse,load,save,add,update,remove,total,level,crossings};
});
