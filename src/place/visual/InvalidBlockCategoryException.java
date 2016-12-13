package place.visual;

import place.circuit.block.AbstractBlock;

public class InvalidBlockCategoryException extends Exception {
    private static final long serialVersionUID = 6316051260470948362L;

    public InvalidBlockCategoryException(AbstractBlock block) {
        super(block.getCategory().toString());
    }
}
