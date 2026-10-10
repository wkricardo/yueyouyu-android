package cn.dot.budget;

import java.util.UUID;

/** One source flow owns the native picker, consent and request until released. */
public final class AiRequestSession {
 public static final class Ticket {
  public final AiRequestContract request;
  public final String id,sessionId;
  public final int generation;
  private Ticket(AiRequestContract request,String sessionId,int generation){this.request=request;this.id=UUID.randomUUID().toString();this.sessionId=sessionId;this.generation=generation;}
 }
 private String sessionId=UUID.randomUUID().toString();
 private boolean alive=true;
 private Ticket active;
 private int generation;
 public synchronized Ticket reserve(AiRequestContract request){if(request==null)throw new IllegalArgumentException("Missing request");if(!alive||active!=null)return null;if(generation==Integer.MAX_VALUE)throw new IllegalStateException("Session exhausted");active=new Ticket(request,sessionId,++generation);return active;}
 public synchronized boolean isCurrent(Ticket ticket){return ticket!=null&&ticket==active;}
 public synchronized boolean isCurrent(String session,String id,int generation,String mode,String source,int revision){return active!=null&&active.sessionId.equals(session)&&active.id.equals(id)&&active.generation==generation&&active.request.mode.equals(mode)&&active.request.source.equals(source)&&active.request.revision==revision;}
 public synchronized boolean ownsAttempt(String attempt){return active!=null&&active.request.clientAttempt.equals(attempt);}
 public synchronized boolean release(Ticket ticket){if(!isCurrent(ticket))return false;active=null;return true;}
 public boolean cancel(Ticket ticket,Runnable disconnect){synchronized(this){if(!release(ticket))return false;}if(disconnect!=null)disconnect.run();return true;}
 public synchronized Ticket reset(){Ticket previous=active;active=null;sessionId=UUID.randomUUID().toString();return previous;}
 public synchronized Ticket close(){Ticket previous=reset();alive=false;return previous;}
 public synchronized boolean isAlive(){return alive;}
 public synchronized boolean busy(){return active!=null;}
}
