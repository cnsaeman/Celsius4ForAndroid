/*
 * Resource-Class
 *
 * contains all the various resources and general data structures used by Celsius
 *
 */

package celsius;

//import atlantis.tools.GuiStates;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import atlantis.tools.TextFile;
import atlantis.tools.Parser;
//import celsius.images.Icons;
/*import celsius.gui.CelsiusTable;
import celsius.gui.MainFrame;
import celsius.tools.Plugins;
import celsius.gui.SafeMessage;
import celsius.data.BibTeXRecord;
import celsius.gui.TabLabel;*/
//import celsius.data.ItemSelection;
import celsius.components.library.Library;
//import celsius.data.LibraryTemplate;
//import celsius.data.RecentLibraryCache;
/*import celsius.gui.InformationPanel;
import celsius.gui.MultiLineMessage;
import celsius.tools.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;*/
import java.io.File;
/*import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;*/
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Driver;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
/*import java.util.Date;
import java.util.Enumeration;*/
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import celsius.tools.*;

/*import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.nimbus.AbstractRegionPainter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;*/

/**
 *
 * @author cnsaeman
 */
public class Resources {

    public final String VersionNumber = "vA4.0.1";
    public final String celsiushome = "https://github.com/cnsaeman/Celsius4";
    public String HomeDirectory;
    
    public TextFile logFile;
    public int logLevel; // 0 : standard stuff, 10: all database interactions, 20: everything
    
    public ScheduledExecutorService executorService;

    public final String[] HistoryFields={"Today","Yesterday","Two Days Ago","This Week","Last Week","This Month","Last Month","This Year","Last Year"};

    public final String stdHTMLString="<html><body><h1>Celsius Library System 4 for Android</h1><br>" +
                "<h2>(w) by Christian Saemann<h2><hr><br>" +
                "<p align=\"justify\">Welcome to the Celsius Library System, the flexible database and file storage system!</p>" +
                "<p align=\"justify\">This is the Android version of Celsius 4, a reader for Celsius 4 libraries. Please edit and create libraries using Celsius 4 for desktop computers.</p>" +
                "<br><hr><br><a name=\"moreinfo\"><h3>More information</font><h3>" +
                "<p align=\"justify\">Celsius's homepage is located at <a href=\"" + celsiushome + "\">" + celsiushome + "</a>, where all the releases and sources are found.</p>" +
                "<br><hr><br><a name=\"moreinfo\"><h3>Copyright information</font></h3>" +
                "<p align=\"justify\">Celsius is open source and released under the GNU General Public License v3.</p>" +
                "</body></html>";

    public ArrayList<Library> libraries;
    public int currentLib;

    public HashMap<String, String> shortCuts; // list of shortcuts, implemented in this way to allow for shortcut editor later
    public HashMap<String,String> journalLinks;
    
    //public Icons icons;       // class for all the icons

    public double guiScaleFactor;

    public final ExecutorService sequentialExecutor;
    public ThreadPoolExecutor TPE;
    public LinkedBlockingQueue<Runnable> LBQ;
    
    public boolean displayHidden;

    public boolean guiNotify;
    
    public final SimpleDateFormat SDF;

    public Activity mainActivity;

    public String celsiusBaseFolder;

    @RequiresApi(api = Build.VERSION_CODES.R)
    public Resources(Activity activity) {
        logLevel=-1;

        mainActivity=activity;
        celsiusBaseFolder="NoBaseFolderOnTablets";
        SDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        try {
            DriverManager.registerDriver((Driver) Class.forName("org.sqldroid.SQLDroidDriver").newInstance());
        } catch (Exception e) {
            throw new RuntimeException("Failed to register SQLDroidDriver");
        }
        guiNotify=true;
        displayHidden=false;
        HomeDirectory = Parser.cutUntil((new File(".")).getAbsolutePath(), "/.");
        currentLib = -1;
        libraries = new ArrayList<>();
        journalLinks = new HashMap<>();
        sequentialExecutor=java.util.concurrent.Executors.newSingleThreadExecutor();
        executorService=java.util.concurrent.Executors.newScheduledThreadPool(5);
        LBQ=new LinkedBlockingQueue<>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
    }

    /**
     * Information Dialog
     */
    public void showInformation(String msg, String head) {
        AlertDialog alertDialog = new AlertDialog.Builder(mainActivity).create();
        alertDialog.setTitle(head);
        alertDialog.setMessage(msg);
        alertDialog.setCancelable(true);
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });

        alertDialog.show();
    }


//    /*public void initResources() {
//        try {
//            out("RES>Verifying standard folders");
//            if (!(new File("icons")).exists()) FileTools.makeDir("icons");
//            if (!(new File("plugins")).exists()) FileTools.makeDir("plugins");
//            if (!(new File("templates")).exists()) FileTools.makeDir("templates");
//            out("RES>Loading configuration file...");
//            configuration = new Configurator(this);
//            icons=new Icons(configuration.getConfigurationProperty("iconfolder"));
//
//            out("RES>Loading library templates...");
//            String templates[] = (new File("templates")).list();
//            for (String template : templates) {
//                libraryTemplates.add(new LibraryTemplate(this,"templates/"+template));
//            }
//        } catch (Exception e) {
//            outEx(e);
//            out("RES>Initializing of resources failed");
//            showWarning("Error while initializing of resources:\n" + e.toString()+"\nCelsius might not be started in the correct folder/directory.", "Exception:");
//            System.exit(255);
//        }
//        out("RES>Setting Proxy server...");
//        configuration.setProxy();
//    }
//
//    public void emptyThreadPoolExecutor() {
//        LBQ.clear();
//        TPE.shutdownNow();
//        LBQ=new LinkedBlockingQueue<>();
//        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
//    }
//
//    /**
//     * Initialize the logging system
//     */
//    private void initLog() {
//        try {
//            logFile=new TextFile(Resources.logFileName, true);
//            out("");
//            out("RES>============================================");
//            out("RES>Celsius Library System " + VersionNumber);
//            out("RES>============================================");
//            out("RES>Started at: " + ToolBox.getCurrentDate());
//            out("");
//            logLevel=20;
//        } catch (final Exception e) {
//            outEx(e);
//            out("Warning:Logging system initialization failed!");
//            showWarning("Logging system initialization failed!", "Warning!");
//            System.exit(100);
//        }
//    }
//
//    public void resetLogFile() {
//        try {
//            logFile.close();
//            FileTools.deleteIfExists(Resources.logFileName);
//            initLog();
//        } catch (Exception ex) {
//            showWarning("Error while resetting log file:\n" + ex.toString(), "Exception:");
//            outEx(ex);
//        }
//    }
//
//    public void loadPlugins() {
//        plugins=new Plugins(this);
//        plugins.readInAvailablePlugins();
//    }
//
//
    public String timestampToString(String ts) {
        if (ts==null) return "Not Set";
        return(SDF.format(new Date(Long.valueOf(ts) * 1000)));
    }
//
//    public Library getCurrentlySelectedLibrary() {
//        if (currentLib==-1) return(null);
//        return(libraries.get(currentLib));
//    }
//
//    public int getCurrentlySelectedLibNo() {
//        return(currentLib);
//    }
//
//    public CelsiusTable getCurrentTable() {
//        if (MF.jTPTabList.getSelectedIndex()==-1) return(null);
//        return(celsiusTables.get(MF.jTPTabList.getSelectedIndex()));
//    }
//
//    public Item getCurrentlySelectedItem() {
//        return(guiInformationPanel.getItem());
//    }
//
//    public Person getCurrentlySelectedPerson() {
//        return(guiInformationPanel.getPerson());
//    }
//
    public void out() {
        out("");
    }

    public void out(String s) {
        try {
            Log.i("C4A",s);
        } catch (Exception e) {
            outEx(e);
        }
    }

    public void out(int ll,String s) {
        if (ll<=logLevel) out(s);
    }

    public void outEx(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        out(sw.toString());
    }

//    public void setTempStatus(String status) {
//        if (guiNotify && (MF!=null)) MF.setTempStatus(status);
//    }
//
//    public Color getLightGray() {
//        return(new java.awt.Color(204,204,204));
//    }
//
//    public void close() {
//        configuration.writeBackLibraryStatus();
//        for (Library Lib : libraries)
//            Lib.close();
//        try {
//            out();
//            out("RES>Application closed at: " + ToolBox.getCurrentDate());
//            //Msg1.finalize();
//        } catch (final Exception e) {
//            outEx(e);
//            (new SafeMessage("Protocol file finalization failed:" + e.toString(), "Exception:", 0)).showMsg();
//            showWarning("RES>Messager finalization failed!", "Warning!");
//        }
//        try {
//            logFile.close();
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }
//    }
//
    public void loadLibrary(String fileName) {
        final Library library = new Library(fileName, this);
        if (library.name.equals("??##cancelled")) return;
        if (library.name.startsWith("??#$")) return;
        if (library.name.startsWith("??##")) {
            if (library.currentStatus==20) {
                out("Error loading library, library " + fileName + " is locked. Library has not been opened.");
            } else {
                out("Error loading library, library " + fileName + " has not been loaded:\n" + ToolBox.stripError(library.name));
            }
        } else {
            if (library.currentStatus==10) {
                out("The library "+library.name+" may be out of synch. Please sychnronize it as soon as possible.");
            }
            library.updateSizeInfo();
            libraries.add(library);
            out("RSC>Library " + library.name + " loaded.");
        }
    }
//
//    public URL getImageURL(String s) {
//        if (guiScaleFactor>1.9) {
//            s=s+".2x";
//        }
//        return getClass().getResource("/celsius/images/"+s+".png") ;
//    }
//
//    public Image getImage(String s) {
//        if (guiScaleFactor>1.9) {
//            s=s+".2x";
//        }
//        Image out=null;
//        try {
//            out=icons.getIcon(s).getImage();
//        } catch (Exception ex) {
//            out("ERROR!! Cannot load image "+s);
//        }
//        return out;
//    }
//
//    public ImageIcon getScaledIcon(String s) {
//        if (guiScaleFactor>1.9) {
//            s=s+".2x";
//        }
//        //if (s==null) return(get("default"));
//        if (s==null) return(null);
//        if (s.equals("")) return(null);
//        if (s.length()==0) return(icons.get("default"));
//        if (!icons.containsKey(s)) return(icons.get("notavailable"));
//        return(icons.get(s));
//    }
//
//    public void adjustComponents(Component[] comp) {
//        for(int x = 0; x < comp.length; x++)
//        {
//          if(comp[x] instanceof Container) adjustComponents(((Container)comp[x]).getComponents());
//          try{
//              if (comp[x].getFont().getSize()<20) comp[x].setFont(new java.awt.Font("Arial", 0, guiScale(12)));
//              if (guiScaleFactor>1) {
//                  if (comp[x] instanceof JRadioButton) {
//                      ((JRadioButton)comp[x]).setIcon(getScaledIcon("iconmonstr-shape-20.svg.16"));
//                      ((JRadioButton)comp[x]).setSelectedIcon(getScaledIcon("iconmonstr-checkbox-28.svg.16"));
//                      ((JRadioButton)comp[x]).setRolloverIcon(getScaledIcon("iconmonstr-shape-20.svg.16"));
//                      ((JRadioButton)comp[x]).setIconTextGap(guiScale(3));
//                  }
//                  if (comp[x] instanceof JCheckBox) {
//                        ((JCheckBox)comp[x]).setIcon(getScaledIcon("iconmonstr-square-4.svg.16"));
//                        ((JCheckBox)comp[x]).setSelectedIcon(getScaledIcon("iconmonstr-checkbox-4.svg.16"));
//                        ((JCheckBox)comp[x]).setRolloverIcon(getScaledIcon("iconmonstr-square-4.svg.16"));
//                        ((JCheckBox)comp[x]).setIconTextGap(guiScale(3));
//                  }
//              }
//          }
//          catch(Exception e){}//do nothing
//        }
//    }
//
//    /**
//     * Create the journal link command
//     */
//    public String getJournalLinkCmd(Item item) {
//        if (item == null)
//            return("");
//        BibTeXRecord BR=new BibTeXRecord(item.get("bibtex"));
//        String tag,gtag;
//        String tmp1=journalLinks.get(BR.get("journal"));
//        if (tmp1 == null)
//            return("");
//        if (tmp1.length() > 0) {
//            // Substitute addinfo tags
//            for (String key : BR.keySet()) {
//                gtag = BR.get(key);
//                if (gtag.length()==0) {
//                    gtag = "";
//                }
//                if (key.equals("pages")) {
//                    if (gtag.indexOf('-')>0) gtag=Parser.cutUntil(gtag, "-");
//                    if (gtag.indexOf('-')>0) gtag=Parser.cutUntil(gtag, "-");
//                }
//                tmp1 = tmp1.replace("#" + key + "#", gtag);
//            }
//        }
//        return(tmp1);
//    }
//
//    public void rememberDir(String dir, JFileChooser FC) {
//        File f=FC.getSelectedFile();
//        if (!f.isDirectory()) f=f.getParentFile();
//        rememberDir(dir,f.getAbsolutePath());
//    }
//
//    public void rememberDir(String dir, String folderPath) {
//        if (getCurrentlySelectedLibrary()==null) return;
//        getCurrentlySelectedLibrary().putConfig("dir::"+dir,folderPath);
//    }
//
//
//    public String getDir(String dir) {
//        if (getCurrentlySelectedLibrary()==null) return(".");
//        String ret=getCurrentlySelectedLibrary().config.get("dir::"+dir);
//        if ((ret==null) || (ret.length()==0)) ret=".";
//        return(ret);
//    }
//
//    public void setCurrentItemTable(String title, String icon) {
//        getCurrentTable().title=title;
//        MF.jTPTabList.setTabComponentAt(MF.jTPTabList.getSelectedIndex(), new TabLabel(title,icon,this,getCurrentTable(),true));
//    }
//
//    public String getTableRowIDs(ArrayList<TableRow> tableRows) {
//        String ids="";
//        for (TableRow tableRow : tableRows) {
//            ids+=","+String.valueOf(tableRow.id);
//        }
//        return("("+ids.substring(1)+")");
//    }
//
//    /**
//     * Sets the MainFrame here and in controller
//     *
//     * @param MF
//     */
//    public void setMainFrame(MainFrame MF) {
//        this.MF=MF;
//    }
//
//    public final static Object[] optionsYNC = { "Yes", "No", "Cancel" };
//    public final static Object[] optionsOC = { "OK", "Cancel" };
//    public final static Object[] optionsYN = { "Yes", "No" };
//
//
//    /**
//     * Shows an information message dialog
//     *
//     * @param title : title of dialog
//     * @param msg : message to be displayed
//     */
//    public void showInformation(String title, String msg) {
//        JOptionPane.showMessageDialog(MF,msg,title, JOptionPane.INFORMATION_MESSAGE);
//    }
//
//    /**
//     * Shows a long information message dialog
//     *
//     * @param title : title of dialog
//     * @param msg : message to be displayed
//     */
//    public void showLongInformation(String title, String msg) {
//        MultiLineMessage MLM=new MultiLineMessage(this,title,msg);
//        MLM.setVisible(true);
//    }
//
//    /**
//     * Shows a warning message dialog
//     *
//     * @param title : title of dialog
//     * @param msg : message to be displayed
//     */
//    public void showWarning(String message, String title) {
//        JOptionPane.showMessageDialog(MF, message, title, JOptionPane.WARNING_MESSAGE);
//    }
//
//    /**
//     * Question dialog with options OC
//     */
//    public int askQuestionOC(String msg,String head) {
//        return(JOptionPane.showOptionDialog(MF,msg, head, JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null, optionsOC, optionsOC[0]));
//    }
//
//    /**
//     * Question dialog with options YNC
//     */
//    public int askQuestionYNC(String msg,String head) {
//        return(JOptionPane.showOptionDialog(MF, msg, head,
//                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE,
//                null, optionsYNC, optionsYNC[0]));
//    }
//
//    /**
//     * Question dialog with options YN
//     */
//    public int askQuestionYN(String msg,String head) {
//        return(JOptionPane.showOptionDialog(MF, msg, head,
//                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//                null, optionsYN, optionsYN[0]));
//    }
//
//    /**
//     * Question dialog with two choices: A,B
//     */
//    public int askQuestionAB(String msg,String head,String A,String B) {
//        Object[] options=new Object[2];
//        options[0]=A; options[1]=B;
//        return(JOptionPane.showOptionDialog(MF, msg, head,
//                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//                null, options, options[0]));
//    }
//
//    /**
//     * Question dialog with three choices: A, B, C
//     */
//    public int askQuestionABC(String msg,String head,String A,String B,String C) {
//        Object[] options=new Object[3];
//        options[0]=A; options[1]=B; options[2]=C;
//        return(JOptionPane.showOptionDialog(MF, msg, head,
//                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//                null, options, options[0]));
//    }
//
//    /**
//     * Question dialog with three choices: A, B, C
//     */
//    public int askQuestionABCD(String msg,String head,String A,String B,String C,String D) {
//        Object[] options=new Object[4];
//        options[0]=A; options[1]=B; options[2]=C; options[3]=D;
//        return(JOptionPane.showOptionDialog(MF, msg, head,
//                JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
//                null, options, options[0]));
//    }
//
//    /**
//     * Let user select folder
//     *
//     * @param RSC
//     * @param title : title of dialogue
//     * @param name : name to remember directory
//     * @return
//     */
//    public String selectFolder(String title, String name) {
//        JFileChooser FC = new JFileChooser();
//        FC.setDialogTitle(title);
//        FC.setCurrentDirectory(new File(getDir(name)));
//        FC.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
//        FC.setDialogType(JFileChooser.OPEN_DIALOG);
//        FC.setFileFilter(new FFilter("_DIR", "Folders"));
//        // cancelled?
//        if (!(FC.showOpenDialog(MF) == JFileChooser.CANCEL_OPTION)) {
//            rememberDir(name, FC.getSelectedFile().getAbsolutePath());
//            return FC.getSelectedFile().getAbsolutePath();
//        }
//        return null;
//    }
//
//    /**
//     * Let user select a file
//     *
//     * @param RSC
//     * @param title : title of dialogue
//     * @param name : name to remember directory
//     * @param fileTypeShort : e.g. .csv
//     * @param fileTypeLong : e.g. CSV-files
//     * @return
//     */
//    public String selectFile(String title, String name, String fileTypeShort, String fileTypeLong) {
//        JFileChooser FC = new JFileChooser();
//        FC.setDialogTitle(title);
//        FC.setCurrentDirectory(new File(getDir(name)));
//        FC.setFileSelectionMode(JFileChooser.FILES_ONLY);
//        FC.setDialogType(JFileChooser.OPEN_DIALOG);
//        FC.setFileFilter(new FFilter(fileTypeShort, fileTypeLong));
//        // cancelled?
//        if (!(FC.showOpenDialog(MF) == JFileChooser.CANCEL_OPTION)) {
//            rememberDir(name, FC.getSelectedFile().getParent());
//            return FC.getSelectedFile().getAbsolutePath();
//        }
//        return null;
//    }
//
//    public void setSelectedLibrary(int i) {
//        if ((i > -1) && (i < MF.jCBLibraries.getItemCount())) {
//            MF.jCBLibraries.setSelectedIndex(i);
//        }
//    }
//
//    public String getBibOutput(Item item) {
//        String n = (String) MF.guiInfoPanel.jCBBibPlugins.getSelectedItem();
//        if (n == null) {
//            return "";
//        }
//        Plugin plugin = plugins.get(n);
//        String params = plugins.parameters.get(n);
//        Library Lib = getCurrentlySelectedLibrary();
//        if (item == null) {
//            return "";
//        }
//        ArrayList<String> msg = new ArrayList<String>();
//        HashMap<String,String> communication=new HashMap<>();
//        try {
//            Thread tst = plugin.Initialize(item, communication, msg);
//            tst.start();
//            tst.join();
//        } catch (Exception ex) {
//            out("jIP>Error while running BibPlugin: " + plugin.metaData.get("title") + ". Exception: " + ex.toString());
//            showWarning("Error while applying Bibplugins:\n" + ex.toString(), "Exception:");
//            outEx(ex);
//        }
//        return communication.get("output");
//    }
//
//    /**
//     * Display item in a form available, following a hierarchy of option
//     */
//    public void viewItem(Item item) {
//        if (item == null) {
//            return;
//        }
//        if (item.linkedAttachments.size() > 0) {
//            configuration.view(item, 0);
//        } else {
//            String cmdln = getJournalLinkCmd(item);
//            if (cmdln.length() > 0) {
//                out("JM>Journal link command: " + cmdln);
//                (new ExecutionShell(cmdln, 0, true)).start();
//            } else {
//                if (!item.getS("url").isBlank()) {
//                    configuration.viewHTML(item.get("url"));
//                } else {
//                    if (item.getS("links").length() > 0) {
//                        if (item.getS("links").indexOf("combines") > -1) {
//                            MF.showLinksOfType("combines");
//                        }
//                        MF.showLinksOfType("Available Links");
//                    } else {
//                        showWarning("No file or journal link associated with this entry.", "Warning");
//                    }
//                }
//            }
//        }
//    }
//
//    public void viewCurrentlySelectedObject() {
//        CelsiusTable celsiusTable=getCurrentTable();
//        if (celsiusTable.getObjectType()==CelsiusTable.ITEM_TABLE) {
//            viewItem(getCurrentlySelectedItem());
//        } else if (celsiusTable.getObjectType()==CelsiusTable.PERSON_TABLE) {
//            String personIDs=celsiusTable.getSelectedIDsString();
//            String name="Several people";
//            if (celsiusTable.jtable.getSelectedRowCount()==1) {
//                TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
//                name=tableRow.toText(false);
//            }
//            CelsiusTable targetTable=makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEMS_OF_PERSONS, name, Resources.personTabIcon);
//            targetTable.library.showItemsWithPersonIDs(personIDs, targetTable);
//            getCurrentTable().selectFirst();
//        }
//    }
//
//    public void loadShortCuts() {
//        shortCuts = new HashMap<String, String>();
//        try {
//            TextFile TD = new TextFile("celsius.shortcuts");
//            String tmp;
//            while (TD.ready()) {
//                tmp = TD.getString();
//                if (tmp.indexOf("::") > 0) {
//                    shortCuts.put(Parser.cutUntil(tmp, "::").trim(), Parser.cutFrom(tmp, "::").trim());
//                }
//            }
//            TD.close();
//            TD = new TextFile("celsius.journallinks");
//            while (TD.ready()) {
//                tmp = TD.getString();
//                if (tmp.indexOf("::") > 0) {
//                    journalLinks.put(Parser.cutUntil(tmp, "::").trim(), Parser.cutFrom(tmp, "::").trim());
//                }
//            }
//            TD.close();
//        } catch (IOException ex) {
//            out("MAIN>Error while loading shortcut/journallinks file:\n" + ex.toString());
//            (new SafeMessage("Error while loading shortcut/journallinks file:\n" + ex.toString(), "Exception:", 0)).showMsg();
//            outEx(ex);
//        }
//    }
//
//    public void removeSelectedFromCurrentTable() {
//        CelsiusTable celsiusTable = getCurrentTable();
//        if (celsiusTable!=null) {
//            for (TableRow tableRow : celsiusTable.getSelectedRows())
//                celsiusTable.removeRow(tableRow);
//        }
//    }
    
}
