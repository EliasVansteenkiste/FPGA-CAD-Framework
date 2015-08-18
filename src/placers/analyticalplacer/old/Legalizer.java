package placers.analyticalplacer.old;

import java.util.Collection;
import java.util.Map;

import circuit.Clb;
import circuit.Net;

public interface Legalizer
{
	
	public void legalize(double[] linearX, double[] linearY, Collection<Net> nets, Map<Clb,Integer> indexMap);
	public double calculateBestLegalCost(Collection<Net> nets, Map<Clb,Integer> indexMap);
	public void getAnchorPoints(int[] anchorX, int[] anchorY);
	public void getBestLegal(int[] bestX, int[] bestY);
	
}
