package cn.dot.budget;

/** Deletion is reported as successful only after both stores verify absence. */
public final class AiKeyDeletion {
 public interface Store {
  boolean clearPreferences()throws Exception;
  boolean preferencesEmpty()throws Exception;
  void deleteAlias()throws Exception;
  boolean aliasAbsent()throws Exception;
 }
 public static final class Result {
  public final boolean preferencesCleared,aliasCleared,success;
  private Result(boolean preferences,boolean alias){preferencesCleared=preferences;aliasCleared=alias;success=preferences&&alias;}
 }
 private AiKeyDeletion(){}
 public static Result clear(Store store,Runnable invalidate){if(store==null||invalidate==null)throw new IllegalArgumentException("Missing deletion boundary");invalidate.run();boolean preferences=false,alias=false;try{boolean committed=store.clearPreferences();preferences=committed&&store.preferencesEmpty();}catch(Exception ignored){}try{store.deleteAlias();alias=store.aliasAbsent();}catch(Exception ignored){}return new Result(preferences,alias);}
}
