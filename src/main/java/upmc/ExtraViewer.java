package upmc;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import java.awt.GridLayout;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ExtraViewer extends JFrame
{

	private JPanel contentPane;
	private JTextArea txaViewer;
	
	private JPopupMenu popup;
	private JMenuItem menuItemClear;
	
	private enum PopupSource
	{
		ExtraViewArea, NONE
	};
	
	private PopupSource popupSource = PopupSource.NONE;

	class PopupListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				popup.show(e.getComponent(), e.getX(), e.getY());
				if (e.getComponent().equals(txaViewer))
				{
					popupSource = PopupSource.ExtraViewArea;
					// lblStatus.setText("txaTransmit clicked X:" + e.getX() +
					// " Y:" + e.getY());
					// e.getComponent().repaint();
				}
			}
		}
	}
	
	/**
	 * Create the frame.
	 */
	public ExtraViewer()
	{
		setTitle("ExtraView");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(new GridLayout(1, 0, 0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		contentPane.add(scrollPane);
		
		txaViewer = new JTextArea();
		
		popup = new JPopupMenu();
		menuItemClear = new JMenuItem("Clear Messages");

		menuItemClear.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (popupSource == PopupSource.ExtraViewArea)
				{
					txaViewer.setText("");
				}
			}
		});

		popup.add(menuItemClear);

		// Add listener to components that can bring up popup menus.
		MouseListener popupListener = new PopupListener();
		
		txaViewer.addMouseListener(popupListener);
		

		scrollPane.setViewportView(txaViewer);
	}

	public void AppendMessage(String msg)
	{
		txaViewer.append(msg);
		txaViewer.setCaretPosition(txaViewer.getText().length() - 1);
	}
}
