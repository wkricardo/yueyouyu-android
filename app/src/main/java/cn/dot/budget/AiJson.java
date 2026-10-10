package cn.dot.budget;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small bounded JSON boundary; rejects duplicate keys, malformed Unicode and non-finite numbers. */
public final class AiJson {
 public static final int MAX_BYTES=2000000,MAX_DEPTH=16,MAX_NODES=4096;
 private final String input;private int at,nodes;
 private AiJson(String input){this.input=input;}
 public static Object parse(String input){return parse(input,MAX_BYTES);}
 public static Object parse(String input,int maxBytes){if(input==null||maxBytes<1||maxBytes>MAX_BYTES||input.length()>maxBytes||input.getBytes(StandardCharsets.UTF_8).length>maxBytes)throw bad();AiJson parser=new AiJson(input);Object result=parser.value(0);parser.space();if(parser.at!=input.length())throw bad();return result;}
 private static IllegalArgumentException bad(){return new IllegalArgumentException("AI JSON 格式无效或超过限制");}
 private void space(){while(at<input.length()&&" \t\r\n".indexOf(input.charAt(at))>=0)at++;}
 private boolean take(char c){space();if(at<input.length()&&input.charAt(at)==c){at++;return true;}return false;}
 private Object value(int depth){space();if(depth>MAX_DEPTH||++nodes>MAX_NODES||at>=input.length())throw bad();char c=input.charAt(at);
  if(c=='"')return string();
  if(c=='{'){at++;Map<String,Object> map=new LinkedHashMap<>();if(take('}'))return map;do{space();if(at>=input.length()||input.charAt(at)!='"'||++nodes>MAX_NODES)throw bad();String key=string();if(map.containsKey(key)||!take(':'))throw bad();map.put(key,value(depth+1));if(take('}'))return map;}while(take(','));throw bad();}
  if(c=='['){at++;List<Object> list=new ArrayList<>();if(take(']'))return list;do{list.add(value(depth+1));if(take(']'))return list;}while(take(','));throw bad();}
  for(String word:new String[]{"true","false","null"})if(input.startsWith(word,at)){at+=word.length();return word.equals("null")?null:Boolean.valueOf(word);}
  int start=at;if(c=='-')at++;if(at>=input.length())throw bad();if(input.charAt(at)=='0')at++;else{if(input.charAt(at)<'1'||input.charAt(at)>'9')throw bad();while(at<input.length()&&digit(input.charAt(at)))at++;}
  if(at<input.length()&&input.charAt(at)=='.'){at++;int begin=at;while(at<input.length()&&digit(input.charAt(at)))at++;if(at==begin)throw bad();}
  if(at<input.length()&&(input.charAt(at)=='e'||input.charAt(at)=='E')){at++;if(at<input.length()&&(input.charAt(at)=='+'||input.charAt(at)=='-'))at++;int begin=at;while(at<input.length()&&digit(input.charAt(at)))at++;if(at==begin)throw bad();}
  try{double number=Double.parseDouble(input.substring(start,at));if(!Double.isFinite(number))throw bad();return number;}catch(NumberFormatException e){throw bad();}
 }
 private static boolean digit(char c){return c>='0'&&c<='9';}
 private String string(){if(input.charAt(at++)!='"')throw bad();StringBuilder out=new StringBuilder();while(at<input.length()){char c=input.charAt(at++);if(c=='"'){String value=out.toString();unicode(value);return value;}if(c<32)throw bad();if(c!='\\'){out.append(c);continue;}if(at>=input.length())throw bad();char e=input.charAt(at++);switch(e){case '"':case '\\':case '/':out.append(e);break;case 'b':out.append('\b');break;case 'f':out.append('\f');break;case 'n':out.append('\n');break;case 'r':out.append('\r');break;case 't':out.append('\t');break;case 'u':if(at+4>input.length())throw bad();int code=0;for(int i=0;i<4;i++){char hex=input.charAt(at++);int part=hex>='0'&&hex<='9'?hex-'0':hex>='a'&&hex<='f'?hex-'a'+10:hex>='A'&&hex<='F'?hex-'A'+10:-1;if(part<0)throw bad();code=code*16+part;}out.append((char)code);break;default:throw bad();}}throw bad();}
 static void unicode(String value){for(int i=0;i<value.length();i++){char c=value.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=value.length()||!Character.isLowSurrogate(value.charAt(i)))throw bad();}else if(Character.isLowSurrogate(c))throw bad();}}
 public static String stringify(Object value){StringBuilder out=new StringBuilder();write(value,out,0,new int[]{0});String text=out.toString();if(text.length()>MAX_BYTES||text.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw bad();return text;}
 private static void write(Object value,StringBuilder out,int depth,int[] nodes){if(depth>MAX_DEPTH||++nodes[0]>MAX_NODES||out.length()>MAX_BYTES)throw bad();if(value==null){out.append("null");return;}if(value instanceof String){unicode((String)value);out.append(AiPayloadBuilder.quote((String)value));return;}if(value instanceof Boolean){out.append(value);return;}if(value instanceof Number){if(!Double.isFinite(((Number)value).doubleValue()))throw bad();String number=value.toString();if(!number.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?"))throw bad();out.append(number);return;}if(value instanceof Map){out.append('{');boolean first=true;for(Map.Entry<?,?> entry:((Map<?,?>)value).entrySet()){if(!(entry.getKey() instanceof String)||++nodes[0]>MAX_NODES)throw bad();if(!first)out.append(',');first=false;unicode((String)entry.getKey());out.append(AiPayloadBuilder.quote((String)entry.getKey())).append(':');write(entry.getValue(),out,depth+1,nodes);}out.append('}');return;}if(value instanceof List){out.append('[');boolean first=true;for(Object item:(List<?>)value){if(!first)out.append(',');first=false;write(item,out,depth+1,nodes);}out.append(']');return;}throw bad();}
}
