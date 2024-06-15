package gui;

import java.util.prefs.Preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

/**
 * Most recently used files
 * 
 * @author Christoph Stamm
 *
 */
public class MRU {
	private static int MaxMRUitems = 6;	// number of items in the MRU list
	private static String LastOperation = "LastOperation";
	private static String[] Keys = { "0", "1", "2", "3", "4", "5" };
	
	private MainWindow m_mainWindow;	// main window
	private Preferences m_mru;			// system preferences used to store the MRU list
	private Menu m_recentFiles = null;	// menu used to display the MRU list

	/**
	 * Creates a MRU wrapper object
	 * @param mw main window
	 */
	public MRU(MainWindow mw) {
		assert Keys.length == MaxMRUitems;
		m_mainWindow = mw;
		Preferences p = Preferences.userRoot();
		if (p != null) {
			m_mru = p.node("/MRU");
		}
	}
	
	/**
	 * Returns last used file name or null
	 * @return
	 */
	public String getTop() {
		if (m_mru == null) return null;
		
		return m_mru.get("0", "");
	}
	
	public String getLastOperation() {
		if (m_mru == null) return null;

		return m_mru.get(LastOperation, null);
	}
	
	public void setLastOperation(String text) {
		if (m_mru == null) return;
		assert text != null : "text is null";

		m_mru.put(LastOperation, text);
	}
	
	/**
	 * Adds MRU list to given menu
	 * @param recent menu
	 */
	public void addRecentFiles(Menu recent) {
		if (m_mru == null) return;
		assert recent != null : "recent menu is null";
		m_recentFiles = recent;		
		
		// read filenames from prefs
		updateMenu();
	}
	
	/**
	 * Delete current menu items and add new menu items according to the saved MRU list
	 */
	private void updateMenu() {
		// delete current menu items
		while (m_recentFiles.getItemCount() > 0) {
			m_recentFiles.getItem(m_recentFiles.getItemCount() - 1).dispose();
		}
		
		for (int i = 0; i < MaxMRUitems; i++) {
			final String fileName = m_mru.get(Keys[i], "");
			final int index = i;
			if (!fileName.isEmpty()) {
				MenuItem item = new MenuItem(m_recentFiles, SWT.PUSH);
				item.setText(fileName);
				item.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent event) {
						if (m_mainWindow.updateFile(fileName)) {
							// move fileName to top position in MRU
							moveFileNameToTop(index, fileName);
						} else {
							// remove fileName from MRU
							removeFileName(index);
						}
					}
				});
			}
		}
	}

	/**
	 * Moves or adds a file name to top of list
	 * @param index current menu item index of fileName or -1 for unknown index
	 * @param fileName
	 */
	public void moveFileNameToTop(int index, String fileName) {
		if (m_mru == null || index == 0) return;
		assert fileName != null : "filename is null";
		assert index >= -1 && index < MaxMRUitems : "invalid index";
		
		if (index == -1) {
			// search fileName
			index = 0;
			while(index < MaxMRUitems && !fileName.equals(m_mru.get(Keys[index], null))) index++;
			if (index == MaxMRUitems) index--;
		}
		for(int i = index - 1; i >= 0; i--) {
			m_mru.put(Keys[i + 1], m_mru.get(Keys[i], ""));
		}
		m_mru.put("0", fileName);
		updateMenu();		
	}

	
	/**
	 * Removes a file name from MRU list
	 * @param index current menu item index of fileName
	 */
	private void removeFileName(int index) {
		if (m_mru == null) return;
		assert index >= 0 && index < MaxMRUitems : "invalid index";
		
		for (int i = index + 1; i < MaxMRUitems; i++) {
			m_mru.put(Keys[i - 1], m_mru.get(Keys[i], ""));
		}
		m_mru.put(Keys[MaxMRUitems - 1], "");
		updateMenu();		
	}
	
}
