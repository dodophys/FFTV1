package GUI;
import javax.swing.*;

import java.awt.BorderLayout;
import java.util.*;


public class Display extends JFrame {
	//define the menu
	private Hashtable<String, JMenuItem> UiAllObj = new Hashtable<String, JMenuItem>();
	private ButtonHandler bh = new ButtonHandler();
	
	public Display(String frameName){
		super(frameName);
		init();
	}
	
	private void init(){
		setMenu();
		
		setSize(1200,700);
		setVisible(true);
		
	}
	private void setMenu(){
		JPanel jpanl=new JPanel();
		JLabel lab=new JLabel();
		lab.setLocation(0, 0);
		JLabel lab2=new JLabel("",SwingConstants.CENTER);
		lab2.setLocation(600,0);
		//getContentPane().add(lab);
		//getContentPane().add(lab2);
		jpanl.add(lab);
		jpanl.add(lab2);
		getContentPane().add(jpanl,BorderLayout.CENTER);
		
		
		
		JMenuBar jBar=new JMenuBar();
		JMenu mfile =new JMenu("File");
		JMenu mProcess=new JMenu("Process");
		JMenuItem mtOpen=new JMenuItem("Open");
		
		JMenuItem mtFourier=new JMenuItem("FT");
		JMenuItem mInvFourier=new JMenuItem("InvFT");
		
		mtOpen.addActionListener(bh);
		mtFourier.addActionListener(bh);
		mInvFourier.addActionListener(bh);
		 mtOpen.setActionCommand("Open");
		 mtFourier.setActionCommand("FT");
		 mInvFourier.setActionCommand("InvFT");
		UiAllObj.put("Open",mtOpen);
		UiAllObj.put("FT",mtFourier);
		UiAllObj.put("InvFT", mInvFourier);
		jBar.add(mfile);
		jBar.add(mProcess);
		mfile.add(mtOpen); mProcess.add(mtFourier); mProcess.add(mInvFourier);
		setJMenuBar(jBar);		
		bh.setButtonObj(UiAllObj,lab,lab2);
	}
}
