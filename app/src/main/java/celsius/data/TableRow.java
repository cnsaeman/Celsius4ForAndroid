/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

import celsius.components.library.Library;
import atlantis.tools.Parser;
import celsius.tools.ToolBox;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author cnsaeman
 */
public class TableRow {
    
    public Library library;
    public String table; 
    public String id;
    public String lastError;
    public HashMap<String,String> properties;
    public int currentLoadLevel;                    // -1: does not exist, 0 : nothing loaded, 1: shortSQLTags, 2: table, 3: all associations  
    public ArrayList<String> dirtyFields; // list of keys that need saving
    public ArrayList<String> orderedStandardKeys; 
    public final HashSet<String> propertyKeys;
    public HashSet<String> tableHeaders;
    
    public HashMap<Integer,ArrayList<Item>> linkedItems;
    
    public TableRow(Library lib, String tab, String i, HashSet<String> pF) {
        library=lib;
        table=tab;
        id=i;
        lastError=null;
        properties = new HashMap<>();
        dirtyFields = new ArrayList<>();
        if(i!=null) {
            this.loadLevel(2);
        }
        propertyKeys=pF;
    }

    /**
     * Load item at loadLevel 1 with data in ResultSet
     * 
     * @param lib
     * @param tab
     * @param rs
     * @param pF 
     */
    public TableRow(Library lib, String tab, ResultSet rs, HashSet<String> pF) {
        library=lib;
        table=tab;
        lastError=null;
        try {
            currentLoadLevel=1;
            readIn(rs);
        } catch (Exception e) {
            e.printStackTrace();
            lastError="E2:"+e.toString();
        }
        propertyKeys=pF;
    }

    public TableRow(Library lib, String tab, HashSet<String> pF) {
        library=lib;
        table=tab;
        lastError=null;
        properties=new HashMap<>();
        currentLoadLevel=0;
        dirtyFields=new ArrayList<>();
        propertyKeys=pF;
    }
        
    public void readIn(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        properties=new HashMap<>();
        dirtyFields=new ArrayList<>();
        int pos=rs.getMetaData().getColumnCount();
        for (int i=0;i<pos;i++) {
            String cn=rs.getMetaData().getColumnName(i+1);
            if (cn.equals("id")) id=rs.getString(i+1);
            if (cn.equals("attributes")) {
                HashMap<String,String> attributes=setAttributes(rs.getBytes(i+1));
                for (String key : attributes.keySet()) {
                    properties.put(key,attributes.get(key));
                }
            } else {
                properties.put(cn,rs.getString(i+1));
            }
        }
        dirtyFields.clear();
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
        if (currentLoadLevel>=targetLoadLevel) return;
        try {
            if (((currentLoadLevel<2) && (targetLoadLevel >= 2)) || ((currentLoadLevel<1) && (targetLoadLevel >= 1))) {
                ResultSet rs = library.executeResEX("SELECT * FROM " + table + " where id = " + id + " LIMIT 1;");
                if (rs.next()) {
                    readIn(rs);
                    currentLoadLevel=2;
                } else {
                    currentLoadLevel=-1;
                }
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public String get(String key) {
        if (properties.containsKey(key)) {
            return(properties.get(key));
        }
        return(null);
    }
    
    public String getS(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        return(tmp);
    }
    
    public void put(String key, String value) {
        if (value==null) {
            if (properties.keySet().contains(key)) {
                properties.remove(key);
                if (!dirtyFields.contains(key)) dirtyFields.add(key);
            }
        } else {
            if (!value.equals(properties.get(key))) {
                properties.put(key, value);
                if (!dirtyFields.contains(key)) {
                    dirtyFields.add(key);
                }
            }
        }
    }

    public boolean isNotSet(String s) {
        return(!properties.containsKey(s));
    }

    public boolean isEmpty(String s) {
        if (isNotSet(s)) return(true);
        return(getS(s).equals(""));
    }
    
    public ArrayList<String> getFields() {
        ArrayList<String> out=new ArrayList<>();
        out.addAll(properties.keySet());
        return(out);
    }
    
    public void flatCopy(TableRow tr) {
        for (String key : tr.getFields()) {
            if (!key.equals("id")) this.put(key,tr.get(key));
        }
    }
    
    public boolean needsSaving() {
        if (dirtyFields.isEmpty()) return(false);
        for (String key : dirtyFields) {
            if (!key.startsWith("$")) return(true);
        }
        return(false);
    }
    
    /**
     * Write an item to the library
     * 
     * @throws Exception 
     */
    public void save() throws Exception {
        library.RSC.out("Saving tablerow: "+toString());
        if (this.needsSaving()) {
            if ((library.dbConnection!=null) && (table!=null) && (!table.isEmpty())) {
                
                boolean saveAttributes = false;
                Long l = ToolBox.now();
                // set last modified, if field available
                if (tableHeaders.contains("last_modifiedTS")) put("last_modifiedTS", Long.toString(l));
                
                // bundle up transactions
                //library.dbConnection.setAutoCommit(false);

                // go through all fields and identify which fields need to written when, etc.
                // Identify all associated persons, find or create, and save in a HashMap
                // also check if attributes need to be saved
                ArrayList<String> itemDirtyFields=new ArrayList<>(); // fields that need to be written for item
                for (String field : dirtyFields) {
                    if (!field.startsWith("$")) {
                        if (!library.isPersonField(field) && !library.isLinkedField(field)) {
                            if (!tableHeaders.contains(field)) {
                                saveAttributes = true;
                            } else {
                                itemDirtyFields.add(field);
                            }
                        }
                    }
                }
                if (id==null) {
                    // adjust time stamp
                    if (tableHeaders.contains("createdTS")) put("createdTS", Long.toString(l));
                    itemDirtyFields.add("createdTS");
                    String fieldsList="";
                    String qmarks="";
                    for (String field : itemDirtyFields) {
                        fieldsList += ",`" + field + "`";
                        qmarks += ",?";
                    }
                    if (saveAttributes) {
                        fieldsList += ",attributes";
                        qmarks += ",?";
                    }
                    String sql="INSERT INTO "+table+" ("+fieldsList.substring(1)+") VALUES ("+qmarks.substring(1)+");";
                    System.out.println("Writing new Using SQL: "+sql);
                    PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                    int i=1;
                    for (String field : itemDirtyFields) {
                        pstmt.setString(i, properties.get(field));
                        i++;
                    }
                    if (saveAttributes) {
                        pstmt.setBytes(i, getAttributesBytes());
                        i++;
                    }
                    pstmt.execute();
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    id=String.valueOf(generatedKeys.getLong(1));
                    System.out.println("Obtained id: "+id);
                } else {
                    String fieldsList="";
                    for (String field : itemDirtyFields) {
                        fieldsList += ", `" + field + "` = ?";
                    }
                    if (saveAttributes) {
                        fieldsList += ", `attributes` = ?";
                    }
                    String sql="UPDATE "+table+" SET "+fieldsList.substring(1)+" WHERE id="+id;
                    library.RSC.out("DB::"+sql);
                    PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                    int i=1;
                    for (String field : itemDirtyFields) {
                        pstmt.setString(i, properties.get(field));
                        i++;
                    }
                    if (saveAttributes) {
                        pstmt.setBytes(i, getAttributesBytes());
                        i++;
                    }
                    pstmt.execute();
                }
                
                // All done, reset dirty fields
                dirtyFields.clear();
            } else {
                throw (new Exception("No db Connection to save item to!"));
            }
        } else {
            System.out.println("No saving required");
        }
    }
    
    private HashMap<String, String> setAttributes(byte[] attBytes) throws IOException, ClassNotFoundException {
        if ((attBytes!=null) && (attBytes.length>2)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(attBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.close();
            bais.close();
            return((HashMap) ois.readObject());
        } else {
            return(new HashMap<>());
        }
    }

    public byte[] getAttributesBytes() throws IOException {
        HashMap<String,String> attributes=new HashMap<>();
        for (String key : properties.keySet()) {
            if (!tableHeaders.contains(key) && !key.startsWith("$$")  && !library.isLinkedField(key) && !library.isPeopleField(key) && (properties.get(key)!=null)) {
                attributes.put(key,properties.get(key));
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(attributes);
        return(baos.toByteArray());
    }
    
    public String getRawData() {
        StringBuilder tmp=new StringBuilder();
        tmp.append("Standard keys:\n");
        tmp.append("------------------\n");
        for (String key : orderedStandardKeys) {
            String value=getS(key);
            tmp.append(key.replaceAll("\\p{C}", "?"));
            tmp.append(": ");
            tmp.append(value.replaceAll("\\p{C}", "?"));
            tmp.append(ToolBox.linesep);
        }
        tmp.append("\n");
        tmp.append("Other properties:\n");
        tmp.append("------------------\n");
        String[] keys=properties.keySet().toArray(new String[properties.size()]);
        for (String key : keys) { 
            if (!orderedStandardKeys.contains(key) && !key.startsWith("$")) {
                String value = getS(key);
                tmp.append(key.replaceAll("\\p{C}", "?"));
                tmp.append(": ");
                tmp.append(value.replaceAll("\\p{C}", "?"));
                tmp.append(ToolBox.linesep);
            }
        }
        return(tmp.toString());
    }
    
    public String getExtended(String tag) {
        return(getS(tag));
    }
    
    public String getIconField(String s) {
        return(null);
    }
    
    public void reloadfullInformation() {
        // do nothing as a default
    }
    
    public String getCompletedDir(String k) {
        return(library.completeDir(k));
    }

    public String getCompletedDirKey(String k) {
        return(library.completeDir(getS(k)));
    }
    
    public String toText(boolean renew) {
        return("TableRowID:"+id);
    }
    
    public void destroy(boolean hard) {
        // do nothing
    }
    
    public String toSort() {
        return("");
    }
    
    // empty method to be overwritten
    public void notifyChanged() {
        
    }
    
    public String getLinkListString() {
        if (linkedItems.isEmpty()) return("");
        StringBuffer out=new StringBuffer();
        for (Integer i : linkedItems.keySet()) {
            out.append("<p><b>");
            out.append(library.linkTypes.get(i));
            out.append("</b></p>");
            ArrayList<String> entries=new ArrayList<>();
            for (Item item : linkedItems.get(i)) {
                entries.add(item.getLinkedText(false));
            }
            Collections.sort(entries);
            out.append("<ul>");
            for(String entry : entries) {
                out.append("<li>");
                out.append(entry);
                out.append("</li>");
            }
            out.append("</ul>");
        }
        return(out.toString());
    }
    
    public boolean hasThumbnail() {
        return(false);
    }
    
    public String getThumbnailPath() {
        return(null);
    }
    
}
