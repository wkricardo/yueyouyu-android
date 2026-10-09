(function(){'use strict';
let largestHeight=0,lastWidth=window.innerWidth,initialized=false;
const dialogStack=[];
function beforeDialogOpen(dialog){const index=dialogStack.indexOf(dialog);if(index!==-1)dialogStack.splice(index,1);dialogStack.push(dialog);}
function topDialog(){for(let index=dialogStack.length-1;index>=0;index--)if(dialogStack[index].isConnected&&dialogStack[index].open)return dialogStack[index];return [...document.querySelectorAll('dialog[open]')].at(-1)||null;}
function syncViewport(){
 const viewport=window.visualViewport,height=viewport?viewport.height:window.innerHeight;
 const focused=document.activeElement,editing=!!(focused&&focused.matches('input:not([type=checkbox]):not([type=radio]),textarea,select,[contenteditable=true]'));
 if(!editing||Math.abs(window.innerWidth-lastWidth)>80)largestHeight=Math.max(window.innerHeight,height);
 lastWidth=window.innerWidth;
 const keyboard=editing&&largestHeight-height>150&&height<largestHeight*.78;
 document.body.classList.toggle('navigation-keyboard-open',keyboard);
 document.body.classList.toggle('navigation-dialog-open',!!document.querySelector('dialog[open]'));
 const toast=document.getElementById('toast');if(toast&&toast.parentElement.matches('dialog:not([open])')){toast.hidden=true;delete toast.dataset.modalHost;document.body.append(toast);}
}
function init(){
 if(initialized)return;
 const budget=document.getElementById('budget-nav'),wellness=document.getElementById('wellness-nav');
 if(!budget||!wellness)return;initialized=true;
 for(const button of budget.querySelectorAll('[data-budget-route]')){
  const source=wellness.querySelector('[data-wellness-route="'+button.dataset.budgetRoute+'"] span');
  if(source)button.querySelector('span').replaceWith(source.cloneNode(true));
 }
 window.addEventListener('resize',syncViewport);if(window.visualViewport)window.visualViewport.addEventListener('resize',syncViewport);
 document.addEventListener('focusin',syncViewport);document.addEventListener('focusout',()=>setTimeout(syncViewport,0));
 if(window.MutationObserver){const observer=new MutationObserver(records=>{if(records.some(record=>record.type==='attributes'?record.target.tagName==='DIALOG':[...record.addedNodes,...record.removedNodes].some(node=>node.nodeType===1&&(node.matches('dialog')||node.querySelector('dialog')))))syncViewport();});observer.observe(document.body,{subtree:true,childList:true,attributes:true,attributeFilter:['open']});}
 syncViewport();
 for(const nav of [budget,wellness]){nav.classList.add('floating-navigation');for(const button of nav.querySelectorAll('button')){button.type='button';button.classList.add('navigation-destination');}}
}
window.NavigationUI={init,syncViewport,beforeDialogOpen,topDialog};
})();
