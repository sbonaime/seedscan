/*
 * Copyright 2012, United States Geological Survey or
 * third-party contributors as indicated by the @author tags.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/  >.
 *
 */

package asl.metadata.meta_new;

import java.util.ArrayList;

public class PoleZeroStage extends ResponseStage
{
    private int numberOfZeros;
    private int numberOfPoles;
    private ArrayList<Complex> poles;
    private ArrayList<Complex> zeros;
    private double normalizationConstant;
    private boolean poleAdded = false;
    private boolean zeroAdded = false;
    private boolean normalizationSet = false;

    // constructor(s)
    public PoleZeroStage(int stageNumber, char stageType, double stageGain)
    {
        super(stageNumber, stageType, stageGain);
        poles = new ArrayList<Complex>();
        zeros = new ArrayList<Complex>();
    }

    public void addPole(Complex pole){
      poles.add(pole);
      numberOfPoles++;
      poleAdded = true;
    }
    public void addZero(Complex zero){
      zeros.add(zero);
      numberOfZeros++;
      zeroAdded = true;
    }
    public void setNormalization(double A0){
      this.normalizationConstant = A0;
      normalizationSet = true;
    }
    public double getNormalization(){
      return normalizationConstant;
    }
    public int getNumberOfPoles(){
      return numberOfPoles;
    }
    public int getNumberOfZeros(){
      return numberOfZeros;
    }

    public void print(){
      super.print();
      System.out.println("-This is a pole-zero stage-");
      System.out.format(" Number of Poles=%d\n",getNumberOfPoles());
      for (int j=0; j<getNumberOfPoles(); j++){
        System.out.println(poles.get(j) );
      }
      System.out.format(" Number of Zeros=%d\n",getNumberOfZeros());
      for (int j=0; j<getNumberOfZeros(); j++){
        System.out.println(zeros.get(j) );
      }
      System.out.format(" A0 Normalization=%f\n",getNormalization());
    }

/*  This is just for checking purposes.
 *  It will cycle over a range of frequencies and call 
 *  evalResp(f) to compute the polezero response at f,
 *  then print it out.
**/
    public void printResponse(){
      Complex response;
      for (double x=.01; x<=100; x += .01){ // 100sec -to- 100Hz
        response = evalResp(x);
        System.out.format("%12.4f\t%12.4f\n",x, response.mod() );
      }
    }

/*  Return complex response computed at given freqs[0,...length]
 *  Should really check that length > 0
**/
    public Complex[] getResponse(double[] freqs){
      if (poleAdded && zeroAdded && normalizationSet) {
      // Looks like the polezero info has been loaded ... so continue ...
      }
      else {
        throw new RuntimeException("[ PoleZeroStage-->getResponse Error: PoleZero info does not appear to be loaded! ]");
      }
      if (!(freqs.length > 0)){
        throw new RuntimeException("[ PoleZeroStage-->getResponse Error: Input freqs[] has no zero length! ]");
      }
      Complex[] response = new Complex[freqs.length];
      for (int i=0; i<freqs.length; i++){
        response[i] = evalResp(freqs[i]);
      //System.out.format("%12.4f\t%12.4f\n",freqs[i], response[i].mod() );
      }
      return response;
    }

/*  Evaluate the polezero response at a single frequency, f
 *  Return G(f) = A0 * pole zero expansion
 *  Note that the stage sensitivity Sd is *not* included
**/
    private Complex evalResp(double f){
      Complex numerator   = new Complex(1,0);
      Complex denomenator = new Complex(1,0);
      Complex iv = new Complex(0.0, 2*Math.PI*f);
      Complex Gf;

      for (int j=0; j<numberOfZeros; j++){
        numerator = numerator.times(iv.minus(zeros.get(j))) ;
      }
      for (int j=0; j<numberOfPoles; j++){
        denomenator = denomenator.times(iv.minus(poles.get(j))) ;
      }
      Complex A0 = new Complex(normalizationConstant, 0);
      Gf = A0.times(numerator);
      Gf = Gf.div(denomenator);
      return Gf;
    }

}
