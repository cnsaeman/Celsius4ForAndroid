//
// Celsius Library System
// (w) by C. Saemann
//
// SearchThread.java
//
// This class contains the thread for searching...
//
// typesafe
//
// checked: 16.09.2007
//

package com.atlantis.celsiusfa;

import android.widget.ListView;

import celsius.data.Item;
import celsius.data.Library;
import atlantis.tools.*;
import celsius.*;
import celsius.data.Person;
import celsius.data.TableRow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ThreadSearch extends Thread {

    private final int batchSize=100;

    private final MainActivity MA;
    private final Resources RSC;
    private final ItemListAdapter ILA;
    private final ListView IL;
    private final Library library;
   
    private String[] search;        // string to search for
    private String[] searchtmp;     // string to search for tmp
    private int mode;
    private ArrayList<String> currentResults;
    
    private SimpleDateFormat SDF;

    private int maxpos;
    private int done;

    public boolean running;
    private String sqlTags;
    private String sqlTable;
    private String sqlOrderBy;
    private String sqlColumn;


    /**
     *  Constructor, read in information
     *  Mother, documenttablemodel, search string, class
     */
    public ThreadSearch(MainActivity ma, Library lib, ListView il, ItemListAdapter ila, String tmp, int m) {
        super();
        //System.out.println(String.valueOf(System.currentTimeMillis())+"Initialized: "+this.toString());
        MA=ma;
        RSC=MA.RSC;
        library =lib;
        IL=il;
        ILA=ila;
        maxpos= library.getSize();
        mode=m;
        search=tmp.toLowerCase().split(" ");
        sqlTags = library.itemTableSQLTags;
        sqlTable = "items";
        sqlOrderBy = "ORDER BY " + library.config.get("item-autosortcolumn");
        sqlColumn="search";
    }

    @Override
    public void interrupt() {
        super.interrupt();
        //reg.interrupt();
    }

    @Override
    public synchronized void run() {
        running=true;
        done=0;
        done = 0;
        int last_id=-1;
        int count=batchSize;
        int lastID=0;
        while (!isInterrupted() && count==batchSize) {
            StringBuilder sql=new StringBuilder("SELECT items.*,filetype,pages from items INNER JOIN item_attachment_links ON items.id=item_id INNER JOIN attachments ON attachments.id=attachment_id WHERE (");
            sql.append(sqlColumn);
            sql.append(" LIKE ?)");
            RSC.out(10,sql.toString());
            for (int i=1;i<search.length;i++) {
                sql.append(" AND (").append(sqlColumn).append(" LIKE ?)");
            }
            sql.append("AND items.id>? AND item_attachment_links.ord=0 COLLATE NOCASE ORDER BY id LIMIT ");
            sql.append(String.valueOf(batchSize));
            sql.append(";");
            count=0;
            try {
                PreparedStatement statement= library.dbConnection.prepareStatement(sql.toString());
                statement.setString(1,"%"+search[0]+"%");
                for (int i=1;i<search.length;i++) {
                    statement.setString(i+1,"%"+search[i]+"%");
                }
                statement.setInt(search.length+1, lastID);
                ResultSet rs = statement.executeQuery();
                while (rs.next() && !isInterrupted()) {
                    TableRow tableRow;
                    if (mode==2) {
                        tableRow = new Person(library, rs);
                    } else {
                        tableRow = new Item(library, rs);
                    }
                    //System.out.println(">> Publishing id: "+tableRow.id+" to table "+postID);
                    IL.post(new Runnable() {
                        public void run() {
                            //DB System.err.println("....Found search result: "+Integer.toString(di));
                            ILA.add((Item) tableRow);
                            IL.setAdapter(ILA);
                        }
                    });
                    lastID = rs.getInt(1);
                    count++;
                }
            } catch (SQLException ex) {
                RSC.outEx(ex);
            }
            done += batchSize;
        }

        running=false;
    }
    
    
}