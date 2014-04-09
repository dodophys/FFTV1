package GUI;
import Usage.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;
;
public class HandleButtonListenser {
	String fileName;
	public BufferedImage image;
	public BufferedImage image2;
	public HandleButtonListenser(){}
	 public void checkAction(String buttonName,JLabel lab,JLabel lab2){
		 if(buttonName.equals("Open")){
			 Open();
			 Image im=ImageProcessing.ReSize(image);
			 lab.setIcon(new ImageIcon(im));
		 }
		 if(buttonName.equals("FT")){
			 Image im1=ImageProcessing.ReSize(image);
			 BufferedImage im2=ImageProcessing.ImagetoBuffered(im1);
			 image2=Transform.Fourier(im2);
			 Image im=ImageProcessing.ReSize(image2);
			 lab2.setIcon(new ImageIcon(im));
		 }
		 if(buttonName.equals("InvFT")){
			 Image im1=ImageProcessing.ReSize(image);
			 BufferedImage im2=ImageProcessing.ImagetoBuffered(im1);
			 image2=Transform.InvFourier(im2);
			 Image im=ImageProcessing.ReSize(image2);
			 lab2.setIcon(new ImageIcon(im));
		 }
	 }
	 private void Open(){
		 FileDialog fileDialog = new FileDialog( new Frame(), 
				 "Plz choose a file", FileDialog.LOAD );
		 fileDialog.setVisible(true);
		 
		 File inputFile = new File(fileDialog.getDirectory() + fileDialog.getFile());
		 try {
			 image = ImageIO.read(inputFile);
		 } catch (IOException ex) {
			 System.err.println("Could not open ");
		 }
		 image=ImageProcessing.convert(image);
		// image=ImageProcessing.ToGray(image);
	 }
}
