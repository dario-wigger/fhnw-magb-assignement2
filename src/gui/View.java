package gui;
import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.*;

import files.Document;

/**
 * Viewer class
 * 
 * @author Christoph Stamm
 *
 */
public class View extends Canvas {
	private static final int MeanAreaRad = 7; // diameter = 2*MeanAreaRad + 1 = Cursor square
	
	private TwinView m_twins;
	private int m_scrollPosX, m_scrollPosY; // origin of the visible view (= pixel when zoom = 1)
	private Image m_image;					// device dependent image used in painting
	private ImageData m_imageData;			// device independent image used in image processing
	private PrinterData m_printerData;
	private float m_zoom = 1.0f;
	private Clipboard m_clipboard;
	private String m_clipboardText;
	private DragSource m_dragSource;
	private boolean m_firstView;
	
	/**
	 * corresponds to getPixelData() return structure
	 *
	 */
	public enum PixelInfo {
		X, Y, StackIndex, Pixel, RGB, PixelHex, RGBformatted, RGBnormalized 
	}
	
	public View(TwinView compo, boolean first) {
		super(compo, SWT.V_SCROLL | SWT.H_SCROLL | SWT.NO_REDRAW_RESIZE | SWT.NO_BACKGROUND);
		m_twins = compo;
		m_firstView = first;
		
		setBackground(new Color(getDisplay(), 128, 128, 255));
		m_clipboard = new Clipboard(getDisplay());
		
		// Hook resize listener
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				if (m_image != null) m_twins.refresh(true);
			}
		});
		
		// Set up the scroll bars.
		ScrollBar horizontal = getHorizontalBar();
		horizontal.setVisible(true);
		horizontal.setMinimum(0);
		horizontal.setEnabled(false);
		horizontal.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final int x = -((ScrollBar)event.widget).getSelection();
				scrollHorizontally(x);
				m_twins.synchronizeHorizontally(View.this, x);
			}
		});
		ScrollBar vertical = getVerticalBar();
		vertical.setVisible(true);
		vertical.setMinimum(0);
		vertical.setEnabled(false);
		vertical.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final int y = -((ScrollBar)event.widget).getSelection();
				scrollVertically(y);
				m_twins.synchronizeVertically(View.this, y);
				vertical.setIncrement(1); // because scrolling by mouse wheel has been accelerated
			}
		});
		
		addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				if (m_image != null) {
					paint(event);
				} else {
					Rectangle bounds = getBounds();
					event.gc.fillRectangle(0, 0, bounds.width, bounds.height);
				}
			}
		});
		addMouseMoveListener(new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent event) {
				View.this.setFocus();
				if (m_image != null) {
					showPixelInfo(event);
				}
			}
		});
		addMouseListener(new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				Transfer[] transfers = new Transfer[]{ TextTransfer.getInstance() };
				Object[] data = new Object[]{m_clipboardText};
				
				m_clipboard.setContents(data, transfers);
			}

			@Override
			public void mouseDown(MouseEvent e) {
			}
			
			@Override
			public void mouseUp(MouseEvent e) {
			}
			
		});
		addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent event) {
				if (event.stateMask == SWT.CTRL) {
					// zooming
					if (m_imageData != null && !m_twins.hasAutoZoom()) {
						if (event.count < 0) {
							zoom(1.5f, event.x, event.y);
						} else if (event.count > 0) {
							zoom(1/1.5f, event.x, event.y);
						}
					}
				} else {
					// vertical scrolling
					getVerticalBar().setIncrement(Math.abs(event.count*5));
				}
			}
		});		
	}
	
	@Override
	public void dispose() {
		m_clipboard.dispose();
		super.dispose();
	}
	
	public boolean isPortrait() {
		// assume 16:10 screen resolution
		assert m_imageData != null : "m_imageData is null";
		return m_imageData.width*10 < m_imageData.height*16;
	}
	
	public float getZoom() {
		return m_zoom;
	}

	public void setZoom(float f) {
		m_zoom = f;
	}
	
	/**
	 * zoom at the given mouse position (mx, my)
	 * @param f	zoom factor
	 * @param mx mouse position x
	 * @param my mouse position y
	 */
	private void zoom(float f, int mx, int my) {
		assert m_imageData != null;

		final float rx = (mx - m_scrollPosX)/(m_zoom*m_imageData.width);	// should be constant during zooming
		final float ry = (my - m_scrollPosY)/(m_zoom*m_imageData.height);	// "
		
		m_zoom *= f;
		if (m_zoom > 100) {
			m_zoom = 100;
		} else if (m_zoom < 0.01f) {
			m_zoom = 0.01f;
		}
		if (m_zoom > 1) {
			// drawing doesn't work correctly if zoomed width or height is larger than Short.MAX_VALUE
			int s = Math.max(m_imageData.width, m_imageData.height);
			if (m_zoom*s > Short.MAX_VALUE) {
				m_zoom = Short.MAX_VALUE/(float)s;
			}
		}
		updateScrollBars(false); // update scrollbars because of change in zoom factor
		
		// scroll for mouse-centric zooming
		scroll(mx - (int)Math.floor(rx*m_zoom*m_imageData.width), my - (int)Math.floor(ry*m_zoom*m_imageData.height));
		
		// synchronize twin view
		m_twins.synchronizeZoomAndScrollPos(this);
	}
	
	public void computeBestZoomFactor() {
		assert m_imageData != null : "m_imageData is null";

		Rectangle canvasBounds = getClientArea();
		final float xFactor = (float)canvasBounds.width/m_imageData.width;
		final float yFactor = (float)canvasBounds.height/m_imageData.height;
		
		m_zoom = Math.min(xFactor, yFactor);
	}
	
	public PrinterData getPrinterData() {
		return m_printerData;
	}
	
	public int getImageWidth() {
		return m_imageData.width;
	}
	
	public int getImageHeight() {
		return m_imageData.height;
	}
	
	public int getScrollPosX() {
		return m_scrollPosX;
	}
	
	public int getScrollPosY() {
		return m_scrollPosY;
	}
	
	public void setImageData(ImageData imageData) {
		if (m_image != null) m_image.dispose();
		m_imageData = imageData;
		if (m_imageData != null) {
			m_image = new Image(getDisplay(), imageData);
			setDragSource();
		} else {
			m_image = null;
			if (m_dragSource != null ) {
				// delete drag source
				m_dragSource.dispose();
				m_dragSource = null;
			}
		}
		updateScrollBars(true);
	}
	
	public void scroll(int scrollPosX, int scrollPosY) {
		if (m_imageData != null) {
			Rectangle clientRect = getClientArea();
			final int zoomW = zoomedWidth();
			final int zoomH = zoomedHeight();

			// Only scroll if the image is bigger than the canvas.
			if (zoomW > clientRect.width) {
				if (zoomH > clientRect.height) {
					scroll(scrollPosX, scrollPosY, m_scrollPosX, m_scrollPosY, zoomW, zoomH, false);
					getHorizontalBar().setSelection(-scrollPosX); // place scroll bar
					getVerticalBar().setSelection(-scrollPosY); // place scroll bar
					m_scrollPosX = scrollPosX;
					m_scrollPosY = scrollPosY;					
				} else {
					scroll(scrollPosX, m_scrollPosY, m_scrollPosX, m_scrollPosY, zoomW, zoomH, false);
					getHorizontalBar().setSelection(-scrollPosX); // place scroll bar
					m_scrollPosX = scrollPosX;
				}
			} else {
				if (zoomH > clientRect.height) {
					scroll(m_scrollPosX, scrollPosY, m_scrollPosX, m_scrollPosY, zoomW, zoomH, false);
					getVerticalBar().setSelection(-scrollPosY); // place scroll bar
					m_scrollPosY = scrollPosY;
				}
			}
		}
	}
	
	public void scrollHorizontally(int scrollPosX) {
		if (m_imageData != null) {
			Rectangle clientRect = getClientArea();
			final int zoomW = zoomedWidth();
			final int zoomH = zoomedHeight();
			
			if (zoomW > clientRect.width) {
				// Only scroll if the image is bigger than the canvas.
				if (scrollPosX + zoomW < clientRect.width) {
					// Don't scroll past the end of the image.
					scrollPosX = clientRect.width - zoomW;
				}
				scroll(scrollPosX, m_scrollPosY, m_scrollPosX, m_scrollPosY, zoomW, zoomH, false);
				getHorizontalBar().setSelection(-scrollPosX); // place scroll bar
				m_scrollPosX = scrollPosX;
			}
		}
	}
	
	public void scrollVertically(int scrollPosY) {
		if (m_imageData != null) {
			Rectangle clientRect = getClientArea();
			final int zoomW = zoomedWidth();
			final int zoomH = zoomedHeight();
			
			if (zoomH > clientRect.height) {
				// Only scroll if the image is bigger than the canvas.
				if (scrollPosY + zoomH < clientRect.height) {
					// Don't scroll past the end of the image.
					scrollPosY = clientRect.height - zoomH;
				}
				scroll(m_scrollPosX, scrollPosY, m_scrollPosX, m_scrollPosY, zoomW, zoomH, false);
				getVerticalBar().setSelection(-scrollPosY); // place scroll bar
				m_scrollPosY = scrollPosY;
			}
		}
	}

	public void updateScrollBars(boolean redraw) {
		// Set the max and thumb for the image canvas scroll bars.
		ScrollBar horizontal = getHorizontalBar();
		ScrollBar vertical = getVerticalBar();
		Rectangle clientRect = getClientArea();
		final int zoomW = zoomedWidth();
		final int zoomH = zoomedHeight();
		
		if (zoomW > clientRect.width) {
			// The image is wider than the canvas.
			horizontal.setEnabled(true);
			horizontal.setMaximum(zoomW);
			horizontal.setThumb(clientRect.width);
			horizontal.setPageIncrement(clientRect.width);
		} else {
			// The canvas is wider than the image.
			horizontal.setEnabled(false);
			if (m_scrollPosX != 0) {
				// Make sure the image is completely visible.
				m_scrollPosX = 0;
			}
		}
		if (zoomH > clientRect.height) {
			// The image is taller than the canvas.
			vertical.setEnabled(true);
			vertical.setMaximum(zoomH);
			vertical.setThumb(clientRect.height);
			vertical.setPageIncrement(clientRect.height);
		} else {
			// The canvas is taller than the image.
			vertical.setEnabled(false);
			if (m_scrollPosY != 0) {
				// Make sure the image is completely visible.
				m_scrollPosY = 0;
			}
		}
		if (redraw) redraw();
	}
	
	/**
	 * Return pixel information at mouse position (x,y)
	 * @param x
	 * @param y
	 * @param radius used for mean color
	 * @return formatted data according to PixelInfo enumeration
	 */
	public Object[] getPixelInfoAt(int x, int y, int radius) {
		if (m_imageData == null) return null;
		
		x = client2ImageX(x);
		y = client2ImageY(y);
		
		if (x >= 0 && x < m_imageData.width && y >= 0 && y < m_imageData.height) {
			int pixel = m_imageData.getPixel(x, y);
			RGB rgb = m_imageData.palette.getRGB(pixel);
			
			if (radius > 0) {
				// compute average color
				int r = 0, g = 0, b = 0; // don't use rgb for summation, because it results in strange side effect
				int cnt = 0;
				
				for(int v = y - radius; v <= y + radius; v++) {
					if (v >= 0 && v < m_imageData.height) {
						for(int u = x - radius; u <= x + radius; u++) {
							if (u >= 0 && u < m_imageData.width) {
								RGB c = m_imageData.palette.getRGB(m_imageData.getPixel(u, v));
								//System.out.println("(" + u + ',' + v + ") = " + m_imageData.getPixel(u, v));
								r += c.red;
								g += c.green;
								b += c.blue;
								cnt++;
							}
						}
					}
				}
				
				rgb.red = r / cnt;
				rgb.green = g / cnt;
				rgb.blue = b / cnt;
				pixel = m_imageData.palette.getPixel(rgb);
			}
			
			boolean hasAlpha = false;
			int alphaValue = 0;
			if (m_imageData.alphaData != null && m_imageData.alphaData.length > 0) {
				hasAlpha = true;
				alphaValue = m_imageData.getAlpha(x, y);
			}
			String rgbMessageFormat = (hasAlpha) ? "RGBA '{'{0},{1},{2},{3}'}'" : "RGB '{'{0},{1},{2}'}'";
			String rgbNormalizedFormat = (hasAlpha) ? "{0},{1},{2},{3}" : "{0},{1},{2}";
			Object[] rgbArgs = {
					Integer.toString(rgb.red),
					Integer.toString(rgb.green),
					Integer.toString(rgb.blue),
					Integer.toString(alphaValue)
			};
			Object[] rgbNormalizedArgs = {
					String.format("%.2f", rgb.red/255.0),
					String.format("%.2f", rgb.green/255.0),
					String.format("%.2f", rgb.blue/255.0),
					String.format("%.2f", alphaValue/255.0)
			};
			// return data in order defined in PixelInfo enumeration
			Object[] args = {
					x, y, 0, pixel, rgb, 
					Integer.toHexString(pixel),
					Picsi.createMsg(rgbMessageFormat, rgbArgs),
					Picsi.createMsg(rgbNormalizedFormat, rgbNormalizedArgs),
					(pixel == m_imageData.transparentPixel) ? "(transparent)" : ""};
			return args;
		} else {
			return null;
		}
	}
	
	int client2ImageX(int x) {
		return (int)Math.floor((x - m_scrollPosX)/m_zoom);
	}
	
	int client2ImageY(int y) {
		return (int)Math.floor((y - m_scrollPosY)/m_zoom);
	}
	
	int client2Image(int d) {
		return (int)Math.floor(d/m_zoom);
	}
	
	int image2Client(int d) {
		return (int)Math.floor(d*m_zoom);
	}
	
	private void paint(PaintEvent event) {		
		GC gc = event.gc;
		final int zoomW = zoomedWidth();
		final int zoomH = zoomedHeight();

		//System.out.println("w = " + zoomW + ", h = " + zoomH + ", w/h = " + (double)zoomW/zoomH 
		//		+ ", scrollX = " + m_scrollPosX + " (" + m_scrollPosX*100/getHorizontalBar().getMaximum() 
		//		+ " %), scrollY = " + m_scrollPosY + " (" + m_scrollPosY*100/getVerticalBar().getMaximum() + " %)");
		
		/* If any of the background is visible, fill it with the background color. */
		Rectangle bounds = getBounds();
		if (m_imageData.getTransparencyType() != SWT.TRANSPARENCY_NONE) {
			/* If there is any transparency at all, fill the whole background. */
			gc.fillRectangle(0, 0, bounds.width, bounds.height);
		} else {
			/* Otherwise, just fill in the backwards L. */
			if (m_scrollPosX + zoomW < bounds.width) gc.fillRectangle(m_scrollPosX + zoomW, 0, bounds.width - (m_scrollPosX + zoomW), bounds.height);
			if (m_scrollPosY + zoomH < bounds.height) gc.fillRectangle(0, m_scrollPosY + zoomH, m_scrollPosX + zoomW, bounds.height - (m_scrollPosY + zoomH));
		}

		if (m_image != null) {
			/* Draw the image */
			gc.drawImage(
				m_image,
				0,
				0,
				m_imageData.width,
				m_imageData.height,
				m_scrollPosX,
				m_scrollPosY,
				zoomW,
				zoomH);		
		}
	}
	
	public Throwable print(Display display) {
		try {
			Printer printer = new Printer(m_printerData);
			Point screenDPI = display.getDPI();
			Point printerDPI = printer.getDPI();
			int scaleFactor = printerDPI.x/screenDPI.x;
			Rectangle trim = printer.computeTrim(0, 0, 0, 0);
			
			if (printer.startJob(m_twins.getDocument(this).getFileName())) {
				if (printer.startPage()) {
					GC gc = new GC(printer);
					Image printerImage = new Image(printer, m_imageData);
					gc.drawImage(
						printerImage,
						0,
						0,
						m_imageData.width,
						m_imageData.height,
						-trim.x,
						-trim.y,
						scaleFactor*m_imageData.width,
						scaleFactor*m_imageData.height);
					printerImage.dispose();
					gc.dispose();
					printer.endPage();
				}
				printer.endJob();
			}
			printer.dispose();
		} catch (SWTError e) {
			return e;
		}
		return null;
	}
	
	private int zoomedWidth() {
		return (m_imageData == null) ? 0 : image2Client(m_imageData.width);
	}

	private int zoomedHeight() {
		return (m_imageData == null) ? 0 : image2Client(m_imageData.height);
	}

	private void setDragSource() {
		if (m_dragSource == null) {
			// add drag and drop source
			m_dragSource = new DragSource(this, DND.DROP_COPY);
			m_dragSource.addDragListener(new DragSourceListener() {
				@Override
				public void dragStart(DragSourceEvent event) {
					//System.out.println("Start Transfer: " + first);
				}
				@Override
				public void dragSetData(DragSourceEvent event) {
					final Document doc = m_twins.getDocument(m_firstView);
					final String filename = doc.getFileName(); // file name can change, so use current file name
					
					if (FileTransfer.getInstance().isSupportedType(event.dataType) && doc.hasFile() && filename != null) {
						//System.out.println("File Transfer: " + filename);
						event.data = new String[] { filename };
					} else if (ImageTransfer.getInstance().isSupportedType(event.dataType) && doc.hasImage()) {
						//System.out.println("Image Transfer");
						event.data = m_imageData;
					} else {
						event.doit = false;
					}
				}
				@Override
				public void dragFinished(DragSourceEvent event) {
				}			
			});
		}
		
		if (m_dragSource != null) {
			final Document doc = m_twins.getDocument(m_firstView);

			if (doc.hasFile()) {
				//System.out.println("Set FileTransfer");
				m_dragSource.setTransfer(new Transfer[] { FileTransfer.getInstance() });	
			} else {
				//System.out.println("Set ImageTransfer");
				m_dragSource.setTransfer(new Transfer[] { ImageTransfer.getInstance() });				
			}
		}
	}
	
	private void showPixelInfo(MouseEvent event) {
		Object[] data;
		
		if (m_twins.useMeanColor()) {
			data = getPixelInfoAt(event.x,  event.y, MeanAreaRad);
			m_twins.m_mainWnd.showPixelInfo(data, true);
		} else {
			data = getPixelInfoAt(event.x,  event.y, 0);
			m_twins.m_mainWnd.showPixelInfo(data, false);
		}
		if (data != null) m_clipboardText = (String)data[View.PixelInfo.RGBformatted.ordinal()];

	}
}
