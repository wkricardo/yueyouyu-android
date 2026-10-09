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
 private Object backCallback;
 private StatementImport statementImport;
 private GitHubUpdater updater;
 private boolean updateChecked=false,cameraPending=false;
 private static final int FOOD_CAMERA=42;
 private static final Uri CAMERA_URI=Uri.parse("content://cn.dot.budget.files/images/capture.jpg");
 private String pendingBackup;
 private int notificationId=100;
 private static final int EXPORT=10, IMPORT=11;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().setStatusBarColor(Color.rgb(244,248,252));
  getWindow().setNavigationBarColor(Color.rgb(244,248,252));
  getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  web=new WebView(this);
  statementImport=new StatementImport(this,json->web.evaluateJavascript("window.receiveRecognition("+JSONObject.quote(json)+")",null),json->web.evaluateJavascript("window.receiveFoodRecognition("+JSONObject.quote(json)+")",null));
  cameraPending=state!=null&&state.getBoolean("cameraPending",false);
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
 public class Bridge {
  @JavascriptInterface public void captureFoodPhoto(){runOnUiThread(()->statementImport.prepareFoodCamera(()->captureFood()));}
  @JavascriptInterface public void chooseFoodPhoto(){runOnUiThread(()->statementImport.chooseFood());}
  @JavascriptInterface public void checkForUpdates(){runOnUiThread(()->updater.check(true));}
  @JavascriptInterface public void chooseStatementScreenshot(){runOnUiThread(()->statementImport.choose());}
  @JavascriptInterface public void configureDeepSeek(){runOnUiThread(()->statementImport.configure(null));}
  @JavascriptInterface public void clearDeepSeekKey(){runOnUiThread(()->statementImport.clear());}
  @JavascriptInterface public void exportBackup(String json){runOnUiThread(()->{if(json.length()>8_000_000){toast("数据过大，无法导出");return;}pendingBackup=json;Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"月有余备份-"+new java.text.SimpleDateFormat("yyyyMMdd-HHmm",java.util.Locale.CHINA).format(new java.util.Date())+".json");startActivityForResult(i,EXPORT);});}
  @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMPORT);});}
  @JavascriptInterface public void notify(String title,String text){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;Notification n=new Notification.Builder(MainActivity.this,"budget").setSmallIcon(cn.dot.budget.R.drawable.ic_launcher).setContentIntent(PendingIntent.getActivity(MainActivity.this,0,new Intent(MainActivity.this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true).build();getSystemService(NotificationManager.class).notify(notificationId++,n);});}
  @JavascriptInterface public void requestNotifications(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);else toast("系统通知已可用，请检查系统通知设置");});}
 }
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==FOOD_CAMERA){cameraPending=false;revokeUriPermission(CAMERA_URI,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(result==RESULT_OK)statementImport.selectedFood(CAMERA_URI);else new File(getCacheDir(),"food-capture.jpg").delete();return;}if(result!=RESULT_OK || data==null || data.getData()==null)return;try{
  if(request==StatementImport.FOOD_PICK){statementImport.selectedFood(data.getData());return;}
  if(request==StatementImport.PICK){statementImport.selected(data.getData());return;}
  if(request==EXPORT){if(pendingBackup==null){toast("请重新导出");return;}try(OutputStream out=getContentResolver().openOutputStream(data.getData())){out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));}pendingBackup=null;toast("备份已保存");}
  if(request==IMPORT){ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(data.getData())){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>8_000_000)throw new IOException("文件超过 8 MB");out.write(b,0,n);}}String json=out.toString("UTF-8");web.evaluateJavascript("window.receiveBackup("+JSONObject.quote(json)+")",null);}
 }catch(Exception e){toast("操作失败，请检查文件或重试");}}
 private void captureFood(){if(cameraPending)return;try{File image=new File(getCacheDir(),"food-capture.jpg");if(image.exists())image.delete();image.createNewFile();Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).putExtra(android.provider.MediaStore.EXTRA_OUTPUT,CAMERA_URI).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);intent.setClipData(ClipData.newRawUri("食物照片",CAMERA_URI));cameraPending=true;startActivityForResult(intent,FOOD_CAMERA);}catch(Exception e){cameraPending=false;new File(getCacheDir(),"food-capture.jpg").delete();toast("无法打开系统相机，请选择已有食物照片");}}
 @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("cameraPending",cameraPending);super.onSaveInstanceState(state);}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void routeBack(){if(web==null||isFinishing())return;web.evaluateJavascript("window.handleBack ? window.handleBack() : false",v->{if(!"true".equals(v))finish();});}
 @Override public void onBackPressed(){routeBack();}
 private static final class Api33Back {
  static Object register(Activity activity,Runnable action){android.window.OnBackInvokedCallback callback=action::run;activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,callback);return callback;}
  static void unregister(Activity activity,Object callback){if(callback!=null)activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((android.window.OnBackInvokedCallback)callback);}
 }
 @Override protected void onDestroy(){if(Build.VERSION.SDK_INT>=33)Api33Back.unregister(this,backCallback);updater.destroy();statementImport.destroy();web.removeJavascriptInterface("Android");web.destroy();super.onDestroy();}
}
