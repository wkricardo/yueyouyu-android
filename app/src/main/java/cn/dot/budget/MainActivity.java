package cn.dot.budget;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.view.*;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MainActivity extends Activity {
 private WebView web;
 private volatile int webGeneration;
 private volatile boolean destroyed;
 private Object backCallback;
 private StatementImport statementImport;
 private GitHubUpdater updater;
 private boolean updateChecked=false;
 private int cameraPendingCode=0;
 private static final Uri LEGACY_CAMERA_URI=Uri.parse("content://cn.dot.budget.files/images/capture.jpg");
 private String cameraFileName;
 private String pendingBackup;
 private int notificationId=100;
 private static final int EXPORT=10, IMPORT=11;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  try{AiSourceRegistry.advanceTo(getSharedPreferences("ai-source-safety",0).getInt("next-code",AiSourceRegistry.FIRST));}catch(Exception ignored){AiSourceRegistry.advanceTo(AiSourceRegistry.LAST+1);}
  if(state!=null){try{AiSourceRegistry.advanceTo(state.getInt("ai-source-floor",AiSourceRegistry.FIRST));}catch(IllegalArgumentException ignored){AiSourceRegistry.advanceTo(AiSourceRegistry.LAST+1);}int pending=state.getInt("ai-camera-code",0);if(AiSourceRegistry.isSourceCode(pending))cameraPendingCode=pending;String name=state.getString("ai-camera-file");if(AiCapturePath.isFileName(name))cameraFileName=name;}
  getWindow().setStatusBarColor(Color.rgb(244,248,252));
  getWindow().setNavigationBarColor(Color.rgb(244,248,252));
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  web=new WebView(this);
  statementImport=new StatementImport(this,json->{if(!destroyed)web.evaluateJavascript("window.receiveAiEvent && window.receiveAiEvent("+JSONObject.quote(json)+")",null);});
  if(!cleanupCamera()||!cleanupCaptureOrphans()||!statementImport.cleanupImageOrphans())toast("上次临时照片未能确认清理，请从相册选择；本次不会继续旧识别。");
  updater=new GitHubUpdater(this,status->web.evaluateJavascript("window.receiveUpdateStatus && window.receiveUpdateStatus("+JSONObject.quote(status)+")",null));
  web.setBackgroundColor(Color.rgb(244,248,252));
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  web.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  web.addJavascriptInterface(new Bridge(), "Android");
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageStarted(WebView view,String url,android.graphics.Bitmap favicon){webGeneration++;statementImport.resetPage();if(!cleanupCamera())toast("临时照片未能确认清理，请重启后重试");}
   @Override public void onPageFinished(WebView view,String url){if(!updateChecked&&url.equals("https://app.local/index.html")){updateChecked=true;updater.check(false);}}
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){
    Uri uri=request.getUrl();
    if("https".equals(uri.getScheme()) && "app.local".equals(uri.getHost())) {
     String path=uri.getPath(); if(path==null || path.equals("/"))path="/index.html";
     if(path.contains("..")) return empty();
     try {String mime=path.endsWith(".js")?"application/javascript":path.endsWith(".css")?"text/css":path.endsWith(".svg")?"image/svg+xml":"text/html";
      return new WebResourceResponse(mime,"UTF-8",getAssets().open(path.substring(1)));
     } catch(IOException e){return empty();}
    }
    return empty();
   }
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !"app.local".equals(r.getUrl().getHost());}
  });
  setContentView(web);
  if(Build.VERSION.SDK_INT>=33)backCallback=Api33Back.register(this,()->routeBack());
  if(Build.VERSION.SDK_INT>=35)web.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout()|WindowInsets.Type.ime());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
  NotificationManager nm=getSystemService(NotificationManager.class);
  nm.createNotificationChannel(new NotificationChannel("budget","预算提醒",NotificationManager.IMPORTANCE_DEFAULT));
  web.loadUrl("https://app.local/index.html");
 }
 private WebResourceResponse empty(){return new WebResourceResponse("text/plain","UTF-8",new ByteArrayInputStream(new byte[0]));}
 private void runAiOnCurrentPage(Runnable action){final int requestedPage=webGeneration;runOnUiThread(()->{if(!destroyed&&requestedPage==webGeneration)action.run();});}
 public class Bridge {
  @JavascriptInterface public void requestAi(String json){final int requestedPage=webGeneration;runOnUiThread(()->{if(destroyed||requestedPage!=webGeneration)return;try{AiRequestContract.requireRequestSize(json);JSONObject input=new JSONObject(json);java.util.Set<String> fields=new java.util.HashSet<>();java.util.Iterator<String> keys=input.keys();while(keys.hasNext())fields.add(keys.next());AiRequestContract.assertFields(fields);Object version=input.get("version"),revision=input.get("revision");if(!(version instanceof Integer)||!(revision instanceof Integer)||!(input.get("mode") instanceof String)||!(input.get("source") instanceof String)||!(input.get("clientAttempt") instanceof String)||(input.has("text")&&!(input.get("text") instanceof String))||(input.has("retryOf")&&!(input.get("retryOf") instanceof String)))throw new IllegalArgumentException();AiRequestContract request=new AiRequestContract((Integer)version,input.getString("mode"),input.getString("source"),(Integer)revision,input.getString("clientAttempt"),input.has("text")?input.getString("text"):null,input.has("retryOf")?input.getString("retryOf"):null);statementImport.request(request,code->captureFood(code));}catch(Exception e){toast("AI 请求无效，请重新打开输入");}});}

  @JavascriptInterface public boolean consumeAiEvent(String token,String requestId,String event){return !destroyed&&statementImport.consumeDelivery(token,requestId,event);}
  @JavascriptInterface public boolean isAiResultCurrent(String token,String requestId){return !destroyed&&statementImport.isResultCurrent(token,requestId);}
  @JavascriptInterface public void clearAiSession(){runAiOnCurrentPage(()->{statementImport.resetPage();if(!cleanupCamera())toast("临时照片未能确认清理，请重启后重试");});}
  @JavascriptInterface public void clearAiRetry(){runAiOnCurrentPage(()->statementImport.clearRetry());}
  @JavascriptInterface public void cancelAi(String attempt){if(attempt==null||!attempt.matches("[A-Za-z0-9-]{1,80}"))return;runAiOnCurrentPage(()->statementImport.cancelAttempt(attempt));}
  @JavascriptInterface public void captureFoodPhoto(){runAiOnCurrentPage(()->statementImport.prepareFoodCamera(code->captureFood(code)));}
  @JavascriptInterface public void chooseFoodPhoto(){runAiOnCurrentPage(()->statementImport.chooseFood());}
  @JavascriptInterface public void checkForUpdates(){runOnUiThread(()->updater.check(true));}
  @JavascriptInterface public void chooseStatementScreenshot(){runAiOnCurrentPage(()->statementImport.choose());}
  @JavascriptInterface public void configureDeepSeek(){runAiOnCurrentPage(()->statementImport.configure(null));}
  @JavascriptInterface public void manageAiConsent(){runAiOnCurrentPage(()->statementImport.manageConsent());}
  @JavascriptInterface public void clearDeepSeekKey(){runAiOnCurrentPage(()->statementImport.clear());}
  @JavascriptInterface public void exportBackup(String json){runOnUiThread(()->{if(json.length()>8_000_000){toast("数据过大，无法导出");return;}pendingBackup=json;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"月有余备份-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.CHINA).format(new java.util.Date())+".json");startActivityForResult(i,EXPORT);});}
  @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});}
  @JavascriptInterface public void notify(String title,String text){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Notification n=new Notification.Builder(MainActivity.this,"budget").setSmallIcon(cn.dot.budget.R.drawable.ic_launcher).setContentIntent(PendingIntent.getActivity(MainActivity.this,0,new Intent(MainActivity.this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).build();getSystemService(NotificationManager.class).notify(notificationId++,n);});}
  @JavascriptInterface public void requestNotifications(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);else toast("系统通知已可用，请检查系统通知设置");});}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(AiSourceRegistry.isSourceCode(request)){if(request==cameraPendingCode){cameraPendingCode=0;try{Uri uri=cameraUri();if(uri!=null)revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);InputStream cameraInput=result==RESULT_OK&&cameraFileName!=null?new FileInputStream(new File(getCacheDir(),cameraFileName)):null;statementImport.sourceResult(request,result==RESULT_OK,uri,cameraInput);}catch(Exception e){statementImport.sourceResult(request,false,null);toast("无法读取相机照片，请重新选择");}finally{if(!cleanupCamera())toast("临时照片未能确认清理，请重启后重试");}}else{boolean valid=result==RESULT_OK&&data!=null&&data.getData()!=null;statementImport.sourceResult(request,valid,valid?data.getData():null);}return;}if(result!=RESULT_OK||data==null||data.getData()==null)return;try{
  if(request==EXPORT){if(pendingBackup==null){toast("请重新导出");return;}try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));}pendingBackup=null;toast("备份已保存");}
  if(request==IMPORT){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>8_000_000)throw new IOException("文件超过 8 MB");out.write(b,0,n);}}String json=out.toString("UTF-8");web.evaluateJavascript("window.receiveBackup("+JSONObject.quote(json)+")",null);}
 }catch(Exception e){toast("操作失败，请检查文件或重试");}}
 private Uri cameraUri(){if(cameraFileName==null)return null;try{return Uri.parse("content://cn.dot.budget.files"+AiCapturePath.path(cameraFileName));}catch(FileNotFoundException e){return null;}}
 private boolean cleanupCamera(){final String name=cameraFileName;final Uri uri=cameraUri();if(name==null)return true;AiCaptureCleanup.Result result=AiCaptureCleanup.clean(new AiCaptureCleanup.Resources(){
  public void revokeGrant(){if(uri!=null)revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);}
  public boolean deleteTemporaryFile(){File image=new File(getCacheDir(),name);return !image.exists()||image.delete();}
  public boolean temporaryFileAbsent(){return !new File(getCacheDir(),name).exists();}
 });if(result.success){AiCapturePath.release(name);cameraFileName=null;}return result.success;}
 private boolean cleanupCaptureOrphans(){File[] files=getCacheDir().listFiles();if(files==null)return false;boolean success=true;int seen=0;for(File file:files){String name=file.getName();if(!AiCapturePath.isFileName(name)&&!"food-capture.jpg".equals(name))continue;if(AiCapturePath.isActive(name))continue;if(++seen>32)return false;try{Uri uri="food-capture.jpg".equals(name)?LEGACY_CAMERA_URI:Uri.parse("content://cn.dot.budget.files"+AiCapturePath.path(name));revokeUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(file.exists()&&!file.delete())success=false;}catch(Exception e){success=false;}}return success;}
 private void captureFood(int requestCode){if(cameraPendingCode!=0){statementImport.cancelSelection();toast("上次系统相机尚未返回，暂不能再次拍照。请先返回相机并取消，或从相册选择；不会复用旧临时照片。");return;}try{if(!cleanupCamera()||!cleanupCaptureOrphans())throw new IOException("临时照片无法清理");cameraFileName=AiCapturePath.acquire();File image=new File(getCacheDir(),cameraFileName);if(!image.createNewFile())throw new IOException("无法创建临时照片");Uri uri=cameraUri();if(uri==null)throw new IOException("相机来源无效");Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("食物照片",uri));cameraPendingCode=requestCode;startActivityForResult(intent,requestCode);}catch(Exception e){if(cameraPendingCode==requestCode)cameraPendingCode=0;boolean cleaned=cleanupCamera();statementImport.cancelSelection();toast(cleaned?"无法打开系统相机，请选择已有食物照片":"临时照片未能确认清理，已停止打开相机，请重启后重试");}}
 @Override protected void onSaveInstanceState(Bundle out){out.putInt("ai-source-floor",AiSourceRegistry.floor());out.putInt("ai-camera-code",cameraPendingCode);if(cameraFileName!=null)out.putString("ai-camera-file",cameraFileName);super.onSaveInstanceState(out);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void routeBack(){if(web==null||isFinishing())return;if(statementImport.handleBack())return;web.evaluateJavascript("window.handleBack ? window.handleBack() : false",v->{if(!"true".equals(v))finish();});}
 @Override public void onBackPressed(){routeBack();}
 private static final class Api33Back {
  static Object register(Activity activity,Runnable action){android.window.OnBackInvokedCallback callback=action::run;activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback);return callback;}
  static void unregister(Activity activity,Object callback){if(callback!=null)activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)callback);}
 }
 @Override protected void onDestroy(){destroyed=true;webGeneration++;if(Build.VERSION.SDK_INT>=33)Api33Back.unregister(this,backCallback);updater.destroy();statementImport.destroy();if(!cleanupCamera())toast("临时照片未能确认清理，请重新打开应用重试");web.removeJavascriptInterface("Android");web.destroy();super.onDestroy();}
}
