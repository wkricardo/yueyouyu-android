package cn.dot.budget;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Pure JVM tests: no sockets, production GitHub, Android installation or credentials. */
public final class UpdaterTest {
 static int count;
 interface Action{void run()throws Exception;}
 static void ok(boolean b){count++;if(!b)throw new AssertionError("test "+count);}
 static void fail(Action a)throws Exception{count++;try{a.run();}catch(IOException expected){return;}throw new AssertionError("expected rejection "+count);}
 static final String ASSET="https://github.com/"+GitHubUpdateTransport.REPOSITORY+"/releases/download/v1.2.0/"+GitHubUpdateTransport.APK;
 static final class Mock extends HttpURLConnection{
  final int response;final byte[] body;String location;long length=-1;boolean disconnected;
  Mock(int code,String body)throws Exception{super(new URL(ASSET));response=code;this.body=body.getBytes(StandardCharsets.UTF_8);}
  public void connect(){}public void disconnect(){disconnected=true;}public boolean usingProxy(){return false;}public int getResponseCode(){return response;}
  public InputStream getInputStream(){return new ByteArrayInputStream(body);}public String getHeaderField(String key){return "Location".equals(key)?location:null;}public long getContentLengthLong(){return length;}
 }
 static GitHubUpdateTransport mock(Mock... responses){Deque<Mock> queue=new ArrayDeque<>(Arrays.asList(responses));return new GitHubUpdateTransport(url->{if(queue.isEmpty())throw new AssertionError("unexpected retry");return queue.remove();});}
 public static void main(String[] args)throws Exception{
  String hash=GitHubUpdateTransport.sha256("hello".getBytes(StandardCharsets.UTF_8));
  ok(hash.equals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824"));
  ok(GitHubUpdateTransport.checksum(hash+"  "+GitHubUpdateTransport.APK).equals(hash));
  ok(GitHubUpdateTransport.checksum(hash+" *"+GitHubUpdateTransport.APK+"\n").equals(hash));
  fail(()->GitHubUpdateTransport.checksum(hash+"  different.apk"));
  fail(()->GitHubUpdateTransport.checksum("bad  "+GitHubUpdateTransport.APK));
  fail(()->GitHubUpdateTransport.checksum(hash+"  "+GitHubUpdateTransport.APK+"\n"+hash+"  "+GitHubUpdateTransport.APK));
  fail(()->GitHubUpdateTransport.verifyChecksum(hash,"0"));
  GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,5,GitHubUpdateTransport.CERT,GitHubUpdateTransport.CERT);count++;
  fail(()->GitHubUpdateTransport.verifyIdentity("other.package",4,5,GitHubUpdateTransport.CERT,GitHubUpdateTransport.CERT));
  fail(()->GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,4,GitHubUpdateTransport.CERT,GitHubUpdateTransport.CERT));
  fail(()->GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,3,GitHubUpdateTransport.CERT,GitHubUpdateTransport.CERT));
  fail(()->GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,5,"bad",GitHubUpdateTransport.CERT));
  fail(()->GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,5,GitHubUpdateTransport.CERT,"bad"));
  ok(GitHubUpdateTransport.compareVersions("v1.2.0","1.1.2")>0);
  ok(GitHubUpdateTransport.compareVersions("v1.2.0","1.2")==0);
  ok(GitHubUpdateTransport.compareVersions("1.1.9","1.2.0")<0);
  fail(()->GitHubUpdateTransport.compareVersions("v1.3.0-beta","1.2.0"));
  fail(()->GitHubUpdateTransport.compareVersions("999999999999999999999","1.2.0"));
  ok(GitHubUpdateTransport.shouldOffer(false,false,"v1.2.0","1.1.2"));
  ok(!GitHubUpdateTransport.shouldOffer(false,false,"v1.1.2","1.1.2"));
  fail(()->GitHubUpdateTransport.shouldOffer(true,false,"v1.2.0","1.1.2"));
  fail(()->GitHubUpdateTransport.shouldOffer(false,true,"v1.2.0","1.1.2"));
  fail(()->GitHubUpdateTransport.shouldOffer(false,false,"banana","1.1.2"));
  fail(()->GitHubUpdateTransport.verifyIdentity("cn.dot.budget",4,5,GitHubUpdateTransport.CERT,null));
  String sums=ASSET.replace(GitHubUpdateTransport.APK,"SHA256SUMS");
  GitHubUpdateTransport.validateAssets("v1.2.0",ASSET,sums,100);count++;
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",ASSET,null,100));
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",null,sums,100));
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",ASSET,sums,0));
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",ASSET,sums,GitHubUpdateTransport.MAX_APK+1));
  fail(()->GitHubUpdateTransport.validateAssets("v1.3.0",ASSET,sums,100));
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",ASSET.replace("v1.2.0","v1.1.0"),sums,100));
  fail(()->GitHubUpdateTransport.validateAssets("v1.2.0",ASSET,sums.replace("SHA256SUMS","update.json"),100));
  for(String u:new String[]{"http://github.com/"+GitHubUpdateTransport.REPOSITORY+"/releases/download/v1/a.apk","https://github.com.evil.example/a","https://github.com@evil.example/a","https://evil.example/a","https://github.com:444/"+GitHubUpdateTransport.REPOSITORY+"/releases/download/v1/a","https://github.com/other/repo/releases/download/v1/a","https://release-assets.githubusercontent.com/a"})fail(()->GitHubUpdateTransport.validateUrl(new URL(u),false,true));
  fail(()->GitHubUpdateTransport.validateUrl(new URL("https://api.github.com/repos/evil/repo/releases/latest"),true,true));
  Mock good=new Mock(200,"hello");ok(mock(good).text(ASSET,10,false).equals("hello"));ok(good.disconnected);
  Mock redir=new Mock(302,"");redir.location="https://release-assets.githubusercontent.com/release-asset/a?sig=public";ok(mock(redir,new Mock(200,"hello")).text(ASSET,10,false).equals("hello"));
  for(String dest:new String[]{"http://release-assets.githubusercontent.com/a","https://evil.example/a","https://github.com/other/repo/releases/download/v1/a","https://api.github.com/a"}){Mock r=new Mock(302,"");r.location=dest;fail(()->mock(r).text(ASSET,10,false));ok(r.disconnected);}
  Mock apiRedirect=new Mock(301,"");apiRedirect.location=GitHubUpdateTransport.API;fail(()->mock(apiRedirect).text(GitHubUpdateTransport.API,10,true));
  Mock loop=new Mock(302,"");loop.location=ASSET;fail(()->mock(loop,loop,loop,loop,loop).text(ASSET,10,false));
  fail(()->mock(new Mock(404,"")).text(GitHubUpdateTransport.API,10,true));
  fail(()->mock(new Mock(429,"")).text(GitHubUpdateTransport.API,10,true));
  fail(()->mock(new Mock(500,"")).text(ASSET,10,false));
  fail(()->mock(new Mock(200,"too long")).text(ASSET,3,false));
  Mock large=new Mock(200,"small");large.length=100;fail(()->mock(large).text(ASSET,10,false));
  GitHubUpdateTransport cancelled=mock();cancelled.cancel();fail(()->cancelled.text(ASSET,10,false));
  File file=File.createTempFile("updater-test", ".apk");
  try{
   ok(mock(new Mock(200,"hello")).download(ASSET,file,5)==5);ok(GitHubUpdateTransport.digest(file).equals(hash));
   fail(()->mock(new Mock(200,"hi")).download(ASSET,file,5));ok(!file.exists());
   fail(()->mock(new Mock(200,"toolong")).download(ASSET,file,3));ok(!file.exists());
   fail(()->mock().download(ASSET,file,GitHubUpdateTransport.MAX_APK+1));
  }finally{file.delete();}
  System.out.println("UpdaterTest passed: "+count+" assertions (mock-only)");
 }
}
