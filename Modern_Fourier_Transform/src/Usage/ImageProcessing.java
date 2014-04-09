package Usage;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.Graphics;
import java.awt.Image;
public class ImageProcessing {
	public static BufferedImage ImagetoBuffered(Image toolkitImage){
			int width = toolkitImage.getWidth(null);
			int height = toolkitImage.getHeight(null);

			// width and height are of the toolkit image
			BufferedImage newImage = new BufferedImage(width, height, 
			      BufferedImage.TYPE_INT_ARGB);
			Graphics g = newImage.getGraphics();
			g.drawImage(toolkitImage, 0, 0, null);
			g.dispose();

		return newImage;
	}
	
	public static BufferedImage ToGray(BufferedImage image){
		BufferedImage image2= new BufferedImage(image.getWidth(),
								image.getHeight(),BufferedImage.TYPE_BYTE_GRAY); 
		
		Graphics g = image2.getGraphics();
		g.drawImage(image,0,0,null);
		g.dispose();
		
		return image2;
	}
	public static BufferedImage ToGray2(BufferedImage image){
		
		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);  
		ColorConvertOp op = new ColorConvertOp(cs, null);  
		BufferedImage image2 = op.filter(image, null);  
		return image2;
	}
	
	public static Image ReSize(BufferedImage image){
		Image dimg = image.getScaledInstance(512, 512,
		        Image.SCALE_SMOOTH);
		return dimg;
		
	}
	 
	public static BufferedImage convert(BufferedImage source) {
	    int width = source.getWidth();
	    int height = source.getHeight();
	    int gray;
	    int[] pixels = new int[width * height];
	    source.getRGB(0, 0, width, height, pixels, 0, width);
	    for (int y = 0; y < height; y++) {
	      int index = y * width;
	      for (int x = 0; x < width; x++) {
	        int rgb = pixels[index];
	        int r = (rgb & 0xff0000) >> 16;
	        int g = (rgb & 0x00ff00) >> 8;
	        int b = rgb & 0x0000ff>>1;
	        gray = (r *30)/ 30;
//	        gray= (int)(0.299*r+0.587*g+0.114*b); // ¥Ñ RGB ¨Ó­pºâ Y ­È
	      //  pixels[index++] = (0xff000000 | (gray << 16) | (gray << 8) | gray);
	        pixels[index++]=gray;
	        
	      }
	    }
	    int max=0;
	    for(int i=0;i<width*height;++i){
	    	if(pixels[i]>max)max=pixels[i];
	    }
	    for(int i=0;i<width*height;++i){
	    	pixels[i]=Math.round((float)((double)pixels[i]/(double)max*250.0));
//System.out.println(pixels[i]);
	    }
	    
	    BufferedImage bimage = new BufferedImage(width, height,
	            BufferedImage.TYPE_BYTE_GRAY);
	       // bimage.setRGB(0, 0, width, height, pixels, 0, width);
	    bimage.getRaster().setPixels(0, 0, width, height, pixels); 
	    //dafsaf
	
	        return bimage;
	}
}
