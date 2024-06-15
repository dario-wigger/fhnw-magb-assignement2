package gui;

import main.Picsi;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;

/**
 * Line tracker for input view
 * 
 * @author Christoph Stamm
 *
 */
public class LineTracker {
	View m_view = Picsi.getTwinView().getView(true);
	MouseMoveListener m_mouseMoveListener;
	MouseListener m_mouseListener;
	PaintListener m_paintListener;
	int[] m_xy;
	int m_index;
	int m_oldX, m_oldY;
	
	public LineTracker() {
		m_view.setFocus();
		Picsi.getTwinView().m_mainWnd.setEnabledMenu(false);
	}
	
	/**
	 * Starts a line tracker for entering n points in the input view
	 * @param n number of points
	 * @param close shows a closed polygon instead of a polyline
	 * @return array of 2n coordinates (x,y,x,y,...) or null
	 */
	public int[] start(int n, boolean close) {
		Display display = m_view.getDisplay();
		m_index = 0;
		m_xy = new int[2*n];
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);
		
		m_mouseListener = new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {}
			@Override
			public void mouseDown(MouseEvent event) {}  
			@Override
			public void mouseUp(MouseEvent event) {
				m_xy[2*m_index] = event.x;
				m_xy[2*m_index + 1] = event.y;
				m_index++;
			}
		};
		m_view.addMouseListener(m_mouseListener);
		
		m_mouseMoveListener = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent event) {
				if (m_index < n) {
					m_xy[2*m_index] = event.x;
					m_xy[2*m_index + 1] = event.y;
				}
				m_view.redraw();
			}
		};
		m_view.addMouseMoveListener(m_mouseMoveListener);

		m_paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				for (int i=1; i <= m_index; i++) {
					int ii = 2*i;
					event.gc.setLineStyle(SWT.LINE_SOLID);
					event.gc.setForeground(white);
					event.gc.drawLine(m_xy[ii - 2], m_xy[ii - 1], m_xy[ii], m_xy[ii + 1]);
					event.gc.setLineStyle(SWT.LINE_DOT);
					event.gc.setForeground(black);
					event.gc.drawLine(m_xy[ii - 2], m_xy[ii - 1], m_xy[ii], m_xy[ii + 1]);
				}
				if (m_index > 0 && close) {
					event.gc.setLineStyle(SWT.LINE_SOLID);
					event.gc.setForeground(white);
					event.gc.drawLine(m_xy[2*m_index], m_xy[2*m_index + 1], m_xy[0], m_xy[1]);
					event.gc.setLineStyle(SWT.LINE_DOT);
					event.gc.setForeground(black);
					event.gc.drawLine(m_xy[2*m_index], m_xy[2*m_index + 1], m_xy[0], m_xy[1]);
				}
			}
		};
		m_view.addPaintListener(m_paintListener);
		
		try {
			while (m_index < n && !m_view.isDisposed()) 
				if (!display.readAndDispatch()) display.sleep();

			// transform coordinates
			for(int i=0; i < n; i++) {
				int ii = 2*i;
				m_xy[ii]     = m_view.client2ImageX(m_xy[ii]);
				m_xy[ii + 1] = m_view.client2ImageY(m_xy[ii + 1]);
			}
			return (m_view.isDisposed()) ? null : m_xy;
			
		} finally {
			stop();
		}
	}
	
	private void stop() {
		if (!m_view.isDisposed()) {
			m_view.removeMouseListener(m_mouseListener);
			m_view.removeMouseMoveListener(m_mouseMoveListener);
			m_view.removePaintListener(m_paintListener);
			Picsi.getTwinView().m_mainWnd.setEnabledMenu(true);
		}
	}

}