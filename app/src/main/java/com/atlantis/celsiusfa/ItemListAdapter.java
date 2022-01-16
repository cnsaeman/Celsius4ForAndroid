package com.atlantis.celsiusfa;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import atlantis.tools.*;
import celsius.Resources;
import celsius.data.*;

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author cnsaeman
 */
public final class ItemListAdapter extends ArrayAdapter<Item> {

    private Resources RSC;
    public String title;
    public Library Lib;
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
    public int sorted;
    public ArrayList<String> Columns;
    public ArrayList<String> Headers;
    public ArrayList<String> IDs;
    public ArrayList<Item> Items;
    public Library Library;
    public int CurrentPages;
    public int CurrentDuration;
    public int CurrentItems;

    public ItemListAdapter(MainActivity ma, Library lib) {
        super(ma, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<Item>());
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
        Items = new ArrayList<>();
        CurrentPages = 0;
        CurrentDuration = 0;
        CurrentItems = 0;
    }

    public synchronized void removeRow(int i) {
        IDs.remove(i);
        Items.remove(i);
    }

    @Override
    public synchronized void clear() {
        int l = Items.size();
        if (l == 0) {
            return;
        }
        IDs.clear();
        Items.clear();
        CurrentItems = 0;
        CurrentPages = 0;
        CurrentDuration = 0;
    }

    public synchronized void sortItems(final int i, int sorted) {
        final boolean invertSort = (i == sorted);
        final ArrayList<Item> tmp = new ArrayList<Item>();
        for (Item item : Items) {
            tmp.add(item);
        }
        if (tmp.size() < 1) {
            return;
        }
        if (tmp == null) {
            return;
        }
        int type = 0;
        if (i == 1000) {
            Collections.sort(tmp, Library.getComparator(null, invertSort, type));
        } else {
            if (Lib.itemTableColumnTypes.get(i).startsWith("unit")) {
                type = 1;
            }
            Collections.sort(tmp, Library.getComparator(Columns.get(i), invertSort, type));
        }
        Items = tmp;
        IDs = new ArrayList<>();
        for (Item item : Items) {
            IDs.add(item.id);
        }
    }

    public void add(Item item) {
        Items.add(item);
        IDs.add(item.id);
        CurrentItems++;
    }

    @Override
    public int getCount() {
        return (Items.size());
    }

    @Override
    public Item getItem(int i) {
        return (Items.get(i));
    }

    private void update(Item item) {
        int row = IDs.indexOf(item.id);
        Items.set(row, item);
    }

    private void updateAll() {
        for (int r = 0; r < IDs.size(); r++) {
            Items.set(r, new Item(Lib, IDs.get(r)));
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // http://devtut.wordpress.com/2011/06/09/custom-arrayadapter-for-a-listview-android/
        View v = convertView;
        if (v == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = inflater.inflate(R.layout.list_item, null);
        }
        Item item = getItem(position);

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
        return v;
    }
}
