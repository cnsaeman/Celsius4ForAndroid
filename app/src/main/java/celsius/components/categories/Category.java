/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.categories;

import celsius.components.library.Library;
import celsius.data.TableRow;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author cnsaeman
 */
public class Category extends TableRow {
    
    public final static HashSet<String> categoryPropertyKeys=new HashSet<String>((List<String>)Arrays.asList("label","remarks"));
    
    public Category(Library library, String id, String label) {
        super(library,"item_categories",categoryPropertyKeys);
        this.id=id;
        this.put("label",label);
        tableHeaders=categoryPropertyKeys;
    }

    public Category(Library library, String id) {
        super(library,"item_categories",id,categoryPropertyKeys);
        tableHeaders=categoryPropertyKeys;
    }

    
    /**
     * Create category from resultset
     * 
     * @param rs
     * @throws SQLException 
     */
    public Category(Library library,ResultSet rs) throws SQLException {
        super(library,"item_categories",rs,categoryPropertyKeys);
        tableHeaders=categoryPropertyKeys;
    }

    public void setRemarks(String remarks) {
        // check if update necessary?
        if (remarks.equals(get("remarks"))) return;
        // in memory rename
        put("remarks",remarks);
        // save in database
        try {
            save();
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
        
}
