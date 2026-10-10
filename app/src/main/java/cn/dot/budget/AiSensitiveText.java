package cn.dot.budget;
import java.util.regex.Pattern;
/** Conservative local guard only; not an exhaustive credential detector or redactor. */
public final class AiSensitiveText {
 private static final Pattern CREDENTIAL=Pattern.compile("(?i)(?:\\bsk-[a-z0-9_-]{8,}|(?:密码|验证码|口令|api[ _-]*key|api[ _-]*密钥|password|otp|token)\\s*[:：=]?\\s*[^\\s，,。;；]{3,}|(?:\\d[ -]?){12,}|(?:卡号|账号|账户号|银行卡|account\\s*(?:number|no))\\s*[:：=]?\\s*[a-z0-9-]{5,})");
 private AiSensitiveText(){}
 public static boolean blocked(String text){return text!=null&&CREDENTIAL.matcher(text).find();}
}
