package Usage;

/**
this clsas provides the methods for the 2D-Fourier transformation
*/

public class FFTTwoD {

	/** size of matrix in M-direction */
	private int M_2P;

	/** size of matrix in N-direction  */
	private int N_2P;
	
	/** largest exponent, so that 2^maxPotM <= (M_2P-1) */
	private int maxPotM;

	/** largest exponent, so that 2^maxPotN <= (N_2P-1) */
	private int maxPotN;


	/** constructor for the use of the 2D transformation routines */ 
	public FFTTwoD(int M, int N) {
		int M_tmp, N_tmp;
   
		// check for power of 2
		M_tmp = M;
		N_tmp = N;
		while( ((M_tmp >>= 1) != 0) && ((N_tmp >>= 1) != 0) ) {
			if( ((M_tmp & 0x1) == 0x1) && (M_tmp>>1 != 0) &&
			    ((N_tmp & 0x1) == 0x1) && (N_tmp>>1 != 0) ) {
				System.out.println("Fourier: both dimensions must be powers of 2");
				System.exit(1);
			} 
		} 
		
		M_2P = M;
		N_2P = N;
		maxPotM=0;
		while ( (1<<maxPotM) <= (M_2P-1) ) {
			maxPotM++;
		}
		maxPotM--;
		maxPotN=0;
		while ( (1<<maxPotN) <= (N_2P-1) ) {
			maxPotN++;
		}
		maxPotN--;
	}



	/** 2D-FFT, based on 1D FFT */
	public Complex[][] fft (Complex[][] G) {
		
		Complex[][] GF; 
		Complex[] v,vf;
		FFTOneD fft1D;
   
		GF = new Complex[M_2P][N_2P];

		// transform first dimension
		fft1D = new FFTOneD(N_2P);
		v = new Complex[N_2P]; 
		for (int m = 0; m < M_2P; m++) {
			for (int n = 0; n < N_2P; n++){
				v[n] = new Complex(G[m][n]);
			}	
			vf = fft1D.fft(v); 
			for (int n = 0; n < N_2P; n++){
				//System.out.println(vf[n].amp());
				GF[m][n] = new Complex(vf[n]);
			}
				
			
		}

		// transform second dimension
		fft1D = new FFTOneD(M_2P);
		v = new Complex[M_2P];
		for (int n = 0; n < N_2P; n++) {
			for (int m = 0; m < M_2P; m++)
				v[m] = new Complex(GF[m][n]);
			vf = fft1D.fft(v); 
			for (int m = 0; m < M_2P; m++){
				GF[m][n] = new Complex(vf[m]);
				
			}
		}

		return GF; 
	}   


	/** 2D-IFFT, based on 1D IFFT */
	public Complex[][] ifft (Complex[][] G) {
  
		Complex[][] GF; 
		Complex[] v,vf;
		FFTOneD ifft1D;
   
		GF = new Complex[M_2P][N_2P];

		// transform first dimension
		ifft1D = new FFTOneD(N_2P);
		v = new Complex[N_2P]; 
		for (int m = 0; m < M_2P; m++) {
			for (int n = 0; n < N_2P; n++)
				v[n] = new Complex(G[m][n]);
			vf = ifft1D.ifft(v); 
			for (int n = 0; n < N_2P; n++)
				GF[m][n] = new Complex(vf[n]);
		}

		// transform second dimension
		ifft1D = new FFTOneD(M_2P);
		v = new Complex[M_2P];
		for (int n = 0; n < N_2P; n++) {
			
			for (int m = 0; m < M_2P; m++)
				v[m] = new Complex(GF[m][n]);
			vf = ifft1D.ifft(v); 
			for (int m = 0; m < M_2P; m++)
				GF[m][n] = new Complex(vf[m]);
		}

		return GF; 
	}   
}