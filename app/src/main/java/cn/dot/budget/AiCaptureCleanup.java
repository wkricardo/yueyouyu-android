package cn.dot.budget;

/** Best-effort cleanup of this request's one private camera file and outgoing URI grant. */
public final class AiCaptureCleanup {
 public interface Resources {void revokeGrant()throws Exception;boolean deleteTemporaryFile()throws Exception;boolean temporaryFileAbsent()throws Exception;}
 public static final class Result {public final boolean grantRevoked,fileRemoved,success;private Result(boolean grant,boolean file){grantRevoked=grant;fileRemoved=file;success=grant&&file;}}
 private AiCaptureCleanup(){}
 public static Result clean(Resources resources){if(resources==null)throw new IllegalArgumentException("Missing capture resources");boolean grant=false,file=false;try{resources.revokeGrant();grant=true;}catch(Exception ignored){}try{boolean deleted=resources.deleteTemporaryFile();file=deleted&&resources.temporaryFileAbsent();}catch(Exception ignored){}return new Result(grant,file);}
}
