package cn.dot.budget;
import android.content.*;import android.database.*;import android.net.Uri;import android.os.ParcelFileDescriptor;import android.provider.OpenableColumns;import java.io.*;
/** Exact cache routes only: read-only update and pre-created native capture UUID files. */
public final class PrivateFilesProvider extends ContentProvider{
 public boolean onCreate(){return true;}
 private File resolve(Uri uri)throws FileNotFoundException{return new File(getContext().getCacheDir(),PrivateFilePolicy.fileName(uri.getAuthority(),uri.getPath(),"r"));}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{
  File f=new File(getContext().getCacheDir(),PrivateFilePolicy.fileName(uri.getAuthority(),uri.getPath(),mode));
  if(!f.isFile())throw new FileNotFoundException("File is no longer available");
  return ParcelFileDescriptor.open(f,ParcelFileDescriptor.parseMode(mode)&~ParcelFileDescriptor.MODE_CREATE);
 }
 public String getType(Uri uri){try{resolve(uri);return uri.getPath().startsWith("/apk/")?"application/vnd.android.package-archive":"image/jpeg";}catch(Exception e){return null;}}
 public Cursor query(Uri uri,String[]projection,String selection,String[]args,String sort){try{File f=resolve(uri);String[] cols=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor c=new MatrixCursor(cols);Object[] values=new Object[cols.length];for(int i=0;i<cols.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(cols[i]))values[i]=f.getName();if(OpenableColumns.SIZE.equals(cols[i]))values[i]=f.length();}c.addRow(values);return c;}catch(Exception e){return null;}}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[]a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[]a){throw new UnsupportedOperationException();}
}
