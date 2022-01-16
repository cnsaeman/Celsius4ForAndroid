package celsius.data;

import atlantis.tools.*;
import celsius.tools.ToolBox;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author cnsaeman
 */
public final class Item extends TableRow {

    public final String charFilter = "[\\u0000-\\u001F\\u007F]";
    
    public int error;

    private String toText;
    private String toSort;
    
    // linked fields
    public final HashMap<String,ArrayList<Person>> linkedPersons;
    public final ArrayList<Attachment> linkedAttachments;

    public Item(Library lib,int i) {
        super(lib,"items",String.valueOf(i),lib.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        tableHeaders=library.itemPropertyKeys;
        orderedStandardKeys=library.orderedItemPropertyKeys;
        error=0;
        toText=null;
        toSort=null;
        currentLoadLevel=0;
    }

    public Item(Library lib,String i) {
        super(lib,"items",i,lib.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        orderedStandardKeys=library.orderedItemPropertyKeys;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }

    public Item(Library lib) {
        super(lib,"items",lib.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        library=lib;
        orderedStandardKeys=library.orderedItemPropertyKeys;
        tableHeaders=library.itemPropertyKeys;
        error=0;
        // mark as newly created, ensure that all "shorts" are updated
        currentLoadLevel=1000; 
    }
    
    /**
     * Load item at loadLevel 1 with data in ResultSet
     * 
     * @param lib
     * @param rs 
     */
    public Item(Library lib, ResultSet rs) {
        super(lib,"items",rs,lib.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        library=lib;
        orderedStandardKeys=library.orderedItemPropertyKeys;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }
    
    /**
     * Load more data at a particular level:
     * 1 : short standard data
     * 2 : everything in table
     * 3 : everything thats linked
     * 
     * @param targetLoadLevel 
     */
    public void loadLevel(int targetLoadLevel) {
        super.loadLevel(targetLoadLevel);
        try {
            if ((targetLoadLevel == 3) && (currentLoadLevel<3)) {
                loadLinkedData();
                currentLoadLevel=3;
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
            
    /**
     *  Loads all the data, including linked data from other tables
     */
    private void loadLinkedData() {
        // read in people
        int linkType = 0;
        for (String peopleField : library.peopleFields) {
            String sql = "SELECT persons.* FROM item_person_links INNER JOIN persons on persons.id=person_id  WHERE item_id=" + id + " AND link_type=" + String.valueOf(linkType) + " ORDER BY ord ASC";
            try {
                ResultSet rs = library.executeResEX(sql);
                linkedPersons.put(peopleField, new ArrayList<>());
                while (rs.next()) {
                    Person person=new Person(library,rs);
                    linkedPersons.get(peopleField).add(person);
                }
            } catch (Exception e) {
                library.RSC.outEx(e);
            }
            linkType++;
        }

        // read in keywords
        try {
            String sql = "SELECT GROUP_CONCAT(label, '|') AS result FROM item_keyword_links INNER JOIN keywords on keywords.id=keyword_id  WHERE item_id=" + id + " ORDER BY label ASC";
            ResultSet rs = library.executeResEX(sql);
            if (rs.next()) {
                properties.put("keywords", rs.getString(1));
            }
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
        
        // read in registration
        try {
            String sql = "SELECT GROUP_CONCAT(label, ' | ') AS result FROM item_category_links INNER JOIN item_categories on item_categories.id=category_id  WHERE item_id=" + id + " ORDER BY label ASC";
            ResultSet rs = library.executeResEX(sql);
            if (rs.next()) {
                properties.put("$categories", rs.getString(1));
            }
        } catch (Exception e) {
            library.RSC.outEx(e);
        }

        // read in attachments
        String sql = "SELECT attachments.* FROM item_attachment_links INNER JOIN attachments on attachments.id=attachment_id  WHERE item_id=" + id + " ORDER BY ord ASC;";
        try {
            ResultSet rs = library.executeResEX(sql);
            while (rs.next()) {
                Attachment attachment = new Attachment(library, this, rs);
                attachment.attachToParent();
            }
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
        
        // read in links
        sql = "SELECT * FROM item_item_links JOIN items on item_item_links.item2_id=items.id WHERE item1_id="+id+";";
        try {
            ResultSet rs = library.executeResEX(sql);
            while (rs.next()) {
                Integer itemLinkType=rs.getInt(3);
                Item item=new Item(library,rs);
                if (!linkedItems.containsKey(itemLinkType)) linkedItems.put(itemLinkType, new ArrayList<>());
                linkedItems.get(itemLinkType).add(item);
            }
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
        // 
        
        properties.put("$attachment-count", String.valueOf(linkedAttachments.size()));
    }
            

    @Override
    public String getIconField(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        if (library.iconDictionary.containsKey(tmp))
            tmp=library.iconDictionary.get(tmp);
        return(tmp);
    }

    @Override
    public String getExtended(String tag) {
        int i = tag.indexOf("&");
        if (i > -1) {
            char tp = tag.charAt(i + 1);
            tag = tag.substring(0, i);
            switch (tp) {
                case '1':
                    return (getShortNames(tag));
                case '2':
                    return (getBibTeXNames(tag));
                case '3':
                    return (getNames(tag,3));
                case '4':
                    return (getNames(tag,4));
                case '5': {
                    String out="<html><b><tt>";
                    String lf=getS(tag);
                    for (int j=0;j<lf.length();j++) {
                        switch (lf.charAt(j)) {
                            case 'F': {
                                out += "<FONT COLOR=\"#83cd53\">F</FONT>";
                                break;
                                }
                            case 'X': {
                                out += "<FONT COLOR=\"#fe5919\">X</FONT>";
                                break;
                                }
                            default : out+="<FONT COLOR=GRAY>.</FONT>";
                        }
                    }
                    return(out+"</tt></b></html>");
                }
                default:
                    return (getS(tag));
            }
        }
        return (getS(tag));
    }

    /**
     * Checks if value is null or blank, if not write it
     * 
     * @param key
     * @param value 
     */
    public void putS(String key,String value) {
        if ((value!=null) && (value.trim().length()!=0)) put(key,value);
    }
    
    @Override
    public boolean hasThumbnail() {
        return((new File(library.completeDir("LD::item-thumbnails/"+id+".jpg"))).exists());
    }
    
    @Override
    public String getThumbnailPath() {
        return(library.completeDir("LD::item-thumbnails/"+id+".jpg"));
    }

    /**
     * Save an item. Note that this should only be done if loadlevel is maximal
     */
    public void save() {
    }
    
    public boolean guaranteeStandardFolder() {
        String folder=library.completeDir(getStandardFolder(),"");
        if (folder==null) return(true);
        if (!(new File(folder)).exists())
            return((new File(folder)).mkdir());
        return(true);
    }

    public String getStandardFolder() {
        String folder=library.itemFolder.fillIn(this, true);
        folder=Parser.replace(folder, "\\\\", "\\");
        return(folder);
    }
        
    /* incorporate into others */
    public void updateShorts() {
        // update other short keys
        if (currentLoadLevel>2) {
            for (String shortkey : library.shortKeys) {
                // update short person keys
                if (library.isPeopleField(shortkey)) {
                    put("short_" + shortkey, getShortNames(shortkey));
                }
            }
            // update short search string
            String newShortSearch = "";
            for (String searchtag : library.itemSearchFields) {
                newShortSearch += " " + getS(searchtag);
            }
            put("search", Parser.normalizeForSearch(newShortSearch));
        }
    }
    
    /**
     * Produces a short representation of the name of a person, either from linked person or from a Celsius array of person names
     * 
     * @param field
     * @return 
     */
    public String getShortNames(String field) {
        StringBuilder people = new StringBuilder();
        if (linkedPersons.get(field)!=null) {
            for (Person person : linkedPersons.get(field)) {
                people.append((", "));
                people.append(person.get("last_name"));
            }
        } else {
            for(String person : getS(field).split("\\|")) {
                people.append(", ");
                people.append(Parser.cutUntil(person, ","));
            }
        }
        return(people.substring(2));
    }
    
    /**
     * Turn an author string into a short string
     */
    public String getBibTeXNames(String field) {
        StringBuilder people = new StringBuilder();
        for (Person person : linkedPersons.get(field)) {
            people.append(" and ");
            people.append(person.get("first_name"));
            people.append(' ');
            people.append(person.get("last_name"));
        }
        return(people.substring(5));
    }

    /**
     * Reloads the full information from the database
     */
    @Override
    public void reloadfullInformation() {
        currentLoadLevel=0;
        linkedAttachments.clear();
        linkedPersons.clear();
        loadLevel(3);
    }

    /**
     * Deletes the files of all attachments
     */
    public void deleteFilesOfAttachments() {
        for (Attachment attachment : linkedAttachments) {
            FileTools.deleteIfExists(attachment.getFullPath());
        }
    }

    /** 
     * Deletes all attachments and removes all references from database
     */
    public void stripOfAttachments() {
        deleteFilesOfAttachments();
        try {
            ResultSet old = library.executeResEX("SELECT attachment_id FROM item_attachment_links WHERE item_id=" + id + ";");
            String idString = "";
            while (old.next()) {
                idString += "," + old.getString(1);
            }
            library.executeEX("DELETE FROM attachments WHERE id IN (" + idString.substring(1) + ");");
            library.executeEX("DELETE FROM item_attachment_links where item_id=" + id + ";");
            linkedAttachments.clear();
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    /**
     * Create a list of names for field key with type 
     * 
     * 3 : full list of names
     * 31 : full list of names with clickable links
     * 
     * @param key
     * @param type
     */
    public String getNames(String key, int type) {
        StringBuilder out=new StringBuilder();
        ArrayList<Person> persons=linkedPersons.get(key);
        int count=1;    
        for (Person person : persons) {
            if ((count>1) && (persons.size()>2)) out.append(", ");
            if ((count>1) && (count==persons.size())) out.append(" and ");
            out.append(person.getName(type));
            count++;
        }
        return(out.toString());
    }

    /**
     * Return an HTML representation of the list of attachments.
     * 
     * @param type : format type
     * @return the HTML string
     */
    public String getAttachments(int type) {
        StringBuilder out=new StringBuilder();
        for (Attachment attachment : linkedAttachments) {
            out.append("<p>");
            out.append(attachment.getHTML(type));
            out.append("</p>");
        }
        return(out.toString());
    }
    
    public ArrayList<Item> getLinksOfType(String type) {
        if ((type!=null) && (type.trim().equals(""))) type=null;
        ArrayList<Item> items = new ArrayList<>();
        for (String key : library.Links.keySet()) {
            if ((type.equals("Available Links")) || (type.equals(key))) {
                //System.out.println("indeed!");
                ArrayList<String> ids=library.Links.get(key);
                for (String id : ids) {
                    if (!id.equals("?")) items.add(new Item(library,id));
                }
            }
        }
        Collections.sort(items, new CompareItems());
        return(items);
    }

    public boolean hasAttribute(String attribute) {
        if (isEmpty("attributes")) return(false);
        return (Parser.listContains(get("attributes"), attribute));
    }

    public void setAttribute(String attribute) {
        if (!hasAttribute(attribute)) {
            String attributes=getS("attributes");
            attributes+="|"+attribute;
            if (attributes.startsWith("|")) attributes=attributes.substring(1);
            put("attributes",attributes);
        }
    }

    /**
     * Create a string representation of item for output
     * 
     * @return the string
     */
    @Override
    public String toText(boolean renew) {
        if (!renew && (toText!=null)) return(toText);
        toText=library.itemRepresentation.fillIn(this,true);
        if (toText.equals("")) toText=this.toString();
        return(toText);
    }
    
    @Override
    public String toString() {
        return(toText(false));
    }

    /**
     * Create a string representation that can be used for sorting the items
     * 
     * @return the string
     */
    @Override
    public String toSort() {
        if (toSort!=null) return(toSort);
        toSort=library.itemSortRepresentation.fillIn(this,true);
        return(toSort);
    }

    public void addLink(String tag, String value) {
        String s=get("links");
        if (s==null) s="";
        if (!s.equals("")) s+="|";
        s+=tag+":"+value;
        put("links",s);
    }
    
    /**
     * Deletes the file and removes all links from the library, deleting unused keywords
     * 
     * @param deleteFilesOfAttachments : delete also the files of the attachments?
     */
    @Override
    public void destroy(boolean deleteFilesOfAttachments) {
        if (deleteFilesOfAttachments) deleteFilesOfAttachments();
        FileTools.deleteIfExists(getThumbnailPath());
        library.executeEX("DELETE FROM items where id="+id+";");
        // delete all links and remove Keywords/authors if no longer used, integrate into saving mechanism
        try {
            ResultSet old=library.executeResEX("SELECT keyword_id FROM item_keyword_links WHERE item_id="+id+";");
            ArrayList<String> oldIDs=new ArrayList<>();
            while (old.next()) oldIDs.add(old.getString(1));
            library.deleteUnusedLinkedObjects("keyword",oldIDs);
            library.executeEX("DELETE FROM item_keyword_links where item_id="+id+";");

            library.executeEX("DELETE FROM item_person_links where item_id="+id+";");

            old=library.executeResEX("SELECT attachment_id FROM item_attachment_links WHERE item_id="+id+";");
            String idString="";
            while (old.next()) idString+=","+old.getString(1);
            if (idString.length()>0) {
                library.executeEX("DELETE FROM attachments WHERE id IN (" + idString.substring(1) + ");");
                library.executeEX("DELETE FROM item_attachment_links where item_id=" + id + ";");
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public boolean containsKey(String key) {
        return(properties.keySet().contains(key));
    }
    
    @Override
    public String getRawData() {
        StringBuilder out=new StringBuilder(super.getRawData());
        out.append("\n");
        out.append("Attachments:\n");
        out.append("------------------\n");
        for (Attachment attachment : linkedAttachments) {
            out.append(attachment.id+" | "+attachment.get("name")+" | "+attachment.get("filetype")+" | "+attachment.get("path")+" | "+attachment.get("pages")+" | "+attachment.get("source")+" | "+attachment.get("createdTS")+" | "+attachment.get("md5")+"\n");
        }
        return(out.toString());
    }
    
    /**
     * Absorb all the data found in source, including attachments
     * 
     * @param source 
     */
    public void replaceData(Item source) {
        loadLevel(3);
        // copy over plain data
        for (String key : source.properties.keySet()) {
            if (!source.isEmpty(key) && !key.startsWith("$")) put(key,source.get(key));
        }
        save();
    }
    
    public void replaceAttachment(Item source) {
        try {
            // copy over first attachment if attachments exist
            if (source.linkedAttachments.size()>0) {
                Attachment newAttachment = source.linkedAttachments.get(0);
                if (linkedAttachments.size() > 0) {
                    Attachment oldAttachment = linkedAttachments.get(0);
                    FileTools.deleteIfExists(oldAttachment.getFullPath());
                    oldAttachment.put("path", newAttachment.getFullPath());
                    oldAttachment.put("filetype", newAttachment.get("filetype"));
                    oldAttachment.put("pages", newAttachment.get("pages"));
                    oldAttachment.put("createdTS", newAttachment.get("createdTS"));
                    oldAttachment.put("md5", newAttachment.get("md5"));
                    oldAttachment.moveToStandardLocation(true);
                    oldAttachment.save();
                } else {
                    newAttachment.parent=this;
                    newAttachment.moveToStandardLocation(true);
                    newAttachment.save();
                    newAttachment.attachToParent();
                    newAttachment.saveAttachmentLinkToDatabase();
                }
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    /**
     * Take the first attachment from source and add it as the first attachment to this item
     * @param source 
     */
    public void insertAsFirstAttachment(Item source) {
        try {
            // copy over first attachment if attachments exist
            if (source.linkedAttachments.size()>0) {
                Attachment newAttachment = source.linkedAttachments.get(0);
                newAttachment.parent=this;
                newAttachment.moveToStandardLocation(true);
                newAttachment.save();
                newAttachment.order=0;
                linkedAttachments.add(0, newAttachment);
                // shift all attachment orders by one.
                library.executeEX("UPDATE item_attachment_links SET ord = ord + 1 WHERE item_id = "+this.id+";");
                newAttachment.saveAttachmentLinkToDatabase();
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public ArrayList<String> getEditableFields() {
        ArrayList<String> fields=new ArrayList<>();
        for (String key : library.itemPropertyKeys) {
            fields.add(key);
        }
        for (String key : library.configToArray("standard-item-fields")) {
            if (!fields.contains(key)) fields.add(key);
        }
        for (String field : getFields()) {
            if (!fields.contains(field) && !field.startsWith("$")) fields.add(field);
        }
        for (String person : library.peopleFields) {
            fields.remove("short_"+person);
            if (!fields.contains(person)) fields.add(person);
        }
        fields.remove("search");
        fields.remove("last_modifiedTS");
        fields.remove("createdTS");
        fields.remove("remarks");
        fields.remove("id");
        return(fields);
    }

    public String getLinkedText(boolean renew) {
        return ("<a href='http://$$item." + id + "'>" + toText(renew).trim() + "</a>");
    }
    
    /*public DefaultListModel getAttachmentListModel() {
        DefaultListModel DLM=new DefaultListModel();
        for (Attachment attachment : linkedAttachments) {
            DLM.addElement(attachment.get("name")+", created "+library.RSC.timestampToString(attachment.get("createdTS"))+", located at "+attachment.get("path"));
        }
        return(DLM);
    }*/
    
    public Library getLibrary() {
        return(library);
    }
    
    class CompareItems implements Comparator<Item> {

        public CompareItems() {
        }

        @Override
        public int compare(final Item A, final Item B) {
            return (A.toSort().compareTo(B.toSort()));
        }

        public boolean equals() {
            return (false);
        }
    }

}
