package gui;

import java.io.IOException;

import javax.swing.JTextArea;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.printing.*;
import org.eclipse.swt.widgets.*;

import main.Picsi;
import files.Document;
import files.ImageFiles;
import imageprocessing.ImageProcessing;
import imageprocessing.colors.ColorSpaces;

/**
 * Picsi SWT main window
 * 
 * @author Christoph Stamm
 *
 */
public class MainWindow {
	private static final int SizePaneWidth = 250;
	
	public TwinView m_views;
	
	private final MRU m_mru = new MRU(this);

	private Shell m_shell;		// sub-classing of Shell is not allowed, therefore containing
	private Display m_display;
    private Clipboard m_clipboard; 
	private Editor m_editor;
	private String m_lastPath; // used to seed the file dialog
	private Label m_statusLabel, m_zoomLabel;
	private MenuItem m_editMenuItem;
	private ImageMenu m_imageMenu; // used in find-and-run

	/////////////////////////////////////////////////////////////////////////////////////////////////////7
	// public methods section

	/**
	 * @wbp.parser.entryPoint
	 */
	public Shell open(Display dpy) {
		// create a window and set its title
		m_display = dpy;
		m_shell = new Shell(m_display);
		m_clipboard = new Clipboard(m_display);
		{
			GridLayout gridLayout = new GridLayout();
			gridLayout.marginLeft = 5;
			gridLayout.marginWidth = 0;
			gridLayout.verticalSpacing = 0;
			gridLayout.marginTop = 5;
			gridLayout.marginHeight = 0;
			m_shell.setLayout(gridLayout);
			m_shell.setCursor(m_display.getSystemCursor(SWT.CURSOR_CROSS)); // a wait-cursor can be used only if the cross-cursor is set for the shell instead of the view
			
			// hook listeners
			m_shell.addShellListener(new ShellAdapter() {
				@Override
				public void shellClosed(ShellEvent e) {
					e.doit = true;
				}
			});
			m_shell.addDisposeListener(new DisposeListener() {
				@Override
				public void widgetDisposed(DisposeEvent e) {
					// clean up
					if (m_views != null) m_views.dispose();
					if (m_editor != null) m_editor.dispose();
				}
			});
	
			// set icon
			try {
				m_shell.setImage(new Image(m_display, getClass().getClassLoader().getResource("images/picsi.png").openStream()));			
			} catch(IOException e) {
			}
			
			// set title
			m_shell.setText(Picsi.APP_NAME);

			// enable drag and drop file (drop target)
			m_shell.setDragDetect(true);
			DropTarget dt = new DropTarget(m_shell, DND.DROP_MOVE | DND.DROP_COPY | DND.DROP_DEFAULT);
			dt.setTransfer(new Transfer[] { ImageTransfer.getInstance(), FileTransfer.getInstance() });
			dt.addDropListener(new DropTargetAdapter() {
				@Override
				public void dragEnter(DropTargetEvent event) {
					if (event.detail == DND.DROP_DEFAULT) {
						if ((event.operations & DND.DROP_COPY) != 0) {
							// set copy as default operation
							event.detail = DND.DROP_COPY;
						}
					}
					// will accept image but prefer to have files dropped
					for(TransferData d: event.dataTypes) {
						if (FileTransfer.getInstance().isSupportedType(d)) {
							// FileTransfer is preferred
							event.currentDataType = d;
				            if (event.detail != DND.DROP_COPY) {
				            	event.detail = DND.DROP_NONE;
				            }
						}
					}
				}
				@Override
				public void drop(DropTargetEvent event) {
					if (event.data != null) {
				        if (ImageTransfer.getInstance().isSupportedType(event.currentDataType)) {
				        	// image transfer
				        	ImageData inData = (ImageData)event.data;
							String name = "Image";
							m_views.showImageInFirstView(inData, name);
				        } else if (FileTransfer.getInstance().isSupportedType(event.currentDataType)) {
				        	// file transfer
				        	String[] fileNames = (String[])event.data;
							m_mru.moveFileNameToTop(-1, fileNames[0]);
							updateFile(fileNames[0]);
				        }
					}
				}			
			});			
		}
		
		// create twin view: must be done before createMenuBar, because of dynamic image processing menu items
		m_views = new TwinView(this, m_shell, SWT.NONE);
		
		// create
		createMenuBar();
		
		// create status bar
		{
			int dpiY = m_shell.getDisplay().getDPI().y;
			Composite compo = new Composite(m_shell, SWT.NONE);
			GridData data = new GridData (SWT.FILL, SWT.BOTTOM, true, false);
			Font font = m_shell.getFont();
			FontData[] fd = font.getFontData();
			data.heightHint = 2*fd[0].getHeight()*dpiY/96; //data.heightHint = 15;
			
			compo.setLayoutData(data);
			compo.setCursor(m_display.getSystemCursor(SWT.CURSOR_ARROW));
			
			GridLayout gridLayout = new GridLayout();
			gridLayout.marginRight = 5;
			gridLayout.numColumns = 2;
			gridLayout.horizontalSpacing = 10;
			gridLayout.marginHeight = 0;
			gridLayout.marginWidth = 0;
			compo.setLayout(gridLayout);
			
			// Label to show status and cursor location in image.
			m_statusLabel = new Label(compo, SWT.NONE);
			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			m_statusLabel.setLayoutData(data);
			
			// Label to show image size and zoom value
			m_zoomLabel = new Label(compo, SWT.RIGHT);
			data = new GridData(SWT.RIGHT, SWT.FILL, false, true);
			data.widthHint = SizePaneWidth;
			m_zoomLabel.setLayoutData(data);
		}
		
		// Open the window
		m_shell.pack();
		m_shell.open();
		return m_shell;
	}
	
	/**
	 * Show image as text in editor
	 * @param doc image document
	 * @param imageData image data
	 * @param text text area of editor
	 */
	public void displayTextOfBinaryImage(Document doc, ImageData imageData, JTextArea text) {
		doc.displayTextOfBinaryImage(imageData, text);
	}
	
	/*
	 * Set the status label to show color information
	 * for the specified pixel in the image.
	 */
	public void showPixelInfo(Object[] args, boolean average) {
		if (args == null) {
			m_statusLabel.setText("");
		} else {
			if (average) {
				m_statusLabel.setText(Picsi.createMsg("Mean color at ({0},{1}) - pixel {3} [0x{5}] - is {6} [{7}] {8}", args));
			} else {
				m_statusLabel.setText(Picsi.createMsg("Image color at ({0},{1}) - pixel {3} [0x{5}] - is {6} [{7}] {8}", args));
			}
		}
	}

	/**
	 * Show cursor position in status
	 * @param pnt
	 */
	public void showImagePosition(Point pnt) {
		if (pnt == null) {
			m_statusLabel.setText("");
		} else {
			m_statusLabel.setText("(" + pnt.x + ',' + pnt.y + ')');
		}
	}
	
	/**
	 * Show image size and zoom factors in status bar
	 * @param zoom1
	 * @param zoom2
	 */
	public void showZoomFactor(float zoom1, float zoom2) {
		View view1 = m_views.getView(true);
		String s = "(" + view1.getImageWidth() + ',' + view1.getImageHeight() + ") " + Math.round(zoom1*100) + '%';
		
		if (m_views.isSynchronized() || !m_views.hasSecondView()) {
			m_zoomLabel.setText(s);
		} else {
			View view2 = m_views.getView(false);
			s += " | (" + view2.getImageWidth() + ',' + view2.getImageHeight() + ") " + Math.round(zoom2*100) + '%';
			m_zoomLabel.setText(s);
		}
	}
	
	/**
	 * Shows a modal error dialog and prints the stack trace to console window
	 * @param operation
	 * @param filename
	 * @param e exception
	 */
	public void showErrorDialog(String operation, String filename, Throwable e) {
		MessageBox box = new MessageBox(m_shell, SWT.ICON_ERROR);
		String message = Picsi.createMsg("Error {0}\nin {1}\n\n", new String[] {operation, filename});
		String errorMessage = "";
		if (e != null) {
			if (e instanceof SWTException) {
				SWTException swte = (SWTException)e;
				errorMessage = swte.getMessage();
				if (swte.throwable != null) {
					errorMessage += ":\n" + swte.throwable.toString();
				}
			} else if (e instanceof SWTError) {
				SWTError swte = (SWTError)e;
				errorMessage = swte.getMessage();
				if (swte.throwable != null) {
					errorMessage += ":\n" + swte.throwable.toString();
				}
			} else {
				errorMessage = e.toString();
			}
			e.printStackTrace();
		}
		box.setText("Error");
		box.setMessage(message + errorMessage);
		box.open();
	}
	
	/**
	 * File information class
	 * @author Christoph Stamm
	 *
	 */
	public static class FileInfo {
		public String filename;
		public int fileType;
		
		public FileInfo(String name, int type) {
			filename = name;
			fileType = type;
		}
	}
	
	/***
	 * Get the user to choose a file name and type to save.
	 * @param imageType type of image (e.g. binary, grayscale, RGB, ...)
	 * @param fileName suggested file name, can be null
	 * @return
	 */
	private FileInfo chooseFileName(int imageType, String fileName) {
		FileDialog fileChooser = new FileDialog(m_shell, SWT.SAVE);
		String[] saveFilterExts = ImageFiles.saveFilterExtensions(imageType);
		fileChooser.setFilterPath(m_lastPath);
		fileChooser.setFilterExtensions(saveFilterExts);
		fileChooser.setFilterNames(ImageFiles.saveFilterNames(imageType));
		
		if (fileName != null) {
			fileChooser.setFileName(fileName);
			fileChooser.setFilterIndex(ImageFiles.determineSaveFilterIndex(saveFilterExts, fileName));
		}
		fileName = fileChooser.open();
		m_lastPath = fileChooser.getFilterPath();
		if (fileName == null)
			return null;

		// Figure out what file type the user wants.
		//fileChooser.getFilterIndex();
		int fileType = ImageFiles.determinefileType(fileName);
		if (fileType == SWT.IMAGE_UNDEFINED) {
			MessageBox box = new MessageBox(m_shell, SWT.ICON_ERROR);
			box.setMessage(Picsi.createMsg("Unknown file extension: {0}\nPlease use bmp, gif, ico, jfif, jpeg, jpg, png, tif, or tiff.", 
					fileName.substring(fileName.lastIndexOf('.') + 1)));
			box.open();
			return null;
		}
		
		if (new java.io.File(fileName).exists()) {
			MessageBox box = new MessageBox(m_shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
			box.setMessage(Picsi.createMsg("Overwrite {0}?", fileName));
			if (box.open() == SWT.CANCEL)
				return null;
		}
		
		return new FileInfo(fileName, fileType);		
	}
	
	/***
	 * Get the user to choose a file to save.
	 * Used in file editor.
	 * @param fileType type of file (e.g. JPEG, BMP, ...)
	 * @param imageType type of image (e.g. binary, grayscale, RGB, ...)
	 * @return
	 */
	public FileInfo chooseFileName(int fileType, int imageType) {
		if (fileType == SWT.IMAGE_UNDEFINED) {
			MessageBox box = new MessageBox(m_shell, SWT.ICON_ERROR);
			box.setMessage(Picsi.createMsg("Unknown file extension: {0}\nPlease use bmp, gif, ico, jfif, jpeg, jpg, png, tif, or tiff.", ""));
			box.open();
			return null;
		}
		
		String[] saveFilterExts = ImageFiles.saveFilterExtensions(imageType);
		int filterIndex = ImageFiles.determineSaveFilterIndex(saveFilterExts, ImageFiles.fileTypeString(fileType));
		FileDialog fileChooser = new FileDialog(m_shell, SWT.SAVE);
		fileChooser.setFilterPath(m_lastPath);		
		fileChooser.setFilterExtensions(new String[]{saveFilterExts[filterIndex]});
		fileChooser.setFilterNames(new String[]{ImageFiles.saveFilterNames(imageType)[filterIndex]});
				
		String filename = fileChooser.open();
		m_lastPath = fileChooser.getFilterPath();
		if (filename == null)
			return null;

		if (new java.io.File(filename).exists()) {
			MessageBox box = new MessageBox(m_shell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);
			box.setMessage(Picsi.createMsg("Overwrite {0}?", filename));
			if (box.open() == SWT.CANCEL)
				return null;
		}
		
		return new FileInfo(filename, fileType);		
	}

	/**
	 * Load file and show image in first view
	 * @param filename
	 * @return
	 */
	public boolean updateFile(String filename) {
		boolean retValue = true;
		Cursor cursor = m_shell.getCursor();
		
		m_shell.setCursor(m_display.getSystemCursor(SWT.CURSOR_WAIT));

		int fileType = ImageFiles.determinefileType(filename);
		
		try {
			m_views.load(filename, fileType);
		} catch (Throwable e) {
			showErrorDialog("loading", filename, e);
			retValue = false;
		} finally {
			m_shell.setCursor(cursor);			
		}
		
		return retValue;
	}

	/**
	 * Used to disable the menu during line tracking
	 * @param enabled
	 */
	public void setEnabledMenu(boolean enabled) {
		Menu menuBar = m_shell.getMenuBar();
		if (menuBar != null) menuBar.setEnabled(enabled);
	}

	/**
	 * Notifies all menus about input/output changes
	 */
	public void notifyAllMenus() {
		Menu menuBar = m_shell.getMenuBar();
		for(MenuItem item: menuBar.getItems()) {
			item.getMenu().notifyListeners(SWT.Show, new Event());
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////////7
	// private methods section
	
	private Menu createMenuBar() {
		// Menu bar.
		Menu menuBar = new Menu(m_shell, SWT.BAR);
		m_shell.setMenuBar(menuBar);
		createFileMenu(menuBar);
		createImageMenu(menuBar);
		createColorSpacesMenu(menuBar);
		createToolsMenu(menuBar);
		createWindowMenu(menuBar);
		createHelpMenu(menuBar);
		return menuBar;
	}
	
	// File menu
	private void createFileMenu(Menu menuBar) {	
		enum ME { New, Open, Run, Recent, Sep1, Copy, Paste, Sep2, CloseIn, CloseOut, CloseBoth, 
			Sep3, Save, SaveOut, SaveIn, Sep4, Edit, Sep5, Print, Sep6, Swap, Sep7, Exit };
		
		// File menu
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&File");
		final Menu fileMenu = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(fileMenu);
		fileMenu.addListener(SWT.Show,  new Listener() {
			@Override
			public void handleEvent(Event e) {
				MenuItem[] menuItems = fileMenu.getItems();
				menuItems[ME.Run.ordinal()].setEnabled(m_mru.getLastOperation() != null);
				menuItems[ME.Copy.ordinal()].setEnabled(m_views.hasSecondView());
				menuItems[ME.CloseIn.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.CloseOut.ordinal()].setEnabled(m_views.hasSecondView());
				menuItems[ME.CloseBoth.ordinal()].setEnabled(m_views.hasSecondView());
				menuItems[ME.Save.ordinal()].setEnabled(m_views.hasSecondView());
				menuItems[ME.SaveOut.ordinal()].setEnabled(m_views.hasSecondView());
				menuItems[ME.SaveIn.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Edit.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Print.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Swap.ordinal()].setEnabled(!m_views.isEmpty());
			}
		});
		
		// File -> New...
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&New...\tCtrl+N");
		item.setAccelerator(SWT.MOD1 + 'N');
		setIcon(item, "images/NewDocument.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				editFile(null, null, null);
			}
		});
		
		// File -> Open...
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Open...\tCtrl+O");
		item.setAccelerator(SWT.MOD1 + 'O');
		setIcon(item, "images/OpenFolder.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// Get the user to choose an image file.
				FileDialog fileChooser = new FileDialog(m_shell, SWT.OPEN);
				if (m_lastPath != null)
					fileChooser.setFilterPath(m_lastPath);
				fileChooser.setFilterExtensions(ImageFiles.openFilterExtensions());
				fileChooser.setFilterNames(ImageFiles.openFilterNames());
				String filename = fileChooser.open();
				if (filename == null)
					return;
				m_lastPath = fileChooser.getFilterPath();

				m_mru.moveFileNameToTop(-1, filename);
				updateFile(filename);
			}
		});
		
		// File -> Open and Run Last operation
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Open and Run Last\tCtrl+D");
		item.setAccelerator(SWT.MOD1 + 'D');
		setIcon(item, "images/OpenFile.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String filename = m_mru.getTop();
				if (filename != null) {
					updateFile(filename);
					String lastOperation = m_mru.getLastOperation();
					if (lastOperation != null) {
						m_imageMenu.findAndRun(lastOperation);
					}
				}
			}
		});
		
		// File -> Open Recent ->
		item = new MenuItem(fileMenu, SWT.CASCADE);
		item.setText("Open Recent");
		final Menu recent = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(recent);
		// add most recently used files
		m_mru.addRecentFiles(recent);
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		// File -> Copy
		item = new MenuItem(fileMenu, SWT.CASCADE);
		item.setText("Copy Output\tCtrl+C");
		item.setAccelerator(SWT.MOD1 + 'C');
		setIcon(item, "images/Copy.png");
	    item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				ImageTransfer imageTransfer = ImageTransfer.getInstance();
				m_clipboard.setContents(new Object[] { m_views.getImage(false) }, new Transfer[] { imageTransfer });
			}
		});

		// File -> Paste
		item = new MenuItem(fileMenu, SWT.CASCADE);
		item.setText("Paste Input\tCtrl+V");
		item.setAccelerator(SWT.MOD1 + 'V');
		setIcon(item, "images/Paste.png");
	    item.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				TransferData[] types = m_clipboard.getAvailableTypes();
				
				for(TransferData type : types) {			
			        if (ImageTransfer.getInstance().isSupportedType(type)) {
			        	// image transfer
			        	ImageData inData = (ImageData)m_clipboard.getContents(ImageTransfer.getInstance());
						String name = "Image";
						m_views.showImageInFirstView(inData, name);
			        } else if (FileTransfer.getInstance().isSupportedType(type)) {
			        	// file transfer
			        	String[] fileNames = (String[])m_clipboard.getContents(FileTransfer.getInstance());
						m_mru.moveFileNameToTop(-1, fileNames[0]);
						updateFile(fileNames[0]);
			        }
				}
			}
		});

		new MenuItem(fileMenu, SWT.SEPARATOR);

		// File -> Close Input
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Close Input");
		setIcon(item, "images/CloseDocument.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (m_views.hasSecondView()) {
					// swap images
					if (swapViews()) {
						// close output view
						m_views.close(false);						
					}
				} else {
					// update title
					m_shell.setText(Picsi.APP_NAME);

					// close input view
					m_views.close(true);
					
					// clear status line
					m_statusLabel.setText("");
					m_zoomLabel.setText("");
				}
				
				// notify all menus about the closed input
				notifyAllMenus();
			}
		});

		// File -> Close Output
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Close Output");
		setIcon(item, "images/CloseDocument.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// close output view
				m_views.close(false);
				// notify all menus about the closed output
				notifyAllMenus();
			}
		});

		// File -> Close Both
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Close Both");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// close output view
				m_views.close(false);
				if (!m_views.isEmpty()) {
					// update title
					m_shell.setText(Picsi.APP_NAME);
					
					// close input view
					m_views.close(true);
				}
				// notify all menus about the closed input and output
				notifyAllMenus();
			}
		});

		new MenuItem(fileMenu, SWT.SEPARATOR);

		// File -> Save
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Save Output\tCtrl+S");
		item.setAccelerator(SWT.MOD1 + 'S');
		setIcon(item, "images/Save.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (m_views.hasSecondView()) saveFile(false, false);
			}
		});
		
		// File -> Save As...
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Save Output As...");
		setIcon(item, "images/SaveAs.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (m_views.hasSecondView()) saveFile(false, true);
			}
		});
		
		// File -> Save Input As...
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Save Input As...");
		setIcon(item, "images/SaveAs.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) saveFile(true, true);
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		// File -> Edit...
		m_editMenuItem = new MenuItem(fileMenu, SWT.PUSH);
		m_editMenuItem.setText("&Edit...\tCtrl+E");
		m_editMenuItem.setAccelerator(SWT.MOD1 + 'E');
		setIcon(m_editMenuItem, "images/EditDocument.png");
		m_editMenuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					Document doc = m_views.getDocument(true);
					
					if (m_views.hasSecondView()) {
						// ask the user to specify the image to print
						Object[] filterTypes = { "Input", "Output" };
						int o = OptionPane.showOptionDialog("Choose the image to edit", SWT.ICON_INFORMATION, filterTypes, 0);
						if (o < 0) return;
						if (o > 0) {
							doc = m_views.getDocument(false);
							String filename = doc.getFileName();
							if (filename == null) {
								// must be saved before
								if (!saveFile(false, true)) return;
							}
						}
					}
					
					String filename = doc.getFileName();
					if (filename != null) {
						editFile(doc, doc.getImage(), filename);
					}
				}
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		// File -> Print
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("&Print...\tCtrl+P");
		item.setAccelerator(SWT.MOD1 + 'P');
		setIcon(item, "images/PrintDocument.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					View view = m_views.getView(true);
					Document doc = m_views.getDocument(true);
				
					if (m_views.hasSecondView()) {
						// ask the user to specify the image to print
						Object[] filterTypes = { "Input", "Output" };
						int o = OptionPane.showOptionDialog("Choose the image to print", SWT.ICON_INFORMATION, filterTypes, 0);
						if (o < 0) return;
						if (o > 0) {
							view = m_views.getView(false);
							doc = m_views.getDocument(false);
						}
					}
					
					// Ask the user to specify the printer.
					PrintDialog dialog = new PrintDialog(m_shell, SWT.NONE);
					PrinterData printerData = view.getPrinterData();
					if (printerData != null) dialog.setPrinterData(printerData);
					printerData = dialog.open();
					if (printerData == null) return;
					
					Throwable ex = view.print(m_display);
					if (ex != null) {
						showErrorDialog("printing", doc.getFileName(), ex);					
					}
				}
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);

		// File -> Swap I/O
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("Swap &Images\tCtrl+I");
		item.setAccelerator(SWT.MOD1 + 'I');
		setIcon(item, "images/Swap.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (m_views.hasSecondView()) {
					// swap images
					swapViews();
				} else {
					// copy from first to second view
					m_views.copyImages();
				}
			}
		});
		
		new MenuItem(fileMenu, SWT.SEPARATOR);
		
		// File -> Exit
		item = new MenuItem(fileMenu, SWT.PUSH);
		item.setText("E&xit");
		setIcon(item, "images/Exit.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				m_shell.close();
			}
		});
	}
	
	// Image menu
	private void createImageMenu(Menu menuBar) {
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Image");
		
		// user defined image menu items
		m_imageMenu = new ImageMenu(item, m_views, m_mru);

	}

	// Color spaces menu
	private void createColorSpacesMenu(Menu menuBar) {
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Color-Spaces");
		final Menu colorSpacesMenu = new Menu(menuBar.getParent(), SWT.DROP_DOWN);
		item.setMenu(colorSpacesMenu);
		
		// Color space -> Luminance
		item = new MenuItem(colorSpacesMenu, SWT.PUSH);
		item.setText("Luminance");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "Luminance";
				m_views.showImageInFirstView(ColorSpaces.grayscale(), name);
			}
		});

		// Color space -> RGB
		item = new MenuItem(colorSpacesMenu, SWT.CASCADE);
		item.setText("RGB");
		final Menu rgb = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(rgb);

		item = new MenuItem(rgb, SWT.PUSH);
		item.setText("Test Image");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "RGB Test Image";
				m_views.showImageInFirstView(ColorSpaces.rgbTestImage(), name);
			}
		});
		item = new MenuItem(rgb, SWT.PUSH);
		item.setText("White on Top");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "RGB Cube White";
				m_views.showImageInFirstView(ColorSpaces.rgbCube(true), name);
			}
		});
		item = new MenuItem(rgb, SWT.PUSH);
		item.setText("Black on Top");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "RGB Cube Black";
				m_views.showImageInFirstView(ColorSpaces.rgbCube(false), name);
			}
		});

		// Color space -> HSV
		item = new MenuItem(colorSpacesMenu, SWT.CASCADE);
		item.setText("HSV");
		final Menu hsv = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(hsv);

		item = new MenuItem(hsv, SWT.PUSH);
		item.setText("V = 1");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "HSV Top";
				m_views.showImageInFirstView(ColorSpaces.hsv(true), name);
			}
		});
		item = new MenuItem(hsv, SWT.PUSH);
		item.setText("V decreasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "HSV Bottom";
				m_views.showImageInFirstView(ColorSpaces.hsv(false), name);
			}
		});
		item = new MenuItem(hsv, SWT.PUSH);
		item.setText("Color Wheel");
		setIcon(item, "images/ColorWheel.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "HSV Wheel";
				m_views.showImageInFirstView(ColorSpaces.hsvWheel(), name);
			}
		});

		// Color space -> YUV
		item = new MenuItem(colorSpacesMenu, SWT.CASCADE);
		item.setText("YUV");
		final Menu yuv = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(yuv);

		item = new MenuItem(yuv, SWT.PUSH);
		item.setText("Y increasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "YUV Top";
				m_views.showImageInFirstView(ColorSpaces.yuv(true), name);
			}
		});
		item = new MenuItem(yuv, SWT.PUSH);
		item.setText("Y decreasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "YUV Bottom";
				m_views.showImageInFirstView(ColorSpaces.yuv(false), name);
			}
		});

		// Color space -> CIE XYZ
		item = new MenuItem(colorSpacesMenu, SWT.CASCADE);
		item.setText("CIE XYZ");
		final Menu xyz = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(xyz);

		item = new MenuItem(xyz, SWT.PUSH);
		item.setText("Y increasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "XYZ White";
				m_views.showImageInFirstView(ColorSpaces.xyz(true), name);
			}
		});
		item = new MenuItem(xyz, SWT.PUSH);
		item.setText("Y decreasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "XYZ Black";
				m_views.showImageInFirstView(ColorSpaces.xyz(false), name);
			}
		});
		item = new MenuItem(xyz, SWT.PUSH);
		item.setText("sRGB Gamut");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "sRGB Gamut";
				m_views.showImageInFirstView(ColorSpaces.sRGBGamut(), name);
			}
		});

		// Color space -> CIE Lab
		item = new MenuItem(colorSpacesMenu, SWT.CASCADE);
		item.setText("CIE L*a*b*");
		final Menu lab = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(lab);

		item = new MenuItem(lab, SWT.PUSH);
		item.setText("L increasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "Lab White";
				m_views.showImageInFirstView(ColorSpaces.lab(true), name);
			}
		});
		item = new MenuItem(lab, SWT.PUSH);
		item.setText("L decreasing");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "Lab Black";
				m_views.showImageInFirstView(ColorSpaces.lab(false), name);
			}
		});
		item = new MenuItem(lab, SWT.PUSH);
		item.setText("Color Wheel");
		setIcon(item, "images/ColorWheel.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				String name = "Lab Wheel";
				m_views.showImageInFirstView(ColorSpaces.labWheel(), name);
			}
		});
	}
	
	// Tools menu
	private void createToolsMenu(Menu menuBar) {
		enum ME { ColorTable, Histogram, Line, PSNR, FFT };
		
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Tools");
		final Menu windowMenu = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(windowMenu);
		windowMenu.addListener(SWT.Show,  new Listener() {
			@Override
			public void handleEvent(Event e) {
				MenuItem[] menuItems = windowMenu.getItems();
				menuItems[ME.ColorTable.ordinal()].setEnabled(!m_views.isEmpty() && (m_views.getImageType(true) != Picsi.IMAGE_TYPE_RGB ||
						(m_views.hasSecondView() && (m_views.getImageType(false) != Picsi.IMAGE_TYPE_RGB))
				));
				menuItems[ME.ColorTable.ordinal()].setSelection(m_views.hasColorTable());		
				menuItems[ME.Histogram.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Histogram.ordinal()].setSelection(m_views.hasHistogram());		
				menuItems[ME.Line.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Line.ordinal()].setSelection(m_views.hasLineViewer());						
				menuItems[ME.PSNR.ordinal()].setEnabled(!m_views.isEmpty() && m_views.hasSecondView() 
						&& m_views.getImageType(true) == m_views.getImageType(false) 
						&& m_views.getView(true).getImageHeight() == m_views.getView(false).getImageHeight()
						&& m_views.getView(true).getImageWidth() == m_views.getView(false).getImageWidth()
				);
				menuItems[ME.FFT.ordinal()].setEnabled(!m_views.isEmpty() && m_views.getImageType(true) == Picsi.IMAGE_TYPE_GRAY);
				menuItems[ME.FFT.ordinal()].setSelection(m_views.hasFrequencies());		
			}
		});

		// Tools -> Color Table
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("Color &Table...\tCtrl+T");
		item.setAccelerator(SWT.MOD1 + 'T');
		setIcon(item, "images/ColorPalette.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					m_views.toggleColorTable();
				}
			}
		});

		// Tools -> Histogram
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("&Histogram...\tCtrl+H");
		item.setAccelerator(SWT.MOD1 + 'H');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					m_views.toggleHistogram();
				}
			}
		});

		// Tools -> Image Line Viewer
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("Image &Line...\tCtrl+L");
		item.setAccelerator(SWT.MOD1 + 'L');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					m_views.toggleLineViewer();
				}
			}
		});

		// Tools -> Compute PSNR
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("Compute &PSNR...\tCtrl+Q");
		item.setAccelerator(SWT.MOD1 + 'Q');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) {
					ImageData imageData1 = m_views.getImage(true);
					ImageData imageData2 = m_views.getImage(false);
					int imageType = m_views.getImageType(true); assert imageType == m_views.getImageType(false);
					double[] psnr = ImageProcessing.psnr(imageData1, imageData2, imageType);
					MessageBox box = new MessageBox(Picsi.s_shell, SWT.OK);
					
					box.setText("PSNR");
					if (psnr != null) {
						if (imageType == Picsi.IMAGE_TYPE_INDEXED || imageType == Picsi.IMAGE_TYPE_RGB) {
							box.setMessage(Picsi.createMsg("Red: {0}, Green: {1}, Blue: {2}", new Object[] { psnr[0], psnr[1], psnr[2] }));
						} else {
							box.setMessage("PSNR: " + psnr[0]);
						}
					}
					box.open();
				}
			}
		});

		// Tools -> Frequency Editor
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("&Frequency Editor...\tCtrl+F");
		item.setAccelerator(SWT.MOD1 + 'F');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty() && m_views.getImageType(true) == Picsi.IMAGE_TYPE_GRAY) {
					m_views.toggleFrequencies();
				}
			}
		});
	}
	
	// Window menu
	private void createWindowMenu(Menu menuBar) {
		enum ME { AutoZoom, Original, Synch, Sep1, Output, Sep2, Mean };
		
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Window");
		final Menu windowMenu = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(windowMenu);
		windowMenu.addListener(SWT.Show,  new Listener() {
			@Override
			public void handleEvent(Event e) {
				MenuItem[] menuItems = windowMenu.getItems();
				menuItems[ME.AutoZoom.ordinal()].setSelection(m_views.hasAutoZoom());
				menuItems[ME.Original.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Synch.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Synch.ordinal()].setSelection(m_views.isSynchronized());
				menuItems[ME.Output.ordinal()].setEnabled(!m_views.isEmpty());
				menuItems[ME.Output.ordinal()].setSelection(m_views.hasSecondView());
				menuItems[ME.Mean.ordinal()].setSelection(m_views.useMeanColor());
			}
		});

		// Window -> Auto Zoom
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("&Auto Zoom\tCtrl+A");
		item.setAccelerator(SWT.MOD1 + 'A');
		setIcon(item, "images/ZoomToFit.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				MenuItem item = (MenuItem)event.widget;
				m_views.setAutoZoom(item.getSelection());
			}
		});

		// Window -> Original Size
		item = new MenuItem(windowMenu, SWT.PUSH);
		item.setText("Original Si&ze\tCtrl+Z");
		item.setAccelerator(SWT.MOD1 + 'Z');
		setIcon(item, "images/ZoomToWidth.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) m_views.zoom100();
			}
		});

		// Window -> Synchronize
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("&Synchronize");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) m_views.synchronize();
			}
		});

		new MenuItem(windowMenu, SWT.SEPARATOR);

		// Window -> Show Output Pane
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("Show &Output Pane\tCtrl+O");
		item.setAccelerator(SWT.MOD1 + 'O');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (!m_views.isEmpty()) m_views.split();
			}
		});

		new MenuItem(windowMenu, SWT.SEPARATOR);

		// Window -> Mean Color
		item = new MenuItem(windowMenu, SWT.CHECK);
		item.setText("&Mean Color\tCtrl+M");
		item.setAccelerator(SWT.MOD1 + 'M');
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				m_views.toggleMeanAreaColor();
			}
		});
	}
	
	// Help menu
	private void createHelpMenu(Menu menuBar) {
		MenuItem item = new MenuItem(menuBar, SWT.CASCADE);
		item.setText("&Help");
		final Menu helpMenu = new Menu(m_shell, SWT.DROP_DOWN);
		item.setMenu(helpMenu);

		// Help -> About
		item = new MenuItem(helpMenu, SWT.PUSH);
		item.setText("About...");
		setIcon(item, "images/picsi.png");
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				MessageBox box = new MessageBox(m_shell, SWT.OK);
				box.setText("About " + Picsi.APP_NAME);
				box.setMessage(Picsi.APP_COPYRIGHT + "\n\nVersion: " + Picsi.APP_VERSION + "\n\nWeb: " + Picsi.APP_URL);
				box.open();
			}
		});
	}
	
	/**
	 * Save image in a file
	 * @param first true: save image in first view, false: save image in second view
	 * @param saveAs true: let the user choose a file name and offer existing name, false: use existing file name
	 * @return true if successful
	 */
	private boolean saveFile(boolean first, boolean saveAs) {
		final int imageType = m_views.getImageType(first);
		String fileName = m_views.getFileName(first);
		FileInfo si = null;
		
		if (saveAs || fileName == null) {
			si = chooseFileName(imageType, fileName);
			if (si == null) return false;
		}
		
		Cursor cursor = m_shell.getCursor();
		m_shell.setCursor(m_display.getSystemCursor(SWT.CURSOR_WAIT));
		
		try {
			if (si != null) {
				m_views.save(first, si.filename, si.fileType);
			} else {
				assert fileName != null : "doc has no filename";
				m_views.save(first, null, -1);
			}
			return true;
		} catch (Throwable e) {
			showErrorDialog("saving", (si != null) ? si.filename : fileName, e);
			return false;
		} finally {
			m_shell.setCursor(cursor);
			m_views.refresh(false);
		}
	}
	
	public void setIcon(Item item, String resourceName) {
		try {
			item.setImage(new Image(m_display, getClass().getClassLoader().getResource(resourceName).openStream()));			
		} catch(IOException e) {
		}
	}
	
	private void editFile(Document doc, ImageData imageData, String path) {
		if (m_editor == null) {
			m_editor = new Editor(this);
		}
		if (path == null) {
			m_editor.newFile();
		} else {
			if (doc.isBinaryFormat()) {
				m_editor.openBinaryFile(doc, imageData, path);
			} else {
				m_editor.openFile(path);
			}
		}
		m_editor.setVisible(true);
	}
	
	private boolean swapViews() {
		// current output view will become input view
		// save output file
		Document doc = m_views.getDocument(false);
		String filename = doc.getFileName();
		if (filename == null) {
			// must be saved before
			if (!saveFile(false, true)) return false;
			filename = doc.getFileName();
		}
		
		// swap images
		m_views.swapImages();

		return true;
	}
}
