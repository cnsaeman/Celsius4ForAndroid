//
// Celsius Library System
// (w) by C. Saemann
//
// toolbox.java
//
// This class contains various tools used in the other classes
//
// typesafe, pre-completed
//
// checked 15.09.2007
//

package celsius.tools;

import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import celsius.Resources;
import celsius.data.Item;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.zip.GZIPInputStream;

public class ToolBox {
    
    // Final Strings
    public final static Object[] optionsYNC = { "Yes", "No", "Cancel" };
    public final static Object[] optionsOC = { "OK", "Cancel" };
    public final static Object[] optionsYN = { "Yes", "No" };
    
    private static int threadindex=0;
    
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String EOP=String.valueOf((char)12);   // EndOfPage signal
    
    /**
     * Returns a new index for each thread, for debugging in Msg1
     */
    public static String getThreadIndex() {
        return(Integer.toString(threadindex++));
    }

    public static String fillLeadingZeros(String n,int i) {
        String out=n;
        while (out.length()<i) out="0"+out;
        return(out);
    }

    public static String formatSeconds(int sec) {
        int h = sec / 3600;
        int m = (sec - h * 3600) / 60;
        int s = (sec - h * 3600 - m * 60);
        String outdur = fillLeadingZeros(String.valueOf(m), 2) + ":" + fillLeadingZeros(String.valueOf(s), 2);
        if (h > 0) {
            outdur = String.valueOf(h) + ":" + outdur;
        }
        return (outdur);
    }
    
    /**
     *  Returns current timestamp
     */
    public static String getCurrentDate() {
        Date ActD=new Date();
        return(ActD.toString());
    }
    
    /**
     * Returns the number of pages out
     * @param RSC
     * @param TI: Thread indicator
     * @param s : original file path
     * @param t : plaintext path
     */
    public static int readNumberOfPagesOf(Resources RSC,String TI,String s,String t) throws IOException {
        RSC.out(TI+"Reading Number of Pages :: "+s);

        int pages=-1;
        String tmp;  // Dummy for reading in strings
        try {
            // Count pages for postscript by reading %%Pages entry in postscript
            if (s.toLowerCase().endsWith(".ps.gz")) {
                RSC.out(TI+"Postscript #pages");
                // Initialize file readers
                GZIPInputStream   fis = new GZIPInputStream(new FileInputStream(new File(s)));
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader    br  = new BufferedReader(isr);
                tmp=br.readLine();
                // Go to %%Pages metadata
                while ((br.ready()) && (!tmp.contains("%%Pages"))) tmp=br.readLine();
                if (tmp==null) {
                    RSC.out(TI+"No plain text file found.");
                    return(0);
                }
                if (tmp.contains("%%Pages")) {
                    tmp=Parser.cutUntil(Parser.cutFrom(tmp," ")," ");
                    try {
                        pages=Integer.valueOf(tmp);
                    } catch (NumberFormatException e) {
                        // %%Pages entry not working.
                        // Looking for another entry
                        RSC.out(TI+"First %%Pages entry does not work, looking for second one in "+s);
                        tmp=br.readLine();
                        while ((br.ready()) && (!tmp.contains("%%Pages"))) tmp=br.readLine();
                        // second one found?
                        if (tmp.contains("%%Pages")) {
                            tmp=Parser.cutUntil(Parser.cutFrom(tmp," ")," ");
                            try {
                                pages=Integer.valueOf(tmp);
                            } catch (NumberFormatException f) {
                                // doesn't work either
                                RSC.out(TI+"Unknown %%Pages format."); pages=-1;
                            }
                        } else { pages=0; }
                    }
                } else {
                    RSC.out("GTI>No Pages tag found."); pages=0;
                }
                br.close(); isr.close(); fis.close();
            }
            // Count pages for pdf by /Type commands
            if (s.toLowerCase().endsWith(".pdf")) {
                RSC.out(TI+"PDF #pages");
                TextFile f1=new TextFile(s);
                pages=0;
                while (f1.ready()) {
                    tmp=f1.getString();
                    if (tmp.contains("/Type"))
                        if (tmp.contains("/Page")) pages++;
                    if (tmp.contains("/Type"))
                        if (tmp.contains("/Pages")) pages--;
                }
                f1.close();
            }
            // Count pages for djvu by reading EOPs in plaintxt
            if ((pages==-1) && (t!=null)) {
                RSC.out(TI+"Other #pages");
                pages=0;
                GZIPInputStream fis  = new GZIPInputStream(new FileInputStream(new File(t)));
                RSC.out(TI+"Reading "+t);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br=new BufferedReader(isr);
                while (((tmp=br.readLine())!=null))
                    if (tmp.indexOf(ToolBox.EOP)>-1) pages++;
                br.close(); isr.close(); fis.close();
            }
        } catch (IOException e) { RSC.out(TI+"Error reading pages: "+e.toString()); }
        RSC.out(TI+"pages read:"+String.valueOf(pages));
        return(pages);
    }

    public static String getFirstPage(String s) {
        if (s==null) return("");
        String firstpage;
        if (!(new File(s)).exists()) {
            return("No plain text file found.");
        }
        try {
            GZIPInputStream fis = new GZIPInputStream(new FileInputStream(new File(s)));
            InputStreamReader isr = new InputStreamReader(fis);
            char[] fp = new char[4000];
            isr.read(fp);
            isr.close();
            fis.close();
            firstpage = new String(fp);
        } catch (IOException e) {
            e.printStackTrace();
            firstpage="error::"+e.toString();
        }
        return(firstpage);
    }
            
    public static String wrap(String s) {
        return(wrap(s,80));
    }

    public static String wrap(String s, int len) {
        StringBuilder t=new StringBuilder(s);
        int i=-1;
        while (t.length()>i+len) {
            int k = t.lastIndexOf(" ", i + len);
            if (k > i) {
                int l = t.lastIndexOf("\n", i + len);
                if ((l>i) && (l < k)) {
                    i = l;
                } else {
                    t.replace(k, k + 1, "\n");
                    i += len;
                }
            } else {
                k=i+len;
                t.insert(k, "\n");
                i += len;
            }
        }
        return(t.toString());
    }

    public static int intvalue(String s) {
        int i;
        try {
            i=Integer.valueOf(s);
        } catch (Exception e) { i=0; }
        return(i);
    }

    public static String normalize(String s) {
        return(Parser.replace(s.toLowerCase().trim()," ",""));
    }

    public static String md5Hash(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = s.getBytes();
            m.update(data,0,data.length);
            BigInteger i = new BigInteger(1,m.digest());
            return(String.format("%1$032X", i));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return("Error!");
    }

    public static String stripError(String tmp) {
        return(Parser.cutFrom(tmp,"??##Error"));
    }

    /**
     * Creates a Celsius author string from a BibTeX one
     */
    public static String authorsBibTeX2Cel(String tmp) {
        String authors="";
        tmp=Parser.lowerEndOfWords(tmp);
        if (tmp.indexOf(',')>-1) {
            authors=tmp.replace(" And ","|");
        } else {
            String tmp2;
            while (tmp.length()>0) {
                tmp2=Parser.cutUntil(tmp," And ").trim();
                authors+="|"+Parser.cutFromLast(tmp2," ")+", "+Parser.cutUntilLast(tmp2, " ");
                tmp=Parser.cutFrom(tmp," And ").trim();
            }
            if (authors.length()>1) authors=authors.substring(1);
        }
        return(authors);
    }
    
    public static long now() {
        return (System.currentTimeMillis()/1000);
    }
    
    public static String normalizeAuthorStringToCelsius(String tmp) {
        if (!tmp.contains("|")) {
            tmp=ToolBox.authorsBibTeX2Cel(tmp);          
        }
        StringBuilder b=new StringBuilder(tmp);
        boolean up=true;
        for (int i=0;i<b.length();i++) {
            char c=b.charAt(i);
            if (up && Character.isLowerCase(c)) b.setCharAt(i, Character.toUpperCase(c));
            if (!up && Character.isUpperCase(c)) b.setCharAt(i, Character.toLowerCase(c));
            up=false;
            if ((c=='|') || (c==' ')) up=true;
        }
        return(b.toString());
    }

    public static ArrayList<String> stringToArrayList(String list) {
        return(new ArrayList<>(Arrays.asList(stringToArray(list))));
    }

    
    public static String[] stringToArray(String list) {
        String[] out={};
        if ((list!=null) && (list.trim().length()>0)) {
            out=list.split("\\|");
        }
        return(out);
    }

    public static String[] stringToArray2(String list) {
        String[] out={};
        if ((list!=null) && (list.trim().length()>0)) {
            out=list.split(",");
        }
        return(out);
    }

}