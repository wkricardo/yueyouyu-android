(function(root,factory){'use strict';const api=factory(typeof module==='object'&&module.exports?require('./ai-contract.js'):root.AiContract);if(typeof module==='object'&&module.exports)module.exports=api;else root.AiSession=api;})(typeof window!=='undefined'?window:this,function(C){'use strict';
 const START_TIMEOUT_MS=10000,SEND_TIMEOUT_MS=130000;
 function create(options={}){let pending=null,active=null,sessionId=null,lastGeneration=0,state='idle',alive=true,deadline=null;const usedAttempts=new Set();
  function clearDeadline(){if(deadline!==null)clearTimeout(deadline);deadline=null;}
  function armDeadline(milliseconds,phase){clearDeadline();if(typeof options.onTimeout!=='function')return;const attempt=owner().clientAttempt;deadline=setTimeout(()=>{const current=owner();if(!alive||!current||current.clientAttempt!==attempt)return;deadline=null;pending=null;active=null;state='error';options.onTimeout(current,phase);},milliseconds);}
  function sameRequest(a,b){return a.mode===b.mode&&a.source===b.source&&a.revision===b.revision&&a.clientAttempt===b.clientAttempt;}
  function begin(input){const request=C.validateRequest(input);if(!alive||pending||active||usedAttempts.has(request.clientAttempt)||usedAttempts.size>=4096)return false;usedAttempts.add(request.clientAttempt);pending=request;state='preparing';armDeadline(START_TIMEOUT_MS,'startup');return true;}
  function accept(input){if(!alive)return false;let event;try{event=C.validateEvent(input);}catch(_){return false;}
   if(event.event==='started'){if(!pending||active||!sameRequest(pending,event)||event.generation<=lastGeneration||(sessionId&&sessionId!==event.sessionId))return false;sessionId=event.sessionId;lastGeneration=event.generation;active=Object.freeze({...event});pending=null;state='preparing';clearDeadline();return true;}
   if(pending&&!active&&['cancelled','error'].includes(event.event)&&sameRequest(pending,event)&&event.generation>lastGeneration&&(!sessionId||sessionId===event.sessionId)){sessionId=event.sessionId;lastGeneration=event.generation;pending=null;state=event.event;clearDeadline();return true;}
   if(!active||event.sessionId!==active.sessionId||event.requestId!==active.requestId||event.generation!==active.generation||!sameRequest(active,event))return false;
   if(event.event==='phase'){if(state==='sending'&&event.phase!=='sending')return false;if(event.phase==='sending'&&state!=='sending')armDeadline(SEND_TIMEOUT_MS,'sending');state=event.phase;return true;}
   active=null;state=event.event==='result'?'ready':event.event;clearDeadline();return true;
  }
  function owner(){return active||pending;}
  function invalidateAttempt(attempt){const current=owner();if(!current||current.clientAttempt!==attempt)return false;invalidate();return true;}
  function invalidate(){clearDeadline();pending=null;active=null;if(alive)state='cancelled';else state='closed';}
  function destroy(){invalidate();alive=false;state='closed';}
  return Object.freeze({begin,accept,invalidate,invalidateAttempt,owner,destroy,current:()=>active,status:()=>state});
 }
 return Object.freeze({create,START_TIMEOUT_MS,SEND_TIMEOUT_MS});
});
