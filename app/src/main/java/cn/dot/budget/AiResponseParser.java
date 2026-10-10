package cn.dot.budget;

import java.util.*;

/** AI can return only review data. Empty/malformed responses never replace an existing draft. */
public final class AiResponseParser {
 public static final int MAX_CONTENT_BYTES=1000000;
 private AiResponseParser(){}
 @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value){if(!(value instanceof Map))throw bad("结果应为对象");return (Map<String,Object>)value;}
 private static IllegalArgumentException bad(String reason){return new IllegalArgumentException(reason+"，原草稿保持不变");}
 private static void allowed(Map<String,Object> value,String... fields){Set<String> set=new HashSet<>(Arrays.asList(fields));if(!set.containsAll(value.keySet()))throw bad("结果含不受支持字段");}
 private static String text(Object value,int max){if(!(value instanceof String)||((String)value).length()>max)throw bad("结果文字字段无效");String result=(String)value;for(int i=0;i<result.length();i++){char c=result.charAt(i);if(c<32&&c!=9&&c!=10&&c!=13||c>=127&&c<=159||c>=0x202a&&c<=0x202e||c>=0x2066&&c<=0x2069)throw bad("结果含不可见控制字符");}return result;}
 private static double number(Object value,double max){if(!(value instanceof Number))throw bad("估算数字无效");double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<0||n>max)throw bad("估算数字超出范围");return n;}
 private static Double optionalNumber(Map<String,Object> draft,String field,double max){Object value=draft.get(field);if(value==null){draft.put(field,null);return null;}return number(value,max);}
 public static String parse(String response,String mode){
  if(!"budget".equals(mode)&&!"food".equals(mode))throw bad("识别模式无效");Map<String,Object> envelope=object(AiJson.parse(response));allowed(envelope,"id","object","created","model","choices","usage","system_fingerprint");
  Object choicesValue=envelope.get("choices");if(!(choicesValue instanceof List)||((List<?>)choicesValue).size()!=1)throw bad("服务未返回唯一完整结果");Map<String,Object> choice=object(((List<?>)choicesValue).get(0));allowed(choice,"index","message","finish_reason","logprobs");if(!"stop".equals(choice.get("finish_reason")))throw bad("识别结果不完整，请缩小输入后重新确认");
  Map<String,Object> message=object(choice.get("message"));allowed(message,"role","content","refusal","reasoning_content");if(!"assistant".equals(message.get("role")))throw bad("结果角色无效");if(message.get("refusal")!=null&&!"".equals(message.get("refusal")))throw bad("服务未能提供识别结果");if(message.get("reasoning_content")!=null&&!"".equals(message.get("reasoning_content")))throw bad("结果含不受支持内容");
  String content=text(message.get("content"),MAX_CONTENT_BYTES);Map<String,Object> draft=object(AiJson.parse(content,MAX_CONTENT_BYTES));
  if("food".equals(mode)&&draft.containsKey("outcome")){allowed(draft,"outcome","reason");if(!"unrecognized".equals(draft.get("outcome"))||!Arrays.asList("nonfood","unreadable","insufficient").contains(draft.get("reason")))throw bad("未识别状态无效");return AiJson.stringify(draft);}
  if("budget".equals(mode)){allowed(draft,"records");Object records=draft.get("records");if(!(records instanceof List)||((List<?>)records).isEmpty()||((List<?>)records).size()>100)throw bad("未识别到交易或超过 100 笔");for(Object item:(List<?>)records){Map<String,Object> row=object(item);allowed(row,"date","rawDate","type","category","amount","note","source","currency","rawAmount");for(String field:row.keySet())text(row.get(field),"note".equals(field)?200:"rawDate".equals(field)?120:"source".equals(field)||"category".equals(field)||"date".equals(field)?80:40);if(row.isEmpty())throw bad("交易记录为空");if(!row.containsKey("currency"))row.put("currency","");if(!row.containsKey("rawAmount"))row.put("rawAmount",row.containsKey("amount")?row.get("amount"):"");}}
  else {allowed(draft,"name","portionGrams","caloriesLow","caloriesHigh","confidence","note");if(text(draft.get("name"),100).trim().isEmpty())throw bad("未识别到可记录的食物");Double grams=optionalNumber(draft,"portionGrams",100000),low=optionalNumber(draft,"caloriesLow",1000000),high=optionalNumber(draft,"caloriesHigh",1000000);if(grams!=null&&grams<0.01||low!=null&&high!=null&&high<low)throw bad("食物份量或热量范围无效");if(draft.get("confidence")==null)draft.put("confidence","unknown");if(!Arrays.asList("low","medium","high","unknown").contains(text(draft.get("confidence"),10)))throw bad("估算不确定性格式无效");if(!draft.containsKey("note"))draft.put("note","");text(draft.get("note"),200);}
  return AiJson.stringify(draft);
 }
}
