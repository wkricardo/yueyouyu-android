package cn.dot.budget;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Fixed public release origin. No credentials, cookies, automatic redirects or retries. */
public final class GitHubUpdateTransport {
 public static final String REPOSITORY="wkricardo/yueyouyu-android";
 public static final String API="https://api.github.com/repos/"+REPOSITORY+"/releases/latest";
 public static final String APK="yueyouyu-android.apk";
 public static final String CERT="42932ba6fc75b6e1e216b8def10e4af37bb73e6ebc9c3b94d5d6e7206b8bd5fb";
 public static final long MAX_APK=150L*1024*1024;
 interface Connector { HttpURLConnection open(URL url)throws IOException; }
 private final Connector connector;
 private volatile HttpURLConnection active;
 private volatile boolean cancelled;
 public GitHubUpdateTransport(){this(url->(HttpURLConnection)url.openConnection());}
 GitHubUpdateTransport(Connector connector){this.connector=connector;}
 public static void validateUrl(URL u,boolean api,boolean initial)throws IOException {
  String h=u.getHost();
  if(!"https".equals(u.getProtocol())||u.getUserInfo()!=null||u.getRef()!=null||(u.getPort()!=-1&&u.getPort()!=443))throw new IOException("更新地址不安全");
  if(api){if(!API.equals(u.toExternalForm()))throw new IOException("更新 API 地址不受信任");return;}
  if("github.com".equals(h)){
   if(!u.getPath().startsWith("/"+REPOSITORY+"/releases/download/")||u.getPath().contains("..")||u.getQuery()!=null)throw new IOException("更新下载地址不受信任");
  }else if(initial||!("release-assets.githubusercontent.com".equals(h)||"objects.githubusercontent.com".equals(h)))throw new IOException("更新下载主机不受信任");
 }
 public String text(String address,int limit,boolean api)throws IOException {
  ByteArrayOutputStream out=new ByteArrayOutputStream();transfer(address,out,limit,api);return new String(out.toByteArray(),StandardCharsets.UTF_8);
 }
 public long download(String address,File target,long size)throws IOException {
  if(size<=0||size>MAX_APK)throw new IOException("更新大小不合法");
  try(OutputStream out=new FileOutputStream(target)){long received=transfer(address,out,size,false);if(received!=size)throw new IOException("更新下载不完整");return received;}
  catch(IOException e){target.delete();throw e;}
 }
 private long transfer(String address,OutputStream out,long limit,boolean api)throws IOException {
  URL u=new URL(address);validateUrl(u,api,true);long deadline=System.nanoTime()+(api?45000000000L:300000000000L);
  for(int redirects=0;redirects<=4;redirects++){
   checkCancelled();if(System.nanoTime()>deadline)throw new IOException("更新请求超时");HttpURLConnection c=connector.open(u);active=c;
   try{
    checkCancelled();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(30000);
    c.setRequestProperty("User-Agent","YueYouYu-Android-Updater");c.setRequestProperty("Accept",api?"application/vnd.github+json":"application/octet-stream");c.setRequestProperty("Accept-Encoding","identity");
    int code=c.getResponseCode();
    if(code==301||code==302||code==303||code==307||code==308){
     if(api||redirects==4)throw new IOException("更新重定向已拒绝");
     String location=c.getHeaderField("Location");if(location==null)throw new IOException("缺少更新重定向地址");
     u=new URL(u,location);validateUrl(u,false,false);continue;
    }
    if(code==404)throw new IOException("尚无可用的公开更新");
    if(code!=200)throw new IOException(code==403||code==429?"GitHub 暂时限制请求，请稍后重试":"更新服务暂不可用（HTTP "+code+"）");
    long length=c.getContentLengthLong();if(length>limit)throw new IOException("更新响应超过大小限制");
    long total=0;byte[] buffer=new byte[16384];try(InputStream in=c.getInputStream()){int n;while((n=in.read(buffer))!=-1){checkCancelled();if(System.nanoTime()>deadline)throw new IOException("更新请求超时");total+=n;if(total>limit)throw new IOException("更新响应超过大小限制");out.write(buffer,0,n);}}
    checkCancelled();return total;
   }finally{c.disconnect();active=null;}
  }
  throw new IOException("更新重定向过多");
 }
 public boolean isCancelled(){return cancelled;}
 void checkCancelled()throws IOException {if(cancelled||Thread.currentThread().isInterrupted())throw new IOException("更新已取消");}
 public void cancel(){cancelled=true;HttpURLConnection c=active;if(c!=null)c.disconnect();}
 public static String sha256(byte[] bytes)throws Exception{return hex(MessageDigest.getInstance("SHA-256").digest(bytes));}
 public static String digest(File file)throws Exception{MessageDigest md=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1)md.update(b,0,n);}return hex(md.digest());}
 private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b)s.append(String.format(Locale.ROOT,"%02x",v&255));return s.toString();}
 public static String checksum(String manifest)throws IOException {
  String result=null;
  for(String line:manifest.split("\\r?\\n")){
   String[] pair=line.trim().split("\\s+",2);if(pair.length!=2)continue;
   String name=pair[1].startsWith("*")?pair[1].substring(1):pair[1];
   if(APK.equals(name)){if(result!=null||!pair[0].matches("[a-fA-F0-9]{64}"))throw new IOException("更新校验文件无效");result=pair[0].toLowerCase(Locale.ROOT);}
  }
  if(result==null)throw new IOException("缺少 APK 的 SHA-256 校验值");return result;
 }
 public static void verifyIdentity(String pkg,long installed,long archive,String installedCert,String archiveCert)throws IOException {
  if(!"cn.dot.budget".equals(pkg))throw new IOException("更新包名不匹配");
  if(archive<=installed)throw new IOException("更新版本未高于当前版本");
  if(!CERT.equals(installedCert)||!CERT.equals(archiveCert))throw new IOException("更新签名不匹配，已拒绝安装");
 }
 public static void verifyChecksum(String expected,String actual)throws IOException{if(!expected.equals(actual))throw new IOException("更新文件 SHA-256 不匹配，已拒绝安装");}
 public static boolean shouldOffer(boolean draft,boolean prerelease,String version,String installed)throws IOException {
  if(draft||prerelease)throw new IOException("尚无可用的正式更新");return compareVersions(version,installed)>0;
 }
 public static void validateAssets(String version,String apk,String sums,long size)throws IOException {
  if(apk==null||sums==null)throw new IOException("此版本缺少 APK 或 SHA256SUMS，暂不能更新");
  if(size<=0||size>MAX_APK)throw new IOException("更新包大小不合法");
  validateUrl(new URL(apk),false,true);validateUrl(new URL(sums),false,true);
  String base="https://github.com/"+REPOSITORY+"/releases/download/"+version+"/";
  if(!apk.equals(base+APK)||!sums.equals(base+"SHA256SUMS"))throw new IOException("更新文件地址与版本不匹配");
 }
 public static int compareVersions(String candidate,String current)throws IOException {
  String a=candidate.startsWith("v")?candidate.substring(1):candidate;String b=current.startsWith("v")?current.substring(1):current;
  if(!a.matches("[0-9]{1,8}(\\.[0-9]{1,8}){1,3}")||!b.matches("[0-9]{1,8}(\\.[0-9]{1,8}){1,3}"))throw new IOException("更新版本格式不支持");
  String[] x=a.split("\\."),y=b.split("\\.");for(int i=0;i<Math.max(x.length,y.length);i++){int n=i<x.length?Integer.parseInt(x[i]):0,m=i<y.length?Integer.parseInt(y[i]):0;if(n!=m)return Integer.compare(n,m);}return 0;
 }
}
