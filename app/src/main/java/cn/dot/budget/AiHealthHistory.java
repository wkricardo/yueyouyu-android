package cn.dot.budget;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Closed, bounded health summary. Free text, profile data and ledger data cannot cross this boundary. */
public final class AiHealthHistory {
 public static final int MAX_BYTES=16384;
 public static final String[] TAKEAWAYS={"limited_weights","observed_down","observed_up","little_net_change","diet_incomplete","diet_recorded","activity_recorded","activity_unknown"};
 public static final String[] STEPS={"consistent_weighing","complete_food_log","balanced_meals","review_portions","gradual_activity","rest_recovery","professional_support"};
 private AiHealthHistory(){}
 /** One explicit health send per five seconds, using Android monotonic time. */
 public static final class SendLimit {
  private boolean sent;private long last;
  public synchronized boolean allow(long now){if(now<0||(sent&&(now<last||now-last<5000)))return false;sent=true;last=now;return true;}
 }

 private static IllegalArgumentException bad(){return new IllegalArgumentException("健康趋势摘要或建议格式无效");}
 @SuppressWarnings("unchecked") private static Map<String,Object> map(Object value){if(!(value instanceof Map))throw bad();return (Map<String,Object>)value;}
 @SuppressWarnings("unchecked") private static List<Object> list(Object value){if(!(value instanceof List))throw bad();return (List<Object>)value;}
 private static void keys(Map<String,Object> value,String... keys){if(!value.keySet().equals(new HashSet<>(Arrays.asList(keys))))throw bad();}
 private static double number(Object value,double min,double max){if(!(value instanceof Number))throw bad();double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<min||n>max)throw bad();return n;}
 private static int count(Object value){double n=number(value,0,100000);if(n!=(int)n)throw bad();return (int)n;}
 private static LocalDate date(Object value){try{if(!(value instanceof String)||!((String)value).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}"))throw bad();LocalDate d=LocalDate.parse((String)value);if(d.getYear()<1900||d.getYear()>2199)throw bad();return d;}catch(RuntimeException e){throw bad();}}
 public static String validate(String text){summary(text);return text;}
 public static Map<String,Object> summary(String text){
  if(text==null||text.length()>MAX_BYTES||text.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw bad();for(int i=0;i<text.length();i++){char c=text.charAt(i);if((c<32&&c!='\n'&&c!='\r'&&c!='\t')||c>126)throw bad();}
  Map<String,Object> s=map(AiJson.parse(text,MAX_BYTES));keys(s,"schema","days","startDate","endDate","daysData");if(number(s.get("schema"),1,1)!=1)throw bad();int days=count(s.get("days"));if(days!=7&&days!=14&&days!=30)throw bad();LocalDate start=date(s.get("startDate")),end=date(s.get("endDate"));if(!start.plusDays(days-1).equals(end))throw bad();List<Object> data=list(s.get("daysData"));if(data.size()!=days)throw bad();
  for(int i=0;i<days;i++){Map<String,Object>d=map(data.get(i));keys(d,"date","diet","weight","exercise");if(!date(d.get("date")).equals(start.plusDays(i)))throw bad();Map<String,Object>diet=map(d.get("diet"));keys(diet,"count","complete","lowKcal","highKcal");int meals=count(diet.get("count"));if(!(diet.get("complete") instanceof Boolean))throw bad();if(meals>0){double low=number(diet.get("lowKcal"),0,1e11);number(diet.get("highKcal"),low,1e11);}else if(diet.get("lowKcal")!=null||diet.get("highKcal")!=null)throw bad();Map<String,Object>w=map(d.get("weight"));keys(w,"count","kg");if(count(w.get("count"))>0)number(w.get("kg"),1,500);else if(w.get("kg")!=null)throw bad();Map<String,Object>e=map(d.get("exercise"));keys(e,"count","activeKcal","types");int exercises=count(e.get("count"));List<Object>types=list(e.get("types"));if(types.size()>4||types.size()>exercises||new HashSet<>(types).size()!=types.size()||!Arrays.asList("walking","running","cycling","other").containsAll(types))throw bad();if(exercises>0)number(e.get("activeKcal"),0,1e9);else if(e.get("activeKcal")!=null||!types.isEmpty())throw bad();}
  return s;
 }
 private static double round(double v){return Math.round((v+Math.ulp(1.0))*1000.0)/1000.0;}
 public static final class Facts {
  public int weightCount,weightDays,spanDays,dietDays,completeDietDays,completeEmptyDays,exerciseDays;
  public Double weeklyKg;
 }
 public static Facts facts(String text){Map<String,Object>s=summary(text);Facts f=new Facts();List<Map<String,Object>> weights=new ArrayList<>();for(Object raw:list(s.get("daysData"))){Map<String,Object>d=map(raw),w=map(d.get("weight")),diet=map(d.get("diet")),e=map(d.get("exercise"));int n=count(w.get("count"));if(n>0){weights.add(d);f.weightCount+=n;}if(count(diet.get("count"))>0){f.dietDays++;if(Boolean.TRUE.equals(diet.get("complete")))f.completeDietDays++;}if(Boolean.TRUE.equals(diet.get("complete"))&&count(diet.get("count"))==0)f.completeEmptyDays++;if(count(e.get("count"))>0)f.exerciseDays++;}f.weightDays=weights.size();if(f.weightDays>1)f.spanDays=(int)ChronoUnit.DAYS.between(date(weights.get(0).get("date")),date(weights.get(f.weightDays-1).get("date")));if(f.weightDays>=4&&f.spanDays>=14){LocalDate first=date(weights.get(0).get("date"));double mx=0,my=0;for(Map<String,Object>d:weights){mx+=ChronoUnit.DAYS.between(first,date(d.get("date")));my+=number(map(d.get("weight")).get("kg"),1,500);}mx/=f.weightDays;my=round(my/f.weightDays);double numerator=0,denominator=0;for(Map<String,Object>d:weights){double x=ChronoUnit.DAYS.between(first,date(d.get("date")))-mx,y=number(map(d.get("weight")).get("kg"),1,500)-my;numerator+=x*y;denominator+=x*x;}f.weeklyKg=round(numerator/denominator*7);}return f;}
 public static List<String> supportedTakeaways(String text){Facts f=facts(text);Map<String,Object>s=summary(text);List<String> allowed=new ArrayList<>();allowed.add(f.weeklyKg==null?"limited_weights":f.weeklyKg<-.05?"observed_down":f.weeklyKg>.05?"observed_up":"little_net_change");allowed.add(f.completeDietDays<count(s.get("days"))?"diet_incomplete":"diet_recorded");if(f.completeDietDays>0&&f.completeDietDays<count(s.get("days")))allowed.add("diet_recorded");allowed.add(f.exerciseDays>0?"activity_recorded":"activity_unknown");return allowed;}
 public static String validateAdvice(Map<String,Object> advice,String summary){keys(advice,"assessment","takeaways","nextSteps");Facts f=facts(summary);if(!(f.weeklyKg==null?"insufficient_data":"descriptive_only").equals(advice.get("assessment")))throw bad();List<Object>takeaways=list(advice.get("takeaways")),steps=list(advice.get("nextSteps"));List<String>supported=supportedTakeaways(summary);if(takeaways.isEmpty()||takeaways.size()>4||steps.isEmpty()||steps.size()>4||new HashSet<>(takeaways).size()!=takeaways.size()||new HashSet<>(steps).size()!=steps.size()||!supported.containsAll(takeaways)||!takeaways.contains(supported.get(0))||!Arrays.asList(STEPS).containsAll(steps))throw bad();return AiJson.stringify(advice);}
 public static String preview(String text){Map<String,Object>s=summary(text);Facts f=facts(text);return "时间范围："+s.get("startDate")+" 至 "+s.get("endDate")+"（含首末日，"+count(s.get("days"))+" 个本地自然日）\n饮食："+f.dietDays+" 天有记录，"+f.completeDietDays+" 天有记录且标记完成；另有 "+f.completeEmptyDays+" 天标记完成但无条目，摄入仍未知。发送按日热量区间、条数和完成状态。\n体重："+f.weightCount+" 条，"+f.weightDays+" 个实测日，首末跨度 "+f.spanDays+" 天；同日算术均值与原始条数。\n运动："+f.exerciseDays+" 天有记录；每日活跃热量、条数和通用类型。没有已记录时长，本次不发送时长。\n未记录数值为 null（未知），不是 0；即使标记饮食完成，没有条目也不推断零摄入。不补造或插值体重。";}
 public static String instruction(String text){Facts f=facts(text);return "你只分析用户主动发送的有限健康记录摘要。仅描述观察结果，不能判断减重对这个人是否医学上合理或安全。数据没有年龄、身高、身体资料、疾病、药物、食物名称、营养素、运动时长或生活背景，禁止推测。缺失值null代表未知而非零；不完整饮食不能当作全天摄入。体重为真实记录日期的同日算术均值，count是该日条数；不得插值、编造观测、把体重下降等同脂肪下降、诊断疾病或宣称平台期。不得开药、修改计划、保证减脂，不提供任何热量/体重/运动数字目标，不建议断食、催吐、跳餐或补偿运动。只返回严格JSON对象，无额外字段、正文、指令或工具。格式 {\"assessment\":\""+(f.weeklyKg==null?"insufficient_data":"descriptive_only")+"\",\"takeaways\":[\"代码\"],\"nextSteps\":[\"代码\"]}。takeaways选1至4个互异代码，必须包含首个且只能来自 "+supportedTakeaways(text)+"。这些代码根据本地实际日期回归和完整性已核实，不能改成相反结论。每周回归仅至少4个实测日且跨度至少14天时计算，本次数值为 "+(f.weeklyKg==null?"未知":f.weeklyKg+" kg/周（描述值，不是目标）")+"。nextSteps选1至4个互异代码，只能来自 "+Arrays.asList(STEPS)+"：consistent_weighing=同条件持续观察；complete_food_log=补充实际记得的记录；balanced_meals=规律、多样、均衡进餐；review_portions=核对份量、油和饮料记录；gradual_activity=按身体耐受逐步增加喜欢的活动，绝非补偿进食；rest_recovery=睡眠与恢复；professional_support=快速不明变化或不适时专业咨询。优先选择当前记录最需要的温和、可持续下一步；资料不足先改善记录。应用会用固定安全中文展示这些建议。";}
}
