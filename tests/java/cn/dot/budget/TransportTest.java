package cn.dot.budget;
import java.io.*;import java.net.*;import java.nio.charset.StandardCharsets;
public class TransportTest{
 static int checks=0;static void check(boolean v,String m){if(!v)throw new AssertionError(m);checks++;}
 static class Mock extends HttpURLConnection{
  ByteArrayOutputStream sent=new ByteArrayOutputStream();byte[] body;int status;boolean closed;
  Mock(URL u,int s,byte[] b){super(u);status=s;body=b;}
  public void connect(){}public void disconnect(){closed=true;}public boolean usingProxy(){return false;}
  public OutputStream getOutputStream(){return sent;}public int getResponseCode(){return status;}public InputStream getInputStream(){return new ByteArrayInputStream(body);}
 }
 static Mock current;
 static URL mock(int status,byte[] body)throws Exception{return new URL(null,"https://api.deepseek.com/chat/completions",new URLStreamHandler(){protected URLConnection openConnection(URL u){return current=new Mock(u,status,body);}});}
 public static void main(String[]a)throws Exception{
  String payload="{\"model\":\"deepseek-flash\",\"messages\":[]}";
  String got=new DeepSeekTransport().sendTo(mock(200,"{\"choices\":[]}".getBytes(StandardCharsets.UTF_8)),"fake-test-key",payload);
  check(got.equals("{\"choices\":[]}"),"body read");check(current.sent.toString("UTF-8").equals(payload),"request exact");check(current.getRequestProperty("Authorization").equals("Bearer fake-test-key"),"auth header");check(!current.getInstanceFollowRedirects(),"redirects disabled");check(current.getRequestMethod().equals("POST"),"post");check(current.closed,"disconnect success");
  for(int code:new int[]{401,402,403,429,302,500}){try{new DeepSeekTransport().sendTo(mock(code,new byte[0]),"fake-test-key",payload);throw new AssertionError("error not raised");}catch(IOException expected){check(!expected.getMessage().contains("fake-test-key"),"no secret in error");check(current.closed,"disconnect error");}}
  try{new DeepSeekTransport().sendTo(mock(200,new byte[2000001]),"fake-test-key",payload);throw new AssertionError("size");}catch(IOException expected){check(expected.getMessage().contains("过大"),"bounded response");}
  DeepSeekTransport t=new DeepSeekTransport();t.cancel();try{t.sendTo(mock(200,new byte[0]),"fake-test-key",payload);throw new AssertionError("cancel");}catch(IOException expected){check(expected.getMessage().contains("取消"),"cancelled before request");}
  check(DeepSeekTransport.ENDPOINT.equals("https://api.deepseek.com/chat/completions"),"fixed official endpoint");
  System.out.println("PASS: "+checks+" mocked transport assertions. No network, real API key, screenshot or paid API call used.");
 }
}
