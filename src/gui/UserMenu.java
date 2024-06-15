package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import imageprocessing.IImageProcessor;

/**
 * Base class for user defined menus
 * @author Christoph Stamm
 *
 */
public class UserMenu {
	private TwinView m_views;
	private MRU m_mru;
	private Menu m_menu;

	protected UserMenu(MenuItem item, TwinView views, MRU mru) {
		assert views != null : "views are null";
		m_menu = new Menu(item.getParent().getParent(), SWT.DROP_DOWN);
		m_menu.setData(this);
		item.setMenu(m_menu);
		m_views = views;
		m_mru = mru;
		
		m_menu.addListener(SWT.Show, new Listener() {
			@Override
			public void handleEvent(Event e) {
				for (int i=0; i < m_menu.getItemCount(); i++) {
					MenuItem mi = m_menu.getItem(i);
					Menu menu = mi.getMenu();

					if (menu != null) {
						mi.setEnabled(true);
					} else {
						mi.setEnabled(!m_views.isEmpty() && isEnabled(i));
					}
				}
			}
		});
	}
	
	public void add(String text, int accelerator, IImageProcessor proc) {
		MenuItem mi = new MenuItem(m_menu, SWT.PUSH);
		mi.setText(text);
		mi.setAccelerator(accelerator);
		mi.setData(proc);
		mi.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				run(mi);
				m_mru.setLastOperation(text);
			}
		});
	}
	
	public UserMenu addMenu(String text) {
		MenuItem mi = new MenuItem(m_menu, SWT.CASCADE);
		mi.setText(text);
		return new UserMenu(mi, m_views, m_mru);
	}
	
	public boolean isEnabled(int index) {
		IImageProcessor proc = (IImageProcessor)m_menu.getItem(index).getData();

		return proc.isEnabled(m_views.getImageType(true));
	}

	public boolean findAndRun(String text) {
		for(int i = 0; i < m_menu.getItemCount(); i++) {
			MenuItem mi = m_menu.getItem(i);
			Menu menu = mi.getMenu();
			
			if (menu != null) {
				UserMenu im = (UserMenu)menu.getData();
				if (im.findAndRun(text)) return true;
			} else if (mi.getText().equals(text)) {
				IImageProcessor proc = (IImageProcessor)mi.getData();
				
				if (proc.isEnabled(m_views.getImageType(true))) {
					run(mi);
				}
				return true;
			}		
		}
		return false;
	}
	
	private void run(MenuItem mi) {
		try {
			IImageProcessor proc = (IImageProcessor)mi.getData();
			
			ImageData output = proc.run(m_views.getImage(true), m_views.getImageType(true));
			if (output != null) {
				m_views.showImageInSecondView(output);
			}		
		} catch(Throwable e) {
			String text = mi.getText();
			int last = text.indexOf('\t');
			if (last == -1) last = text.length();
			String location = text.substring(0, last).replace("&", "");
			m_views.m_mainWnd.showErrorDialog("ImageProcessing", location, e);
		}						
	}
}
