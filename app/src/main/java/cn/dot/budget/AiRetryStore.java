package cn.dot.budget;

import java.util.Objects;

/** At most one failed outbound snapshot is retained in memory, for one explicit retry preview. */
public final class AiRetryStore {
 private AiRequestSession.Ticket failed;private AiConsentSnapshot snapshot;
 public synchronized void offer(AiRequestSession.Ticket ticket,AiConsentSnapshot value){if(ticket==null||value==null||!value.matches(ticket))throw new IllegalArgumentException("Retry owner mismatch");failed=ticket;snapshot=value;}
 public synchronized boolean availableFor(String requestId){return failed!=null&&failed.id.equals(requestId);}
 public synchronized AiConsentSnapshot take(AiRequestSession.Ticket replacement)throws Exception{
  if(replacement==null||failed==null||!failed.id.equals(replacement.request.retryOf)||replacement.id.equals(failed.id)||!replacement.sessionId.equals(failed.sessionId)||!replacement.request.mode.equals(failed.request.mode)||!replacement.request.source.equals(failed.request.source)||replacement.request.revision!=failed.request.revision||!Objects.equals(replacement.request.text,failed.request.text)||replacement.request.clientAttempt.equals(failed.request.clientAttempt))throw new IllegalArgumentException("重试内容已失效，请重新输入或选择");
  AiConsentSnapshot result=snapshot.rebindForRetry(replacement);clear();return result;
 }
 public synchronized void clear(){failed=null;snapshot=null;}
}
