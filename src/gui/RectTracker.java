package gui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;

import main.Picsi;

/**
 * Rectangle tracker for input view
 * 
 * @author Christoph Stamm
 *
 */
public class RectTracker {
	View m_view = Picsi.getTwinView().getView(true);
	MouseMoveListener m_mouseMoveListener;
	MouseListener m_mouseListener;
	PaintListener m_paintListener;
	Rectangle m_rect;
	int m_idx;
	
	public RectTracker() {
		m_view.setFocus();
		Picsi.getTwinView().m_mainWnd.setEnabledMenu(false);
	}
	
	/**
	 * Starts a rectangle tracker for entering a x-y-oriented rectangle
	 * @param w rectangle width
	 * @param h rectangle height
	 * @return rectangle or null
	 */
	public Rectangle start(int w, int h) {
		Display display = m_view.getDisplay();
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);
		m_rect = new Rectangle(0, 0, w, h);
		m_idx = 0;
		
		m_mouseListener = new MouseListener() {
			@Override
			public void mouseDoubleClick(MouseEvent event) {}
			@Override
			public void mouseDown(MouseEvent event) {}  
			@Override
			public void mouseUp(MouseEvent event) {
				if (m_idx == 0) {
					m_rect.x = event.x;
					m_rect.y = event.y;
					m_idx++;
				} else if (m_idx == 1) {
					m_rect.width = event.x - m_rect.x;
					m_rect.height = event.y - m_rect.y;
					m_idx++;
				}
			}
		};
		m_view.addMouseListener(m_mouseListener);
		
		m_mouseMoveListener = new MouseMoveListener() {
			@Override
			public void mouseMove(MouseEvent event) {
				if (m_idx == 0) {
					m_rect.x = event.x;
					m_rect.y = event.y;
				} else if (m_idx == 1) {
					m_rect.width = event.x - m_rect.x;
					m_rect.height = event.y - m_rect.y;
				}
				
				m_view.redraw();
			}
		};
		m_view.addMouseMoveListener(m_mouseMoveListener);

		m_paintListener = new PaintListener() {
			@Override
			public void paintControl(PaintEvent event) {
				event.gc.setLineStyle(SWT.LINE_SOLID);
				event.gc.setForeground(white);
				event.gc.drawRectangle(m_rect);
				event.gc.setLineStyle(SWT.LINE_DOT);
				event.gc.setForeground(black);
				event.gc.drawRectangle(m_rect);
			}
		};
		m_view.addPaintListener(m_paintListener);
		
		try {
			while (m_idx < 2 && !m_view.isDisposed()) 
				if (!display.readAndDispatch()) display.sleep();

			// transform coordinates
			m_rect.x = m_view.client2ImageX(m_rect.x);
			m_rect.y = m_view.client2ImageX(m_rect.y);
			m_rect.width = m_view.client2ImageX(m_rect.width);
			m_rect.height = m_view.client2ImageX(m_rect.height);
			return (m_view.isDisposed()) ? null : m_rect;
			
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
