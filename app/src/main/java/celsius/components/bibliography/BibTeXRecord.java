//
// Celsius Library System
// (w) by C. Saemann
//
// BibTeXRecord.java
//
// This class reflects a bibtex record
//
// typesafe
//
// checked: 
// 11/2009
// testing a modification

package celsius.components.bibliography;

import java.util.LinkedHashMap;
import atlantis.tools.Parser;
import celsius.data.Item;
import celsius.tools.ToolBox;

/**
 *
 * @author cnsaeman
 */
public class BibTeXRecord extends LinkedHashMap<String,String> {

    // parse errors: 255: null string, 254: empty string
    
    // Status messages after adding an item
    public static final String[] status={
        "Everything OK",  // 0
        "Unknown parse Error", // 1
        "Empty tag or type", // 2
        "Bracketing mismatch", // 3
        "Empty key or value" // 4
    };

    private static final String spaces="                                                ";

    public String type;
    public String celtype;
    private String tag;
    
    public int parseError;

    /**
     * Returns an empty BibTeX record
     */
    public BibTeXRecord() {
        super();
        type = "Article";
        celtype= "Other";
        tag = "";
        put("author", "");
        put("title", "");
        put("journal", "");
        put("volume", "");
        put("year", "");
        put("pages", "");
        parseError=0;
    }
    
    public BibTeXRecord (String bibtex) {
        super();
        if (bibtex==null) { parseError=255; return; }
        parseError = 0;
        // See if the string just indicates a type
        if (bibtex.equals("Thesis")) {
            type = "Article";
            celtype = bibtex;
            tag = "";
            put("author", "");
            put("title", "");
            put("university", "");
            put("year", "");
            return;
        } else if (bibtex.equals("Book") || bibtex.equals("eBook")) {
            type = "Book";
            celtype = bibtex;
            tag = "";
            put("author", "");
            put("title", "");
            put("publisher", "");
            put("location", "");
            put("year", "");
            return;
        } else if (bibtex.equals("Preprint") || bibtex.equals("Paper") || bibtex.length()<20) {
            type = "Article";
            celtype = bibtex;
            tag = "";
            put("author", "");
            put("title", "");
            put("journal", "");
            put("volume", "");
            put("year", "");
            put("pages", "");
            return;
        }
        bibtex=bibtex.replaceAll("(?m)^%.+?$", "");
        if (bibtex.trim().equals("")) {
            parseError = 254;
            type = "empty";
            tag = "empty";
            return;
        }
        try {
            if (bibtex.trim().equals("")) {
                type = "empty";
                tag = "empty";
                return;
            }
            type = Parser.cutFrom(Parser.cutUntil(bibtex, "{"), "@").trim();
            tag = Parser.normalizeSpecialCharacters(Parser.cutFrom(Parser.cutUntil(bibtex, ","), "{"));
            if (type.equals("") || tag.equals("")) {
                type = "empty";
                tag = "empty";
                parseError=2;
                return;
            }
            if (Parser.howOftenContains(bibtex,"{")!=Parser.howOftenContains(bibtex,"}")) {
                type = "empty";
                tag = "empty";
                parseError=3;
                return;
            }
            String remainder = Parser.cutFrom(bibtex, ",");
            String key, value;
            int i,j,k,l,c,s,e;
            while (remainder.length()>1) {
                // cut key
                i=remainder.indexOf('=');
                key=remainder.substring(0, i).trim().toLowerCase();
                remainder=remainder.substring(i+1).trim();

                // cut value
                j=remainder.indexOf('{');
                k=remainder.indexOf('\"');
                l=remainder.indexOf(',');
                if (l==-1) l=remainder.length();
                if (j==-1) j=remainder.length();
                if (k==-1) k=remainder.length();
                if ((j<l) && (k>j)) {
                    // enclosed in { }
                    s=1;
                    c=1; i=j;
                    while (c!=0) {
                        i++;
                        if (remainder.charAt(i)=='{') c++;
                        if (remainder.charAt(i)=='}') c--;
                    }
                } else {
                    if (k<l) {
                        // enclosed in " "
                        s=1;
                        i=remainder.indexOf('\"',k+1);
                        if (i>0) {
                            while (remainder.charAt(i-1)=='\\') i=remainder.indexOf('\"',i+1);
                        } else i=1;
                    } else {
                        // not enclosed in delimeters
                        s=0;
                        i=l;
                    }
                }
                value=remainder.substring(s,i).trim();
                l=remainder.indexOf(',',i)+1;
                if (l==0) l=remainder.length();

                // adjust value
                value = value.replace('\n', ' ');
                while (value.indexOf("  ") > -1) {
                    value = value.replace("  ", " ");
                }
                if (key.equals("") || value.equals("")) {
                    //parseError=4;
                }
                put(key, value);
                remainder = remainder.substring(l);
            }
            adjustCelType();
        } catch (Exception e) {
            e.printStackTrace();
            parseError=1;
        }
    }
    
    public String getTag() {
        return(tag);
    }

    public void setTag(String t) {
        tag=Parser.normalizeSpecialCharacters(t);
    }

    private void adjustCelType() {
        String t=type.toLowerCase();
        if (t.equals("article") || t.equals("unpublished")) celtype="Preprint";
        if (keySet().contains("journal")) celtype="Paper";
        if (t.indexOf("thesis")>-1) celtype="Thesis";
        if (t.indexOf("book")>-1) celtype="Book";
    }
        
    private String filler(String key) {
        int maxKeyLength=0;
        for (String s : keySet()) {
            if (s.length()>maxKeyLength) maxKeyLength=s.length();
        }
        int l=maxKeyLength-key.length();
        if (l>spaces.length()) l=spaces.length();
        return(key+(spaces.substring(0,l)));
    }


    public boolean isNotSet(String s) {
        return(!this.containsKey(s));
    }

    public boolean isEmpty(String s) {
        if (!this.containsKey(s)) return(true);
        return(getS(s).equals(""));
    }

    public String getS(String s) {
        String tmp=get(s);
        if (tmp==null) tmp=new String("");
        return(tmp);
    }

    public String getIdentifier() {
        String identifier=new String("");
        if (get("journal")!=null) {
            identifier=get("journal");
            if (get("volume")!=null) identifier+=" "+get("volume");
            if (get("year")!=null) identifier+=" ("+get("year")+")";
            if (get("pages")!=null) identifier+=" "+get("pages");
        }
        identifier=identifier.trim();
        return(identifier);
    }
    
    @Override
    public String toString() {
        if (type.equals("empty")) return("");
        String tmp="@"+type+"{"+tag;
        for (String key : keySet()) {
            tmp+=",\n   "+filler(key)+" = \""+get(key)+"\"";
        }
        tmp+="\n}";
        return(tmp);
    }
    
    public boolean equals(BibTeXRecord btr) {
        if (!tag.equals(btr.tag)) return(false);
        if (!type.equals(btr.type)) return(false);
        if (keySet().size()!=btr.keySet().size()) return(false);
        for (String key : this.keySet()) {
            if ((btr.get(key)==null) || (!get(key).equals(btr.get(key)))) return(false);
        }
        return(true);
    }    

    /**
     * Checks whether a bibtex entry is formed consistently
     */
    public static boolean isBibTeXConsistent(String bibtex) {
        boolean consistent=true;
        try {
            BibTeXRecord btr=new BibTeXRecord(bibtex);
            consistent=((btr.parseError>0) || (btr.parseError<250));
            if (btr.tag.indexOf(" ")>-1) consistent=false;
            // check matching brackets
            String lines[] = Parser.cutUntilLast(Parser.cutFrom(bibtex,"{"),"}").split("\",");
            for(String l:lines) {
                if (Parser.howOftenContains(l, "{")!=Parser.howOftenContains(l, "}")) consistent=false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            consistent=false;
        }
        return(consistent);
    }

    /**
     * Creates a Celsius author string from a BibTeX one
     */
    public static String convertBibTeXAuthorsToCelsius(String tmp) {
        String authors=new String("");
        if (tmp.indexOf(',')>-1) {
            authors=tmp.replace(" and ","|");
        } else {
            String tmp2;
            while (tmp.length()>0) {
                tmp2=Parser.cutUntil(tmp," and ").trim();
                authors+="|"+Parser.cutFromLast(tmp2," ")+", "+Parser.cutUntilLast(tmp2, " ");
                tmp=Parser.cutFrom(tmp," and ").trim();
            }
            authors=authors.substring(1);
        }
        return(authors);
    }
    
    /**
     * Turn an author string into a short string
     */
    public static String convertCelsiusAuthorsToBibTeX(String authors) {
        return (authors.replaceAll("\\|", " and "));
    }
    
    public static String normalizeTitle(String title) {
        StringBuffer out=new StringBuffer();
        int pos=0;
        title=title.trim();
        int mode=-1; // mode: 0: just after space, 1: inside word, 2 and higher: inside {
        while (pos<title.length()) {
            if (mode==-1) {
                out.append(Character.toUpperCase(title.charAt(pos)));
            } else if (mode==0) {
                out.append(Character.toLowerCase(title.charAt(pos)));
            } else {
                out.append(title.charAt(pos));
            }
            if (title.charAt(pos)==' ') mode=0;
            if (title.charAt(pos)=='{') mode++;
            if (title.charAt(pos)=='}') mode--;
            if (mode<0) mode=1;
            pos++;
        }
        return(out.toString());
    }
    
    
    /**
     * Normalize the BibTeX-String passed as an argument
     */    
    public static String normalizeBibTeX(String tmp) {
        if (tmp.trim().length()<1) return("");
        BibTeXRecord btr=new BibTeXRecord(tmp);
        if (btr.parseError!=0) {
            return("BibTeX entry not consistent: "+BibTeXRecord.status[btr.parseError]);
        }
        btr.put("title", normalizeTitle(btr.get("title")));
        return(sanitize(btr.toString()));
    }   

    public static String Identifier(Item item) {
        String tmp = "";
        String arxref = item.get("arxiv-ref");
        String arxname = item.get("arxiv-name");
        if (arxref != null) {
            if (arxref.contains(arxname)) {
                tmp = arxref;
            } else {
                tmp = arxref + " [" + arxname + "]";
            }
        }
        String bibtexstr = item.get("bibtex");
        if (bibtexstr != null) {
            BibTeXRecord btr = new BibTeXRecord(bibtexstr);
            if (btr.parseError == 0) {
                tmp += " " + btr.getIdentifier();
                String eprint = btr.get("eprint");
                if (eprint != null) {
                    int i = eprint.indexOf("/");
                    if (i > 0) {
                        if (eprint.charAt(i + 1) == '9') {
                            tmp = "19" + eprint.substring(i + 1, i + 3) + " " + tmp;
                        } else {
                            tmp = "20" + eprint.substring(i + 1, i + 3) + " " + tmp;
                        }
                    } else {
                        tmp = "20" + eprint.substring(0, 2) + " " + tmp;
                    }
                } else {
                    if (btr.get("year") != null) {
                        tmp = btr.get("year") + " " + tmp;
                    }
                }
            }
        }
        return tmp.trim();
    }
    
    /**
     * Removes all special UTF8-characters from auhors names
     * 
     * @param input
     * @return 
     */
    public static String sanitize(String input) {
        input=input.replace("č", "\\v{c}");
        input=input.replace("á", "\\'{a}");
        input=input.replace("à", "\\`{a}");
        input=input.replace("é", "\\'{e}");
        input=input.replace("è", "\\`{e}");
        input=input.replace("ê", "\\^{e}");
        input=input.replace("í", "\\'{\\i}");
        input=input.replace("ô", "\\^{o}");
        return(input);
    }
    
    
}
