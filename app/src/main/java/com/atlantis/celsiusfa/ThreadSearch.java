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

import celsius.components.categories.Category;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.*;
import celsius.data.Person;
import celsius.data.TableRow;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class ThreadSearch extends Thread {

    private final int batchSize=100;

    private final MainActivity MA;
    private final Resources RSC;
    private final TableRowListAdapter ILA;
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
    private String sqlColumn;


    /**
     *  Constructor, read in information
     *  Mother, documenttablemodel, search string, class
     */
    public ThreadSearch(MainActivity ma, Library lib, ListView il, TableRowListAdapter ila, String tmp, int m) {
        super();
        //System.out.println(String.valueOf(System.currentTimeMillis())+"Initialized: "+this.toString());
        MA=ma;
        RSC=MA.RSC;
        library=lib;
        IL=il;
        ILA=ila;
        ILA.mode=m;
        maxpos= library.getSize();
        mode=m;
        search=tmp.toLowerCase().split(" ");
        if (mode==0) {
            sqlColumn = "search";
        } else if (mode==1) {
            sqlColumn="search";
        } else if (mode==2) {
            sqlColumn="label";
        }
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
            StringBuilder sql= new StringBuilder();
            if (mode==0) {
                sql = new StringBuilder("SELECT items.*,filetype,pages from items INNER JOIN item_attachment_links ON items.id=item_id INNER JOIN attachments ON attachments.id=attachment_id WHERE (");
                sql.append(sqlColumn);
                sql.append(" LIKE ?)");
                RSC.out(10, sql.toString());
                for (int i = 1; i < search.length; i++) {
                    sql.append(" AND (").append(sqlColumn).append(" LIKE ?)");
                }
                sql.append("AND items.id>? AND item_attachment_links.ord=0 COLLATE NOCASE ORDER BY id LIMIT ");
                sql.append(String.valueOf(batchSize));
                sql.append(";");
            } else if (mode==1)  {
                sql = new StringBuilder("SELECT * from persons WHERE (");
                sql.append(sqlColumn);
                sql.append(" LIKE ?)");
                RSC.out(10, sql.toString());
                for (int i = 1; i < search.length; i++) {
                    sql.append(" AND (").append(sqlColumn).append(" LIKE ?)");
                }
                sql.append("AND persons.id>? ORDER BY last_name LIMIT ");
                sql.append(String.valueOf(batchSize));
                sql.append(";");
            }  else if (mode==2)  {
                sql = new StringBuilder("SELECT * from item_categories WHERE (");
                sql.append(sqlColumn);
                sql.append(" LIKE ?)");
                RSC.out(10, sql.toString());
                for (int i = 1; i < search.length; i++) {
                    sql.append(" AND (").append(sqlColumn).append(" LIKE ?)");
                }
                sql.append("AND id>? ORDER BY label LIMIT ");
                sql.append(String.valueOf(batchSize));
                sql.append(";");
            }
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
                    if (mode==0) {
                        tableRow = new Item(library, rs);
                    } else if (mode==1) {
                        tableRow = new Person(library, rs);
                    } else  {
                        tableRow = new Category(library, rs);
                    }
                    RSC.out("Result: "+tableRow.id);
                    //System.out.println(">> Publishing id: "+tableRow.id+" to table "+postID);
                    IL.post(new Runnable() {
                        public void run() {
                            //DB System.err.println("....Found search result: "+Integer.toString(di));
                            ILA.add(tableRow);
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