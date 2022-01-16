//
// Celsius Library System
// (w) by C. Saemann
//
// TextDatei.java
//
// This class contains the basic textfile routines used by the XML-engines and
// Celsius.
//
// typesafe
//
// checked 15.09.2007
//

package atlantis.tools;

import java.net.*;
import java.util.Properties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.zip.*;

// to be cleaned

public class TextFile {
    private boolean VWrite;               // not (read only)
    private String VName;                 // file name
    private int line;                     // current line in file
    private String lf;					  // linefeed
    private FileReader fr=null;           // Readers for internal use.
    private BufferedReader br=null;
    private OutputStream out=null;
    
    private GZIPInputStream fis=null;
    private InputStreamReader isr=null;
    private FileOutputStream fos=null;
    
    /**
     * Open a compressed TextDatei for reading, name and compression.
     */
    public TextFile(String s,String z) throws IOException {
        VName=s;
        VWrite=false;
        if (z.equals("GZIP")) {
            fis  = new GZIPInputStream(new FileInputStream(new File(s)));
            isr = new InputStreamReader(fis);
            br=new BufferedReader(isr);
        }
    }
    
    /**
     * Open a TextDatei for Reading
     */
    public TextFile(String s) throws IOException {
        VName=s;
        VWrite=false;
        fr=new FileReader(s);
        br=new BufferedReader(fr);
        line=1;
    }
    
    /**
     * Create a new compressed TextDatei for writing: filename, compression, append
     */
    public TextFile(String s,String z,boolean a) throws IOException {
        VName=s;
        VWrite=true;
        Properties p=System.getProperties();
        lf=p.getProperty("line.separator");
        if (z.equals("GZIP")) {
            fos=new FileOutputStream(s,((new File(s)).exists() && a));
            out=new GZIPOutputStream(fos);
        }
    }
    
    /**
     * Create a new Textdatei for writing: filename, append
     */
    public TextFile(String s,boolean a) throws IOException {
        VName=s;
        VWrite=true;
        Properties p=System.getProperties();
        lf=p.getProperty("line.separator");
        out=new FileOutputStream(s,((new File(s)).exists() && a));
    }
    
    /**
     * Gzip a certain file and delete the original
     */
    public static void GZip(String s1) throws IOException {
        FileInputStream fis  = new FileInputStream(new File(s1));
        GZIPOutputStream fos = new GZIPOutputStream(new FileOutputStream(new File(s1+".gz")));
        byte[] buf = new byte[1024];
        int i = 0;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
        (new File(s1)).delete();
    }
    
    public static void GUnZip(String s1) throws IOException {
        GZIPInputStream  fis = new GZIPInputStream(new FileInputStream(new File(s1)));
        s1=s1.substring(0,s1.length()-3);
        FileOutputStream fos = new FileOutputStream(new File(s1));
        byte[] buf = new byte[1024];
        int i = 0;
        while((i=fis.read(buf))!=-1) {
            fos.write(buf, 0, i);
        }
        fis.close();
        fos.close();
        (new File(s1+".gz")).delete();
    }
    
    /**
     * Open a zip file
     */
    public static ZipOutputStream OpenZip(String tos) throws IOException {
        ZipOutputStream to=new ZipOutputStream(new FileOutputStream(tos,(new File(tos)).exists()));
        return(to);
    }
    
    /**
     * Add a Zip entry to zip file
     */
    public static void AddToZip(ZipOutputStream to,String s) throws IOException {
        FileInputStream from=new FileInputStream(s);
        to.putNextEntry(new ZipEntry(s));
        byte[] buffer=new byte[4096];
        int bytes_read;
        while((bytes_read=from.read(buffer))!=-1)
            to.write(buffer,0,bytes_read);
        from.close();
        to.closeEntry();
    }
    
    /**
     * Close Zip-File
     */
    public static void CloseZip(ZipOutputStream to) throws IOException {
        to.close();
    }

    public TextFile(URL url) throws FileNotFoundException {
        VName=url.getPath();
        VWrite=false;
        fr=new FileReader(url.getPath());
        br=new BufferedReader(fr);
        line=1;
    }
    
    /**
     * write a string with linefeed
     */
    public void putString(String s) throws IOException {
        if (VWrite) {
            String tmp=new String(s+lf);
            out.write(tmp.getBytes());
        }
        line++;
    }
    
    /**
     * write a string without linefeed
     */
    public void putStringO(String s) throws IOException {
        if (VWrite) {
            String tmp=new String(s);
            out.write(tmp.getBytes());
        }
    }
    
    /**
     * read a string
     */
    public String getString() throws IOException {
        if (!VWrite) { line++;return(br.readLine()); }
        return(null);
    }

    /**
     * read a string
     */
    public String getFastString() throws IOException {
        return(br.readLine());
    }
    
    /**
     * read a string
     */
    public String getSafeString() throws IOException {
        if (!ready()) return(new String(""));
        if (!VWrite) { line++;return(br.readLine()); }
        return(new String(""));
    }
    
    /**
     * returns, whether file is ready for reading
     */
    public boolean ready() throws IOException {
        return(br.ready());
    }
    
    /**
     * returns the current line number
     */
    public int getLine() {
        return(line);
    }
    
    /**
     * returns the name of the TextDatei
     */
    public String getName() {
        return(VName);
    }
    
    /**
     * Returns true, if a file is writable
     */
    public boolean getWrite() {
        return(VWrite);
    }
    
    /**
     * Close the TextDatei
     */
    public void close() throws IOException {
        if (out!=null) {
            out.flush();
            out.close();
        }
        if (br!=null) br.close();
        if (fr!=null) fr.close();
        if (fis!=null) fis.close();
        if (isr!=null) isr.close();
        if (fos!=null) fos.close();
    }
    
    /**
     * Copies the content of URL s1 into File s2.
     * Returns true/false, depending on the success
     */
    public static boolean ReadOutURLToFile(String s1, String s2) {
        boolean Success=true;
        InputStream in=null;
        OutputStream out=null;
        try {
            URL url=new URL(s1);
            in=url.openStream();
            out=new FileOutputStream(s2);
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                out.write(buffer,0,bytes_read);
            }
        } catch (Exception e) {
            Success=false;
        } finally {
            try {
                in.close();
                out.close();
            } catch(Exception e) {}
        }
        return(Success);
    }
    
    /**
     * Returns the content of a URL as a string
     */
    public static String ReadOutURL(String s1) {
        StringBuffer rtn=new StringBuffer(4000);
        try {
            URL url=new URL(s1);
            URLConnection urlconn=url.openConnection();
            urlconn.setReadTimeout(10000);
            urlconn.setConnectTimeout(10000);
            InputStream in=urlconn.getInputStream();
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                rtn.append(new String(buffer,0,bytes_read));
            }
            in.close();
        } catch (Exception e) { rtn=new StringBuffer("##??"+e.toString()); }
        return(rtn.toString());
    }
    
    /**
     * Returns the content of a file as a String
     */
    public static String ReadOutFile(String s1) {
        StringBuffer rtn=new StringBuffer(4000);
        try {
            InputStream in=new FileInputStream(s1);
            
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                rtn.append(new String(buffer,0,bytes_read));
            }
            in.close();
        } catch (Exception e) { rtn=new StringBuffer("##??"+e.toString()); }
        return(rtn.toString());
    }

    /**
     * Returns the content of a file as a String
     */
    public static StringBuffer ReadOutZipFile(String filename) {
        StringBuffer buffer=new StringBuffer();
        String tmp;
        try {
            GZIPInputStream fis = new GZIPInputStream(new FileInputStream(new File(filename)));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            while ((tmp = br.readLine()) != null) {
                buffer.append(tmp);
                buffer.append('\n');
            }
            br.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (buffer);
    }
    
    




    
}