The FPGA CAD Framework
==============================

An FPGA CAD framework focused on rapid prototyping of new CAD algorithms.
The framework is implemented in Java. At this moment pack and placement algorithms are implemented in the framework.


What can I do with this tool flow?
---------------




<ul>

<li>
Packing:
<ul>
  <li>Partitioning based packing: A multi-threaded implementation of a packer. Packing happens in two phases. Firstly the partitioningsbased packing and secondly the seed-based packing phase. A second phase is necessary to allow the packer to check architectural constraints. </li>
</ul>
</li>

<li>
Placement:
<ul>
  <li>Simulated Annealing based Placement</li>
  <li>Placement with an iterative analytical solver based placement</li>
  <li>Placement with Liquid: A placer that uses steepest gradient descent moves to place a design</li>
</ul>
</li>

</ul>


Usage
---------------

Some parts of this toolflow require external packages, you can find these in the file "requirements".

To calculate point to point delays, vpr is used (see option --vpr_command). When compiling vpr, the macro PRINT_ARRAYS has to be defined in "place/timing_place_lookup.c".

Architecture files are currently json-formatted and based on the vpr xml-files.


The command line options can be found by calling interfaces.CLI with the option "--help". The most simple invocation of the toolflow is:

java -cp bin:lib/json-simple-1.1.1.jar interfaces.CLI \
benchmarks/k6_frac_N10_mem32K_40nm.xml benchmarks/or1200.blif --placer wld_ap


License
---------------
see license file

Contact us
---------------
The FPGA Placement Framework is released by Ghent University, ELIS department, Hardware and Embedded Systems (HES) group (http://hes.elis.ugent.be).

If you encounter bugs, want to use the FPGA Placement Framework but need support or want to tell us about your results, please contact us. We can be reached at Elias.Vansteenkiste@gmail.com

Referencing the FPGA Placement Framework
---------------
If you use the FPGA Placement Framework in your work, please reference the following paper in your publications: <br>
<b>Liquid: Fast Placement Prototyping Through Steepest Gradient Descent Movement <br>
Elias Vansteenkiste, Seppe Lenders, Dirk Stroobandt</b> <br>
<i> 26th International Conference on Field-Programmable Logic and Applications, FPL2016</i>


You may also refer to one of our others papers if you think it is more related.

Contributors
---------------
Active Contributors
<ul>
  <li>Dries Vercruyce - <a href="mailto:Elias.Vansteenkiste@gmail.com">Elias.Vansteenkiste@gmail.com</a></li>
  <li>Elias Vansteenkiste - <a href="mailto:Elias.Vansteenkiste@gmail.com">Elias.Vansteenkiste@gmail.com</a></li>
</ul>

Past Contributors
<ul>
  <li>Arno Messiaen - <a href="mailto:Arno.Messiaen@gmail.com">Arno.Messiaen@gmail.com</a></li>
  <li>Seppe Lenders - <a href="mailto:Seppe.Lenders@gmail.com"> Seppe.Lenders@gmail.com</a></li>
</ul>

Development
---------------
The FPGA Placement Framework is a work in progress, input is welcome.

