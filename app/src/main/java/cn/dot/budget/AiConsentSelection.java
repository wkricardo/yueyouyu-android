package cn.dot.budget;

/** Replaceable preview slot. A dismissed/replaced preview can never authorize a send. */
public final class AiConsentSelection {
 private AiRequestSession.Ticket owner;private AiConsentSnapshot selected;
 public synchronized void replace(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot){if(ticket==null||snapshot==null||!snapshot.matches(ticket))throw new IllegalArgumentException("Preview owner mismatch");owner=ticket;selected=snapshot;}
 public synchronized boolean isCurrent(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot){return ticket!=null&&snapshot!=null&&owner==ticket&&selected==snapshot&&snapshot.matches(ticket);}
 public synchronized boolean invalidate(AiRequestSession.Ticket ticket){if(ticket==null||ticket!=owner)return false;reset();return true;}
 public synchronized boolean consume(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot){if(!isCurrent(ticket,snapshot))return false;reset();return true;}
 public synchronized void reset(){owner=null;selected=null;}
}
