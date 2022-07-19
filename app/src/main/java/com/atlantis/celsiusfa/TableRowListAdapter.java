package com.atlantis.celsiusfa;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import celsius.Resources;
import celsius.components.categories.Category;
import celsius.components.library.Library;
import celsius.data.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author cnsaeman
 */
public final class TableRowListAdapter extends ArrayAdapter<TableRow> {

    private Resources RSC;
    public String title;
    public celsius.components.library.Library Lib;
    public MainActivity MA;
    public String lastHTMLview;
    public String creationHTMLview;
    public int creationType;
    private ArrayList<Integer> sizes;
    public int type; // 0: empty, 1: category, 2: author, 3: search
    // 4: citedin, 5: bibtexproblems, 6: search identifier
    public String header;
    private boolean resizable;
    public boolean tableview;
    public int selectedfirst;
    public int selectedlast;
    public int mode;
    public int sorted;
    public ArrayList<String> Columns;
    public ArrayList<String> Headers;
    public ArrayList<String> IDs;
    public ArrayList<TableRow> tableRows;
    public Library Library;
    public int CurrentPages;
    public int CurrentDuration;

    public TableRowListAdapter(MainActivity ma, Library lib) {
        super(ma, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<>());
        sorted = -1;
        MA = ma;
        RSC = ma.RSC;
        resizable = false;
        lastHTMLview = "";
        creationHTMLview = "";
        tableview = true;
        Lib = lib;
        Columns = new ArrayList<>();
        Headers = new ArrayList<>();
        IDs = new ArrayList<>();
        tableRows = new ArrayList<>();
        CurrentPages = 0;
        CurrentDuration = 0;
    }

    public synchronized void removeRow(int i) {
        IDs.remove(i);
        tableRows.remove(i);
    }

    //@Override
    public synchronized void clear(int mode) {
        this.mode=mode;
        IDs.clear();
        tableRows.clear();
        CurrentPages = 0;
        CurrentDuration = 0;
    }

    public void add(TableRow tableRow) {
        tableRows.add(tableRow);
        IDs.add(tableRow.id);
    }

    @Override
    public int getCount() {
        return(tableRows.size());
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // http://devtut.wordpress.com/2011/06/09/custom-arrayadapter-for-a-listview-android/
        View v = convertView;
        if (mode==0) {
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.list_item, null);
            }
            Item item = (Item) tableRows.get(position);
            if (item != null) {
                ImageView iv = (ImageView) v.findViewById(R.id.type_icon);
                String type=item.getS("type");
                if (type.equals("Book")) iv.setImageResource(R.drawable.book);
                if (type.equals("Book Chapter")) iv.setImageResource(R.drawable.book_chapter);
                if (type.equals("Cartoon")) iv.setImageResource(R.drawable.cartoon);
                if (type.equals("eBook")) iv.setImageResource(R.drawable.ebook);
                if (type.equals("Icon")) iv.setImageResource(R.drawable.icon);
                if (type.equals("Lecture Notes")) iv.setImageResource(R.drawable.lecture_notes);
                if (type.equals("Manual")) iv.setImageResource(R.drawable.manual);
                if (type.equals("Newspaper Article")) iv.setImageResource(R.drawable.newspaper_article);
                if (type.equals("")) iv.setImageResource(R.drawable.not_set);
                if (type.equals("Other")) iv.setImageResource(R.drawable.other);
                if (type.equals("Paper")) iv.setImageResource(R.drawable.paper);
                if (type.equals("Preprint")) iv.setImageResource(R.drawable.preprint);
                if (type.equals("Review")) iv.setImageResource(R.drawable.review);
                if (type.equals("Sheet Music")) iv.setImageResource(R.drawable.sheet_music);
                if (type.equals("Talk")) iv.setImageResource(R.drawable.talk);
                if (type.equals("Thesis")) iv.setImageResource(R.drawable.thesis);
                //iv.setImageResource(type);
                TextView tt = (TextView) v.findViewById(R.id.item_authors);
                tt.setText(item.getS("short_"+item.library.peopleFields[0]));
                tt = (TextView) v.findViewById(R.id.item_title);
                tt.setText(item.getS("title"));
                tt = (TextView) v.findViewById(R.id.item_addinfo);
                StringBuffer addinfo=new StringBuffer();
                if (item.getS("filetype").length()>0) {
                    addinfo.append(item.get("filetype")+"-file, " +item.getS("pages")+" pages, ");
                }
                addinfo.append(item.getS("identifier"));
                tt.setText(addinfo.toString());
            }
        } else if (mode==1) {
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.list_person, null);
            }
            Person person = (Person) tableRows.get(position);
            if (person != null) {
                TextView tt = (TextView) v.findViewById(R.id.name);
                tt.setText(person.getS("last_name")+", "+person.getS("first_name"));
            }
        }  else if (mode==2) {
            if (v == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = inflater.inflate(R.layout.list_category, null);
            }
            Category category = (Category) tableRows.get(position);
            if (category != null) {
                TextView tt = (TextView) v.findViewById(R.id.label);
                tt.setText(category.getS("label"));
            }
        }

        return v;
    }
}
