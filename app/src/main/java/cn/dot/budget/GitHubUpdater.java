package cn.dot.budget;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/** Public GitHub checks only; downloading and invoking Android installer each need a user tap. */
public final class GitHubUpdater {
 private final Activity activity;
 private final Consumer<String> status;
 private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private final AtomicBoolean busy=new AtomicBoolean();
 private volatile boolean destroyed;
 private volatile GitHubUpdateTransport transport;
 private AlertDialog dialog;
 private ProgressDialog progress;
 public GitHubUpdater(Activity activity,Consumer<String> status){this.activity=activity;this.status=status;}
 private void ui(Runnable action){activity.runOnUiThread(()->{if(!destroyed&&!activity.isFinishing()&&!activity.isDestroyed())action.run();});}
 private void report(String text){ui(()->status.accept(text));}
 public void check(boolean manual){
  if(destroyed)return;
  if(!busy.compareAndSet(false,true)){if(manual)report("正在检查或下载更新，请稍候");return;}
  if(manual)report("正在检查 GitHub 更新…");
  executor.execute(()->{
   GitHubUpdateTransport current=new GitHubUpdateTransport();transport=current;
   try{
    JSONObject release=new JSONObject(current.text(GitHubUpdateTransport.API,512*1024,true));
    String version=release.getString("tag_name");PackageInfo installed=installed();
    if(!GitHubUpdateTransport.shouldOffer(release.optBoolean("draft",true),release.optBoolean("prerelease",true),version,installed.versionName)){if(manual)report("当前已是最新版本（"+installed.versionName+"）");return;}
    JSONArray assets=release.getJSONArray("assets");String apk=null,sums=null;long size=0;
    for(int i=0;i<assets.length();i++){
     JSONObject asset=assets.getJSONObject(i);String name=asset.optString("name");
     if(GitHubUpdateTransport.APK.equals(name)){if(apk!=null)throw new IOException("更新文件重复");apk=asset.getString("browser_download_url");size=asset.getLong("size");}
     if("SHA256SUMS".equals(name)){if(sums!=null)throw new IOException("更新校验文件重复");sums=asset.getString("browser_download_url");}
    }
    GitHubUpdateTransport.validateAssets(version,apk,sums,size);
    Release next=new Release(version,apk,sums,size);ui(()->offerDownload(next));
   }catch(Exception e){if(manual)report(message(e));}
   finally{transport=null;busy.set(false);}
  });
 }
 private void offerDownload(Release release){
  if(dialog!=null&&dialog.isShowing())return;
  status.accept("发现新版本 "+release.version);
  dialog=new AlertDialog.Builder(activity).setTitle("发现新版本 "+release.version)
   .setMessage("来源：GitHub / "+GitHubUpdateTransport.REPOSITORY+"\n大小："+size(release.size)+"\n\n下载后会校验文件、包名和签名，再由你确认打开 Android 安装界面。更新保留现有账目。")
   .setNegativeButton("暂不更新",null).setPositiveButton("下载更新",(d,w)->download(release)).create();dialog.show();
 }
 private void download(Release release){
  if(!busy.compareAndSet(false,true)){report("正在处理更新，请稍候");return;}
  GitHubUpdateTransport current=new GitHubUpdateTransport();transport=current;
  progress=new ProgressDialog(activity);progress.setTitle("下载 "+release.version);progress.setMessage("正在下载并校验（"+size(release.size)+"）…");progress.setIndeterminate(true);progress.setCancelable(true);progress.setCanceledOnTouchOutside(false);progress.setOnCancelListener(d->current.cancel());progress.setButton(ProgressDialog.BUTTON_NEGATIVE,"取消",(d,w)->current.cancel());progress.show();
  executor.execute(()->{
   File part=new File(activity.getCacheDir(),"update.pending.apk"),apk=new File(activity.getCacheDir(),"update.apk");
   try{
    if(apk.exists()&&!apk.delete())throw new IOException("无法清理旧更新文件");
    String expected=GitHubUpdateTransport.checksum(current.text(release.sums,64*1024,false));
    current.download(release.apk,part,release.size);GitHubUpdateTransport.verifyChecksum(expected,GitHubUpdateTransport.digest(part));
    verifyArchive(part,release.version);
    current.checkCancelled();if(destroyed)throw new IOException("更新已取消");
    if(!part.renameTo(apk))throw new IOException("无法保存更新文件");
    ui(()->{dismissProgress();if(current.isCancelled()){apk.delete();status.accept("更新已取消");}else showInstall(release,expected);});
   }catch(Exception e){part.delete();apk.delete();ui(()->{dismissProgress();status.accept(message(e));});}
   finally{transport=null;busy.set(false);}
  });
 }
 private void dismissProgress(){if(progress!=null){progress.dismiss();progress=null;}}
 private void showInstall(Release release,String expected){
  dialog=new AlertDialog.Builder(activity).setTitle("更新已通过校验")
   .setMessage("版本："+release.version+"\n大小："+size(release.size)+"\nSHA-256、包名与原应用签名均匹配。\n\n点击后打开 Android 安装界面，由你最后确认安装。")
   .setNegativeButton("稍后",null).setPositiveButton("安装更新",(d,w)->install(release,expected)).create();dialog.show();
 }
 private void install(Release release,String expected){
  if(!activity.getPackageManager().canRequestPackageInstalls()){
   dialog=new AlertDialog.Builder(activity).setTitle("需要允许安装应用")
    .setMessage("Android 尚未允许月有余安装更新。你可以前往系统设置自行开启，返回后再次点击“安装更新”。")
    .setNegativeButton("取消",null).setPositiveButton("前往系统设置",(d,w)->{
     try{activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));showInstall(release,expected);}
     catch(Exception e){status.accept("无法打开安装权限设置，请在系统设置中手动查看");}
    }).create();dialog.show();return;
  }
  // Recheck immediately before granting the installer read access; no downloaded file is executed here.
  if(!busy.compareAndSet(false,true)){report("正在处理更新，请稍候");return;}
  report("正在再次校验安装文件…");
  executor.execute(()->{
   try{
    File apk=new File(activity.getCacheDir(),"update.apk");if(apk.length()!=release.size)throw new IOException("更新文件大小不匹配");
    GitHubUpdateTransport.verifyChecksum(expected,GitHubUpdateTransport.digest(apk));verifyArchive(apk,release.version);
    ui(()->{
     try{
      Uri uri=Uri.parse("content://cn.dot.budget.files/apk/update.apk");
      Intent intent=new Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      intent.setClipData(ClipData.newRawUri("月有余更新",uri));activity.startActivity(intent);status.accept("已打开系统安装界面，请确认是否安装");
     }catch(Exception e){status.accept("无法打开系统安装界面，请稍后重试");}
    });
   }catch(Exception e){report(message(e));}
   finally{busy.set(false);}
  });
 }
 private int flags(){return Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;}
 private PackageInfo installed()throws Exception{return activity.getPackageManager().getPackageInfo(activity.getPackageName(),flags());}
 private long code(PackageInfo info){return Build.VERSION.SDK_INT>=28?info.getLongVersionCode():info.versionCode;}
 private String signer(PackageInfo info)throws Exception{
  Signature[] signatures=Build.VERSION.SDK_INT>=28?(info.signingInfo==null?null:info.signingInfo.getApkContentsSigners()):info.signatures;
  if(signatures==null||signatures.length!=1)throw new IOException("无法验证应用签名");return GitHubUpdateTransport.sha256(signatures[0].toByteArray());
 }
 private void verifyArchive(File file,String version)throws Exception{
  PackageInfo archive=activity.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(),flags());if(archive==null)throw new IOException("APK 无效或无法读取签名");
  PackageInfo current=installed();GitHubUpdateTransport.verifyIdentity(archive.packageName,code(current),code(archive),signer(current),signer(archive));
  if(GitHubUpdateTransport.compareVersions(version,archive.versionName)!=0)throw new IOException("更新版本与发布说明不一致");
 }
 private static String size(long bytes){return String.format(Locale.CHINA,"%.1f MB",bytes/(1024.0*1024.0));}
 private static String message(Exception e){return e instanceof IOException&&e.getMessage()!=null?e.getMessage():"无法检查或验证更新，请稍后重试";}
 public void destroy(){destroyed=true;GitHubUpdateTransport current=transport;if(current!=null)current.cancel();executor.shutdownNow();if(dialog!=null)dialog.dismiss();dismissProgress();}
 private static final class Release {final String version,apk,sums;final long size;Release(String v,String a,String s,long z){version=v;apk=a;sums=s;size=z;}}
}
