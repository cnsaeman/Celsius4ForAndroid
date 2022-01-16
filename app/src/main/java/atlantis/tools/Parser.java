//
// Celsius Library System
// (w) by C. Saemann
//
// Parser.java
//
// This class contains the routines for parsing text.
//
// typesafe
//
// checked: 16.09.2007
//

package atlantis.tools;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class Parser {
    
    /**
     * Routines to treat enumlists: lists, consisting of strings separated by a comma
     */
    
    /**
     * Cut all html tags out of string s
     */
    public static String cutTags(String s) {
        int i=s.indexOf("<");
        int j=s.indexOf(">",i);
        String tmp;
        while ((i>-1) && (j>-1)) {
            tmp="";
            if (i>0) tmp=s.substring(0,i);
            if (j<s.length()-1) tmp=tmp+s.substring(j+1);
            s=tmp.trim();
            i=s.indexOf("<");
            j=s.indexOf(">",i);
        }
        return(s);
    }
    
    /**
     * public static String Substitute(String s,String e,String f)
     * Substitutes e in s by f, Deprecated, replaced with String's replace-method
     */
    public static String replace(String s, String e, String f) {
        int j = s.indexOf(e);
        if (j > -1) {
            String tmp = "";
            while (j > -1) {
                tmp += s.substring(0, j) + f;
                if (s.length() > j + e.length()) {
                    s = s.substring(j + e.length());
                } else {
                    s = "";
                }
                j = s.indexOf(e);
            }
            tmp += s;
            return (tmp);
        } else {
            return (s);
        }
    }

    public static boolean isBlank(String s) {
        return ((s==null) || ("".equals(s)));
    }
    
    public static boolean hasAREF(String s) {
        String tmp=s.toLowerCase();
        int i=tmp.indexOf("<a ");
        i=tmp.indexOf("href",i);
        int j=tmp.indexOf("</a>",i);
        return((i>0) && (j>0));
    }
    
    public static String cutUntilAREF(String s) {
        String tmp=s.toLowerCase();
        try {
            int i=tmp.indexOf("<a ");
            if (i>-1) {
                s=s.substring(i);
            } else {
                s="";
            }
        } catch(Exception e) { }
        if (s.toLowerCase().indexOf("href")>s.toLowerCase().indexOf("</a>")) {
            s=cutUntilAREF(s.substring(1));
        }
        return(s);
    }
    
    public static String cutAREF(String s) {
        String tmp=s.toLowerCase();
        try {
            int i=tmp.indexOf("<a ");
            int j=tmp.indexOf("</a>",i);
            if (j>0) {
                s=s.substring(j+4);
            }
        } catch(Exception e) {
        }
        return(s);
    }
    
    /**
     * Remove or simplify special characters, trim and to lowercase
     * @param in input string
     * @return normalized strings
     */
    public static String normalizeForSearch(String in) {
        return(Normalizer.normalize(in.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
    }
    
    /**
     * Remove or simplify special characters
     * @param in input string
     * @return normalized strings
     */
    public static String normalizeSpecialCharacters(String in) {
        return(Normalizer.normalize(in.trim(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
    }

    /**
     * Substitutes problematic characters in filenames by descriptions
     */
    public static String cutProhibitedChars2(String txt) {
        if (txt.startsWith("(")) txt="~~"+txt;
        txt=txt.replace("/","~slash~");
        txt=txt.replace(":","~colon~");
        txt=txt.replace("*","~asterisk~");
        txt=txt.replace("?","~questionmark~");
        txt=txt.replace("\"","~quotationmark~");
        txt=txt.replace("\\","~backslash~");
        txt=txt.replace("<","~lessthan~");
        txt=txt.replace(">","~greaterthan~");
        txt=txt.replace("|","~pipe~");
        txt=txt.replace("$","~dollar~");
        txt=txt.replace("{","~curly(~");
        txt=txt.replace("}","~curly)~");
        txt=txt.replace("&","~amp~");
        txt=txt.replace("'","~prime~");
        return(txt);
    }
    
    public static String restoreProhibitedChars2(String txt) {
        if (txt.startsWith("~~")) txt=txt.substring(2);
        txt=txt.replace("~slash~","/");
        txt=txt.replace("~prime~","'");
        txt=txt.replace("~colon~",":");
        txt=txt.replace("~asterisk~","*");
        txt=txt.replace("~questionmark~","?");
        txt=txt.replace("~quotationmark~","\"");
        txt=txt.replace("~backslash~","\\");
        txt=txt.replace("~lessthan~","<");
        txt=txt.replace("~greaterthan~",">");
        txt=txt.replace("~pipe~","|");
        txt=txt.replace("~dollar~","$");
        txt=txt.replace("~curly(~","{");
        txt=txt.replace("~amp~","&");
        txt=txt.replace("~curly)~","}");
        return(txt);
    }
    
    public static String cutProhibitedChars(String txt) {
        txt=txt.replace("/","");
        txt=txt.replace(":","");
        txt=txt.replace("*","");
        txt=txt.replace("?","");
        txt=txt.replace("\"","");
        txt=txt.replace("\\","");
        txt=txt.replace("<","");
        txt=txt.replace(">","");
        txt=txt.replace("|","");
        txt=txt.replace("$","");
        txt=txt.replace("{","");
        txt=txt.replace("&","");
        txt=txt.replace("}","");
        txt=txt.replace("\n"," ");
        return(txt);
    }
    
    /**
     * schneidet gemeinsamen Anfang ab, dann s1 zurück.
     */
    public static String cutCommonFirst(String s1,String s2) {
        StringBuffer t1=new StringBuffer(s1);
        StringBuffer t2=new StringBuffer(s2);
        while (t1.charAt(0)==t2.charAt(0)){
            t1=t1.deleteCharAt(0);
            t2=t2.deleteCharAt(0);
        }
        return(t1.toString());
    }
    
    /**
     * liefert String vor Folge s2 bzw. den vollen String bzw. leeren String zurück.
     */
    public static String cutUntil(String s1,String s2) {
        int i=s1.indexOf(s2);
        if (i==-1) return(s1);
        return(new String(s1.substring(0,i)));
    }
    
    /**
     * liefert Reststring ab Folge s2 bzw. leeren String zurück.
     */
    public static String cutFrom(String s1,String s2) {
        int i=s1.indexOf(s2);
        if (i==-1) return("");
        i+=s2.length();
        return(new String(s1.substring(i)));
    }
    
    /**
     * liefert String vor Folge s2 bzw. den vollen String bzw. leeren String zurück.
     */
    public static String cutUntilLast(String s1,String s2) {
        int i=s1.lastIndexOf(s2);
        if (i==-1) return(s1);
        return(new String(s1.substring(0,i)));
    }
    
    /**
     * liefert Reststring ab Folge s2 bzw. leeren String zurück.
     */
    public static String cutFromLast(String s1,String s2) {
        int i=s1.lastIndexOf(s2);
        if (i==-1) return("");
        i+=s2.length();
        return(new String(s1.substring(i)));
    }
    
    /**
     * schneidet vorangehendes s2 aus s1 aus oder liefert s1 komplett zurück.
     */
    public static String cutLeading(String s1,String s2) {
        if (s1.indexOf(s2)==0) return(s1.substring(s2.length()));
        return(s1);
    }
    
    /**
     * schneidet abschließendes s2 von s1 ab oder liefert s1 komplett zurück.
     */
    public static String cutLast(String s1,String s2) {
        if (s1.endsWith(s2)) return(s1.substring(0,s1.length()-s2.length()));
        return(s1);
    }
    
    public static String lowerPart(String s1,String s2) {
        String tmp=s1.toLowerCase();
        String target="";
        int i=tmp.indexOf(s2);
        int j=0;
        if (i<0) return(s1);
        while (i>-1){
            target+=s1.substring(j,i);
            target+=s2;
            j=i+s2.length();
            i=tmp.indexOf(s2,i+1);
        }
        if (j<s1.length()) target+=s1.substring(j);
        return(target);
    }
    
    public static String cutLastCharacters(String s,int i) {
        if (s.length()<i) return(s);
        return(s.substring(0,s.length()-i));
    }
    
    public static boolean isCapitalized(String s1) {
        return (s1.equals(s1.toUpperCase()));
    }
    
    public static String lowerEndOfWords(String s1) {
        String spaces=" {=,::.!-(\"";
        String tmp=s1.toUpperCase();
        String target="";
        int i;
        if (tmp.length()<1) return("<empty>");
        target+=tmp.substring(0,1);
        for(i=1;i<tmp.length();i++) {
            if (spaces.indexOf(tmp.substring(i-1,i))==-1) {
                target+=tmp.substring(i,i+1).toLowerCase();
            } else {
                target+=tmp.substring(i,i+1);
            }
        }
        target=target.replace("Ii","II");
        target=target.replace("Iii","III");
        return(target);
    }

    public static String lowerEndOfWords2(String s1) {
        String spaces="={}.:;!'-(\"";
        String tmp=s1.toUpperCase();
        String target="";
        int i;
        if (tmp.length()<1) return("<empty>");
        target+=tmp.substring(0,1);
        for(i=1;i<tmp.length();i++) {
            if ((spaces.indexOf(tmp.substring(i-1,i))==-1) && 
                !((tmp.charAt(i-1)==' ') && (spaces.indexOf(tmp.substring(i-2,i-1))>-1))) {
                target+=tmp.substring(i,i+1).toLowerCase();
            } else {
                target+=tmp.substring(i,i+1);
            }
        }
        target=target.replace("Ii","II");
        target=target.replace("Iii","III");
        return(target);
    }
    
    
    public static String cutAREFURL(String s) {
        String tmp=s.toLowerCase();
        try {
            int i=tmp.indexOf("<a ");
            i=tmp.indexOf("href",i);
            int k=tmp.indexOf("=",i)+1;
            int j=tmp.indexOf(">",k);
            return(s.substring(k,j).replace("\"",""));
        } catch(Exception e) {
        }
        return("");
    }
    
    public static String cutAREFTXT(String s) {
        String tmp=s.toLowerCase();
        int i=tmp.indexOf("<a ");
        int k=tmp.indexOf("<a",i);
        k=tmp.indexOf(">",k+1);
        int j=tmp.indexOf("</a>",k);
        if (j==-1) return("");
        String txt;
        try {
            txt=s.substring(k+1,j).trim();
            txt=cutTags(txt).trim();
        } catch(Exception e) { txt=e.getMessage(); }
        return(txt);
    }
    
    public static String extractInBrackets(String s1) {
        String tmp=s1;
        String tmp2="";
        while (tmp.length()>0) {
            tmp=cutFrom(tmp,"(");
            tmp2+=cutUntil(tmp,")");
        }
        return(tmp2);
    }
    
    public static String extractInBracketsWS(String s1) {
        String tmp=s1;
        String tmp2="";
        while (tmp.length()>0) {
            tmp=cutFrom(tmp,"(");
            tmp2+=cutUntil(tmp,")")+" ";
        }
        return(tmp2.trim());
    }
    
    public static String cutBrackets(String s1) {
        String tmp=s1;
        String tmp2="";
        while (tmp.length()>0) {
            tmp2+=cutUntil(tmp,"(");
            tmp=cutFrom(tmp,")");
        }
        return(tmp2);
    }
    
    // Counts number of occurences of a substring. cleaned 1
    public static int howOftenContains(String s1,String s2) {
        int j=0; // number of occurences
        int k=s2.length(); // length of big string
        int i=-k; // starting point
        while ((i=s1.indexOf(s2,i+k))>-1)
            j++;
        return(j);
    }
    
    public static String returnFirstItem(String s1) {
        if (s1.length()==0) return(s1);
        String tmp;
        if (s1.charAt(0)=='"') {
            s1=cutFrom(s1,"\"");
            tmp=cutUntil(s1,"\"");
        } else { tmp=cutUntil(s1,"|"); }
        return(tmp.trim());
    }
    
    public static String cutFirstItem(String s1) {
        if (s1.length()==0) return(s1);
        String tmp;
        if (s1.charAt(0)=='"') {
            s1=cutFrom(s1,"\"");
            tmp=cutFrom(s1,"\" ");
        } else { tmp=cutFrom(s1,"|"); }
        return(tmp.trim());
    }
    
    public static String decodeHTML(String s1) {
        s1=s1.replaceAll(Pattern.quote("&quot;"),"\"");
        s1=s1.replaceAll(Pattern.quote("&lt;"),"<");
        s1=s1.replaceAll(Pattern.quote("&gt;"),">");
        s1=s1.replaceAll(Pattern.quote("&amp;"),"&");
        int i=s1.indexOf("&#x");
        while (i>-1) {
            int j=s1.indexOf(";",i);
            String sub=s1.substring(i+3,j);
            char c=(char)((int)(Integer.parseInt(sub,16)));
            s1=s1.substring(0,i)+c+s1.substring(j+1);
            i=s1.indexOf("&#x");
        }
        return(s1);
    }
    
    /**
     * Returns true if |-separated list s1 contains s2
     */
    public static boolean listContains(String s1,String s2) {
        if (s1==null) return(false);
        if (s1.length()<s2.length()) return(false);
        if (s1.equals(s2)) return(true);
        if (s1.indexOf("|"+s2+"|")>-1) return(true);
        if (s1.startsWith(s2+"|")) return(true);
        if (s1.endsWith("|"+s2)) return(true);
        return(false);
    }
    
}