package Usage;
import java.util.Arrays;
import java.awt.image.BufferedImage;
import java.math.*;
import java.awt.image.Raster;
import java.awt.Image;
import edu.emory.mathcs.jtransforms.fft.*;
public class Transform {
	public static BufferedImage Fourier(BufferedImage image){
		int [][] pixels= getPixelsArray(image);
		int w=image.getWidth();
		int h=image.getHeight();
		//int[][] Fourier=FFT(pixels,w,h);
		int[][] Fourier=FFT_JTransform(pixels,w,h);
		return ArrayToImage(Fourier,image) ;
	//return ArrayToImage(pixels,image) ;
	}
	public static BufferedImage InvFourier(BufferedImage image){
		int [][] pixels= getPixelsArray(image);
		int w=image.getWidth();
		int h=image.getHeight();
		//int[][] Fourier=FFT(pixels,w,h);
		int[][] Fourier=Inv_FFT_JTransform(pixels,w,h);
		return InvArrayToImage(Fourier,image) ;
	}
	private static int set2Power(int i){
		int num=0;
		while(i>0){
			i=(i>>1);
			++num;
		}
		
		if(num>0) i=(1<<(num-1));
	return i;
	}
	private static int[][] Inv_FFT_JTransform(int[][]pixels,int w,int h){
		double [][] transformArr =new double[w][2*h];
		int max=0;
		int x_max=0;
		int y_max=0;
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]>max) max=pixels[i][j];
			}
		}
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]==max) {
					x_max=i;
					y_max=j;
				}
			}
		}
		for(int i=0;i<w-x_max;++i){
			for(int j=0;j<h-y_max;++j){
				pixels[i][j]=pixels[i+x_max][y_max];
			}
		}
		for(int i=w-max;i<w;++i){
			for(int j=h-y_max;j<h;++j){
				pixels[i][j]=0;
			}
		}
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				transformArr[i][j]=(double)(Math.pow(10,(double)pixels[i][j]));		
			}
		}
		
		DoubleFFT_2D fft=new DoubleFFT_2D(w,h);
		fft.realInverseFull(transformArr,true);
		int[][] transformArr2=new int[w][h];
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				transformArr2[i][j]=Math.round((float)transformArr[i][j]*1000);
			//	 System.out.println(transformArr2[i][j]);
			}
		}
		return transformArr2;
	}
	private static int[][] FFT_JTransform(int[][]pixels,int w,int h){
		double [][] transformArr =new double[w][2*h];
		/*int max=0;
		int x_max=0;
		int y_max=0;
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]>max) max=pixels[i][j];
			}
		}
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]==max) {
					x_max=i;
					y_max=j;
				}
			}
		}
		for(int i=0;i<w-x_max;++i){
			for(int j=0;j<h-y_max;++j){
				pixels[i][j]=pixels[i+x_max][y_max];
			}
		}
		for(int i=w-max;i<w;++i){
			for(int j=h-y_max;j<h;++j){
				pixels[i][j]=0;
			}
		}*/
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				transformArr[i][j]=(double)pixels[i][j];		
			}
		}
		DoubleFFT_2D fft=new DoubleFFT_2D(w,h);
		fft.realForwardFull(transformArr);
		int[][] transformArr2=new int[w][h];
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				transformArr2[i][j]=Math.round((float)transformArr[i][j]*1000);
			//	 System.out.println(transformArr2[i][j]);
			}
		}
		return transformArr2;
	}
	
	
	private static int[][] FFT(int[][]pixels,int w,int h){
		int wprime=set2Power(w);
		int hprime=set2Power(h);
		//System.out.println(hprime+" "+w);
		Complex comp[][]=new Complex[wprime][hprime];
		for(int i=0;i<wprime;++i){
			comp[i]=new Complex[hprime];
		}
		for(int i=0;i<wprime;++i){
			for(int j=0;j<hprime;++j){
		//	System.out.println(pixels[i][j]);
				Complex cp=new Complex();
				cp.setReal(pixels[i][j]);
				comp[i][j]=cp;
			}
		}
		FFTTwoD ft=new FFTTwoD(wprime,hprime);
		comp = ft.fft (comp);
		int[][] Fourier=new int[w][h];
		for(int i=0;i<wprime;++i){
			for(int j=0;j<hprime;++j){
				Fourier[i][j]=Math.round(comp[i][j].amp()*10000);
			}
		}
		return Fourier;
	}
	
	
	private static int[][] DFT(int [][] pixels,int w,int h){
		
		int[][] Fourier=new int[w][h];
		
		for (int u=0;u<w;++u){
			for(int v=0;v<h;++v){
				Complex temp3=new Complex(0,0);
				for(int x=0;x<w;++x){
					Complex temp2=new Complex(0,0);
					for(int y=0;y<h;++y){
						Complex temp1 = Complex.expi(-2.0*Math.PI*y*v/h);
						temp1.multiply(pixels[x][y]);
						temp2.add(temp1);
					}
					Complex temp1 = Complex.expi(-2.0*Math.PI*x*u/h);
					temp2.multiply(temp1);
					temp3.add(temp2);
				}
				temp3.multiply(1/(h*w));
				Fourier[u][v]=(int)Math.round(temp3.amp());
			}
			
		}
		return Fourier;
	}


	
	
	
	private static int[][] getPixelsArray(BufferedImage image){
		int w=image.getWidth();
		int h=image.getHeight();
		//System.out.println(w+" "+h);
		//int [] temp=new int[w*h];
		int [][] pixels = new int[w][h];
		//image.getData().getPixels(0, 0, w, h, temp);
		/*for(int j = 0; j < w; j++) {
		     for (int k = 0; k < h; k++) {
		         pixels[j][k] =image.getData()
		         System.out.println(pixels[j][k]);
		     }
		 }*/
		//int [][] pixels = new int[w][h];
		Raster raster = image.getData();
		 for (int j = 0; j < w; j++) {
		     for (int k = 0; k < h; k++) {
		         pixels[j][k] =raster.getSample(j, k, 0);
		      //   System.out.println(pixels[j][k]);
		     }
		 }
	
		//find maximum
		 /*
		 for (int j = 0; j < w; j++) {
		     for (int k = 0; k < h; k++) {
		         if(pixels[j][k]>max) max=pixels[j][k];
		     }
		 }
		 //
		 for (int j = 0; j < w; j++) {
		     for (int k = 0; k < h; k++) {
		    	 pixels[j][k]= Math.round(pixels[j][k]/max*255);
		     }
		 }
		 */
		 return pixels;
	}
	
	private static BufferedImage  InvArrayToImage(int [][] pixels,BufferedImage image){
		int w=image.getWidth();
		int h=image.getHeight();
		BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_BYTE_GRAY);

		/*int array[]=new int[w*h];
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				array[i*w+j]=pixels[i][j];
			if(array[i*w+j]!=0) array[i*w+j]=(int)Math.log10((double)array[i*w+j]);
			}
		}
	
		Arrays.sort(array);
		float m= (float) (array[w*h/2]/125.0);
		System.out.println(m);
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){ 

				pixels[i][j]=Math.round(pixels[i][j]/m);
			}
		}
		*/
		int [] temp=new int[w*h];
		double [][] temp2=new double[w][h];
		double max=0;
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]<=0) temp2[i][j]=-pixels[i][j];
				else temp2[i][j]=(Math.log10((double)pixels[i][j]));
			//	else temp2[i][j]=(double)pixels[i][j];
				if(temp2[i][j]>max) max=temp2[i][j];
			/*	if(temp2[i][j]>5){
					System.out.println(i+"lss "+j);
				}
			*/
			/*	if(temp2[i][j]<3){
					temp2[i][j]=0;					 
				}
			*/
			}
		}
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(temp2[i][j]<max-2){
					temp2[i][j]=0;					 
				}
			
			}
			
		}
		/*
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				temp2[i][j]=temp2[i][j]*(25000/max);
			}
		}
		*/
		System.out.println(max);
		//System.out.println(temp2[511][10]);
	//shift/
				for(int i=0;i<w/2;++i){
			for(int j=0;j<h/2;++j)
				temp2[i+w/2][j+h/2]=temp2[i][j];
		}
		for(int i=w/2;i<w;++i){
			for(int j=0;j<h/2;++j){
				temp2[i][j]=temp2[i][h-1-j];
			}
		}
		for(int i=0;i<w/2;++i){
			for(int j=h/2;j<h;++j){
				temp2[i][j]=temp2[w-1-i][j];
			}
		}
		for(int i=0;i<w/2;++i){
			for(int j=0;j<h/2;++j){
				temp2[i][j]=temp2[i][h-1-j];
			}
		}
		//shift
	 
		System.out.println(max);
		for (int y = 0; y < h; y++) {
		      int index = y * w;
		      for (int x = 0; x < w; x++) {
		    	temp[index++]=Math.round((float)temp2[x][y]);
		    //	if(Math.round((float)temp2[x][y])>0) System.out.println("msss "+temp2[x][y]);
		    	   // temp[index++]=(int)(Math.log10((double)pixels[x][y]));
		        
		      }
		 }

/*	for(int i=0;i<h;++i){
		for(int j=0;j<h;++j){
			if(temp2[i][j]>10)
				System.out.println(temp2[j][i]);
		}
	}
*/
		dest.getRaster().setPixels(0, 0, w, h,temp);
		
	/*	
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				//dest.setRGB(i, j, pixels[i][j]);
				
			}
		}
		*/
		return dest;
	}

	
	
	private static BufferedImage  ArrayToImage(int [][] pixels,BufferedImage image){
		int w=image.getWidth();
		int h=image.getHeight();
		BufferedImage dest = new BufferedImage(image.getWidth(),image.getHeight(),BufferedImage.TYPE_BYTE_GRAY);

		/*int array[]=new int[w*h];
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				array[i*w+j]=pixels[i][j];
			if(array[i*w+j]!=0) array[i*w+j]=(int)Math.log10((double)array[i*w+j]);
			}
		}
	
		Arrays.sort(array);
		float m= (float) (array[w*h/2]/125.0);
		System.out.println(m);
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){ 

				pixels[i][j]=Math.round(pixels[i][j]/m);
			}
		}
		*/
		int [] temp=new int[w*h];
		double [][] temp2=new double[w][h];
		double max=0;
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(pixels[i][j]<=0) temp2[i][j]=(Math.log10((double)(-pixels[i][j])));
				else temp2[i][j]=(Math.log10((double)pixels[i][j]));
				//else temp2[i][j]=(double)pixels[i][j];
				if(temp2[i][j]>max) max=temp2[i][j];
				/*if(temp2[i][j]>5){
					System.out.println(i+"lss "+j);
				}
				*/			 
			}
		}
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				if(temp2[i][j]<max-2){
					temp2[i][j]=0;
				}
			}
		}
		System.out.print(max);
		
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				temp2[i][j]=temp2[i][j]*(2500/max);
			}
		}
		//System.out.println(temp2[511][10]);
		//shift/
		
		for(int i=0;i<w/2;++i){
			for(int j=0;j<h/2;++j)
				temp2[i+w/2][j+h/2]=temp2[i][j];
		}
		for(int i=w/2;i<w;++i){
			for(int j=0;j<h/2;++j){
				temp2[i][j]=temp2[i][h-1-j];
			}
		}
		for(int i=0;i<w/2;++i){
			for(int j=h/2;j<h;++j){
				temp2[i][j]=temp2[w-1-i][j];
			}
		}
		for(int i=0;i<w/2;++i){
			for(int j=0;j<h/2;++j){
				temp2[i][j]=temp2[i][h-1-j];
			}
		}
		//shift
		for (int y = 0; y < h; y++) {
		      int index = y * w;
		      for (int x = 0; x < w; x++) {
		    	temp[index++]=Math.round((float)temp2[x][y]);
		    	//if(Math.round((float)temp2[x][y])>0) System.out.println("msss "+temp2[x][y]);
		    	   // temp[index++]=(int)(Math.log10((double)pixels[x][y]));
		        
		      }
		 }
/*	for(int i=0;i<h;++i){
		for(int j=0;j<h;++j){
			if(temp2[i][j]>10)
				System.out.println(temp2[j][i]);
		}
	}
*/
		dest.getRaster().setPixels(0, 0, w, h,temp);
		
	/*	
		for(int i=0;i<w;++i){
			for(int j=0;j<h;++j){
				//dest.setRGB(i, j, pixels[i][j]);
				
			}
		}
		*/
		return dest;
	}
}
