package cn.dot.budget;

import java.util.*;

/** A queued WebView event is usable once, and only in the native epoch that issued it. */
public final class AiDeliveryGate {
 public static final int MAX_PENDING=256,MAX_RECEIPTS=32;
 public static final class Owner {public final String requestId;private final long epoch;private boolean terminal;private Owner(String id,long epoch){this.requestId=id;this.epoch=epoch;}}
 private static final class Delivery {final String id,event;final long epoch;Delivery(String id,String event,long epoch){this.id=id;this.event=event;this.epoch=epoch;}}
 private long epoch=1;private boolean alive=true;
 private final Set<Owner> owners=Collections.newSetFromMap(new IdentityHashMap<Owner,Boolean>());
 private final Map<String,Delivery> pending=new LinkedHashMap<>();
 private final Map<String,Delivery> receipts=new LinkedHashMap<>();
 public synchronized Owner begin(String id){if(!alive||id==null||!id.matches("[A-Za-z0-9-]{1,80}")||owners.size()>=32)throw new IllegalStateException("Delivery owner unavailable");Owner owner=new Owner(id,epoch);owners.add(owner);return owner;}
 public synchronized String issue(Owner owner,String event){if(!alive||owner==null||owner.epoch!=epoch||!owners.contains(owner)||owner.terminal)return null;if(!Arrays.asList("started","phase","result","error","cancelled").contains(event))throw new IllegalArgumentException("Unsupported event");String token=put(owner.requestId,event);if(Arrays.asList("result","error","cancelled").contains(event))owner.terminal=true;return token;}
 private String put(String id,String event){if(pending.size()>=MAX_PENDING)return null;String token=UUID.randomUUID().toString();pending.put(token,new Delivery(id,event,epoch));return token;}
 public synchronized String cancellation(String id){if(!alive||id==null||!id.matches("[A-Za-z0-9-]{1,80}"))return null;return put(id,"cancelled");}
 public synchronized void finish(Owner owner){owners.remove(owner);}
 public synchronized boolean consume(String token,String id,String event){if(!alive||token==null)return false;Delivery delivery=pending.get(token);if(delivery==null||delivery.epoch!=epoch||!delivery.id.equals(id)||!delivery.event.equals(event))return false;pending.remove(token);if("result".equals(event)){if(receipts.size()>=MAX_RECEIPTS)receipts.remove(receipts.keySet().iterator().next());receipts.put(token,delivery);}return true;}
 public synchronized boolean isResultCurrent(String token,String id){Delivery receipt=receipts.get(token);return alive&&receipt!=null&&receipt.epoch==epoch&&receipt.id.equals(id);}
 public synchronized void revokeRequest(String id){owners.removeIf(owner->owner.requestId.equals(id));pending.values().removeIf(delivery->delivery.id.equals(id));receipts.values().removeIf(delivery->delivery.id.equals(id));}
 public synchronized void invalidate(){epoch++;owners.clear();pending.clear();receipts.clear();}
 public synchronized void close(){invalidate();alive=false;}
}
