The FPGA CAD Framework
==============================

An FPGA CAD framework focused on rapid prototyping of new CAD algorithms.
The framework is implemented in Java. At this moment packing, placement and routing algorithms are implemented in the framework.


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

<li>
Routing:
<ul>
  <li>Connection-based routing: a fast timing-driven connection based router.</li>
</ul>
</li>

</ul>

Usage
---------------

Some parts of this toolflow require external packages, you can find these in the file "requirements".

To calculate point to point delays, vpr is used (see option --vpr_command). When compiling vpr, the macro PRINT_ARRAYS has to be defined in "place/timing_place_lookup.c".

License
---------------
see license file

Contact us
---------------
The FPGA Placement Framework is released by Ghent University, ELIS department, Hardware and Embedded Systems (HES) group (http://hes.elis.ugent.be).

If you encounter bugs, want to use the FPGA CAD Framework but need support or want to tell us about your results, please contact us. We can be reached at dries.vercruyce[at]ugent.be

Referencing the FPGA Placement Framework
---------------
If you use the FPGA CAD Framework in your work, please reference the following papers in your publications: <br>

Packing:
<b>How preserving circuit design hierarchy during FPGA packing leads to better performance <br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> IEEE Transactions on Computer-Aided Design of Integrated Circuits and Systems}, 37(3), pp. 629-642.</i>

Placement:
<b>Liquid: High quality scalable placement for large heterogeneous FPGAs<br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> Field Programmable Technology (ICFPT), 2017 17th International Conference on. IEEE, 2017</i>

Routing:
<b>CRoute: A fast high-quality timing-driven connection-based FPGA router<br>
Dries Vercruyce, Elias Vansteenkiste and Dirk Stroobandt</b> <br>
<i> accepted for publication</i>

Contributors
---------------
Active Contributors
<ul>
  <li>Dries Vercruyce - <a href="mailto:dries.vercruyce@ugent.be">dries.vercruyce@ugent.be</a></li>
  <li>Yun Zhou - <a href="mailto:yun.zhou@ugent.be">yun.zhou@ugent.be</a></li>
</ul>

Past Contributors
<ul>
  <li>Elias Vansteenkiste - <a href="mailto:Elias.Vansteenkiste@gmail.com">Elias.Vansteenkiste@gmail.com</a></li>
  <li>Arno Messiaen - <a href="mailto:Arno.Messiaen@gmail.com">Arno.Messiaen@gmail.com</a></li>
  <li>Seppe Lenders - <a href="mailto:Seppe.Lenders@gmail.com"> Seppe.Lenders@gmail.com</a></li>
</ul>

Development
---------------
The FPGA CAD Framework is a work in progress, input is welcome.

