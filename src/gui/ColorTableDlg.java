package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * Color Table Dialog
 * @author Christoph Stamm
 *
 */
public class ColorTableDlg extends Dialog {
	private static final int MaxColors = 256;
	
	private Shell m_shell;
	private Composite m_labelsComp;
	private Button m_inputBtn, m_outputBtn;
	private ImageData m_imageData;

    public ColorTableDlg(Shell parent, int style) {
        super(parent, style);
    }

	public ColorTableDlg(Shell parent) {
		super(parent);
	}

    public Object open(TwinView views) {
        Shell parent = getParent();
        
        // create shell
        m_shell = new Shell(parent, SWT.RESIZE | SWT.DIALOG_TRIM | SWT.MODELESS);
        m_shell.setText("Color Table");
        {
	        GridLayout gl = new GridLayout(1, false);
	        gl.horizontalSpacing = 7;
	        gl.verticalSpacing = 7;
	        gl.marginHeight = 7;
	        gl.marginWidth = 7;
	        m_shell.setLayout(gl);
        }
        
        m_labelsComp = new Composite(m_shell, SWT.NONE);
        m_labelsComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        {
	        GridLayout gl = new GridLayout(16, true);
	        gl.marginWidth = 0;
	        gl.verticalSpacing = 0;
	        gl.marginHeight = 0;
	        gl.horizontalSpacing = 0;
	        m_labelsComp.setLayout(gl);
        }
        
        // create labels
        for(int i=0; i < MaxColors; i++) {
            Label label = new Label(m_labelsComp, SWT.NONE);
            label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1)); 
        }
        
        Composite buttonsComp = new Composite(m_shell, SWT.NONE);
        buttonsComp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
        buttonsComp.setLayout(new FillLayout(SWT.HORIZONTAL));
        
        m_inputBtn = new Button(buttonsComp, SWT.RADIO);
        m_inputBtn.setText("Input");
        m_inputBtn.setSelection(true);
        m_inputBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
        
        m_outputBtn = new Button(buttonsComp, SWT.RADIO);
        m_outputBtn.setText("Output");
        m_outputBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
        
        // update data
        update(views.hasSecondView(), views);
        
		m_shell.pack();
        m_shell.open();
        Display display = parent.getDisplay();
        while (!m_shell.isDisposed()) {
        	if (!display.readAndDispatch()) display.sleep();
        }
        views.closeColorTable();
        return null;
    }

    public void close() {
    	if (m_shell != null && !m_shell.isDisposed()) m_shell.dispose();
    }
    
    public void update(boolean hasOutput, TwinView views) {
    	// enable input/output buttons
    	if (!hasOutput) {
    		m_inputBtn.setSelection(true);
    		m_outputBtn.setSelection(false);
    	}
		m_outputBtn.setEnabled(hasOutput);
		m_imageData = views.getImage(!m_outputBtn.getSelection());
    	
    	if (m_imageData.palette.isDirect) {
    		Control labels[] = m_labelsComp.getChildren();
    		
    		for(int i = 0; i < labels.length; i++) {
    			Label label = (Label)labels[i];
    			label.setBackground(m_shell.getBackground());
    		}
    	} else {
    		Display display = m_shell.getDisplay();
    		RGB colors[] = m_imageData.palette.getRGBs();
    		int nColors = Math.min(MaxColors, colors.length);
    		Control labels[] = m_labelsComp.getChildren();
    		
    		// set color of used labels
    		for(int i=0; i < nColors; i++) {
    			Label label = (Label)labels[i];
    			Color c = new Color(display, colors[i]);
    			label.setBackground(c);
    			c.dispose();
    		}
    		
    		// set color of unused labels
    		for(int i=nColors; i < MaxColors; i++) {
    			Label label = (Label)labels[i];
    			label.setBackground(m_shell.getBackground());
    		}
    	}
    }
}
