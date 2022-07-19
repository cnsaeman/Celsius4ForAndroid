/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.tools;

import atlantis.tools.Parser;
import celsius.Resources;
import celsius.components.bibliography.BibTeXRecord;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.data.Person;
import celsius.data.TableRow;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 *
 * @author cnsaeman
 */
public class CelsiusTemplate {
    
    public final String templateString;
    public ArrayList<String> ifs;
    public ArrayList<String> keys;
    public final SimpleDateFormat FDF;
    
    public final Resources RSC;
    public final Library library;


    public CelsiusTemplate (Resources rsc,String ts,Library lib) {
        RSC=rsc;
        library=lib;
        FDF=new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");  
        if (ts==null) templateString="EMPTY"; else templateString=ts;
        ifs=new ArrayList<>();
        keys=new ArrayList<>();
        int i=templateString.indexOf("#");
        int j=0;
        while (i>-1) {
            if (templateString.substring(i+1,i+4).equals("if#")) {
                i=i+3;
                j=templateString.indexOf("#",i+1);
                String ifstring=templateString.substring(i+1,j);
                if (!ifs.contains(ifstring)) ifs.add(ifstring);
            } else {
                j=templateString.indexOf("#",i+1);
                String key=templateString.substring(i+1,j);
                if (!keys.contains(key)) keys.add(key);
                i=templateString.indexOf("#",j+1);                
            }
        }
    }
    
    public String fillIn(Item item,Boolean fast) {
        if (!fast) item.loadLevel(3);
        String  out=templateString;
        
        // setup bibtex fields
        String bibtex = item.getS("bibtex");
        if ((bibtex != null) && (bibtex.length() > 0)) {
            BibTeXRecord BTR = new BibTeXRecord(bibtex);
            for (String key : BTR.keySet()) item.put("$bibtex."+key,BTR.get(key));
            if (RSC.journalLinks.containsKey(BTR.get("journal"))) {
                item.put("$journallink", "Yes");
            }
        }
        
        // setup thumbnail
        if (item.hasThumbnail()) {
            item.put("$thumbnail", item.getThumbnailPath());
        }
        
        
        // setup times, catch Number format exceptions
        try {
            if (item.isEmpty("$created")) {
                item.put("$created", RSC.timestampToString(item.getS("createdTS")));
            }
            if (item.isEmpty("$last_modified")) {
                item.put("$last_modified", RSC.SDF.format(new Date(Long.valueOf(item.getS("last_modifiedTS")) * 1000)));
            }
        } catch (Exception ex) { }
        
        // go through all if statements and adjust
        for (String ift : ifs) {
            int i=out.indexOf("#if#"+ift+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                if (!item.getS(ift).isEmpty()) {
                    out2+=out.substring(i+5+ift.length());
                } else {
                    int j=out.indexOf("\n",i);
                    if (j>0) out2+=out.substring(j+1);
                }
                out=out2;
                i=out.indexOf("#if#"+ift+"#");
            }
        }
        
        for (String key : keys) {
            String field=key;
            String insert = item.getS(field);
            if (field.equals("last_modifiedTS")) {
                insert = timeStampToDateTimeString(Long.valueOf(insert));
            } else if (field.equals("list_links")) {
                insert=item.getLinkListString();
            } else if (key.contains("&")) {
                int modifier = 0;
                modifier=Integer.valueOf(Parser.cutFrom(key, "&"));
                field=Parser.cutUntil(key,"&");
                if (library.isPeopleField(field)) {
                    if (modifier > 0) {
                        switch (modifier) {
                            case 1:
                                insert = item.getShortNames(field);
                                break;
                            case 2:
                                insert = item.getBibTeXNames(field);
                                break;
                            case 3:
                                insert = item.getNames(field, 3);
                                break;
                            case 4:
                                insert = item.getNames(field, 4);
                                break;
                            case 31:
                                insert = item.getNames(field, 31);
                                break;
                            default:

                        }
                    }
                }
                if (field.equals("attachments")) insert=item.getAttachments(modifier);
            } else if (item.library.isPeopleField(key)) {
                insert=item.getNames(field, 3);
            }
            if (field.equals("$categories")) {
                insert = item.getClickableCategoriesList();
            }
            int i=out.indexOf("#"+key+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                out2+=insert+out.substring(i+2+key.length());
                out=out2;
                i=out.indexOf("#"+key+"#");
            }
        }
        return(out.toString());
    }

    public String fillIn(TableRow tableRow,Boolean fast) {
        if (!fast) tableRow.loadLevel(3);
        String  out=templateString;
        // setup thumbnail
        String thumb=tableRow.getThumbnailPath();
        if (thumb!=null) {
            tableRow.put("$thumbnail",thumb);
        }
        for (String ift : ifs) {
            int i=out.indexOf("#if#"+ift+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                if (!tableRow.getS(ift).isEmpty()) {
                    out2+=out.substring(i+5+ift.length());
                } else {
                    int j=out.indexOf("\n",i);
                    if (j>0) out2+=out.substring(j+1);
                }
                out=out2;
                i=out.indexOf("#if#"+ift+"#");
            }
        }
        for (String key : keys) {
            String field=key;
            String insert = tableRow.getS(field);
            if (field.equals("last_modifiedTS")) {
                insert = timeStampToDateTimeString(Long.valueOf(insert));
            } else if (field.equals("list_links")) {
                insert=((Person) tableRow).getLinkListString();
            } else if (field.equals("collaborators")) {
                String[] collaborators = ToolBox.stringToArray(((Person) tableRow).collaborators);
                String[] collaboratorsID = ToolBox.stringToArray(((Person) tableRow).collaboratorsID);
                insert = "";
                for (int j = 0; j < collaborators.length; j++) {
                    insert += ", <a href='http://$$person." + collaboratorsID[j] + "'>" + collaborators[j] + "</a>";
                }
                if (insert.length() > 0) {
                    insert = insert.substring(2);
                }
            } else if (key.contains("&")) {
                int modifier = 0;
                modifier=Integer.valueOf(Parser.cutFrom(key, "&"));
                field=Parser.cutUntil(key,"&");
                if (modifier > 0) {
                    switch (modifier) {
                        default:

                    }
                }
            } 
            int i=out.indexOf("#"+key+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                out2+=insert+out.substring(i+2+key.length());
                out=out2;
                i=out.indexOf("#"+key+"#");
            }
        }
        return(out.toString());
    }
    
    public String timeStampToDateTimeString(long ts) {
        Date date = new java.util.Date(ts*1000L); 
        return(FDF.format(date));
    }
    
    public String getS(HashMap<String,String> properties, String key) {
        String s=properties.get(key);
        if (s!=null) return s;
        return("");
    }
    
    public String fillIn(HashMap<String,String> data) {
        String  out=templateString;
        for (String ift : ifs) {
            int i=out.indexOf("#if#"+ift+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                if (!getS(data,ift).isEmpty()) {
                    out2+=out.substring(i+5+ift.length());
                } else {
                    int j=out.indexOf("\n",i);
                    if (j>0) out2+=out.substring(j+1);
                }
                out=out2;
                i=out.indexOf("#if#"+ift+"#");
            }
        }
        for (String key : keys) {
            int i=out.indexOf("#"+key+"#");
            while (i>-1) {
                String out2="";
                if (i>0) out2+=out.substring(0,i);
                out2+=getS(data,key)+out.substring(i+2+key.length());
                out=out2;
                i=out.indexOf("#"+key+"#");
            }
        }
        return(out.toString());
    }
 
    
    /**
     * Fills in an HTML template according to the properties given
     * @param s the template
     * @param properties the properties
     * @return the filled-out HTML string
     */
    private String replaceInTemplate(String s, HashMap<String,String> properties) {
        String template=s;
        String line,tag,value;
        String out="";

        while (template.length() > 0) {
            line = Parser.cutUntil(template, "\n");
            template = Parser.cutFrom(template, "\n");
            if (line.startsWith("#if#")) {
                while (line.startsWith("#if#")) {
                    line=Parser.cutFrom(line,"#if#");
                    tag=Parser.cutUntil(line,"#");
                    if (tag.charAt(0)=='!') {
                        tag=tag.substring(1);
                        if ((!properties.containsKey(tag)) || (properties.get(tag).length()==0))
                            line=Parser.cutFrom(line,"#");
                        else line="";
                    } else {
                        if ((properties.containsKey(tag)) && (properties.get(tag).length()>0))
                            line=Parser.cutFrom(line,"#");
                        else line="";
                    }
                }
            } else {
                out+="\n";
            }
            if (line.trim().length() > 0) {
                for (String key : properties.keySet()) {
                    value = properties.get(key);
                    line=line.replace("#" + key + "#", value);
                    line=line.replace("#|" + key + "#", "<ul><li>"+Parser.replace(value,"|", "</li><li>")+"</li></ul>");
                    line=line.replace("#$" + key + "#", value);
                }
                out+=line;
            }
        }
        return(out.trim());
    }

    
}
