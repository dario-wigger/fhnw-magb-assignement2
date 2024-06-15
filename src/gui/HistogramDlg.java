package gui;

import java.util.Arrays;
import java.util.IntSummaryStatistics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
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

import imageprocessing.ImageProcessing;
import main.Picsi;

/**
 * Histogram dialog
 * 
 * @author Christoph Stamm
 *
 */
public class HistogramDlg extends Dialog {
	final static int ColorHeight = 20;
	final static int RGBHist = 3;

	private Shell m_shell;
	private Label m_statLbl;
	private ImageData m_imageData;
	private Button m_inputBtn, m_outputBtn, m_logBtn;
	private Canvas m_canvas;
	private Composite m_channelsComp;
	private IntSummaryStatistics m_stat;
	private int[][] m_hist;
	private int m_imageType;
	private int m_selectedChannel;
	private int m_xMin, m_dx;
	
    public HistogramDlg(Shell parent, int style) {
        super(parent, style);
    }

	public HistogramDlg(Shell parent) {
		super(parent);
	}

    public Object open(TwinView views) {
        Shell parent = getParent();

        // create shell
        m_shell = new Shell(parent, SWT.RESIZE | SWT.DIALOG_TRIM | SWT.MODELESS);
        m_shell.setAlpha(220);
        m_shell.setText("Histogram");
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
		m_canvas = new Canvas(m_shell, SWT.RESIZE | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.minimumHeight = 4*ColorHeight;
		data.minimumWidth = 256 + 2*m_canvas.getBorderWidth();
		m_canvas.setLayoutData(data);
		
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

		m_logBtn = new Button(buttonsComp, SWT.CHECK);
		m_logBtn.setText("Logarithmic");
		m_logBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				m_canvas.redraw();	// redraws histogram;
			}
		});
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
		Button rgbBtn = new Button(m_channelsComp, SWT.RADIO);
		rgbBtn.setText("RGB");
		rgbBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				// radio buttons are also unselected
				if (((Button)event.widget).getSelection()) update(views.hasSecondView(), views);
			}
		});
		
		// update data
        update(views.hasSecondView(), views);
        
        // draw histogram
        m_canvas.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				onPaint(event);
			}
		});
        
        m_canvas.addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent e) {
				if (e.x >= m_xMin && e.x < m_xMin + m_hist[0].length*m_dx) {
					final int x = (e.x - m_xMin)/m_dx;
					
					if (m_hist.length == 1) {
				    	// update statistic label
						m_statLbl.setText(Picsi.createMsg("Min = {0}   Max = {1}   @{2} = {3}", 
							new Object[] { m_stat.getMin(), m_stat.getMax(), x, m_hist[0][x] }
						));
					} else {
				    	// update statistic label
						m_statLbl.setText(Picsi.createMsg("Min = {0}   Max = {1}   @{2} = ({3},{4},{5})", 
							new Object[] { m_stat.getMin(), m_stat.getMax(), x, m_hist[0][x], m_hist[1][x], m_hist[2][x] }
						));
					}					
					m_shell.layout();	// necessary to update m_statLbl
				}
			}
        });
		
		m_shell.pack();
        m_shell.open();
        Display display = parent.getDisplay();
        while (!m_shell.isDisposed()) {
        	if (!display.readAndDispatch()) display.sleep();
        }
        views.closeHistogram();
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
				}
			}
		}
		
    	// update histogram and statistic
    	if (m_imageType == Picsi.IMAGE_TYPE_RGB) {
    		if (m_selectedChannel == RGBHist) {
    			m_hist = new int[3][];
    			m_hist[0] = ImageProcessing.histogramRGB(m_imageData, 0);
    			m_hist[1] = ImageProcessing.histogramRGB(m_imageData, 1);
    			m_hist[2] = ImageProcessing.histogramRGB(m_imageData, 2);
    		} else {
    			m_hist = new int[1][];
    			m_hist[0] = ImageProcessing.histogramRGB(m_imageData, m_selectedChannel);
    		}
    	} else {
			m_hist = new int[1][];
    		m_hist[0] = ImageProcessing.histogram(m_imageData, 1 << Math.min(8, m_imageData.depth));
    	}
    	m_stat = Arrays.stream(m_hist[0]).summaryStatistics();
    	
    	// update statistic label
		m_statLbl.setText(Picsi.createMsg("Min = {0}   Max = {1}", 
			new Object[] { m_stat.getMin(), m_stat.getMax()}));

		m_shell.layout();	// necessary to update m_statLbl
		m_canvas.redraw();	// redraws histogram
    }
    
    private Color rgb2color(RGB rgb) {
    	return new Color(m_shell.getDisplay(), rgb);
    }

    private Color gray2color(int p) {
    	return new Color(m_shell.getDisplay(), p, p, p);
    }
    
    private void onPaint(PaintEvent event) {
		// draw histogram
		final Rectangle rect = m_canvas.getClientArea();
		final GC gc = event.gc;
		final int h = rect.height - ColorHeight;
		final int max = Math.max(1, (m_logBtn.getSelection()) ? (int)Math.round(Math.log(m_stat.getMax())) : m_stat.getMax());
		final RGB[] colors = m_imageData.palette.colors;
		int x, pixel;
		Color histColor;
		
		switch(m_imageType) {
		case Picsi.IMAGE_TYPE_BINARY:
			m_dx = rect.width/2;
			m_xMin = (rect.width - 2*m_dx)/2;
			histColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
			{
				// draw histogram
				final int v = (m_logBtn.getSelection()) ? (int)Math.round(Math.log(m_hist[0][0])) : m_hist[0][0];
				final int height = h*v/max;
				gc.setBackground(histColor);
				gc.fillRectangle(m_xMin + m_dx/2, h - height, 2, height);
				// draw first bar
				Color c = rgb2color(colors[0]);
				gc.setBackground(c);
				gc.fillRectangle(m_xMin, h, m_dx, ColorHeight);
				c.dispose();
			}
			{
				// draw histogram
				final int v = (m_logBtn.getSelection()) ? (int)Math.round(Math.log(m_hist[0][1])) : m_hist[0][1];
				final int height = h*v/max;
				gc.setBackground(histColor);
				gc.fillRectangle(m_xMin + m_dx + m_dx/2, h - height, 2, height);
				// draw second bar
				Color c = rgb2color(colors[1]);
				gc.setBackground(c);
				gc.fillRectangle(m_xMin + m_dx, h, m_dx, ColorHeight);
				c.dispose();
			}
			break;
		case Picsi.IMAGE_TYPE_GRAY:
		case Picsi.IMAGE_TYPE_INDEXED:
			m_dx = rect.width/m_hist[0].length;
			m_xMin = (rect.width - m_hist[0].length*m_dx)/2;
			x = m_xMin;
			pixel = 0;
			histColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
			
			for(int v: m_hist[0]) {
				// draw histogram
				if (m_logBtn.getSelection()) v = (int)Math.round(Math.log(v));
				final int height = h*v/max;
				gc.setBackground(histColor);
				gc.fillRectangle(x, h - height, m_dx, height);
				// draw bar
				Color c = (colors != null) ? rgb2color(colors[pixel]) : gray2color(pixel);
				gc.setBackground(c);
				gc.fillRectangle(x, h, m_dx, ColorHeight);
				c.dispose();
				x += m_dx;
				pixel++;
			}
			break;
		case Picsi.IMAGE_TYPE_RGB:
			m_dx = rect.width/m_hist[0].length;
			m_xMin = (rect.width - m_hist[0].length*m_dx)/2;
			x = m_xMin;
			pixel = 0;
			if (m_selectedChannel < RGBHist) {
				// R or G or B
				switch(m_selectedChannel) {
				case 0: histColor = Display.getCurrent().getSystemColor(SWT.COLOR_RED); break;
				case 1: histColor = Display.getCurrent().getSystemColor(SWT.COLOR_GREEN); break;
				case 2: histColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLUE); break;
				default: histColor = Display.getCurrent().getSystemColor(SWT.COLOR_BLACK); break;
				}
				
				for(int v: m_hist[0]) {
					// draw histogram
					if (m_logBtn.getSelection()) v = (int)Math.round(Math.log(v));
					final int height = h*v/max;
					gc.setBackground(histColor);
					gc.fillRectangle(x, h - height, m_dx, height);
					// draw bar
					Color c = new Color(m_shell.getDisplay(), (m_selectedChannel == 0) ? pixel : 0, (m_selectedChannel == 1) ? pixel : 0, (m_selectedChannel == 2) ? pixel : 0);
					gc.setBackground(c);
					gc.fillRectangle(x, h, m_dx, ColorHeight);
					c.dispose();
					x += m_dx;
					pixel++;
				}
			} else {
				// RGB
				for(int i = 0; i < m_hist[0].length; i++) {
					// draw histogram
					int vR = m_hist[0][i];
					int vG = m_hist[1][i];
					int vB = m_hist[2][i];
					
					if (m_logBtn.getSelection()) vR = (int)Math.round(Math.log(vR));
					if (m_logBtn.getSelection()) vG = (int)Math.round(Math.log(vG));
					if (m_logBtn.getSelection()) vB = (int)Math.round(Math.log(vB));

					final int heightR = h*vR/max;
					final int heightG = h*vG/max;
					final int heightB = h*vB/max;
					int heightW;
					
					if (vR >= vG && vR >= vB) {
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
						gc.fillRectangle(x, h - heightR, m_dx, heightR);
						if (vG >= vB) {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
							gc.fillRectangle(x, h - heightG, m_dx, heightG);
							heightW = heightB;
						} else {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
							gc.fillRectangle(x, h - heightB, m_dx, heightB);
							heightW = heightG;
						}
					} else if (vG >= vR && vG >= vB) {
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
						gc.fillRectangle(x, h - heightG, m_dx, heightG);
						if (vR >= vB) {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
							gc.fillRectangle(x, h - heightR, m_dx, heightR);
							heightW = heightB;
						} else {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
							gc.fillRectangle(x, h - heightB, m_dx, heightB);
							heightW = heightR;
						}
					} else {
						gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
						gc.fillRectangle(x, h - heightB, m_dx, heightB);
						if (vR >= vG) {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
							gc.fillRectangle(x, h - heightR, m_dx, heightR);
							heightW = heightG;
						} else {
							gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_CYAN));
							gc.fillRectangle(x, h - heightG, m_dx, heightG);
							heightW = heightR;
						}
					}
					gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
					gc.fillRectangle(x, h - heightW, m_dx, heightW);
					
					// draw bar
					Color c = gray2color(pixel);
					gc.setBackground(c);
					gc.fillRectangle(x, h, m_dx, ColorHeight);
					c.dispose();
					x += m_dx;
					pixel++;
				}
			}
			break;
		}
		gc.dispose();
	}
}
