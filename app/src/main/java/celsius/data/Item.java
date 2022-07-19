/*

 Celsius Item Class - Atlantis Software 

*/

package celsius.data;

//import atlantis.gui.KeyValueTableModel;
import celsius.components.library.Library;
import celsius.components.categories.Category;
import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import celsius.tools.ToolBox;
import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
//import javax.swing.DefaultListModel;

/**
 *
 * @author cnsaeman
 */
public final class Item extends TableRow { //AND implements Editable {

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

    public Item(Library library,String id) {
        super(library,"items",id,library.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        orderedStandardKeys=library.orderedItemPropertyKeys;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }

    public Item(Library library) {
        super(library,"items",library.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        this.library=library;
        orderedStandardKeys=library.orderedItemPropertyKeys;
        tableHeaders=library.itemPropertyKeys;
        error=0;
        // mark as newly created, ensure that all "shorts" are updated
        currentLoadLevel=1000; 
    }
    
    /**
     * Load item at loadLevel 1 with data in ResultSet
     * 
     * @param library
     * @param rs 
     */
    public Item(Library library, ResultSet rs) {
        super(library,"items",rs,library.itemPropertyKeys);
        linkedPersons=new HashMap<>();
        linkedAttachments=new ArrayList<>();
        linkedItems=new HashMap<>();
        this.library=library;
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
    
    public void loadLinkedPeople() {
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
    }
            
    /**
     *  Loads all the data, including linked data from other tables
     */
    private void loadLinkedData() {
        // read in people
        loadLinkedPeople();

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
            String sql = "SELECT GROUP_CONCAT(label || '$' || item_categories.id, ' | ') AS result FROM item_category_links INNER JOIN item_categories on item_categories.id=category_id  WHERE item_id=" + id + " ORDER BY label ASC";
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
    
    /**
     * Returns true if a value is associated with the key
     * 
     * @param key
     * @return 
     */
    public boolean isPropertySet(String key) {
        if (library.isPeopleField(key)) loadLinkedPeople();
        boolean isSet = false;
        isSet = isSet
                || (get(key) != null)
                || (library.isPeopleField(key) && (linkedPersons.get(key) != null) && (linkedPersons.get(key).size() > 0));
        return(isSet);
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
        return((new File(getThumbnailPath())).exists());
    }
    
    @Override
    public String getThumbnailPath() {
        return(library.completeDir("LD::item-thumbnails/"+id+".jpg"));
    }
    
    public void saveLinkedPeople(String key, int linkType){
        int order = 0;

        // delete all person links
        library.executeEX("DELETE FROM item_person_links WHERE item_id=" + id + " AND link_type=" + String.valueOf(linkType) + ";");

        // create new person links
        String data = "";
        for (Person person : linkedPersons.get(key)) {
            data += ",(" + id + "," + person.id + "," + String.valueOf(linkType) + "," + String.valueOf(order) + ")";
            order++;
        }
        library.executeEX("INSERT INTO item_person_links (item_id, person_id, link_type, ord) VALUES " + data.substring(1) + ";");
        
    }

    /**
     * Save an item. Note that this should only be done if loadlevel is maximal
     */
    public void save() {
        
        if (dirtyFields.size()==0) {
            library.RSC.out("Nothing to save for item "+id);
            return;
        }
        
        try {
            library.RSC.out("Saving item ");

            // identify dirty linked fields and save all persons
            ArrayList<String> personDirtyFields = new ArrayList<>(); // person fields that need to be written
            ArrayList<String> linkedDirtyFields = new ArrayList<>(); // other linked fields that need to be written
            for (String field : dirtyFields) {
                if (!field.startsWith("$")) {
                    // deal with persons, so that all relevant ids exist for later linking
                    if (library.isPersonField(field)) {
                        personDirtyFields.add(field);
                        if ((get(field)!=null) && (get(field).length()>0)) {
                            // get from description and ignore all links
                            linkedPersons.put(field, new ArrayList<>());
                            String[] personList = ToolBox.stringToArray(get(field));
                            for (String personDescription : personList) {
                                Person person = library.findOrCreatePerson(personDescription);
                                person.save();
                                // Check that that person is not already linked
                                boolean add=true;
                                for (Person linkedPerson : linkedPersons.get(field)) {
                                    if (linkedPerson.id.equals(person.id)) add=false;
                                }
                                if (add) linkedPersons.get(field).add(person);
                            }
                        } else {
                            // make sure that all people exist and are saved
                            for (Person person : linkedPersons.get(field)) {
                                if (person.id==null) person.save();
                            }
                        }
                    } else if (library.isLinkedField(field) || field.startsWith("attachment")) {
                        linkedDirtyFields.add(field);
                    }
                }
            }

            // Before saving more, try moving the attachments and see if this works, if not cancel
            for (Attachment attachment : linkedAttachments) {
                if (attachment.needsSaving()) {
                    boolean newAttachment=attachment.id==null;
                    if (newAttachment && (library.addItemMode == 0)) {
                        if (attachment.moveToStandardLocation(false)>0) return;
                    }
                }
            }

            String thumbnailPath=get("$$thumbnail");
            
            updateShorts();
            super.save();
            
            // link persons
            for (String field : personDirtyFields) {
                int linkType = 0; // position in peopleFields
                for (String peopleField : library.peopleFields) {
                    if (field.equals(peopleField)) {
                        saveLinkedPeople(peopleField,linkType);
                        // do not clean up old personlinks, as there may be other data associated with person as remarks, etc.
                    }
                    linkType++;
                }
            }
            
            // deal with all other link fields
            for (String field : linkedDirtyFields) {
                // linked Fields
                if (field.equals("references") || field.equals("citations")) {
                    ArrayList<String> newIDs = library.findIDs("items", getS(field));
                    library.updateLinks(field.substring(0, field.length() - 1), newIDs, String.valueOf(id));
                }

                if (field.equals("categories")) {
                    String[] categoryList=ToolBox.stringToArray(getS(field));
                    ArrayList<String> newIDs = new ArrayList<>();
                    for (String description : categoryList) {
                        Category cat=library.findOrCreateCategory(description);
                        if (!newIDs.contains(cat.id)) newIDs.add(cat.id);
                    }
                    library.updateLinks("category", newIDs, String.valueOf(id));
                }

                // keywords
                if (field.equals("keywords")) {
                    // get currently used keyword IDs
                    ArrayList<String> oldIDs = library.getIDArrayList("SELECT GROUP_CONCAT(keyword_id, ',') AS result FROM item_keyword_links WHERE item_id=" + String.valueOf(id) + ";");

                    ArrayList<String> newIDs = new ArrayList<>();
                    if (!this.isEmpty("keywords")) {
                        String[] keywordList = ToolBox.stringToArray(getS("keywords"));
                        HashMap<String, String> idList = new HashMap<>();
                        //AND String qmarks = ",?".repeat(keywordList.length).substring(1);
                        String qmarks=",?";
                        ResultSet rs = library.executeResEX("SELECT id, label FROM keywords WHERE label IN (" + qmarks + ");", keywordList);
                        while (rs.next()) {
                            idList.put(rs.getString(2), rs.getString(1));
                            newIDs.add(rs.getString(1));
                        }

                        // create unknown keywords in list 
                        for (String keyword : keywordList) {
                            if (!idList.keySet().contains(keyword)) {
                                String keyword_id = library.executeInsertEX("INSERT INTO keywords (label) VALUES (?);", new String[] {keyword});
                                idList.put(keyword, keyword_id);
                                newIDs.add(keyword_id);
                            }
                        }
                        library.updateLinks("keyword", newIDs, String.valueOf(id));
                    }
                    // clean up unused keyword entries
                    library.deleteUnusedLinkedObjects("keyword", oldIDs);
                }
            }
            
            // save all attachments
            for (Attachment attachment : linkedAttachments) {
                if (attachment.needsSaving()) {
                    boolean newAttachment=(attachment.id==null);
                    try {
                        attachment.save();
                        // new attachments: link in database and insert search results for text
                        if (newAttachment) {
                            attachment.parent=this;
                            attachment.saveAttachmentLinkToDatabase();
                            StringBuffer search = new StringBuffer();
                            if (attachment.get("$plaintext")!=null) {
                                search=TextFile.ReadOutZipFile(library.completeDir(library.completeDir(attachment.get("$plaintext")), id));
                            }
                            search.append("\n");
                            for (String tag : library.itemSearchFields) {
                                search.append(getS(tag));
                                search.append("\n");
                            }
                            // Add full text to search library
                            PreparedStatement searchStatement = library.searchDBConnection.prepareStatement("INSERT INTO search(rowid,text) VALUES(?,?);");
                            searchStatement.setString(1, String.valueOf(id));
                            searchStatement.setString(2, search.toString());
                            searchStatement.execute();
                            if (attachment.get("$plaintext")!=null) {
                                FileTools.deleteIfExists(library.completeDir(library.completeDir(attachment.get("$plaintext")), id));
                            }
                        }
                    } catch (Exception ex) {
                        library.RSC.outEx(ex);
                    }
                }
            }
            
            // save thumbnail
            if (thumbnailPath!=null) {
                try {
                    FileTools.moveFile(thumbnailPath, getThumbnailPath());
                } catch (Exception ex) {
                    library.RSC.outEx(ex);
                }
            }
            
            library.itemChanged(id);
            
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
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
    // AND @Override
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
            people.append(person.getBibTeXForm());
        }
        if (people.length()==0) return("");
        return(people.substring(5));
    }
    
    /** 
     * Re-extract all plaintext files and add to search database
     */
    public void redoPlainText() {
        loadLevel(3);
        try {
            for (Attachment attachment : linkedAttachments) {
                //AND library.RSC.configuration.extractText("LIBAF>", attachment.get("path"), library.baseFolder + "/tmp.txt");
                StringBuffer search = TextFile.ReadOutZipFile(library.baseFolder + "/tmp.txt.gz");
                search.append("\n");
                for (String tag : library.itemSearchFields) {
                    search.append(getS(tag));
                    search.append("\n");
                }
                if ((search != null) && (search.length()>0)) {
                    // Add full text to search library
                    try {
                        PreparedStatement searchStatement = library.searchDBConnection.prepareStatement("INSERT INTO search(rowid,text) VALUES(?,?);");
                        searchStatement.setString(1, String.valueOf(id));
                        searchStatement.setString(2, search.toString());
                        searchStatement.execute();
                    } catch (Exception ex) {
                        library.RSC.outEx(ex);
                    }
                }
                attachment.put("pages",Integer.toString(ToolBox.readNumberOfPagesOf(library.RSC, "RedoTXT", attachment.get("path"), library.baseFolder+"/tmp.txt.gz")));
                attachment.save();
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }

    public void associateWithFile(String path, String name) throws IOException {
        loadLevel(3);
        String filetype=""; //AND library.RSC.configuration.getFileType(path);
        // get plain text
        //AND library.RSC.configuration.extractText("LIBAF>",path,library.baseFolder+"/tmp.txt");
        StringBuffer search = TextFile.ReadOutZipFile(library.baseFolder+"/tmp.txt.gz");
        search.append("\n");
        for (String tag : library.itemSearchFields) {
            search.append(getS(tag));
            search.append("\n");
        }
        // create attachment and try to save 
        Attachment attachment=new Attachment(library,this);
        attachment.put("name",name);
        attachment.put("path",path);
        attachment.put("filetype",filetype);
        attachment.put("pages",Integer.toString(ToolBox.readNumberOfPagesOf(library.RSC, "AssocFile", path, library.baseFolder+"/tmp.txt.gz")));
        if (attachment.moveToStandardLocation(true)==0) {
            // save attachment
            attachment.attachToParent();
            attachment.saveAttachmentLinkToDatabase();
            // add search to index
            if ((search != null) && (search.length()>0)) {
                // Add full text to search library
                try {
                    PreparedStatement searchStatement = library.searchDBConnection.prepareStatement("INSERT INTO search(rowid,text) VALUES(?,?);");
                    searchStatement.setString(1, String.valueOf(id));
                    searchStatement.setString(2, search.toString());
                    searchStatement.execute();
                } catch (Exception ex) {
                    library.RSC.outEx(ex);
                }
            }
        }
        library.itemChanged(id);
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
    
    public String getClickableCategoriesList() {
        StringBuilder out=new StringBuilder();
        for (String categoryString : getS("$categories").split("\\|")) {
            if (categoryString.length()>0) {
                String[] categoryData = categoryString.split("\\$");
                out.append(", <a href=\"http://$$category.").append(categoryData[1]).append("\">").append(categoryData[0]).append("</a>");
            }
        }
        if (out.length()==0) return(null);
        return(out.substring(2));
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
        // delete all links and remove Keywords/people if no longer used, integrate into saving mechanism
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
    
    //AND @Override
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
                    oldAttachment.put("createdTS", Long.toString(ToolBox.now()));
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
        fields.addAll(Arrays.asList(library.itemEditFields));
        for (String key : library.itemPropertyKeys) {
            if (!fields.contains(key)) fields.add(key);
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

    /* AND
    @Override
    public KeyValueTableModel getEditModel() {
        KeyValueTableModel KVTM=new KeyValueTableModel("Tag", "Value");
        for (String personType : linkedPersons.keySet()) {
            StringBuilder persons=new StringBuilder();
            for (Person person : linkedPersons.get(personType)) {
                persons.append(", ");
                persons.append(person.getName(1));
            }
            if (persons.length()>0) KVTM.addRow(personType, persons.substring(2));
        }
        ArrayList<String> tags=getEditableFields();
        for (String key : tags) {
            if (!KVTM.keys.contains(key)) {
                String t = get(key);
                if (t == null) {
                    t = "<unknown>";
                }
                KVTM.addRow(key, t);
            }
        }
        return(KVTM);
    }*/
    
    public String getLinkedText(boolean renew) {
        return ("<a href='http://$$item." + id + "'>" + toText(renew).trim() + "</a>");
    }
    
    /* AND public DefaultListModel getAttachmentListModel() {
        DefaultListModel DLM=new DefaultListModel();
        for (Attachment attachment : linkedAttachments) {
            DLM.addElement(attachment.get("name")+", created "+library.RSC.timestampToString(attachment.get("createdTS"))+", located at "+attachment.get("path"));
        }
        return(DLM);
    }*/
    
    // AND @Override
    public Library getLibrary() {
        return(library);
    }
    
    @Override
    public void notifyChanged() {
        library.itemChanged(id);        
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
