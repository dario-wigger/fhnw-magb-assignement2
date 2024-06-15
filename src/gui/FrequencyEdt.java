package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;

import imageprocessing.fourier.FFT;
import utils.FrequencyDomain;
import utils.Parallel;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.RowData;

/**
 * Image analyzing tool in the frequency domain
 * 
 * @author Christoph Stamm
 *
 */
public class FrequencyEdt extends Dialog {
	private static final int V = 0;
	private static final int U = 1;
	private static final int Amp = 2;
	private static final int Phi = 3;
	
	private Shell m_shell;
	private Table m_table;
	private Text m_lowPassEdt;
	private Text m_highPassEdt;
	private Button m_shiftedBtn;
	private Button m_equalizedBtn;
	private Composite m_outputComp;
	private FrequencyDomain m_fd;
	private ImageData m_transformed;
	private boolean m_disableUpdate = false;
	
	public FrequencyEdt(Shell parent) {
		super(parent);
	}

	public FrequencyEdt(Shell parent, int style) {
		super(parent, style);
	}

	public Object open(TwinView views) {
		Shell parent = getParent();

		m_shell = new Shell(parent, SWT.RESIZE | SWT.DIALOG_TRIM | SWT.MODELESS);
		m_shell.setSize(459, 300);
		m_shell.setText("Frequency Editor");
        {
			GridLayout gl = new GridLayout(1, false);
			gl.horizontalSpacing = 7;
			gl.verticalSpacing = 7;
			gl.marginHeight = 7;
			gl.marginWidth = 7;
			m_shell.setLayout(gl);
        }
		
		final String[] columnTitles = { "Y Freq", "X Freq", "Amplitude", "Phase" };

		// create virtual table: table items are created on demand using the SetData listener
		m_table = new Table(m_shell, SWT.BORDER | SWT.VIRTUAL );
		{
			final TableEditor editor = new TableEditor(m_table); // overlay above the table
			editor.horizontalAlignment = SWT.LEFT;
			editor.grabHorizontal = true;

			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
			gd.minimumHeight = 300;
			m_table.setLayoutData(gd);
			m_table.setLinesVisible(true);
			m_table.setHeaderVisible(true);
			
			m_table.addListener(SWT.SetData, new Listener() {  
				public void handleEvent(Event event) {  
				    // Read frequency domain object m_fd and update table
				    // Table entries are either in range [0,height)x[0,width) or [-height/2,height/2)x[-width/2,width/2)
				    // Amplitudes are normalized by 1/(width*height), hence amplitude(0,0) is mean image intensity in range [0,255]
					TableItem item = (TableItem)event.item;  
					final int index = event.index;				
			    	final int width = m_fd.getSpectrumWidth();
			    	final int height = m_fd.getSpectrumHeight();
			    	final int size = width*height;

			    	if (m_shiftedBtn.getSelection()) {
			    		final int hD2 = height/2;
			    		final int wD2 = width/2;
			    		final int v2 = index/width - hD2;
						final int v = (v2 < 0) ? height + v2 : v2;
			    		final int u2 = index%width - wD2;
						final int u = (u2 < 0) ? width + u2 : u2;
			    		
						item.setText(V, Integer.toString(v2));
						item.setText(U, Integer.toString(u2));
						item.setText(Amp, Double.toString(m_fd.getAmplitude(u, v)/size));
						item.setText(Phi, Double.toString(m_fd.getPhase(u, v)));
			    	} else {
			    		final int v = index/width;
			    		final int u = index%width;

			    		item.setText(V, Integer.toString(v));
						item.setText(U, Integer.toString(u));
						item.setText(Amp, Double.toString(m_fd.getAmplitude(u, v)/size));
						item.setText(Phi, Double.toString(m_fd.getPhase(u, v)));
			    	}
				}  
			});  

			// table item editor
			m_table.addListener(SWT.MouseDown, new Listener() {
				@Override
				public void handleEvent(Event event) {
					Rectangle clientArea = m_table.getClientArea();
					Point pt = new Point(event.x, event.y);
					int index = m_table.getTopIndex();
					boolean visible = true;
					
					while (visible && index < m_table.getItemCount()) {
						final TableItem item = m_table.getItem(index);
						
						visible = false;
						for (int i = Amp; i < m_table.getColumnCount(); i++) {
							Rectangle rect = item.getBounds(i);
							if (rect.contains(pt)) {
								final int column = i;
								final Text text = new Text(m_table, SWT.NONE); // editable text item
								
								Listener textListener = new Listener() {
									public void handleEvent(final Event e) {
										switch (e.type) {
										case SWT.FocusOut:
											e.detail = SWT.TRAVERSE_RETURN; // makes sure that the new text is set to item
											// FALL THROUGH
										case SWT.Traverse:
											switch (e.detail) {
											case SWT.TRAVERSE_RETURN:
												String curText = item.getText(column);
												item.setText(column, text.getText());
												boolean success = updateFD(item);
												if (success) {
													updateOutput(views, 0);
												} else {
													item.setText(column, curText);
												}
												// FALL THROUGH
											case SWT.TRAVERSE_ESCAPE:
												text.dispose();
												e.doit = false;
											}
											break;
										}
									}
								};
								text.addListener(SWT.FocusOut, textListener);
								text.addListener(SWT.Traverse, textListener);
								editor.setEditor(text, item, i); // set text item above table cell
								text.setText(item.getText(i));
								text.selectAll();
								text.setFocus();
								return;
							}
							if (!visible && rect.intersects(clientArea)) {
								visible = true;
							}
						}
						index++;
					}
				}
			});
		}		
		
		// buttons
		Composite panelComp = new Composite(m_shell, SWT.NONE);
		{
			GridLayout gl = new GridLayout(3, false);
			gl.horizontalSpacing = 10;
			gl.marginWidth = 0;
			gl.marginHeight = 0;
			panelComp.setLayout(gl);
			panelComp.setLayoutData(new GridData(SWT.FILL, SWT.BOTTOM, false, false, 1, 1));
		}

		m_outputComp = new Composite(panelComp, SWT.NONE);
		m_outputComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
		{
			RowLayout rl = new RowLayout();
			rl.wrap = false;
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginTop = 0;
			rl.marginBottom = 0;
			m_outputComp.setLayout(rl);
		}
		
		Label outputLbl = new Label(m_outputComp, SWT.NONE);
		outputLbl.setText("Output     ");
		
		Button noneBtn = new Button(m_outputComp, SWT.RADIO);
		noneBtn.setText("None");
		noneBtn.setSelection(true);
		noneBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) updateOutput(views, 1);
			}
		});
		
		Button powerBtn = new Button(m_outputComp, SWT.RADIO);
		powerBtn.setText("Power");
		powerBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) updateOutput(views, 2);
			}
		});
		
		Button phaseBtn = new Button(m_outputComp, SWT.RADIO);
		phaseBtn.setText("Phase");
		phaseBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) updateOutput(views, 3);
			}
		});
		
		Button transformedBtn = new Button(m_outputComp, SWT.RADIO);
		transformedBtn.setText("Transformed");
		transformedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) updateOutput(views, 4);
			}
		});
		
		m_shiftedBtn = new Button(m_outputComp, SWT.CHECK);
		m_shiftedBtn.setSelection(true);
		m_shiftedBtn.setText("Shifted");
		m_shiftedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateTable();
				updateOutput(views, 0);
			}
		});
		
		m_equalizedBtn = new Button(m_outputComp, SWT.CHECK);
		m_equalizedBtn.setText("Equalized");
		m_equalizedBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				updateOutput(views, 0);
			}
		});
		
		Composite lowPassComp = new Composite(panelComp, SWT.NO_FOCUS);
		{
			RowLayout rl = new RowLayout(SWT.HORIZONTAL);
			rl.wrap = false;
			rl.fill = true;
			rl.spacing = 0;
			rl.marginTop = 0;
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginBottom = 0;
			lowPassComp.setLayout(rl);
			Label label = new Label(lowPassComp, SWT.READ_ONLY);
			label.setText("Low Pass Filter Radius  ");
			m_lowPassEdt = new Text(lowPassComp, SWT.NONE);
			m_lowPassEdt.setLayoutData(new RowData(40, SWT.DEFAULT));
			m_lowPassEdt.addVerifyListener(new VerifyListener() {
				@Override
				public void verifyText(VerifyEvent e) {
					verifyNumericInput(e);
				}
			});
			m_lowPassEdt.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// enter pressed: apply high pass filter to table
					applyFilter(((Text)e.getSource()).getText(), true);
					updateOutput(views, 0);
				}
			});
		}
		
		Composite highPassComp = new Composite(panelComp, SWT.NONE);
		{
			RowLayout rl = new RowLayout(SWT.HORIZONTAL);
			rl.fill = true;
			rl.spacing = 0;
			rl.marginTop = 0;
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginBottom = 0;
			highPassComp.setLayout(rl);
			Label lblHighPassFilter = new Label(highPassComp, SWT.NONE);
			lblHighPassFilter.setText("High Pass Filter Radius  ");
			m_highPassEdt = new Text(highPassComp, SWT.NONE);
			m_highPassEdt.setLayoutData(new RowData(40, SWT.DEFAULT));
			m_highPassEdt.addVerifyListener(new VerifyListener() {
				@Override
				public void verifyText(VerifyEvent e) {
					verifyNumericInput(e);
				}
			});
			m_highPassEdt.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetDefaultSelected(SelectionEvent e) {
					// enter pressed: apply high pass filter to table
					applyFilter(((Text)e.getSource()).getText(), false);
					updateOutput(views, 0);
				}
			});
		}
		
		Button resetBtn = new Button(panelComp, SWT.NONE);
		resetBtn.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1));
		resetBtn.setText("Reset");
		resetBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// reset table entries to input image
				update(views);
			}
		});
		
		for (int i = 0; i < columnTitles.length; i++) {
			TableColumn column = new TableColumn(m_table, SWT.NONE);
			column.setText(columnTitles[i]);
		}

		// adjust layout before inserting data
		m_shell.pack();

		// insert/update data
        update(views);
		
		for (int i = 0; i < columnTitles.length; i++) {
			m_table.getColumn(i).pack();
		}

		// Ask the shell to display its content
		m_shell.open();
		Display display = parent.getDisplay();
		while (!m_shell.isDisposed()) {
			if (!display.readAndDispatch())
				display.sleep();
		}
		views.closeFrequencies();
		return null;
	}

	/**
	 * Close waves editor
	 */
	public void close() {
		if (m_shell != null && !m_shell.isDisposed())
			m_shell.dispose();
	}

	/**
	 * Create frequency domain object of input image and update table
	 * @param views
	 */
    public void update(TwinView views) {
    	if (m_disableUpdate) return;
    	
		// update table
    	Shell shell = views.getShell();
    	Cursor cursor = shell.getCursor();
    	
		shell.setCursor(shell.getDisplay().getSystemCursor(SWT.CURSOR_WAIT));   	
    	
    	final ImageData inData = views.getImage(true);
    	m_fd = FFT.fft2D(inData);
    	updateTable();

    	// update output: recursively calling update is prevented in updateOutput
    	updateOutput(views, 0);
    	m_lowPassEdt.setText("");
    	m_highPassEdt.setText("");
    	shell.setCursor(cursor);			
    }

    /**
     * Clears virtual table and sets new item count
     */
    private void updateTable() {
    	//System.out.println("updateTable");
    	final int width = m_fd.getSpectrumWidth();
    	final int height = m_fd.getSpectrumHeight();
    	final int size = width*height;
    	
    	m_table.clearAll();
    	m_table.setItemCount(size);

    	FrequencyDomain fd = m_fd.clone();
    	m_transformed = FFT.ifft2D(fd);
    }
    
    /**     
     * Read table item and update frequency domain object
     * Amplitudes are normalized by 1/(width*height), hence amplitude(0,0) is mean image intensity in range [0,255]    
     */   
    private boolean updateFD(TableItem item) {
    	final int width = m_fd.getSpectrumWidth();
    	final int height = m_fd.getSpectrumHeight();
    	final int size = width*height;
    	
    	double amp = 0, phi = 0;
    	int v = 0, u = 0;
    	
    	try {
    		amp = Double.parseDouble(item.getText(Amp))*size;  
    		phi = Double.parseDouble(item.getText(Phi));
    		v = Integer.parseInt(item.getText(V));    		
    		u = Integer.parseInt(item.getText(U));    		

    		if (m_shiftedBtn.getSelection()) {				
    			if (v < 0) v += height;				
    			if (u < 0) u += width;    			    		
    		}    		
    		m_fd.setValue(u, v, amp, phi);

    		FrequencyDomain fd = m_fd.clone();
        	m_transformed = FFT.ifft2D(fd);
        	return true;
    	} catch(NumberFormatException ex) {  
    		return false;
    	}    	
    }

    /**
     * Apply low- or high-pass filter to frequency domain object
     * @param s String containing radius
     * @param lowPass
     */
    private void applyFilter(String s, boolean lowPass) {
    	final int sigmoidScale = 1;
		final int sigmoidDomain = 16*sigmoidScale;
    	final int width = m_fd.getSpectrumWidth();
    	final int height = m_fd.getSpectrumHeight();
		final int hD2 = height/2;
		final int wD2 = width/2;

		try {
            double r = Double.parseDouble(s) - ((lowPass) ? sigmoidDomain/2 : -sigmoidDomain/2);
            
            if (r >= 0) {
            	Parallel.For(-hD2, height - hD2, v2 -> {
            		final int v = (v2 < 0) ? v2 + height : v2;
            		
        			for (int u2=-wD2; u2 < width - wD2; u2++) {
        				final int u = (u2 < 0) ? u2 + width : u2;
                		final double dist = Math.hypot(u2, v2);
                		
                		if ((u != 0 || v != 0) && (lowPass && dist > r || !lowPass && dist < r)) {
                			if (lowPass) {
	                			if (dist < r + sigmoidDomain) {
	                				final double t = dist - r - sigmoidDomain/2;
	                				m_fd.multiply(u, v, 1 - sigmoid(t/sigmoidScale));
	                			} else {
	                           		m_fd.setValue(u, v, 0, 0);
	                			}
                			} else {
                				if (dist >= r - sigmoidDomain) {
	                				final double t = dist - r + sigmoidDomain/2;
	                				m_fd.multiply(u, v, sigmoid(t/sigmoidScale));                					
                				} else {
	                           		m_fd.setValue(u, v, 0, 0);
                				}
                			}
                		}
        				
        			}
          		});
            	
            	updateTable();
            }
        }
        catch(NumberFormatException ex) {
        }
    }

	private static double sigmoid(double t) {
		return 0.5 + Math.tanh(t/2)/2;
	}

	/**
     * Update output image
     * @param views
     * @param index 0 if selected radio button has to be determined
     */
    private void updateOutput(TwinView views, int index) {
    	//System.out.println("updateOutput");
    	ImageData outData;
    	
    	if (index == 0) {
			// find selected radio button
			Control[] children = m_outputComp.getChildren();
			
			for(int i=0; i < children.length; i++) {
				final Control ctrl = children[i];
				if (ctrl instanceof Button) {
					final Button btn = (Button)ctrl;
					if (btn.getSelection()) {
						index = i;
						break;
					}
				}
			}
    	}
    	switch(index) {
    	case 1: 
	    	views.close(false); 
    		break;
    	case 2: 
    		outData = FFT.getPowerSpectrum(m_shiftedBtn.getSelection() ? m_fd.swapQuadrants() : m_fd);
			m_disableUpdate = true;
			views.showImageInSecondView(outData);
			m_disableUpdate = false;
			break;
    	case 3:
			outData = FFT.getPhaseSpectrum(m_shiftedBtn.getSelection() ? m_fd.swapQuadrants() : m_fd);
			m_disableUpdate = true;
			views.showImageInSecondView(outData);
			m_disableUpdate = false;
			break;
    	case 4:
			m_disableUpdate = true;
			outData = m_transformed;
			if (m_equalizedBtn.getSelection()) {
				outData = (ImageData)outData.clone();
				//ContrastEnhancement.equalization(outData);
			}
			views.showImageInSecondView(outData);
			m_disableUpdate = false;
			break;
    	}
    }
    
    /**
     * Check text event for numeric input
     * @param e
     */
    private void verifyNumericInput(VerifyEvent e) {
        Text text = (Text)e.getSource();

        // get old text and create new text by using the VerifyEvent.text
        final String oldS = text.getText();
        String newS = oldS.substring(0, e.start) + e.text + oldS.substring(e.end);
        
        if (!newS.isEmpty()) {
	        try {
	            double d = Double.parseDouble(newS);
	            if (d < 0) e.doit = false;
	        }
	        catch(NumberFormatException ex) {
	            e.doit = false;
	        }
        }
    }
    
}
