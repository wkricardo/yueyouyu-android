package cn.dot.budget;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** The WebView may choose a source, never a native request identity or hidden context. */
public final class AiRequestContract {
 public static final int VERSION=1, MAX_REQUEST_BYTES=16384;
 public final int version, revision;
 public final String mode, source, clientAttempt, text, retryOf;
 public AiRequestContract(int version,String mode,String source,int revision){this(version,mode,source,revision,"legacy-"+java.util.UUID.randomUUID().toString());}
 public AiRequestContract(int version,String mode,String source,int revision,String clientAttempt){this(version,mode,source,revision,clientAttempt,null);}
 public AiRequestContract(int version,String mode,String source,int revision,String clientAttempt,String text){this(version,mode,source,revision,clientAttempt,text,null);}
 public AiRequestContract(int version,String mode,String source,int revision,String clientAttempt,String text,String retryOf){
  if((retryOf!=null&&!retryOf.matches("[A-Za-z0-9-]{1,80}"))||version!=VERSION||revision<0||clientAttempt==null||!clientAttempt.matches("[A-Za-z0-9-]{1,80}")||!("budget".equals(mode)&&("statement_image".equals(source)||"budget_text".equals(source))||"food".equals(mode)&&("food_image".equals(source)||"food_camera".equals(source)||"food_text".equals(source))))throw new IllegalArgumentException("AI 请求不受支持");
  boolean typed="budget_text".equals(source)||"food_text".equals(source);if(typed)validateText(text);else if(text!=null)throw new IllegalArgumentException("图片请求不能包含附加文字");this.text=text;this.retryOf=retryOf;
  this.version=version;this.mode=mode;this.source=source;this.revision=revision;this.clientAttempt=clientAttempt;
 }
 public static void requireRequestSize(String json){if(json==null||json.length()>MAX_REQUEST_BYTES||json.getBytes(StandardCharsets.UTF_8).length>MAX_REQUEST_BYTES)throw new IllegalArgumentException("AI 请求过大");}
 public static String validateText(String text){if(text==null||text.trim().isEmpty()||text.length()>4000||text.getBytes(StandardCharsets.UTF_8).length>12000)throw new IllegalArgumentException("文字内容需为 1–4000 字且不超过 12000 字节");AiJson.unicode(text);return text;}
 public static void assertFields(Set<String> fields){Set<String> base=new HashSet<String>(Arrays.asList("version","mode","source","revision","clientAttempt"));Set<String> typed=new HashSet<String>(base);typed.add("text");Set<String> retry=new HashSet<String>(base);retry.add("retryOf");Set<String> typedRetry=new HashSet<String>(typed);typedRetry.add("retryOf");if(!fields.equals(base)&&!fields.equals(typed)&&!fields.equals(retry)&&!fields.equals(typedRetry))throw new IllegalArgumentException("AI 请求字段不受支持");}
}
