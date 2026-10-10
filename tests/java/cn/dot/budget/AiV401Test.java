package cn.dot.budget;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** Offline production-policy tests; no Android/device/API reproduction is claimed. */
public final class AiV401Test {
 private static int checks;
 private static void ok(boolean condition){checks++;if(!condition)throw new AssertionError("v4.0.1 check "+checks);}
 interface Work {void run()throws Exception;}
 private static void reject(Work work)throws Exception{checks++;try{work.run();}catch(IllegalArgumentException|IOException expected){return;}throw new AssertionError("Expected reject "+checks);}
 static class Store implements AiRememberedConsent.Store {
  int revision;Set<String> values=new HashSet<>();boolean writeSuccess=true,clearSuccess=true,readback=true;
  public int version(){return revision;} public boolean allowed(String scope){return readback&&values.contains(scope);}
  public boolean write(String scope){if(revision!=AiRememberedConsent.VERSION)values.clear();revision=AiRememberedConsent.VERSION;values.add(scope);return writeSuccess;}
  public boolean clear(){if(clearSuccess){values.clear();revision=0;}return clearSuccess;}public boolean empty(){return values.isEmpty()&&revision==0;}
 }
 static InputStream generated(long length){return new InputStream(){long remaining=length;public int read(){return remaining-->0?7:-1;}public int read(byte[] b,int off,int len){if(remaining==0)return -1;int count=(int)Math.min(remaining,len);Arrays.fill(b,off,off+count,(byte)7);remaining-=count;return count;}};}
 static final OutputStream DISCARD=new OutputStream(){public void write(int value){}public void write(byte[] bytes,int off,int len){}};
 static AiRequestContract request(String source){return new AiRequestContract(1,source.startsWith("food")?"food":"budget",source,3,"test-"+source.replace("_","-"),source.endsWith("_text")?"合成午餐32元":null,null);}
 public static void main(String[] args)throws Exception{
  AiRequestContract photo=request("food_image"),camera=request("food_camera"),foodText=request("food_text"),bill=request("statement_image"),billText=request("budget_text");
  Store store=new Store();AiRememberedConsent consent=new AiRememberedConsent(store);
  for(AiRequestContract r:Arrays.asList(photo,camera,foodText,bill,billText))ok(!consent.allows(r));
  int generation=consent.generation();ok(!consent.remember(photo,false,generation));ok(store.values.isEmpty());
  ok(consent.remember(photo,true,generation));ok(consent.allows(photo)&&consent.allows(camera));ok(!consent.allows(foodText)&&!consent.allows(bill)&&!consent.allows(billText));
  AiRememberedConsent restart=new AiRememberedConsent(store);ok(restart.allows(photo));ok(restart.remember(bill,true,restart.generation()));ok(restart.allows(bill));ok(!restart.allows(billText));
  AtomicInteger cancelled=new AtomicInteger();int beforeRevoke=restart.generation();ok(restart.revoke(cancelled::incrementAndGet));ok(cancelled.get()==1);ok(!restart.current(beforeRevoke));ok(!restart.remember(foodText,true,beforeRevoke));ok(!new AiRememberedConsent(store).allows(photo));
  store.revision=0;store.values.addAll(Arrays.asList(AiRememberedConsent.SCOPES));AiRememberedConsent legacy=new AiRememberedConsent(store);for(AiRequestContract r:Arrays.asList(photo,camera,foodText,bill,billText))ok(!legacy.allows(r));
  store.writeSuccess=false;ok(!legacy.remember(photo,true,legacy.generation()));ok(!legacy.allows(photo));store.writeSuccess=true;store.readback=false;ok(!legacy.remember(photo,true,legacy.generation()));ok(!legacy.allows(photo));store.readback=true;ok(legacy.remember(photo,true,legacy.generation()));
  store.clearSuccess=false;ok(!legacy.revoke(cancelled::incrementAndGet));ok(!legacy.allows(photo));ok(cancelled.get()==2);store.clearSuccess=true;ok(legacy.revoke(cancelled::incrementAndGet));
  ok(legacy.remember(photo,true,legacy.generation()));AiRequestContract retry=new AiRequestContract(1,"food","food_image",3,"new-attempt",null,"previous-id");ok(legacy.allows(retry)); // Sending still requires frozen retry store identity and an explicit UI action.
  ok(!legacy.allowsScope("food_health"));ok(!legacy.allowsScope("all"));
  ok(AiBoundedBytes.MAX_BYTES==9_999_999);AiBoundedBytes bounded=new AiBoundedBytes(AiBoundedBytes.MAX_BYTES);bounded.write(new byte[9_999_999]);ok(bounded.size()==9_999_999);reject(()->bounded.write(1));ok(bounded.exceeded());reject(()->new AiBoundedBytes(10_000_000));
  byte[] lastValid=new byte[9_999_999];String payload=AiPayloadBuilder.image(photo,lastValid,"image/jpeg");ok(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=15_000_000);reject(()->AiPayloadBuilder.image(photo,new byte[10_000_000],"image/jpeg"));
  AiRequestSession session=new AiRequestSession();AiRequestSession.Ticket ticket=session.reserve(photo);AiConsentSnapshot snapshot=new AiConsentSnapshot(ticket,lastValid,"image/jpeg",b->AiPayloadBuilder.image(photo,b,"image/jpeg"));ok(snapshot.copyImageBytes().length==9_999_999);reject(()->new AiConsentSnapshot(ticket,new byte[10_000_000],"image/jpeg",b->"{}"));reject(()->new AiConsentSnapshot(ticket,new byte[]{1},"image/jpeg",b->String.join("",Collections.nCopies(5_000_001,"餐"))));
  // Originals above both former binary10MiB and new decimal10MB are streamed successfully.
  ok(AiSourceCopy.copy(generated(16_000_000),DISCARD,()->false)==16_000_000);ok(AiSourceCopy.copy(generated(AiSourceCopy.MAX_SOURCE_BYTES),DISCARD,()->false)==AiSourceCopy.MAX_SOURCE_BYTES);reject(()->AiSourceCopy.copy(generated(AiSourceCopy.MAX_SOURCE_BYTES+1),DISCARD,()->false));reject(()->AiSourceCopy.copy(generated(0),DISCARD,()->false));AtomicInteger polls=new AtomicInteger();reject(()->AiSourceCopy.copy(generated(16_000_000),DISCARD,()->polls.incrementAndGet()>2));ok(polls.get()==3);
  // Camera descriptor remains readable after exact-path cleanup unlinks its source.
  Path cameraFile=Files.createTempFile("v401-camera-",".jpg");Files.write(cameraFile,new byte[]{1,2,3,4});try(InputStream owned=Files.newInputStream(cameraFile)){Files.delete(cameraFile);ByteArrayOutputStream copied=new ByteArrayOutputStream();ok(AiSourceCopy.copy(owned,copied,()->false)==4);ok(Arrays.equals(copied.toByteArray(),new byte[]{1,2,3,4}));}
  for(int orientation=1;orientation<=8;orientation++){AiImagePolicy.Plan p=AiImagePolicy.plan(16384,12288,true,orientation,64L*1024*1024);ok(p.outputWidth<=2048&&p.outputHeight<=2048);ok(p.estimatedBytes<=64L*1024*1024);ok(p.sampleSize>=8);}
  reject(()->AiImagePolicy.plan(65536,1,true,1,64L*1024*1024));reject(()->AiImagePolicy.plan(65535,65535,true,1,64L*1024*1024));reject(()->AiImagePolicy.plan(1080,20000,false,1,64L*1024*1024));reject(()->AiImagePolicy.plan(9000,9000,false,1,64L*1024*1024));ok(AiImagePolicy.plan(4000,3000,false,1,64L*1024*1024).sampleSize==1);
  // Check actual transform coefficients with asymmetric coordinate (2,3), not merely swapped dimensions.
  int[][] expected={{2,3},{-2,3},{-2,-3},{2,-3},{3,2},{-3,2},{-3,-2},{3,-2}};for(int o=1;o<=8;o++){float[] m=AiImagePolicy.matrix(o);ok(m[0]*2+m[1]*3==expected[o-1][0]);ok(m[3]*2+m[4]*3==expected[o-1][1]);}reject(()->AiImagePolicy.matrix(9));
  for(String secret:Arrays.asList("卡号 6222 8888 9999 0000","账号:ABC012345","验证码123456","密码:abcDef!","API Key sk-synthetic-key-for-tests"))ok(AiSensitiveText.blocked(secret));for(String safe:Arrays.asList("午餐32元","2026-10-08，咖啡人民币18.50元","半份饭200克","退款15元"))ok(!AiSensitiveText.blocked(safe));
  // Exercise the exact production quality/size ladder with a controllable codec.
  class Codec implements AiImageEncoding.Codec {int writes,shrinks;boolean alpha,allLarge,falseResult;int[] sizes;java.util.List<String> calls=new ArrayList<>();Codec(int...sizes){this.sizes=sizes;}public boolean compress(boolean png,int quality,OutputStream out)throws IOException{calls.add((png?"png":"jpeg")+quality);int size=allLarge?101:sizes[Math.min(writes,sizes.length-1)];writes++;out.write(new byte[size]);return !falseResult;}public void shrink(){shrinks++;}public boolean hasAlpha(){return alpha;}public int width(){return 2048-shrinks*100;}public int height(){return 1024-shrinks*100;}}
  Codec foodCodec=new Codec(101,101,99);AiImageEncoding.Encoded encoded=AiImageEncoding.encode(foodCodec,true,100,()->false);ok(encoded.bytes.length==99&&encoded.mime.equals("image/jpeg"));ok(foodCodec.calls.equals(Arrays.asList("jpeg90","jpeg82","jpeg72")));ok(foodCodec.shrinks==0);
  Codec reduced=new Codec(101,101,101,101,70);encoded=AiImageEncoding.encode(reduced,true,100,()->false);ok(reduced.shrinks==1&&reduced.writes==5);ok(encoded.width==1948&&encoded.bytes.length==70);
  Codec billCodec=new Codec(101,101,99);encoded=AiImageEncoding.encode(billCodec,false,100,()->false);ok(encoded.mime.equals("image/jpeg"));ok(billCodec.calls.equals(Arrays.asList("png100","jpeg94","jpeg88")));ok(billCodec.shrinks==0);
  Codec transparent=new Codec(101);transparent.alpha=true;reject(()->AiImageEncoding.encode(transparent,false,100,()->false));ok(transparent.writes==1&&transparent.shrinks==0);
  Codec exhausted=new Codec(101);exhausted.allLarge=true;reject(()->AiImageEncoding.encode(exhausted,true,100,()->false));ok(exhausted.writes==12&&exhausted.shrinks==2);
  Codec exhaustedBill=new Codec(101);exhaustedBill.allLarge=true;reject(()->AiImageEncoding.encode(exhaustedBill,false,100,()->false));ok(exhaustedBill.writes==4&&exhaustedBill.shrinks==0);
  Codec cancelledCodec=new Codec(99);reject(()->AiImageEncoding.encode(cancelledCodec,true,100,()->true));ok(cancelledCodec.writes==0);
  Codec cancelAfterWrite=new Codec(99);reject(()->AiImageEncoding.encode(cancelAfterWrite,true,100,()->cancelAfterWrite.writes>0));ok(cancelAfterWrite.writes==1);
  Codec falseCodec=new Codec(10);falseCodec.falseResult=true;reject(()->AiImageEncoding.encode(falseCodec,true,100,()->false));ok(falseCodec.writes==1);
  System.out.println("PASS: "+checks+" v4.0.1 native policy/source/consent checks.");
 }
}
