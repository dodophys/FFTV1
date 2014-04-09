package Usage;
import java.math.*;
public class Complex {
	public float _re;
	private float _im;
	public Complex(float re,float im){
		_re=re;
		_im=im;
	}
	public Complex(Complex cp){
		_re=cp._re;
		_im=cp._im;
	}
	public Complex(){
		_re=0;
		_im=0;
	}
	public static Complex expi(double i){
		Complex cp=new Complex((float)Math.cos(i),(float)Math.sin(i));
		return cp;
	}
	public void add(Complex cp){
		_re =_re+cp._re;
		_im=_im+cp._im;
	}
	
	public static Complex add(Complex cp1,Complex cp2){
		Complex cp=new Complex(cp1._re+cp2._re,cp1._im+cp2._im);
		return cp;
	}
	public static Complex sub(Complex cp1,Complex cp2){
		Complex cp=new Complex(cp1._re-cp2._re,cp1._im-cp2._im);
		return cp;
	}
	public static Complex multiply(Complex temp1,double i){
	
		
		Complex cp=new Complex((float)(temp1._re*i),(float)(temp1._im*i));
		return cp;
	}
	
	public static Complex multiply(Complex cp2,Complex cp1){
		Complex cp=new Complex(cp2._re*cp1._re-cp2._im*cp1._im, cp2._re*cp1._im+cp2._im*cp1._re);
		return cp;
	}
	
	public void multiply(float temp1){
		_re=_re*temp1;
		_im=_im*temp1;
	}
	
	public  void multiply(Complex cp1){
		_re=_re*cp1._re-_im*cp1._im;
		_im=_re*cp1._im+_im*cp1._re;
		
	}
	public void setEq(Complex cp){
		_re=cp._re;
		_im=cp._im;
	}
	public float amp(){
		return (float)Math.sqrt(_re*_re+_im*_im);
	}
	public void setReal(float i){
		_re=i;
	}
	public void setIm(float i){
		_im=i;
	}
}
