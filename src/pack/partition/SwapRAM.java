package pack.partition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import pack.netlist.B;
import pack.netlist.P;
import pack.util.ErrorLog;
import pack.util.Output;

public class SwapRAM {
	private Part[] result;
	private TreeSet<HardBlockGroup> hardBlockGroups;
	private ArrayList<HashMap<HardBlockGroup,ArrayList<B>>> primitivesInEachHardBlockGroup;

	private double ratio = 0.5;
	
	public SwapRAM(Part[] result){
		this.result = result;
		this.hardBlockGroups = new TreeSet<HardBlockGroup>();
		this.primitivesInEachHardBlockGroup = new ArrayList<HashMap<HardBlockGroup,ArrayList<B>>>();
		
		this.initializePrimitives();
	}
	public void initializePrimitives(){
		for(int i=0;i<2;i++){
			HashMap<HardBlockGroup,ArrayList<B>> tempHashedRam = new HashMap<HardBlockGroup,ArrayList<B>>();
			for(B ram:this.result[i].getBlocks()){
				if(ram.get_type().contains("stratixiv_ram_block")){
					HardBlockGroup hbg = ram.getHardBlockGroup();
					this.hardBlockGroups.add(hbg);
					if(!tempHashedRam.containsKey(hbg)){
						tempHashedRam.put(hbg, new ArrayList<B>());
					}
					tempHashedRam.get(hbg).add(ram);
				}
			}
			this.primitivesInEachHardBlockGroup.add(tempHashedRam);
		}
	}
	public void run(){
		for(HardBlockGroup hbg:this.hardBlockGroups){
			//CASE 1. BOTH PARTS HAVE RAM SLICES OF TYPE HBG
			if(this.primitivesInEachHardBlockGroup.get(0).containsKey(hbg) && this.primitivesInEachHardBlockGroup.get(1).containsKey(hbg)){
				this.bothSidesInHardBlockGroupHavePrimitives(hbg);
			//CASE 2. ONE SIDE HAS RAM SLICES
			}else{
				this.oneSideInHardBlockGroupHasPrimitives(hbg);
			}
		}
	}
	private void bothSidesInHardBlockGroupHavePrimitives(HardBlockGroup hbg){
		Integer positions = hbg.getNumberOfAvailablePlacesInOneBlock();
		ArrayList<B> p0 = this.primitivesInEachHardBlockGroup.get(0).get(hbg);
		ArrayList<B> p1 = this.primitivesInEachHardBlockGroup.get(1).get(hbg);
		//CASE 1. BOTH PARTS HAVE LESS OR EQUAL THAN P SLICES OF TYPE HBG
		if(p0.size() <= positions && p1.size() <= positions){
			ArrayList<B> m0 = new ArrayList<B>(p0);
			this.makeRamMolecule(m0, hbg);
			ArrayList<B> m1 = new ArrayList<B>(p1);
			this.makeRamMolecule(m1, hbg);
		//CASE 2. BOTH PARTS HAVE MORE THAN P SLICES OF TYPE HBG => BALANCE BOTH PARTS
		}else if(p0.size() > positions && p1.size() > positions){
			if(usedFreePositions(p0,p1) > hbg.getNumberOfFreePositions()){
				//IF NOT ENOUGH FREE POSITIONS, THEN BALANCE THE PARTITIONS
				int requiredMoves0 = Math.min(p0.size()%positions, positions - p0.size()%positions);
				int requiredMoves1 = Math.min(p1.size()%positions, positions - p1.size()%positions);
				if(requiredMoves0 < requiredMoves1){
					while(!(p0.size()%positions == 0)){
						if(p0.size()%positions < positions - p0.size()%positions){
							this.move(this.result[0], this.result[1], hbg);
						}else{
							this.move(this.result[1], this.result[0], hbg);
						}
					}
				}else if(requiredMoves0 > requiredMoves1){
					while(!(p1.size()%positions == 0)){
						if(p1.size()%positions < positions - p1.size()%positions){
							this.move(this.result[1], this.result[0], hbg);
						}else{
							this.move(this.result[0], this.result[1], hbg);
						}
					}
				}else{
					while(!(p0.size()%positions == 0)){
						if(p0.size()%positions < positions - p0.size()%positions){
							this.move(this.result[0], this.result[1], hbg);
						}else{
							this.move(this.result[1], this.result[0], hbg);
						}
					}
				}
				if(p0.size() <= positions){
					ArrayList<B> m0 = new ArrayList<B>(p0);
					this.makeRamMolecule(m0, hbg);
				}
				if(p1.size() <= positions){
					ArrayList<B> m1 = new ArrayList<B>(p1);
					this.makeRamMolecule(m1, hbg);
				}
			}
		//CASE 3. ONE OF THE PARTS HAS LESS OR EQUAL THAN P SLICES OF TYPE HBG
		}else{
			int i = (p0.size() <= positions) ? 0 : 1;
			int j = (i+1)%2;
			ArrayList<B> pi = this.primitivesInEachHardBlockGroup.get(i).get(hbg);
			ArrayList<B> pj = this.primitivesInEachHardBlockGroup.get(j).get(hbg);
			//SUBSUBCASE 1. LESS THAN LOWER LIMIT
			if(pi.size() <= positions*this.ratio){
				while(!pi.isEmpty()){
					pi.get(0).move(this.result[i], this.result[j], this.primitivesInEachHardBlockGroup);
				}
			}else{
				//USED FREE POSITIONS = LEFT(positions-pi.size())  +  RIGHT(positions-pj.size()%positions) 
				while((usedFreePositions(pi,pj) > hbg.getNumberOfFreePositions()) && (pj.size() > 0)){
					this.move(this.result[j], this.result[i], hbg);
				}
				
				ArrayList<B> molecule = new ArrayList<B>(pi);
				this.makeRamMolecule(molecule, hbg);
				
				if(pj.size() <= positions){
					ArrayList<B> mj = new ArrayList<B>(pj);
					this.makeRamMolecule(mj, hbg);
				}
			}
		}
	}
	private int usedFreePositions(ArrayList<B> pi, ArrayList<B> pj){
		Integer positions = null;
		for(B b:pi){
			if(positions == null){
				positions = b.getHardBlockGroup().getNumberOfAvailablePlacesInOneBlock();
			}else if(positions != b.getHardBlockGroup().getNumberOfAvailablePlacesInOneBlock()){
				ErrorLog.print("Problem");
			}
		}
		return (positions - pi.size()%positions)%positions + (positions - pj.size()%positions)%positions;
	}
	private void oneSideInHardBlockGroupHasPrimitives(HardBlockGroup hbg){
		Integer positions = hbg.getNumberOfAvailablePlacesInOneBlock();
		int part = this.primitivesInEachHardBlockGroup.get(0).containsKey(hbg) ? 0 : 1;
		if(this.primitivesInEachHardBlockGroup.get(part).get(hbg).size() <= positions){
			ArrayList<B> molecule = new ArrayList<B>(this.primitivesInEachHardBlockGroup.get(part).get(hbg));
			this.makeRamMolecule(molecule, hbg);
		}
	}
	private void makeRamMolecule(ArrayList<B> molecule, HardBlockGroup hbg){
		Integer part = null;
		for(B atom:molecule){
			if(part == null){
				part = atom.get_part();
			}else if(part != atom.get_part()){
				ErrorLog.print(part + " != " + atom.get_part());
			}
		}
		for(B b:molecule){
			this.primitivesInEachHardBlockGroup.get(part).get(hbg).remove(b);
			if(this.primitivesInEachHardBlockGroup.get(part).get(hbg).size() == 0){
				this.primitivesInEachHardBlockGroup.get(part).remove(hbg);
			}
		}
		this.result[part].addRamMolecule(molecule);
		hbg.moleculeGenerated(molecule);
	}
	private void move(Part from, Part to, HardBlockGroup hbg){
		//MOVE BLOCK THAT LEADS TO SMALLEST INCREASE IN TERMINAL COUNT
		B moveBlock = null;
		int minTerminalIncrease = Integer.MAX_VALUE;
		int criticality = Integer.MAX_VALUE;
		HashSet<B> ramSlices = new HashSet<B>(from.getRAMPrimitives(hbg));
		for(B b:ramSlices){
			if(!from.hasBlock(b)){
				ErrorLog.print("Block b is no element of group from");
			}
			if(!b.getHardBlockGroup().equals(hbg)){
				Output.println("The HBG of B is not equal to HBG");
				Output.println("B: " + b.toString() + " B_HBG: " + b.getHardBlockGroup().getIdNumber() + " " + b.getHardBlockGroup().getTame());
				Output.println("HBG: " + hbg.getTame());
				ErrorLog.print("The HBG of B is not equal to HBG");
			}
			int terminalIncrease = from.connections(b) - to.connections(b);
			if(terminalIncrease < minTerminalIncrease){
				minTerminalIncrease = terminalIncrease;
				moveBlock = b;
				int localCriticality = 0;
				for(P p:b.get_data_inputs()){
					localCriticality += p.get_net_weight();
				}
				criticality = localCriticality;
			}else if(terminalIncrease == minTerminalIncrease){
				int localCriticality = 0;
				for(P p:b.get_data_inputs()){
					localCriticality += p.get_net_weight();
				}
				if(localCriticality < criticality){
					moveBlock = b;
					criticality = localCriticality;
				}
			}
		}
		//System.out.println("Pin increase: " + minTerminalIncrease);
		if(from.hasBlock(moveBlock)){
			moveBlock.move(from, to, this.primitivesInEachHardBlockGroup);
		}else if(to.hasBlock(moveBlock)){
			ErrorLog.print("Move block " + moveBlock + " is element of part" + to + " and should be element of part" + from);
		}else{
			ErrorLog.print("Move block " + moveBlock + " is no element of part0 and part1");
		}
	}
}
