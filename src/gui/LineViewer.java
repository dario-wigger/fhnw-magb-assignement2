package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ScrollBar;

import main.Picsi;

/**
 * Image line (row or column) viewer
 * 
 * @author Christoph Stamm
 *
 */
public class LineViewer extends Dialog {
	private Shell m_shell;
	private Label m_statLbl;
	private ImageData m_imageData;
	private Button m_inputBtn, m_outputBtn;
	private Canvas m_canvas;
	private Composite m_channelsComp;
	private int m_imageType;
	private int m_selectedChannel;
	private int[] m_line = new int[0];
	private boolean m_showRow = true;
	private Color[] colors;
	
    public LineViewer(Shell parent, int style) {
        super(parent, style);
    }

	public LineViewer(Shell parent) {
		super(parent);
	}

    public Object open(TwinView views) {
        Shell parent = getParent();
        colors = new Color[] {
        	Display.getCurrent().getSystemColor(SWT.COLOR_RED),		
        	Display.getCurrent().getSystemColor(SWT.COLOR_GREEN),		
        	Display.getCurrent().getSystemColor(SWT.COLOR_BLUE),
        	Display.getCurrent().getSystemColor(SWT.COLOR_BLACK)
        };
        
        // create shell
        m_shell = new Shell(parent, SWT.RESIZE | SWT.DIALOG_TRIM | SWT.MODELESS);
        m_shell.setSize(299, 252);
        //m_shell.setAlpha(220);
        m_shell.setText("Image Line");
        {
			GridLayout gl = new GridLayout();
			gl.horizontalSpacing = 7;
			gl.verticalSpacing = 7;
			gl.marginHeight = 7;
			gl.marginWidth = 7;
			m_shell.setLayout(gl);
        }
        	
		// set label text
		m_statLbl = new Label(m_shell, SWT.NONE);
		m_statLbl.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_CENTER));

		// create canvas
		m_canvas = new Canvas(m_shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.heightHint = 150;
		data.minimumHeight = 20;
		data.minimumWidth = 256 + 2*m_canvas.getBorderWidth();
		m_canvas.setLayoutData(data);
		m_canvas.getVerticalBar().setMinimum(0);
		m_canvas.getVerticalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				update(views.hasSecondView(), views);
			}
		});
		m_canvas.getHorizontalBar().setMinimum(0);
		m_canvas.getHorizontalBar().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				update(views.hasSecondView(), views);
			}
		});
		if (m_showRow) {
			m_canvas.getHorizontalBar().setVisible(false);
		} else {
			m_canvas.getVerticalBar().setVisible(false);			
		}
		
		// create buttons
		Composite buttonsComp = new Composite(m_shell, SWT.NONE);
		{
			RowLayout rl = new RowLayout();
			rl.marginTop = 0;
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginBottom = 0;
			rl.spacing = 15;
			buttonsComp.setLayout(rl);
		}
		Composite ioComp = new Composite(buttonsComp, SWT.NONE);
		{
			RowLayout rl = new RowLayout();
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginTop = 0;
			rl.marginBottom = 0;
			ioComp.setLayout(rl);
		}
		m_inputBtn = new Button(ioComp, SWT.RADIO);
		m_inputBtn.setText("Input");
		m_inputBtn.setSelection(true);
		m_inputBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		m_outputBtn = new Button(ioComp, SWT.RADIO);
		m_outputBtn.setText("Output");
		m_outputBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		
		Composite rowComp = new Composite(buttonsComp, SWT.NONE);
		{
			RowLayout rl = new RowLayout();
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginTop = 0;
			rl.marginBottom = 0;
			rowComp.setLayout(rl);
		}
		
		Button rowBtn = new Button(rowComp, SWT.RADIO);
		rowBtn.setSelection(true);
		rowBtn.setBounds(0, 0, 90, 16);
		rowBtn.setText("Row");
		rowBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ScrollBar sb = m_canvas.getVerticalBar();
				sb.setVisible(true);
				m_canvas.getHorizontalBar().setVisible(false);
				m_showRow = true;
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		
		Button colBtn = new Button(rowComp, SWT.RADIO);
		colBtn.setBounds(0, 0, 90, 16);
		colBtn.setText("Column");
		colBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				ScrollBar sb = m_canvas.getHorizontalBar();
				sb.setVisible(true);
				m_canvas.getVerticalBar().setVisible(false);
				m_showRow = false;
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		m_channelsComp = new Composite(buttonsComp, SWT.NONE);
		{
			RowLayout rl = new RowLayout();
			rl.marginRight = 0;
			rl.marginLeft = 0;
			rl.marginTop = 0;
			rl.marginBottom = 0;
			m_channelsComp.setLayout(rl);
		}
		Button redBtn = new Button(m_channelsComp, SWT.RADIO);
		redBtn.setText("Red");
		redBtn.setSelection(true);
		redBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		Button greenBtn = new Button(m_channelsComp, SWT.RADIO);
		greenBtn.setText("Green");
		greenBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		Button blueBtn = new Button(m_channelsComp, SWT.RADIO);
		blueBtn.setText("Blue");
		blueBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
				
		// update data
        update(views.hasSecondView(), views);
        
        // draw line
        m_canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				final int margin = 2;
				Rectangle rect = m_canvas.getClientArea();
				GC gc = event.gc;
				int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
				
				for(int i=0; i < m_line.length; i++) {
					if (m_line[i] < min) {
        				min = m_line[i];
        			}
					if (m_line[i] > max) {
        				max = m_line[i];
        			}
				}
				
				// create polyline
				final int dy = Math.max(1, max - min);
				int[] pointArray = new int[m_line.length*2];
				for(int i=0; i < m_line.length; i++) {
					pointArray[2*i] = rect.width*i/m_line.length;
					pointArray[2*i + 1] = rect.height - margin - (rect.height - 2*margin)*(m_line[i] - min)/dy;
				}
				
				// draw polyline
				gc.drawPolyline(pointArray);
				
				gc.dispose();
			}
		});
		
		m_shell.pack();
        m_shell.open();
        Display display = parent.getDisplay();
        while (!m_shell.isDisposed()) {
        	if (!display.readAndDispatch()) display.sleep();
        }
        views.closeLineViewer();
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
		m_imageType = views.getImageType(!m_outputBtn.getSelection());
    	
		// enable channel buttons and get selected channel
		boolean b = m_imageType == Picsi.IMAGE_TYPE_RGB;
		Control[] children = m_channelsComp.getChildren();
		
		for(int i=0; i < children.length; i++) {
			final Control ctrl = children[i];
			ctrl.setEnabled(b);
			if (ctrl instanceof Button) {
				final Button btn = (Button)ctrl;
				if (btn.getSelection()) {
					m_selectedChannel = i;
					m_canvas.setForeground(colors[i]);
				}
			}
		}
		if (!b) m_canvas.setForeground(colors[3]);
		
		// update line
		if (m_showRow) {
			ScrollBar sb = m_canvas.getVerticalBar();
			sb.setMaximum(m_imageData.height + sb.getThumb() - 1);
			
			final int lineNumber = sb.getSelection();

			m_statLbl.setText("Row: " + lineNumber);
			if (m_line.length != m_imageData.width) m_line = new int[m_imageData.width];
			for(int i=0; i < m_imageData.width; i++) {
				final int p = m_imageData.getPixel(i, lineNumber);
				final RGB rgb = m_imageData.palette.getRGB(p);
		    	if (m_imageType == Picsi.IMAGE_TYPE_RGB) {
		    		switch(m_selectedChannel) {
		    		case 0: m_line[i] = rgb.red; break;
		    		case 1: m_line[i] = rgb.green; break;
		    		case 2: m_line[i] = rgb.blue; break;
		    		}
		    	} else {
		    		m_line[i] = rgb.red;
		    	}
			}
		} else {
			ScrollBar sb = m_canvas.getHorizontalBar();
			sb.setMaximum(m_imageData.width + sb.getThumb() - 1);
			
			final int lineNumber = sb.getSelection();

			m_statLbl.setText("Column: " + lineNumber);			
			if (m_line.length != m_imageData.height) m_line = new int[m_imageData.height];
			for(int i=0; i < m_imageData.height; i++) {
				final int p = m_imageData.getPixel(lineNumber, i);
				final RGB rgb = m_imageData.palette.getRGB(p);
		    	if (m_imageType == Picsi.IMAGE_TYPE_RGB) {
		    		switch(m_selectedChannel) {
		    		case 0: m_line[i] = rgb.red; break;
		    		case 1: m_line[i] = rgb.green; break;
		    		case 2: m_line[i] = rgb.blue; break;
		    		}
		    	} else {
		    		m_line[i] = rgb.red;
		    	}
			}
		}

		m_shell.layout();	// necessary to update m_statLbl
		m_canvas.redraw();	// redraws histogram
    }
}
