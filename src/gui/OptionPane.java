package gui;

import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import main.Picsi;

/**
 * Option pane class (based on SWT MessageBox)
 * http://grepcode.com/file/repo1.maven.org/maven2/org.eclipse.rap/org.eclipse.rap.rwt/1.4.0/org/eclipse/swt/widgets/MessageBox.java
 * 
 * @author Christoph Stamm
 *
 */
public class OptionPane extends Dialog {
	private static final int SPACING = 10;
	private static final int MAX_WIDTH = 640;

	private Shell m_shell;
	private Image m_image; // this image should not be freed because it was allocated by the system
	private String m_message;
	private String m_title;
	private Text m_text;
	private int m_returnCode;
	private String m_returnText;

	public OptionPane(Shell parent, int style) {
		super(parent, style);
	}

	public int open(Object[] options, int defOption, boolean textInput) {
		determineImageFromStyle();
		m_shell = new Shell(getParent(), SWT.TITLE | SWT.BORDER | SWT.APPLICATION_MODAL);
		m_shell.setText(m_title);
		createControls(options, defOption, textInput);
		m_shell.setBounds(computeShellBounds());
		m_shell.pack();
		m_shell.open();
		Display display = m_shell.getDisplay();
		while (!m_shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return m_returnCode;
	}

	private void determineImageFromStyle() {
		m_image = null;
		int style = getStyle();
		int systemImageId = -1;
		if ((style & SWT.ICON_ERROR) != 0) {
			systemImageId = SWT.ICON_ERROR;
		} else if ((style & SWT.ICON_INFORMATION) != 0) {
			systemImageId = SWT.ICON_INFORMATION;
		} else if ((style & SWT.ICON_QUESTION) != 0) {
			systemImageId = SWT.ICON_QUESTION;
		} else if ((style & SWT.ICON_WARNING) != 0) {
			systemImageId = SWT.ICON_WARNING;
		} else if ((style & SWT.ICON_WORKING) != 0) {
			systemImageId = SWT.ICON_WORKING;
		}
		if (systemImageId != -1) {
			m_image = getParent().getDisplay().getSystemImage(systemImageId);
		}
	}

	private Rectangle computeShellBounds() {
		Rectangle result = new Rectangle(0, 0, 0, 0);
		Point preferredSize = m_shell.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle parentSize = getParent().getBounds();
		result.x = (parentSize.width - preferredSize.x) / 2 + parentSize.x;
		result.y = (parentSize.height - preferredSize.y) / 2 + parentSize.y;
		result.width = Math.min(preferredSize.x, MAX_WIDTH);
		result.height = preferredSize.y;
		return result;
	}

	private void createControls(Object[] options, int defOption, boolean textInput) {
		GridLayout gl = new GridLayout(2, false);
		gl.marginRight = gl.marginLeft = SPACING;
		m_shell.setLayout(gl);
		createImage();
		createText();
		if (textInput) createInput();
		createButtons(options, defOption);
	}

	private void createImage() {
		if (m_image != null) {
			Label label = new Label(m_shell, SWT.CENTER);
			GridData data = new GridData(SWT.CENTER, SWT.TOP, false, false);
			data.widthHint = m_image.getBounds().width + SPACING;
			label.setLayoutData(data);
			label.setImage(m_image);
		}
	}

	private void createText() {
		Label textLabel = new Label(m_shell, SWT.WRAP);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int imageWidth = m_image == null ? 0 : m_image.getBounds().width;
		int maxTextWidth = MAX_WIDTH - imageWidth - 2*SPACING;
		int maxLineWidth = getMaxMessageLineWidth();
		if (maxLineWidth > maxTextWidth) {
			data.widthHint = maxTextWidth;
		}
		textLabel.setLayoutData(data);
		textLabel.setText(m_message);
	}

	private void createInput() {
		m_text = new Text(m_shell, SWT.SINGLE | SWT.BORDER);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		data.horizontalSpan = 2;
		m_text.setLayoutData(data);
	}

	private void createButtons(Object[] options, int defOption) {
		assert defOption >= 0 && defOption < options.length;

		Composite buttonArea = new Composite(m_shell, SWT.NONE);
		buttonArea.setLayout(new GridLayout(0, true));
		GridData buttonData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
		buttonData.horizontalSpan = 2;
		buttonArea.setLayoutData(buttonData);
		for(int i=0; i < options.length; i++) {
			createButton(buttonArea, options[i].toString(), i);
		}
		m_shell.setDefaultButton((Button)buttonArea.getChildren()[defOption]);
	}

	private void createButton(Composite parent, String text, int option) {
		((GridLayout) parent.getLayout()).numColumns++;
		Button result = new Button(parent, SWT.PUSH);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		int widthHint = convertTextWidthToPixels(text);
		Point minSize = result.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
		data.widthHint = Math.max(widthHint, minSize.x);
		result.setLayoutData(data);
		result.setText(text);
		result.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				OptionPane.this.m_returnCode = option;
				if (option == 0 && OptionPane.this.m_text != null) {
					// save text input
					OptionPane.this.m_returnText = OptionPane.this.m_text.getText();
				}
				m_shell.close();
			}
		});
	}

	private int getMaxMessageLineWidth() {
		GC gc = new GC(m_shell);
		gc.setFont(m_shell.getFont());
		int result = 0;
		StringTokenizer tokenizer = new StringTokenizer(m_message, "\n");
		
		while (tokenizer.hasMoreTokens()) {
			String line = tokenizer.nextToken();
			int lineWidth = gc.stringExtent(line).x;
			result = Math.max(result, lineWidth);
		}
		return result;
	}

	private int convertTextWidthToPixels(String text) {
		GC gc = new GC(m_shell);
		gc.setFont(m_shell.getFont());
		return gc.stringExtent(text).x + SPACING;
	}

	public static int showOptionDialog(String message, int style, Object[] options, int defOption) {
		OptionPane op = new OptionPane(Picsi.s_shell, style);
		op.m_title = "Options";
		op.m_message = message;
		op.m_returnCode = -1;
		return op.open(options, defOption, false);
	}
	
	public static String showInputDialog(String message) {
		OptionPane op = new OptionPane(Picsi.s_shell, SWT.ICON_QUESTION);
		op.m_title = "Input";
		op.m_message = message;
		op.m_returnCode = -1;
		if (op.open(new Object[]{ SWT.getMessage("SWT_OK"), SWT.getMessage("SWT_Cancel")}, 0, true) == 0) {
			// OK
			return op.m_returnText;
		} else {
			return null;
		}
	}

	public static Integer showIntegerDialog(String message, int defaultValue) {
		try {
			String s = OptionPane.showInputDialog(message + " (" + defaultValue + ")");
			if (s == null) return null;
			if (!s.isEmpty()) return Integer.parseInt(s);
		} catch(NumberFormatException ex) {
			return null;
		}
		return defaultValue;
	}

	public static Float showFloatDialog(String message, float defaultValue) {
		try {
			String s = OptionPane.showInputDialog(message + " (" + defaultValue + ")");
			if (s == null) return null;
			if (!s.isEmpty()) return Float.parseFloat(s);
		} catch(NumberFormatException ex) {
			return null;
		}
		return defaultValue;
	}

	public static Double showDoubleDialog(String message, double defaultValue) {
		try {
			String s = OptionPane.showInputDialog(message + " (" + defaultValue + ")");
			if (s == null) return null;
			if (!s.isEmpty()) return Double.parseDouble(s);
		} catch(NumberFormatException ex) {
			return null;
		}
		return defaultValue;
	}
}
