package com.atlantis.celsiusfa;

import static android.os.Build.VERSION.SDK_INT;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
//import celsius.Resources;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import celsius.Resources;
import celsius.data.Attachment;
import celsius.data.Item;
import celsius.data.Library;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, OnItemSelectedListener, TextWatcher {

    public Resources RSC;

    // GUI
    Spinner librarySpinner;
    ArrayAdapter<String> librarySpinnerAdapter;
    TextView textView;
    EditText searchField;
    ListView itemList;
    ItemListAdapter itemListAdapter;
    ArrayList<String> libraryList;

    Library currentLibrary;

    ThreadSearch threadSearch;

    private StringBuffer startupMessage;

    private final int READ_EXTERNAL_STORAGE=100;
    private final int WRITE_EXTERNAL_STORAGE=101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startupMessage=new StringBuffer();
        RSC=new Resources(this);
        RSC.out("onCreate, starting...");
        setContentView(R.layout.activity_main);

        // find all objects and register listeners
        librarySpinner = (Spinner) this.findViewById(R.id.librarySpinner);
        librarySpinner.setOnItemSelectedListener(this);
        textView=(TextView) this.findViewById(R.id.textView);
        itemList = (ListView) findViewById(R.id.LVResults);
        itemList.setOnItemClickListener(this);
        itemListAdapter = new ItemListAdapter(this,null);
        itemList.setAdapter(itemListAdapter);
        searchField = (EditText) findViewById(R.id.TFsearch);
        searchField.addTextChangedListener(this);
        RSC.out("ZZZ:"+String.valueOf(itemList.getWidth()));
        searchField.setWidth((int)(0.8*itemList.getWidth()));

        RSC.out(String.valueOf(librarySpinner ==null));
        RSC.out("Loading libraries");
        readInLibraries();
        librarySpinner.setAdapter(librarySpinnerAdapter);
        currentLibrary=null;

        final Button button = (Button) findViewById(R.id.clear_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                searchField.setText("");
            }
        });
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
        File[] files = (new File(baseFolder)).listFiles();
        if (files!=null) {
            RSC.out("Found "+String.valueOf(files.length)+" files.");
            for (File f : files) {
                RSC.out("Found&loaded library "+f.getName());
                startupMessage.append("Found&loaded library "+f.getName()+"\n");
                RSC.loadLibrary(f.getPath());
                libraryList.add(f.getName());
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
                for (File file : files) {
                    if (file.isDirectory()) {
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
        librarySpinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, libraryList);
        librarySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //RSC.showInformation(startupMessage.toString(),"Celsius4 | (w) by C. Saemann");
    }

    public void startSearch(String srch,int mode) {
        if (currentLibrary==null) {
            RSC.showInformation("Current library is null","Error!");
        } else {
            itemListAdapter.clear();
            itemList.setAdapter(itemListAdapter);
            stopSearch();
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

    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Item item = itemListAdapter.getItem(position);
        item.loadLevel(3);
        if (item.linkedAttachments.size()>0) {
            Attachment attachment=item.linkedAttachments.get(0);
            String path=attachment.getFullPath();
            // Old mime code
            /*String mime=attachment.getS("filetype");
            if (mime.equals("epub")) mime="epub+zip";*/

            // Better, new mime code
            String mime= URLConnection.guessContentTypeFromName(path);
            RSC.out("MIME: "+mime);
            Intent viewerIntent = new Intent(Intent.ACTION_VIEW);
            viewerIntent.setDataAndType(getUriFromFile(new File(path)), mime);
            viewerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|
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

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        if ((librarySpinner.getSelectedItemPosition()>-1) && (librarySpinner.getSelectedItemPosition()<RSC.libraries.size())) {
            Library library = RSC.libraries.get(librarySpinner.getSelectedItemPosition());
            currentLibrary = library;
            textView.setText("Current Library: " + library.name + ". Items: " + String.valueOf(library.numberOfItems) + ". People: " + String.valueOf(library.numberOfPeople));
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String srch = searchField.getText().toString();
        if (srch.length() > 0) startSearch(srch, 0);
    }

    public void afterTextChanged(Editable s) {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

}