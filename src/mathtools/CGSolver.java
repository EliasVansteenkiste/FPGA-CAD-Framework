package mathtools;

/*
 * Solves a linear system using the conjugate gradient method
 * Uses a Jacobi preconditioner
 */
public class CGSolver 
{
    
    private double[] val;
    private int[] col_ind;
    private int[] row_ptr;
    private double[] vector;
    
    public CGSolver(Crs crs, double[] vector)
    {
        this.val = crs.getVal();
        this.col_ind = crs.getColInd();
        this.row_ptr = crs.getRow_ptr();
        this.vector = vector;
    }
    
    public double[] solve(double epselon)
    {
        int dimensions = this.vector.length;
        double[] x = new double[dimensions];
        double[] r = new double[dimensions];
        double[] s = new double[dimensions];
        double[] m;
        double[] d = new double[dimensions];
        double[] q = new double[dimensions];
        double deltaNew;
        double deltaFirst;
        double deltaOld;
        double alpha;
        double beta;
        double temp;
        
        //Initialize everything
        for(int i = 0; i < dimensions; i++)
        {
            x[i] = 0.0;
            r[i] = this.vector[i];
        }
        m = constructJacobi();
        elementWiseProduct(m, r, s);
        for(int i = 0; i < dimensions; i++)
        {
            d[i] = s[i];
        }
        deltaNew = dotProduct(s, r);
        deltaFirst = deltaNew;
        
        //Main loop of the algorithm
        while(deltaNew > epselon * epselon * deltaFirst)
        {
            sparseMatrixVectorProduct(d, q);
            temp = dotProduct(d,q);
            alpha = deltaNew / temp;
            vectorUpdate(x, d, alpha, x);
            vectorUpdate(r, q, -alpha, r);
            elementWiseProduct(m, r, s);
            deltaOld = deltaNew;
            deltaNew = dotProduct(s, r);
            beta = deltaNew / deltaOld;
            vectorUpdate(s, d, beta, d);
        }
        
        checkCorrect(x);
        return x;
    }
    
    private boolean checkCorrect(double[] solution)
    {
        double[] calculatedB = new double[solution.length];
        sparseMatrixVectorProduct(solution, calculatedB);
        for(int i = 0; i < calculatedB.length; i++)
        {
            if(calculatedB[i] - this.vector[i] < -0.3 || calculatedB[i] - this.vector[i] > 0.3)
            {
                //System.err.println("Solution was not correct");
                //System.err.println("i: " + i + ", calc: " + calculatedB[i] + ", orig: " + vector[i]);
//                for(int j = 0; j < val.length; j++)
//                {
//                    System.err.println("" + j + ": " + val[j]);
//                }
                return false;
            }
        }
        return true;
    }
    
    private double[] constructJacobi()
    {
        int dimensions = this.vector.length;
        double[] jacobi = new double[dimensions];
        int index;
        for(int row = 0; row < dimensions; row++)
        {
            index = this.row_ptr[row];
            while(this.col_ind[index] != row) //We suppose the diagonal elements are always non-zero
            {
                index++;
            }
            jacobi[row] = 1.0 / this.val[index];
        }
        return jacobi;
    }
    
    private void elementWiseProduct(double[] a, double[] b, double[] c)
    {
        for(int i = 0; i < a.length; i++)
        {
            c[i] = a[i] * b[i];
        }
    }
    
    private double dotProduct(double[] a, double[] b)
    {
        double sum = 0.0;
        for(int i = 0; i < a.length; i++)
        {
            sum += a[i] * b[i];
        }
        return sum;
    }
    
    private void vectorUpdate(double[] a, double[] b, double constant, double[] c)
    {
        for(int i = 0; i < a.length; i++)
        {
            c[i] = a[i] + constant * b[i];
        }
    }
    
    private void sparseMatrixVectorProduct(double[] b, double[] c)
    {
        int index = 0;
        for(int row = 0; row < this.vector.length; row++)
        {
            double sum = 0.0;
            int nextRow = row + 1;
            while(index < this.row_ptr[nextRow])
            {
                sum += this.val[index] * b[this.col_ind[index]];
                index++;
            }
            c[row] = sum;
        }
    }
    
}
