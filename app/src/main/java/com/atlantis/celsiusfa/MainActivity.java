package com.atlantis.celsiusfa;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.atlantis.celsiusfa.databinding.ActivityMain2Binding;
import com.google.android.material.tabs.TabLayout;

import java.io.File;
import java.net.URLConnection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import celsius.Resources;
import celsius.components.categories.Category;
import celsius.data.Attachment;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.data.Person;
import celsius.gui.CustomMovementMethod;
import celsius.tools.CelsiusTemplate;

public class MainActivity extends AppCompatActivity  implements AdapterView.OnItemClickListener, MenuItem.OnMenuItemClickListener, TextWatcher {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMain2Binding binding;

    public Resources RSC;

    // GUI
    Spinner searchSpinner;
    Menu navigation;
    TextView textView;
    EditText searchField;
    ListView itemList;
    TableRowListAdapter itemListAdapter;
    Button clearButton;
    TextView infoBox;

    Item currentItem;

    ArrayList<String> libraryList;

    Library currentLibrary;

    int currentListType;

    ThreadSearch threadSearch;

    private StringBuffer startupMessage;

    private final int READ_EXTERNAL_STORAGE=100;
    private final int WRITE_EXTERNAL_STORAGE=101;

    @RequiresApi(api = Build.VERSION_CODES.R)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain2.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main2);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        startupMessage=new StringBuffer();
        currentItem=null;

        RSC=new Resources(this);
        RSC.out("onCreate, starting...");

        // find all objects and register listeners
        textView=(TextView) this.findViewById(R.id.textView);
        itemList = (ListView) findViewById(R.id.LVResults);
        itemList.setOnItemClickListener(this);


        infoBox= (TextView)this.findViewById(R.id.infoBox);

        setInfo(RSC.stdHTMLString);

        clearButton=(Button) this.findViewById(R.id.clear_button);
        clearButton.setEnabled(false);
        searchSpinner=(Spinner) this.findViewById(R.id.searchSpinner);
        searchSpinner.setEnabled(false);

        searchField = (EditText) findViewById(R.id.TFsearch);
        searchField.addTextChangedListener(this);
        RSC.out("ZZZ:"+String.valueOf(itemList.getWidth()));
        searchField.setWidth((int)(0.8*itemList.getWidth()));
        searchField.setEnabled(false);

        navigation = ((NavigationView) findViewById(R.id.nav_view)).getMenu();
        itemListAdapter = new TableRowListAdapter(this,null);
        itemList.setAdapter(itemListAdapter);

        RSC.out("Loading libraries");
        readInLibraries();
        currentLibrary=null;

        final Button button = (Button) findViewById(R.id.clear_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchField.setText("");
            }
        });

    }

    public void setInfo(String info) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            infoBox.setText(Html.fromHtml(info, Html.FROM_HTML_MODE_COMPACT));
        } else {
            infoBox.setText(Html.fromHtml(info));
        }
        infoBox.setMovementMethod(new CustomMovementMethod(RSC,this));
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            RSC.showInformation("This is Celsius for Android, version "+RSC.VersionNumber,"About");
            return(true);
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main2, menu);
        menu.getItem(0).setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main2);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    public void showBaseFiles() {
        RSC.showInformation("Showing base files","Information:");
        try {
            File[] filesInBase = new File(".").listFiles();
            StringBuffer out = new StringBuffer();
            for (File file : filesInBase) {
                out.append("\n" + file.getName());
            }
            if (out.length() > 0) out = out.deleteCharAt(1);
            RSC.showInformation(out.toString(), "Files in base folder:");
        } catch(Exception ex) {
            RSC.showInformation("Error!","Error!");
        }
    }

    /**
     * Load the libraries contained in Celsius4-folder baseFolder
     * @param baseFolder
     */
    public void loadLibrariesFromBaseFolder(String baseFolder) {
        RSC.out("Loading Libraries from "+baseFolder);
        int position=0;
        File[] files = (new File(baseFolder)).listFiles();
        if (files!=null) {
            RSC.out("Found "+String.valueOf(files.length)+" files.");
            for (File f : files) {
                RSC.out("Found&loaded library "+f.getName());
                startupMessage.append("Found&loaded library "+f.getName()+"\n");
                RSC.loadLibrary(f.getPath());
                MenuItem item=navigation.add(f.getName());
                final int p=position;
                item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        DrawerLayout layout = (DrawerLayout)findViewById(R.id.drawer_layout);
                        if (layout.isDrawerOpen(GravityCompat.START)) {
                            layout.closeDrawer(GravityCompat.START);
                        }
                        switchToLibrary(p);
                        return(true);
                    }
                });
                libraryList.add(f.getName());
                position++;
            }
        } else {
            startupMessage.append("Folder is empty!\n");
            RSC.out("ERROR: No libraries found.");
            RSC.showInformation("No libraries found.","Error:");
        }
    }

    public void lookThroughFolder(String folder) {
        RSC.out("Checking folder: "+folder);
        File directory = new File(folder);
        File[] files = directory.listFiles();
        boolean baseFolder=false;
        if (files!=null) {
            for (File file : files) {
                RSC.out("Found file: " + file.getName());
                if (file.getName().equals("Android")) baseFolder = true;
            }
            if (baseFolder) {
                RSC.out("Base folder found: " + folder);
                String basefolder= folder + "/Android/data/com.atlantis.celsiusfa/files";
                RSC.out("Setting basefolder: "+basefolder);
                if ((new File(basefolder)).exists()) {
                    RSC.out("Found Celsius folder: " + basefolder);
                    loadLibrariesFromBaseFolder(basefolder);
                }
            } else {
                RSC.out("Not a base folder. Recursively going through directories.");
                for (File file : files) {
                    if (file.isDirectory()) {
                        RSC.out("Entering: "+folder + "/" + file.getName());
                        lookThroughFolder(folder + "/" + file.getName());
                    }
                }
            }
        }
    }

    public void listFolder(String baseFolder) {
        File[] files = (new File(baseFolder)).listFiles();
        RSC.out("Listing folder " + baseFolder);
        if (files==null) {
            RSC.out("Folder is empty!");
        } else {
            for (File file : files) {
                RSC.out("File: " + file.getName());
            }
        }
    }

    public void readInLibraries() {
        libraryList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE);
        }
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE);
        }
        /*if(SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                Snackbar.make(findViewById(android.R.id.content), "Permission needed!", Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                try {
                                    Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
                                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                                    startActivity(intent);
                                } catch (Exception ex) {
                                    Intent intent = new Intent();
                                    intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                                    startActivity(intent);
                                }
                            }
                        })
                        .show();
            }
        }*/
        // Check available base folders
        lookThroughFolder("/storage");
        if (RSC.libraries.size()==0) lookThroughFolder("/sdcard");
        //RSC.showInformation(startupMessage.toString(),"Celsius4 | (w) by C. Saemann");
    }

    public void startSearch(String srch,int mode) {
        if (currentLibrary==null) {
            RSC.showInformation("Current library is null","Error!");
        } else {
            itemListAdapter.clear(searchSpinner.getSelectedItemPosition());
            itemList.setAdapter(itemListAdapter);
            stopSearch();
            currentListType=mode;
            if (srch.length()>1) {
                threadSearch = new ThreadSearch(this, currentLibrary, itemList, itemListAdapter, srch, mode);
                threadSearch.start();
            }
        }
    }

    public void stopSearch() {
        if (threadSearch!=null)
            if (threadSearch.running) {
                threadSearch.interrupt();
            }
    }

    private Uri getUriFromFile(File file){
        if (SDK_INT < Build.VERSION_CODES.N) {
            return Uri.fromFile(file);
        }else {
            return FileProvider.getUriForFile(this, "com.atlantis.fileprovider", file);
        }
    }

    public void viewAttachment(int position) {
        Item item = currentItem;
        item.loadLevel(3);
        if ((item.linkedAttachments.size()>0) && (item.linkedAttachments.size()>position)) {
            Attachment attachment=item.linkedAttachments.get(position);
            String path=attachment.getFullPath();

            String mime= URLConnection.guessContentTypeFromName(path);
            RSC.out("MIME: "+mime);
            Intent viewerIntent = new Intent(Intent.ACTION_VIEW);
            viewerIntent.setDataAndType(getUriFromFile(new File(path)), mime);
            viewerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|
                    Intent.FLAG_ACTIVITY_NO_HISTORY|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PackageManager pm = getPackageManager();
            List<ResolveInfo> activities = pm.queryIntentActivities(viewerIntent, 0);
            if (activities.size() > 0) {
                try {
                    startActivity(viewerIntent);
                } catch (Exception e) {
                    RSC.outEx(e);
                    RSC.showInformation("Permission error viewing this file. Location: " + path, "Note:");
                }
            } else {
                RSC.showInformation("No viewer found. Location: " + path, "Note:");
            }
        } else {
            RSC.showInformation( "No file linked.", "Note:");
        }
    }

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (currentListType==0) {
            Item item = (Item)itemListAdapter.tableRows.get(position);
            currentItem=item;
            item.loadLevel(3);
            CelsiusTemplate template = item.library.getHTMLTemplate(0);
            String info = template.fillIn(item, false);
            setInfo(info);
        } else if (currentListType==1) {
            Person person = (Person)itemListAdapter.tableRows.get(position);
            RSC.out("Listing associated items for person "+person.id);
            itemListAdapter.clear(0);
            currentListType=0;
            itemList.setAdapter(itemListAdapter);
            try {
                ResultSet RS=person.library.executeResEX("SELECT items.* FROM item_person_links INNER JOIN items ON items.id=item_person_links.item_id WHERE person_id = ("+person.id+");");
                while (RS.next()) {
                    Item item=new Item(person.library,RS);
                    RSC.out("Found item: "+item.id);
                    itemListAdapter.add(item);
                }
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            itemList.refreshDrawableState();
        } else if (currentListType==2) {
            Category category = (Category)itemListAdapter.tableRows.get(position);
            RSC.out("Listing associated items for category "+category.id);
            itemListAdapter.clear(0);
            currentListType=0;
            itemList.setAdapter(itemListAdapter);
            try {
                ResultSet RS=category.library.executeResEX("SELECT items.* FROM item_category_links INNER JOIN items ON items.id=item_category_links.item_id WHERE category_id = ("+category.id+");");
                while (RS.next()) {
                    Item item=new Item(category.library,RS);
                    RSC.out("Found item: "+item.id);
                    itemListAdapter.add(item);
                }
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            itemList.refreshDrawableState();
        }
    }

    public void switchToLibrary(int position) {
        if ((position>-1) && (position<RSC.libraries.size())) {
            searchField.setEnabled(true);
            clearButton.setEnabled(true);
            searchSpinner.setEnabled(true);
            RSC.out("Switching to Library number:"+position);
            Library library = RSC.libraries.get(position);
            currentLibrary = library;
            textView.setText("Current Library: " + library.name + ". Items: " + String.valueOf(library.numberOfItems) + ". People: " + String.valueOf(library.numberOfPeople));
            CelsiusTemplate template = library.htmlTemplates.get("-1");
            setInfo(template.fillIn(library.getDataHash()));
        }
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String srch = searchField.getText().toString();
        if (srch.length() > 0) startSearch(srch, searchSpinner.getSelectedItemPosition());
    }

    public void afterTextChanged(Editable s) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

}