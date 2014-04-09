package GUI;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

public class ButtonHandler implements ActionListener{
	 private Hashtable  UIAllObj = new Hashtable();
	 private JLabel lab = new JLabel();
	 private JLabel lab2 = new JLabel();
	 private HandleButtonListenser hj =new HandleButtonListenser();
	 public ButtonHandler(){};
	 
	 public void setButtonObj(Hashtable allObj,JLabel lab1,JLabel lab3){
		 UIAllObj= allObj;
		 lab=lab1;
		 lab2=lab3;
	 }
	@Override
	public void actionPerformed(ActionEvent e) {
		Object obj = e.getSource(); 
		if(obj instanceof JMenuItem){
			String btnName = e.getActionCommand();
	        //for debug purpose                   
	        System.out.println(btnName+ "click");
	        hj.checkAction(btnName,lab,lab2);
		}
			
	}
	
}
