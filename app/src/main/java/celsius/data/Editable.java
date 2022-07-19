/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

// AND import atlantis.gui.KeyValueTableModel;
import celsius.components.library.Library;

/**
 *
 * @author cnsaeman
 */
public interface Editable {

    public Library getLibrary();
    // AND public KeyValueTableModel getEditModel();
    public void put(String key, String value);
    public String get(String key);
    public boolean containsKey(String key);
    public void save();
    public void updateShorts();
    public void notifyChanged();
    
}
