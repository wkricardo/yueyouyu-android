package cn.dot.budget;

/** Versioned native scopes. A per-use acceptance is never a persistent grant. */
public final class AiRememberedConsent {
 public static final int VERSION=1;
 public static final String[] SCOPES={"food_image","food_text","statement_image","budget_text"};
 public interface Store {int version();boolean allowed(String scope);boolean write(String scope);boolean clear();boolean empty();}
 private final Store store;
 private boolean blocked;
 private int generation;
 public AiRememberedConsent(Store store){if(store==null)throw new IllegalArgumentException();this.store=store;}
 public static String scope(AiRequestContract request){if(request==null)throw new IllegalArgumentException();String scope="food_camera".equals(request.source)?"food_image":request.source;for(String known:SCOPES)if(known.equals(scope))return scope;throw new IllegalArgumentException("Unknown consent scope");}
 public synchronized int generation(){return generation;}
 public synchronized boolean allows(AiRequestContract request){return request!=null&&allowsScope(scope(request));}
 public synchronized boolean allowsScope(String scope){try{boolean known=false;for(String item:SCOPES)if(item.equals(scope))known=true;return known&&!blocked&&store.version()==VERSION&&store.allowed(scope);}catch(RuntimeException e){return false;}}
 public synchronized boolean remember(AiRequestContract request,boolean explicitlyChecked,int openedGeneration){if(!explicitlyChecked||openedGeneration!=generation)return false;try{if(store.write(scope(request))&&store.version()==VERSION&&store.allowed(scope(request))){blocked=false;return true;}}catch(RuntimeException ignored){}blocked=true;return false;}
 public boolean revoke(Runnable cancel){synchronized(this){blocked=true;generation++;}cancel.run();try{return store.clear()&&store.empty();}catch(RuntimeException e){return false;}}
 public synchronized boolean current(int openedGeneration){return generation==openedGeneration;}
 public static String label(String scope){switch(scope){case "food_image":return "食物照片（拍照或选择）";case "food_text":return "食物文字";case "statement_image":return "已遮盖敏感字段的账单图片";case "budget_text":return "已移除敏感字段的记账文字";default:throw new IllegalArgumentException();}}
}
