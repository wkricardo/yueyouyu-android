package cn.dot.budget;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.*;
import android.text.InputType;
import android.util.Base64;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

final class StatementImport {
 private final Activity activity;
 private final java.util.function.Consumer<String> events;
 private final AiDeliveryGate deliveries=new AiDeliveryGate();
 private final AiSourceRegistry sources=new AiSourceRegistry();
 private final java.util.Map<AiRequestSession.Ticket,AiDeliveryGate.Owner> deliveryOwners=new java.util.IdentityHashMap<>();
 private final AiRequestSession requests=new AiRequestSession();
 private AiRequestSession.Ticket activeTicket;
 private final AiConsentSelection selection=new AiConsentSelection();
 private Runnable sourceLauncher;
 private final AiRetryStore retryStore=new AiRetryStore();
 private String sessionKey="";
 private boolean keyBlocked;
 private DeepSeekTransport transport;
 private int pageGeneration;
 private final java.util.Set<Dialog> dialogs=new java.util.HashSet<>();
 private final AiRememberedConsent consent;
 private final AiImagePreparer imagePreparer;
 private AiImagePreparer.Job imageJob;
 private View safetyPage;
 private View safetyBackground;
 private int backgroundAccessibility,backgroundDescendants;
 private boolean backgroundEnabled,backgroundFocusable,backgroundTouchFocusable,backgroundHadFocus,safetyWasSecure;
 private Bitmap safetyBitmap;
 private AiRequestSession.Ticket safetyTicket;
 private static final String ALIAS="yueyouyu.deepseek.key.v1";
 StatementImport(Activity a,java.util.function.Consumer<String> events){activity=a;this.events=events;imagePreparer=new AiImagePreparer(a.getCacheDir());consent=new AiRememberedConsent(new AiRememberedConsent.Store(){
  private android.content.SharedPreferences store(){return activity.getSharedPreferences("deepseek-send-consent-v1",0);}
  public int version(){return store().getInt("version",0);}
  public boolean allowed(String scope){return store().getBoolean(scope,false);}
  public boolean write(String scope){android.content.SharedPreferences.Editor edit=store().edit();if(version()!=AiRememberedConsent.VERSION)edit.clear();return edit.putInt("version",AiRememberedConsent.VERSION).putBoolean(scope,true).commit();}
  public boolean clear(){return store().edit().clear().commit();}
  public boolean empty(){return store().getAll().isEmpty();}
 });}
 void request(AiRequestContract request,java.util.function.IntConsumer camera){
  if(!requests.isAlive()||activity.isFinishing()||activity.isDestroyed())return;
  final AiRequestSession.Ticket ticket=requests.reserve(request);if(ticket==null){message("已有识别请求正在进行，请先完成或取消");return;}activeTicket=ticket;deliveryOwners.put(ticket,deliveries.begin(ticket.id));if(request.retryOf==null)retryStore.clear();emit(ticket,"started",null,null);
  Runnable start=()->{if(!requests.isCurrent(ticket))return;emit(ticket,"phase","preparing",null);try{if(request.retryOf!=null){AiConsentSnapshot frozen=retryStore.take(ticket);selection.replace(ticket,frozen);if(frozen.isText())confirmText(ticket,frozen);else confirmImage(ticket,frozen,"food".equals(request.mode));return;}if(request.text!=null){confirmText(ticket,null);return;}if("food_camera".equals(request.source)){AiSourceRegistry.Launch launch=reserveSource(ticket,request.source);camera.accept(launch.code);return;}Intent intent=new Intent(Intent.ACTION_GET_CONTENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);AiSourceRegistry.Launch launch=reserveSource(ticket,request.source);activity.startActivityForResult(intent,launch.code);}catch(Exception e){cancelSelection();message("本次图片来源暂不可用，请改用文字或手动填写；也可完全关闭应用后重试");}};
  sourceLauncher=start;if(loadKey().isEmpty())configure(start,()->finishSelection(ticket));else start.run();
 }
 private void emit(AiRequestSession.Ticket ticket,String event,String data,String error){emit(ticket,event,data,error,null);}
 private void emit(AiRequestSession.Ticket ticket,String event,String data,String error,String errorCode){try{AiRequestContract request=ticket.request;JSONObject value=new JSONObject().put("version",AiRequestContract.VERSION).put("event",event).put("mode",request.mode).put("source",request.source).put("requestId",ticket.id).put("sessionId",ticket.sessionId).put("generation",ticket.generation).put("revision",request.revision).put("clientAttempt",request.clientAttempt);String deliveryToken="cancelled".equals(event)?deliveries.cancellation(ticket.id):deliveries.issue(deliveryOwners.get(ticket),event);if(deliveryToken!=null)value.put("deliveryToken",deliveryToken);if(data!=null)value.put("phase".equals(event)?"phase":"data",data);if(error!=null)value.put("error",error).put("errorCode",errorCode).put("retryable",retryStore.availableFor(ticket.id));events.accept(value.toString());if("result".equals(event)||"error".equals(event)||"cancelled".equals(event)){deliveries.finish(deliveryOwners.remove(ticket));}}catch(JSONException ignored){}}
 private void finishSelection(AiRequestSession.Ticket ticket){if(requests.release(ticket)){cancelPreparation();closeSafetyPage();sources.revoke(ticket);deliveries.revokeRequest(ticket.id);activeTicket=null;sourceLauncher=null;selection.invalidate(ticket);emit(ticket,"cancelled",null,null);}}
 void cancelSelection(){finishSelection(activeTicket);}
 private android.content.SharedPreferences prefs(){return activity.getSharedPreferences("deepseek-secure",0);}
 private javax.crypto.SecretKey storageKey()throws Exception{KeyStore k=KeyStore.getInstance("AndroidKeyStore");k.load(null);if(!k.containsAlias(ALIAS)){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}return (javax.crypto.SecretKey)k.getKey(ALIAS,null);}
 private String loadKey(){if(keyBlocked)return "";if(!sessionKey.isEmpty())return sessionKey;String encoded=prefs().getString("encrypted",null);if(encoded==null)return "";try{Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,storageKey(),new GCMParameterSpec(128,Base64.decode(prefs().getString("iv",""),Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(encoded,Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){message("保存的密钥不可读取，请重新输入");return "";}}
 void configure(Runnable after){configure(after,null);}
 private void configure(Runnable after,Runnable cancelled){
  final boolean[] used={false};final int openedPage=pageGeneration;
  LinearLayout layout=new LinearLayout(activity);layout.setOrientation(1);layout.setPadding(40,12,40,12);
  TextView notice=new TextView(activity);notice.setText("直接连接 DeepSeek 官方 API（deepseek-flash）。API 可能产生费用，由你的 DeepSeek 账户承担。密钥只在手机输入，不放入账目备份。\n默认仅本次运行使用；勾选后通过 Android Keystore 加密保存在本机。");layout.addView(notice);
  EditText key=new EditText(activity);key.setHint("输入 DeepSeek API Key");key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setSingleLine();key.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);layout.addView(key);
  CheckBox remember=new CheckBox(activity);remember.setText("允许加密保存此密钥到本机");layout.addView(remember);
  AlertDialog d=new AlertDialog.Builder(activity).setTitle("配置 DeepSeek").setView(layout).setNegativeButton("取消",null).setPositiveButton("使用",null).create();d.getWindow();d.setOnShowListener(v->{d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w->{if(openedPage!=pageGeneration||!requests.isAlive()){d.dismiss();return;}String s=key.getText().toString().trim();if(s.length()<10||s.length()>512||s.contains("\n")||s.contains("\r")){key.setError("请输入有效密钥");return;}try{if(remember.isChecked()){Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,storageKey());if(!prefs().edit().putString("encrypted",Base64.encodeToString(c.doFinal(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP)).putString("iv",Base64.encodeToString(c.getIV(),Base64.NO_WRAP)).commit())throw new IOException();}else if(!prefs().edit().clear().commit()||!prefs().getAll().isEmpty())throw new IOException();sessionKey=s;keyBlocked=false;used[0]=true;key.setText("");d.dismiss();if(after!=null)after.run();}catch(Exception e){message("密钥保存失败，未开始识别");}});});d.setOnDismissListener(v->{dialogs.remove(d);key.setText("");if(!used[0]&&cancelled!=null)cancelled.run();});showDialog(d);
 }
 void clear(){final int openedPage=pageGeneration;AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("清除 DeepSeek 密钥？").setMessage("清除本次运行和本机保存的密钥，并取消尚未完成的识别。不会删除账目或注销 DeepSeek 账户。已发送的请求仍可能产生费用。").setNegativeButton("取消",null).setPositiveButton("清除",(d,w)->{
  if(openedPage!=pageGeneration||!requests.isAlive())return;
  AiKeyDeletion.Result cleared=AiKeyDeletion.clear(new AiKeyDeletion.Store(){
   public boolean clearPreferences(){return prefs().edit().clear().commit();}
   public boolean preferencesEmpty(){return prefs().getAll().isEmpty();}
   private KeyStore keyStore()throws Exception{KeyStore keyStore=KeyStore.getInstance("AndroidKeyStore");keyStore.load(null);return keyStore;}
   public void deleteAlias()throws Exception{keyStore().deleteEntry(ALIAS);}
   public boolean aliasAbsent()throws Exception{return !keyStore().containsAlias(ALIAS);}
  },()->{sessionKey="";keyBlocked=true;cancelCurrent();});
  message(cleared.success?"本次运行和本机保存的密钥已确认清除":"本次运行已停用旧密钥，识别已停止；本机保存项未能确认全部删除。重新打开应用前请重试清除。");
 }).create();dialog.setOnDismissListener(d->dialogs.remove(dialog));showDialog(dialog);}
 boolean consumeDelivery(String token,String requestId,String event){return deliveries.consume(token,requestId,event);}
 boolean isResultCurrent(String token,String requestId){return deliveries.isResultCurrent(token,requestId);}
 void clearRetry(){retryStore.clear();}
 void cancelAttempt(String attempt){if(requests.ownsAttempt(attempt))cancelCurrent();}
 private void cancelCurrent(){cancelPreparation();closeSafetyPage();deliveries.invalidate();deliveryOwners.clear();sources.reset();retryStore.clear();AiRequestSession.Ticket ticket=activeTicket;DeepSeekTransport old=transport;if(ticket!=null&&requests.cancel(ticket,old==null?null:old::cancel)){activeTicket=null;sourceLauncher=null;selection.invalidate(ticket);transport=null;emit(ticket,"cancelled",null,null);}for(Dialog dialog:new java.util.ArrayList<Dialog>(dialogs))dialog.dismiss();dialogs.clear();}

 void choose(){request(new AiRequestContract(1,"budget","statement_image",0),null);}
 void chooseFood(){request(new AiRequestContract(1,"food","food_image",0),null);}
 void prepareFoodCamera(java.util.function.IntConsumer start){request(new AiRequestContract(1,"food","food_camera",0),start);}
 private AiSourceRegistry.Launch reserveSource(AiRequestSession.Ticket ticket,String kind){return sources.reserve(ticket,kind,floor->{android.content.SharedPreferences store=activity.getSharedPreferences("ai-source-safety",0);return store.edit().putInt("next-code",floor).commit()&&store.getInt("next-code",0)>=floor;});}
 boolean cleanupImageOrphans(){return imagePreparer.cleanupOrphans();}
 void sourceResult(int code,boolean success,Uri uri){sourceResult(code,success,uri,null);}
 // Ownership of a camera descriptor transfers here before MainActivity unlinks its temporary file.
 void sourceResult(int code,boolean success,Uri uri,InputStream cameraInput){AiSourceRegistry.Launch launch=sources.take(code);if(launch==null||!requests.isCurrent(launch.ticket)){AiImagePreparer.close(cameraInput);return;}if(!success||uri==null){AiImagePreparer.close(cameraInput);finishSelection(launch.ticket);return;}selected(launch.ticket,uri,!"statement_image".equals(launch.kind),cameraInput);}
 void selected(Uri uri){selected(activeTicket,uri,false,null);}
 void selectedFood(Uri uri){selected(activeTicket,uri,true,null);}
 private CheckBox rememberChoice(LinearLayout layout,AiRequestSession.Ticket ticket){CheckBox remember=new CheckBox(activity);remember.setChecked(false);remember.setText("记住此选择：以后我主动识别"+AiRememberedConsent.label(AiRememberedConsent.scope(ticket.request))+"时，允许发送给 DeepSeek 并按账户计费，不再弹出发送同意。包括我主动重试（可能再次收费）；只限本次这类输入，不包含历史、健康资料或后台上传；设置中可撤销。");layout.addView(remember);return remember;}
 private boolean saveChoice(AiRequestSession.Ticket ticket,CheckBox remember,int generation){if(!consent.current(generation)||!requests.isCurrent(ticket))return false;if(remember.isChecked()&&!consent.remember(ticket.request,true,generation)){message("发送授权保存未能确认，本次未发送。请重新确认或只同意本次。");return false;}return true;}
 private void confirmText(AiRequestSession.Ticket ticket,AiConsentSnapshot prepared){if(!requests.isCurrent(ticket))return;try{
  AiConsentSnapshot snapshot=prepared!=null?prepared:new AiConsentSnapshot(ticket,ticket.request.text,text->AiPayloadBuilder.text(ticket.request,text));selection.replace(ticket,snapshot);
  if(AiSensitiveText.blocked(snapshot.text())){finishSelection(ticket);message("这段文字可能包含完整卡号、账号、密码、验证码或 API 密钥。请在本机移除这些字段后重新点击整理；本次未发送。检测只作补充，不能代替你检查文字。");return;}
  if(consent.allows(ticket.request)){recognize(ticket,snapshot,"food".equals(ticket.request.mode));return;}
  emit(ticket,"phase","consent",null);final int consentGeneration=consent.generation();
  LinearLayout layout=new LinearLayout(activity);layout.setOrientation(1);layout.setPadding(32,12,32,12);TextView disclosure=new TextView(activity);disclosure.setText((ticket.request.retryOf!=null?"重新发送同一内容可能再次收费，请重新确认。\n":"")+AiDisclosure.text(ticket.request));layout.addView(disclosure);TextView exact=new TextView(activity);exact.setText(snapshot.text());exact.setTextIsSelectable(true);exact.setContentDescription("本次实际发送的完整文字");exact.setPadding(12,24,12,24);layout.addView(exact);addPrompt(layout,ticket);
  CheckBox reviewed=new CheckBox(activity);reviewed.setText("我已核对以上完整文字，已移除完整卡号、账号、密码、验证码和 API 密钥");reviewed.setChecked(false);layout.addView(reviewed);CheckBox remember=rememberChoice(layout,ticket);ScrollView scroll=new ScrollView(activity);scroll.addView(layout);final boolean[] approved={false};
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("确认文字发送").setView(scroll).setNegativeButton("取消",null).setPositiveButton("同意发送（可能收费）",null).create();dialog.setOnShowListener(d->{dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);reviewed.setOnCheckedChangeListener((b,checked)->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(AiDisclosure.maySend(checked,requests.isCurrent(ticket)&&selection.isCurrent(ticket,snapshot))));dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(!AiDisclosure.maySend(reviewed.isChecked(),requests.isCurrent(ticket)&&selection.isCurrent(ticket,snapshot))||!saveChoice(ticket,remember,consentGeneration))return;approved[0]=true;dialog.dismiss();recognize(ticket,snapshot,"food".equals(ticket.request.mode));});});dialog.setOnDismissListener(d->{dialogs.remove(dialog);if(!approved[0])finishSelection(ticket);});showDialog(dialog);
 }catch(Exception e){inputFailure(ticket,e);}}
 private void addPrompt(LinearLayout layout,AiRequestSession.Ticket ticket){Button reveal=new Button(activity);reveal.setText("查看固定识别说明");layout.addView(reveal);TextView prompt=new TextView(activity);prompt.setText(AiPayloadBuilder.instruction(ticket.request));prompt.setTextIsSelectable(true);prompt.setVisibility(View.GONE);layout.addView(prompt);reveal.setOnClickListener(v->prompt.setVisibility(prompt.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE));}
 private void selected(AiRequestSession.Ticket ticket,Uri uri,boolean food,InputStream cameraInput){if(!requests.isCurrent(ticket)||food!="food".equals(ticket.request.mode)){AiImagePreparer.close(cameraInput);return;}cancelPreparation();final int preparedGeneration=consent.generation();
  imageJob=imagePreparer.prepare(()->activity.getContentResolver().openInputStream(uri),cameraInput,food,new AiImagePreparer.Result(){
   public void ready(AiImagePreparer.Prepared prepared){try{if(!requests.isCurrent(ticket)||!consent.current(preparedGeneration))return;AiConsentSnapshot snapshot=new AiConsentSnapshot(ticket,prepared.bytes,prepared.mime,bytes->AiPayloadBuilder.image(ticket.request,bytes,prepared.mime));activity.runOnUiThread(()->{if(!requests.isCurrent(ticket)||!consent.current(preparedGeneration))return;imageJob=null;try{confirmImage(ticket,snapshot,food);}catch(Exception e){inputFailure(ticket,e);}});}catch(Exception|OutOfMemoryError e){failed(new IOException("图片请求准备失败，请裁剪后重试"));}}
   public void failed(Exception error){activity.runOnUiThread(()->{if(!requests.isCurrent(ticket)||!consent.current(preparedGeneration))return;imageJob=null;inputFailure(ticket,error);});}
  });
 }
 private void cancelPreparation(){AiImagePreparer.Job job=imageJob;imageJob=null;if(job!=null)job.cancel();}
 private void inputFailure(AiRequestSession.Ticket ticket,Exception error){if(requests.release(ticket)){activeTicket=null;sourceLauncher=null;selection.invalidate(ticket);sources.revoke(ticket);AiErrors.Info failure=AiErrors.forCode("input");emit(ticket,"error",null,failure.message,failure.code);message(error instanceof IOException||error instanceof IllegalArgumentException?error.getMessage():failure.message);}}
 private void confirmImage(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot,boolean food)throws IOException{if(!requests.isCurrent(ticket))return;selection.replace(ticket,snapshot);
  if(consent.allows(ticket.request)){if(food||ticket.request.retryOf!=null)recognize(ticket,snapshot,food);else showBillSafetyPage(ticket,snapshot);return;}
  emit(ticket,"phase","consent",null);final int consentGeneration=consent.generation();byte[] previewBytes=snapshot.copyImageBytes();BitmapFactory.Options sentBounds=bounds(previewBytes);
  LinearLayout layout=new LinearLayout(activity);layout.setOrientation(1);layout.setPadding(32,12,32,8);TextView text=new TextView(activity);text.setText((ticket.request.retryOf!=null?"重新发送同一图片可能再次收费，请重新确认。\n":"")+AiDisclosure.image(ticket.request,sentBounds.outWidth,sentBounds.outHeight,previewBytes.length,snapshot.mime()));text.setTextIsSelectable(true);layout.addView(text);Bitmap bm=addPreview(layout,previewBytes,sentBounds);addPrompt(layout,ticket);
  CheckBox redaction=new CheckBox(activity);redaction.setText("我已检查发送预览，移除完整卡号、账号、密码、验证码和 API 密钥，仅保留本次识别所需内容");redaction.setChecked(false);layout.addView(redaction);CheckBox remember=rememberChoice(layout,ticket);ScrollView scroll=new ScrollView(activity);scroll.addView(layout);
  final boolean[] approved={false};final Runnable reselect=sourceLauncher;AlertDialog d=new AlertDialog.Builder(activity).setTitle(food?"确认发送食物照片？":"确认发送账单图片？").setView(scroll).setNegativeButton("取消",null).setNeutralButton("换一张（不发送）",(x,w)->{if(!requests.isCurrent(ticket)||!selection.isCurrent(ticket,snapshot))return;selection.invalidate(ticket);approved[0]=true;if(reselect!=null)reselect.run();else finishSelection(ticket);}).setPositiveButton("同意发送（可能收费）",null).create();d.setOnShowListener(x->{d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);if(ticket.request.retryOf!=null)d.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(View.GONE);redaction.setOnCheckedChangeListener((button,checked)->d.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(AiDisclosure.maySend(checked,requests.isCurrent(ticket)&&selection.isCurrent(ticket,snapshot))));d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{if(!AiDisclosure.maySend(redaction.isChecked(),requests.isCurrent(ticket)&&selection.isCurrent(ticket,snapshot))||!saveChoice(ticket,remember,consentGeneration))return;approved[0]=true;d.dismiss();recognize(ticket,snapshot,food);});});d.setOnDismissListener(x->{dialogs.remove(d);bm.recycle();if(!approved[0])finishSelection(ticket);});showDialog(d);
 }
 private BitmapFactory.Options bounds(byte[] bytes)throws IOException{BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);if(bounds.outWidth<1||bounds.outHeight<1)throw new IOException("无法显示发送预览，请重新选择");return bounds;}
 private Bitmap addPreview(LinearLayout layout,byte[] bytes,BitmapFactory.Options bounds)throws IOException{BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=AiImagePolicy.previewSample(bounds.outWidth,bounds.outHeight);options.inPreferredConfig=Bitmap.Config.ARGB_8888;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null)throw new IOException("无法显示发送预览，请重新选择");ImageView image=new ImageView(activity);image.setImageBitmap(bitmap);image.setContentDescription("本次实际发送图片的预览，"+bounds.outWidth+" × "+bounds.outHeight+" 像素");image.setAdjustViewBounds(true);layout.addView(image,new LinearLayout.LayoutParams(-1,-2));return bitmap;}
 // A current-image task preview, not a recurring recipient/fee consent dialog.
 private void showBillSafetyPage(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot)throws IOException{
  closeSafetyPage();emit(ticket,"phase","consent",null);final int consentGeneration=consent.generation();byte[] bytes=snapshot.copyImageBytes();BitmapFactory.Options bounds=bounds(bytes);
  LinearLayout content=new LinearLayout(activity);content.setOrientation(1);content.setPadding(32,32,32,32);content.setBackgroundColor(Color.WHITE);TextView title=new TextView(activity);title.setText("检查这张账单图片");title.setTextSize(22);content.addView(title);TextView reminder=new TextView(activity);reminder.setText("已按你保存的授权准备识别这张图。请只保留交易明细，先在相册遮盖完整卡号、银行账号、密码、验证码、API 密钥等，再重新选择。压缩只去除元数据，不会遮盖可见文字。\n请核对字迹清晰；过长账单请分段截图。已发送内容无法撤回。");content.addView(reminder);safetyBitmap=addPreview(content,bytes,bounds);TextView size=new TextView(activity);size.setText(bounds.outWidth+" × "+bounds.outHeight+" 像素 · "+bytes.length+" 字节 · "+("image/png".equals(snapshot.mime())?"PNG":"JPEG"));content.addView(size);
  Button recognize=new Button(activity);recognize.setText("已移除敏感字段，识别此图");content.addView(recognize);Button replace=new Button(activity);replace.setText("换一张已遮盖的图片");content.addView(replace);Button cancel=new Button(activity);cancel.setText("取消识别");content.addView(cancel);
  ScrollView page=new ScrollView(activity);page.setFillViewport(true);page.setBackgroundColor(Color.WHITE);page.addView(content);page.setClickable(true);safetyPage=page;safetyTicket=ticket;safetyWasSecure=(activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0;ViewGroup host=activity.findViewById(android.R.id.content);if(host.getChildCount()>0){safetyBackground=host.getChildAt(0);backgroundAccessibility=safetyBackground.getImportantForAccessibility();backgroundEnabled=safetyBackground.isEnabled();backgroundFocusable=safetyBackground.isFocusable();backgroundTouchFocusable=safetyBackground.isFocusableInTouchMode();backgroundHadFocus=safetyBackground.hasFocus();safetyBackground.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);safetyBackground.clearFocus();safetyBackground.setFocusable(false);safetyBackground.setEnabled(false);if(safetyBackground instanceof ViewGroup){backgroundDescendants=((ViewGroup)safetyBackground).getDescendantFocusability();((ViewGroup)safetyBackground).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);}}activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);activity.addContentView(page,new ViewGroup.LayoutParams(-1,-1));
  page.setOnApplyWindowInsetsListener((view,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());view.setPadding(safe.left,safe.top,safe.right,safe.bottom);}else{view.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());}return insets;});page.requestApplyInsets();page.setFocusableInTouchMode(true);page.requestFocus();
  recognize.setOnClickListener(v->{if(!consent.current(consentGeneration)||!consent.allows(ticket.request)||!requests.isCurrent(ticket)||!selection.isCurrent(ticket,snapshot))return;closeSafetyPage();recognize(ticket,snapshot,false);});
  replace.setOnClickListener(v->{if(!requests.isCurrent(ticket)||!selection.isCurrent(ticket,snapshot))return;Runnable launch=sourceLauncher;closeSafetyPage();selection.invalidate(ticket);if(launch!=null)launch.run();else finishSelection(ticket);});cancel.setOnClickListener(v->finishSelection(ticket));
 }
 private void closeSafetyPage(){View page=safetyPage;safetyPage=null;safetyTicket=null;if(page!=null){ViewParent parent=page.getParent();if(parent instanceof ViewGroup)((ViewGroup)parent).removeView(page);if(!safetyWasSecure)activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}View background=safetyBackground;safetyBackground=null;if(background!=null){background.setImportantForAccessibility(backgroundAccessibility);background.setEnabled(backgroundEnabled);background.setFocusable(backgroundFocusable);background.setFocusableInTouchMode(backgroundTouchFocusable);if(background instanceof ViewGroup)((ViewGroup)background).setDescendantFocusability(backgroundDescendants);if(backgroundHadFocus)background.requestFocus();}Bitmap old=safetyBitmap;safetyBitmap=null;if(old!=null&&!old.isRecycled())old.recycle();}
 boolean handleBack(){if(safetyPage==null)return false;AiRequestSession.Ticket ticket=safetyTicket;finishSelection(ticket);return true;}
 void manageConsent(){final int openedPage=pageGeneration;StringBuilder status=new StringBuilder("接收方：DeepSeek 官方 API\n记住的授权仅用于你主动选择并识别的当前内容，按 DeepSeek 账户计费；不包括后台、历史或健康资料。账单图片仍需检查可见敏感字段，只会在你点击重试时重新发送，可能再次收费。\n\n");for(String scope:AiRememberedConsent.SCOPES){status.append(AiRememberedConsent.label(scope)).append(consent.allowsScope(scope)?"：已记住\n":"：每次询问\n");}AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("AI 发送授权").setMessage(status.toString()).setNegativeButton("关闭",null).setPositiveButton("撤销全部发送授权",(d,w)->{if(openedPage!=pageGeneration||!requests.isAlive())return;boolean cleared=consent.revoke(this::cancelCurrent);message(cleared?"已撤销记住的发送授权，并取消未完成识别。已发送内容无法撤回；下次会重新询问。":"本次运行已停用发送授权并取消识别，但本机保存项未能确认清除；请重试撤销后再关闭应用。");}).create();dialog.setOnDismissListener(d->dialogs.remove(dialog));showDialog(dialog);}
 private void recognize(AiRequestSession.Ticket ticket,AiConsentSnapshot snapshot,boolean food){if(!requests.isCurrent(ticket.sessionId,ticket.id,ticket.generation,ticket.request.mode,ticket.request.source,ticket.request.revision)||!selection.consume(ticket,snapshot))return;String key=loadKey();if(key.isEmpty()){finishSelection(ticket);message("请先配置密钥");return;}final DeepSeekTransport requestTransport=new DeepSeekTransport();transport=requestTransport;emit(ticket,"phase","sending",null);new Thread(()->{String answer=null;AiErrors.Info failure=null;try{ answer=AiResponseParser.parse(requestTransport.send(key,snapshot.payload()),ticket.request.mode);
 }catch(Exception e){failure=AiErrors.from(e);}final String ok=answer;final AiErrors.Info error=failure;activity.runOnUiThread(()->{if(!requests.release(ticket))return;activeTicket=null;sourceLauncher=null;selection.invalidate(ticket);if(transport==requestTransport)transport=null;if(!activity.isFinishing()&&!activity.isDestroyed()){if(ok!=null){emit(ticket,"result",ok,null);}else {if(!"cancelled".equals(error.code))retryStore.offer(ticket,snapshot);emit(ticket,"error",null,error.message,error.code);showFailure(error);}}});},"statement-recognition").start();}
 private void showFailure(AiErrors.Info error){if(!requests.isAlive()||activity.isFinishing()||activity.isDestroyed())return;AlertDialog.Builder builder=new AlertDialog.Builder(activity).setTitle("识别未完成").setMessage(error.message+"\n取消或失败后服务仍可能已计费。应用不会自动重试；再次发送可能再次收费。").setNegativeButton("继续本地记录",null);if("configure".equals(error.action))builder.setPositiveButton("配置密钥",(d,w)->configure(null));else builder.setPositiveButton("知道了",null);AlertDialog dialog=builder.create();dialog.setOnDismissListener(d->dialogs.remove(dialog));showDialog(dialog);}
 void resetPage(){cancelPreparation();closeSafetyPage();deliveries.invalidate();deliveryOwners.clear();sources.reset();retryStore.clear();pageGeneration++;requests.reset();activeTicket=null;sourceLauncher=null;selection.reset();DeepSeekTransport old=transport;transport=null;if(old!=null)old.cancel();for(Dialog dialog:new java.util.ArrayList<Dialog>(dialogs))dialog.dismiss();dialogs.clear();}
 void destroy(){deliveries.close();requests.close();resetPage();imagePreparer.destroy();sessionKey="";}
 private void showDialog(Dialog dialog){if(!requests.isAlive()||activity.isFinishing()||activity.isDestroyed())return;dialogs.add(dialog);dialog.show();}

 private void message(String s){if(!requests.isAlive()||activity.isFinishing()||activity.isDestroyed())return;AlertDialog dialog=new AlertDialog.Builder(activity).setMessage(s).setPositiveButton("知道了",null).create();dialog.setOnDismissListener(d->dialogs.remove(dialog));showDialog(dialog);}
}
