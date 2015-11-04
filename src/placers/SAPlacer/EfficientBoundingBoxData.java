package placers.SAPlacer;

import java.util.HashSet;
import java.util.Set;

import circuit.block.AbstractSite;
import circuit.block.GlobalBlock;
import circuit.pin.GlobalPin;



public class EfficientBoundingBoxData
{

    private double weight;
    private GlobalBlock[] blocks;
    private boolean alreadySaved;

    private int min_x;
    private int nb_min_x;
    private int max_x;
    private int nb_max_x;
    private int min_y;
    private int nb_min_y;
    private int max_y;
    private int nb_max_y;
    private int boundingBox;

    private int min_x_old;
    private int nb_min_x_old;
    private int max_x_old;
    private int nb_max_x_old;
    private int min_y_old;
    private int nb_min_y_old;
    private int max_y_old;
    private int nb_max_y_old;
    private int boundingBox_old;


    public EfficientBoundingBoxData(GlobalPin pin)
    {
        Set<GlobalBlock> blockSet = new HashSet<GlobalBlock>();
        blockSet.add(pin.getOwner());

        int numSinks = pin.getNumSinks();
        for(int i = 0; i < numSinks; i++) {
            blockSet.add(pin.getSink(i).getOwner());
        }

        this.blocks = new GlobalBlock[blockSet.size()];
        blockSet.toArray(this.blocks);

        this.setWeightandSize();

        this.boundingBox = -1;
        this.min_x = Integer.MAX_VALUE;
        this.min_y = -1;
        this.max_x = Integer.MAX_VALUE;
        this.max_y = -1;

        this.calculateBoundingBoxFromScratch();
        this.alreadySaved = false;
    }


    public double calculateDeltaCost(GlobalBlock block, AbstractSite newSite) {
        double originalBB = this.boundingBox;

        if((block.getX() == this.min_x && this.nb_min_x == 1 && newSite.getX() > this.min_x)
                || (block.getX() == this.max_x && this.nb_max_x == 1 && newSite.getX() < this.max_x)
                || (block.getY() == this.min_y && this.nb_min_y == 1 && newSite.getY() > this.min_y)
                || (block.getY() == this.max_y && this.nb_max_y == 1 && newSite.getY() < this.max_y)) {

            calculateBoundingBoxFromScratch(block, newSite);

        } else {
            if(newSite.getX() < this.min_x) {
                this.min_x = newSite.getX();
                this.nb_min_x = 1;
            } else if(newSite.getX() == this.min_x && block.getX() != this.min_x) {
                this.nb_min_x++;
            } else if(newSite.getX() > this.min_x && block.getX() == this.min_x) {
                this.nb_min_x--;
            }

            if(newSite.getX() > this.max_x) {
                this.max_x = newSite.getX();
                this.nb_max_x = 1;
            } else if(newSite.getX() == this.max_x && block.getX() != this.max_x) {
                this.nb_max_x++;
            } else if(newSite.getX() < this.max_x && block.getX() == this.max_x) {
                this.nb_max_x--;
            }

            if(newSite.getY() < this.min_y) {
                this.min_y = newSite.getY();
                this.nb_min_y = 1;
            } else if(newSite.getY() == this.min_y && block.getY() != this.min_y) {
                this.nb_min_y++;
            } else if(newSite.getY() > this.min_y && block.getY() == this.min_y) {
                this.nb_min_y--;
            }

            if(newSite.getY() > this.max_y) {
                this.max_y = newSite.getY();
                this.nb_max_y = 1;
            } else if(newSite.getY() == this.max_y && block.getY() != this.max_y) {
                this.nb_max_y++;
            } else if(newSite.getY() < this.max_y && block.getY() == this.max_y) {
                this.nb_max_y--;
            }
        }

        this.boundingBox = (this.max_x - this.min_x + 1) + (this.max_y - this.min_y + 1);

        return this.weight * (this.boundingBox - originalBB);
    }


    public void pushThrough() {
        this.alreadySaved = false;
    }


    public void revert() {
        this.boundingBox = this.boundingBox_old;

        this.min_x = this.min_x_old;
        this.nb_min_x = this.nb_min_x_old;

        this.max_x = this.max_x_old;
        this.nb_max_x = this.nb_max_x_old;

        this.min_y = this.min_y_old;
        this.nb_min_y = this.nb_min_y_old;

        this.max_y = this.max_y_old;
        this.nb_max_y = this.nb_max_y_old;

        this.alreadySaved = false;
    }


    public void saveState() {
        if(!this.alreadySaved) {
            this.min_x_old = this.min_x;
            this.nb_min_x_old = this.nb_min_x;

            this.max_x_old = this.max_x;
            this.nb_max_x_old = this.nb_max_x;

            this.min_y_old = this.min_y;
            this.nb_min_y_old = this.nb_min_y;

            this.max_y_old = this.max_y;
            this.nb_max_y_old = this.nb_max_y;

            this.boundingBox_old = this.boundingBox;
            this.alreadySaved = true;
        }
    }


    public double getNetCost() {
        return this.boundingBox * this.weight;
    }


    public void calculateBoundingBoxFromScratch()  {
        this.calculateBoundingBoxFromScratch(null, null);
    }

    public void calculateBoundingBoxFromScratch(GlobalBlock block, AbstractSite alternativeSite)  {
        this.min_x = Integer.MAX_VALUE;
        this.max_x = -1;
        this.min_y = Integer.MAX_VALUE;
        this.max_y = -1;

        AbstractSite site;
        for(int i = 0; i < this.blocks.length; i++) {
            if(this.blocks[i] == block) {
                site = alternativeSite;
            } else {
                site = this.blocks[i].getSite();
            }


            if(site.getX() < this.min_x) {
                this.min_x = site.getX();
                this.nb_min_x = 1;
            } else if(site.getX() == this.min_x){
                this.nb_min_x++;
            }

            if(site.getX() > this.max_x) {
                this.max_x = site.getX();
                this.nb_max_x = 1;
            } else if(site.getX() == this.max_x) {
                this.nb_max_x++;
            }

            if(site.getY() < this.min_y) {
                this.min_y = site.getY();
                this.nb_min_y = 1;
            } else if(site.getY() == this.min_y) {
                this.nb_min_y++;
            }

            if(site.getY() > this.max_y) {
                this.max_y = site.getY();
                this.nb_max_y = 1;
            } else if(site.getY() == this.max_y) {
                this.nb_max_y++;
            }
        }

        this.boundingBox = (this.max_x - this.min_x + 1) + (this.max_y - this.min_y + 1);
    }


    private void setWeightandSize() {
        int size = this.blocks.length;
        switch (size)  {
            case 1:  this.weight = 1; break;
            case 2:  this.weight = 1; break;
            case 3:  this.weight = 1; break;
            case 4:  this.weight = 1.0828; break;
            case 5:  this.weight = 1.1536; break;
            case 6:  this.weight = 1.2206; break;
            case 7:  this.weight = 1.2823; break;
            case 8:  this.weight = 1.3385; break;
            case 9:  this.weight = 1.3991; break;
            case 10: this.weight = 1.4493; break;
            case 11:
            case 12:
            case 13:
            case 14:
            case 15: this.weight = (size-10) * (1.6899-1.4493) / 5 + 1.4493; break;
            case 16:
            case 17:
            case 18:
            case 19:
            case 20: this.weight = (size-15) * (1.8924-1.6899) / 5 + 1.6899; break;
            case 21:
            case 22:
            case 23:
            case 24:
            case 25: this.weight = (size-20) * (2.0743-1.8924) / 5 + 1.8924; break;
            case 26:
            case 27:
            case 28:
            case 29:
            case 30: this.weight = (size-25) * (2.2334-2.0743) / 5 + 2.0743; break;
            case 31:
            case 32:
            case 33:
            case 34:
            case 35: this.weight = (size-30) * (2.3895-2.2334) / 5 + 2.2334; break;
            case 36:
            case 37:
            case 38:
            case 39:
            case 40: this.weight = (size-35) * (2.5356-2.3895) / 5 + 2.3895; break;
            case 41:
            case 42:
            case 43:
            case 44:
            case 45: this.weight = (size-40) * (2.6625-2.5356) / 5 + 2.5356; break;
            case 46:
            case 47:
            case 48:
            case 49:
            case 50: this.weight = (size-45) * (2.7933-2.6625) / 5 + 2.6625; break;
            default: this.weight = (size-50) * 0.02616 + 2.7933; break;
        }

        this.weight *= 0.01;
    }

}
