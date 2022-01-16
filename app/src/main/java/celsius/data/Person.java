/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

import atlantis.tools.Parser;
import java.io.File;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author cnsaeman
 */
public class Person extends TableRow {

    public String collaborators;
    public String collaboratorsID;

    public Person(Library lib,String id) {
        super(lib,"persons",id,lib.personPropertyKeys);
        orderedStandardKeys=library.orderedPersonPropertyKeys;
        tableHeaders=library.personPropertyKeys;
        linkedItems=new HashMap<>();
    }
    
    public Person(Library lib, ResultSet rs) {
        super(lib,"persons",rs,lib.itemPropertyKeys);
        library=lib;
        orderedStandardKeys=library.orderedPersonPropertyKeys;
        tableHeaders=library.personPropertyKeys;
        linkedItems=new HashMap<>();
    }

    public void loadCollaborators() {
        try {        
            collaborators = "";
            collaboratorsID = "";
            ResultSet rs=library.dbConnection.prepareStatement("SELECT id, first_name, last_name FROM persons WHERE id IN (SELECT DISTINCT p2.person_id FROM item_person_links p1 INNER JOIN item_person_links p2 ON p2.item_id=p1.item_id AND (p1.person_id<>p2.person_id) WHERE p1.person_id IN ("+id+"));").executeQuery();
            while (rs.next()) {
                collaborators+="|"+rs.getString(3)+", "+rs.getString(2);
                collaboratorsID+="|"+rs.getString(1);
            }
            if (collaborators.length()>1) {
                collaborators=collaborators.substring(1);
                collaboratorsID=collaboratorsID.substring(1);
            } else {
                collaborators="None.";
                collaboratorsID="0";
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public void loadLinkedData() {
        // read in links
        String sql = "SELECT * FROM person_item_links JOIN items on person_item_links.item_id=items.id WHERE person_id="+id+";";
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
    }
    
    public String getShortName(String field) {
        String person=getS(field);
        if (!person.contains(",")) return(person.replaceAll("\\|",", "));
       return(Parser.cutUntilLast(person.replaceAll(", .*?\\|", ", "),",").trim());
    }

    public String getName(int type) {
        if (type==31) {
            return("<a href='http://$$person."+id+"'>"+get("first_name")+" "+get("last_name").trim()+"</a>");
        }
        return(get("first_name")+" "+get("last_name").trim());
    }
    
    public String toText(boolean renew) {
        return(getName(0));
    }
    
    /**
     * This function removes accents etc from names for better search compatibility
     * 
     * @param firstName
     * @param lastName
     * @return 
     */
    public static String toSearch(String firstName, String lastName) {
        return(Normalizer.normalize((firstName+" "+lastName).toLowerCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
    }

    public Library getLibrary() {
        return(library);
    }

    public ArrayList<String> getEditableFields() {
        ArrayList<String> fields=new ArrayList<>();
        for (String field : getFields()) {
            if (!fields.contains(field) && !field.startsWith("$")) fields.add(field);
        }
        fields.remove("last_modifiedTS");
        fields.remove("createdTS");
        fields.remove("remarks");
        fields.remove("id");
        fields.remove("search");
        return(fields);
    }

    public boolean containsKey(String key) {
        return(properties.keySet().contains(key));
    }

    public void updateShorts() {
        // update short search string
        String newShortSearch = "";
        for (String searchtag : library.personSearchFields) {
            newShortSearch += " " + getS(searchtag);
        }
        put("search", Parser.normalizeForSearch(newShortSearch));
    }

    @Override
    public String getExtended(String tag) {
        int i = tag.indexOf("&");
        if (i > -1) {
            char tp = tag.charAt(i + 1);
            tag = tag.substring(0, i);
            switch (tp) {
                default:
                    return (getS(tag));
            }
        }
        return (getS(tag));
    }
    
    @Override
    public boolean hasThumbnail() {
        return((new File(library.completeDir("LD::person-thumbnails/"+id+".jpg"))).exists());
    }
    
    @Override
    public String getThumbnailPath() {
        return(library.completeDir("LD::person-thumbnails/"+id+".jpg"));
    }
    

}
