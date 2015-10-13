#!/usr/bin/env python

import os
from place_and_route import PlaceCaller, RouteCaller


placers = ['wld_sa', 'td_sa', 'wld_ap', 'td_ap', ['wld_ap', 'greedy_wld_sa'], ['wld_ap', 'detailed_wld_sa'], ['td_ap', 'greedy_td_sa'], ['td_ap', 'detailed_td_sa']]
options = [None, None, None, None, ['effort_level=5'], ['rlim=5', 't_multiplier=2', 'effort_level=0.5'], ['effort_level=5'], ['rlim=5', 't_multiplier=2', 'effort_level=0.5']]

base_folder = 'benchmarks/'
architecture = '10fle'


os.chdir('..')
place_caller = PlaceCaller(base_folder)
route_caller = RouteCaller(base_folder)

# Place all designs
for i in range(len(placers)):
    placer = placers[i]
    option = options[i]
    place_caller.place_all(architecture, placer, option)

# Route all designs
for placer in placers:
    route_caller.route_all(architecture, placer)
