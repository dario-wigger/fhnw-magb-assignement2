package gui;
import files.Document;
import files.ImageFiles;
import files.PNM;
import gui.MainWindow.FileInfo;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

/**
 * Swing style editor
 * 
 * @author Christoph Stamm
 *
 */
public class Editor extends JFrame {
	{
		//Set Look & Feel
		try {
			javax.swing.UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	private static final long serialVersionUID = 1L;
	private JTextArea m_textPane;
	private JToolBar jToolBar;
	private JButton jSaveBtn;
	private JButton jSaveAsBtn;
	private JSlider jFontSizeSld;
	private String m_path;
	private boolean m_save;
	private MainWindow m_mainWnd;

	public Editor(MainWindow mainWnd) {
		m_mainWnd = mainWnd;
		{
			jToolBar = new JToolBar();
			jToolBar.setFloatable(false);
			{
				jSaveBtn = new JButton();
				jSaveBtn.setIcon(new ImageIcon(getClass().getClassLoader().getResource("images/Save.png")));
				jSaveBtn.setToolTipText("Save");
				jSaveBtn.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						//System.out.println("jSaveBtn.actionPerformed, event="+evt);
						saveFile(m_path == null);
					}
				});
			}
			{
				jSaveAsBtn = new JButton();
				jSaveAsBtn.setIcon(new ImageIcon(getClass().getClassLoader().getResource("images/SaveAs.png")));
				jSaveAsBtn.setToolTipText("Save As");
				jSaveAsBtn.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent evt) {
						//System.out.println("jSaveAsBtn.actionPerformed, event="+evt);
						saveFile(true);
					}
				});
			}
			{
				// font size slider
				jFontSizeSld = new JSlider(JSlider.HORIZONTAL, 8, 36, 11);
				jFontSizeSld.setMaximumSize(new Dimension(100, 20));
				jFontSizeSld.setToolTipText("Font Size");
				jFontSizeSld.addChangeListener(new ChangeListener() {
					@Override
					public void stateChanged(ChangeEvent evt) {
						//System.out.println("jFontSizeSld.stateChanged, event="+((JSlider)evt.getSource()).getValue());
					    m_textPane.setFont(m_textPane.getFont().deriveFont((float)((JSlider)evt.getSource()).getValue()));

					}					
				});
			}
			jToolBar.add(jSaveBtn);
			jToolBar.add(jSaveAsBtn);
			jToolBar.add(jFontSizeSld);
		}
		m_textPane = new JTextArea();
		m_textPane.setEditable(true);
		m_textPane.setOpaque(true);
		add(new JScrollPane(m_textPane), BorderLayout.CENTER);
		setBounds(100, 100, 700, 500);
		add(jToolBar, BorderLayout.PAGE_START);
	}
	
	public void openFile(String path) {
		m_path = path;
		setTitle(m_path);
		try {
			m_textPane.setText("");
			m_textPane.read(new FileReader(m_path), m_path);
		} catch(IOException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void openBinaryFile(Document doc, ImageData imageData, String path) {
		m_path = path;
		setTitle(m_path);
		m_textPane.setText("");
		m_mainWnd.displayTextOfBinaryImage(doc, imageData, m_textPane);
	}
	
	public void newFile() {
		m_path = null;
		setTitle("New Image");
		m_textPane.setText("");
	}
	
	private void saveFile(boolean createNew) {
		if (m_textPane.getText().isEmpty()) return;
		
		m_save = true;
		
		// determine file type
		final int imageType = PNM.imageType(m_textPane.getText());
		final int fileType = PNM.fileType(imageType);
		
		if (createNew || (m_path != null && fileType != ImageFiles.determinefileType(m_path))) {
			// this Swing thread doesn't have direct access to SWT display thread, hence we need syncExec
			Display.getDefault().syncExec(new Runnable() {
			    public void run() {
					FileInfo si = m_mainWnd.chooseFileName(fileType, imageType);
					if (si != null) {
						m_path = si.filename;
					} else {
						m_save = false;
					}
			    }
			});
		}
		
		if (m_save) {
			try {
				// save image in ASCII format
				m_textPane.write(new FileWriter(m_path));
				
				// read header of written file and check validity
				int retValue = new PNM().checkHeader(m_path);
				if (retValue < 0) {
					if (retValue == -1)
						JOptionPane.showMessageDialog(this, "Invalid header.\nHeader has to be a valid PBM, PGM, or PPM image header (ASCII format).", "Error", JOptionPane.ERROR_MESSAGE);
					else
						JOptionPane.showMessageDialog(this, "Invalid or unknown image creator tag.", "Error", JOptionPane.ERROR_MESSAGE);
						
					return;
				} else if (retValue > 0) {
					// image matrix has been created in checkHeader by a ImageCreator
					// reload file
					m_textPane.setText("");
					m_textPane.read(new FileReader(m_path), m_path);
				}
				
				setTitle(m_path);
				
				// this Swing thread doesn't have direct access to SWT display thread, hence we need syncExec
				Display.getDefault().syncExec(new Runnable() {
				    public void run() {
						m_mainWnd.updateFile(m_path);
				    }
				});
			} catch(IOException e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
	}
}
