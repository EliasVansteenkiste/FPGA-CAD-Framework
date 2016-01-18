#!/usr/bin/env python

import json
import sys
import os
import itertools
from collections import OrderedDict
import subprocess
import csv
import re
import math

base_commands = {
    'vpr': [
        './vpr',
        '{architecture_file}',
        '{circuit}',
        '--blif_file', '{blif_file}',
        '--net_file', '{net_file}',
        '--place_file', '{place_file}',
        '--route_file', '{route_file}',
        '--fix_pins', 'random',
        '--place'
    ],
    'java': [
        'java',
        '-cp',
        'bin',
        'interfaces.CLI',
        '{architecture_file}',
        '{blif_file}',
        '--net_file', '{net_file}',
        '--output_place_file', '{place_file}',
    ],
}

statistics_arguments = [
    '--input_place_file', '{place_file}',
]


def ascii_encode(string):
    if isinstance(string, unicode):
        return string.encode('ascii')
    else:
        return string

def ascii_encode_ordered_dict(data):
    return OrderedDict(map(ascii_encode, pair) for pair in data)


def substitute(command, substitutions):
    new_command = []

    for i in range(len(command)):
        argument = command[i]
        for key, value in substitutions.items():
            argument = argument.replace('{' + key + '}', value)

        new_command.append(argument)

    return new_command


def argument_sets(arguments):
        argument_names = arguments.keys()
        argument_values = itertools.product(*arguments.values())

        for argument_value in argument_values:
            argument_set = []
            for i in range(len(argument_names)):
                argument_set += [str(argument_names[i]), str(argument_value[i])]

            yield argument_set


def call(command, stats, regexes):
    process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = process.communicate()
    out = out.decode('utf-8')
    err = err.decode('utf-8')

    if err:
        print(err)
        print('There was a problem with command "{0}"'.format(' '.join(command)))
        sys.exit(1)

    for line in out.splitlines():
        for i in range(len(regexes)):
            match = re.search(regexes[i], line)
            if match:
                stats[i] = match.group(1)


folder = os.path.join(os.path.split(os.getcwd())[1], sys.argv[1])
os.chdir('..')

config_path = os.path.join(folder, 'config.json')
config_file = open(config_path)
config = json.load(config_file, object_pairs_hook=ascii_encode_ordered_dict)

# Get some config options
placer = config['placer']
call_statistics = (placer == 'vpr')

route = config['route']
architecture_file = config['architecture']
blif_file = config['blif_file']
net_file = config['net_file']

stat_names = [str(k) for k in config['stats'].keys()]
stat_regexes = [str(v) for v in config['stats'].values()]
num_stats = len(stat_names)

# Build the base command
base_command = substitute(base_commands[placer], {
    'architecture_file': architecture_file,
    'blif_file': blif_file,
    'net_file': net_file,
})
base_stats_command = substitute(base_commands['java'], {
    'architecture_file': architecture_file,
    'blif_file': blif_file,
    'net_file': net_file,
})

# Get the circuits
circuits = config['circuits']
if not isinstance(circuits, list):
    circuits = str(circuits).split(' ')

# Get the arguments
arguments = config['arguments']
for argument in arguments:
    if not isinstance(arguments[argument], list):
        arguments[argument] = [arguments[argument]]


# Loop over the different combinations of arguments
argument_sets = list(argument_sets(arguments))
num_iterations = len(argument_sets)

summary_file = open(os.path.join(folder, 'summary.csv'), 'w')
summary_writer = csv.writer(summary_file)

for iteration in range(num_iterations):

    # Create the output folder and stats file
    iteration_name = 'arguments' + str(iteration)
    output_folder = os.path.join(folder, iteration_name)
    if not os.path.isdir(output_folder):
        os.mkdir(output_folder)

    stats_file = open(os.path.join(output_folder, 'stats.csv'), 'w')
    stats_writer = csv.writer(stats_file)
    stats_writer.writerow(['circuit'] + stat_names)
    geomeans = [1] * num_stats

    # Build the complete command (without circuit substituted)
    argument_set = argument_sets[iteration]
    place_file = os.path.join(output_folder, '{circuit}.place')
    route_file = os.path.join(output_folder, '{circuit}.route')
    substitutions = {
        'place_file': place_file},
        'route_file': route_file}
    }

    command = substitute(base_command + argument_set, substitutions)
    if call_statistics:
        stats_command = substitute(base_stats_command + statistics_arguments, substitutions)


    print(iteration_name + ': ' + ' '.join(argument_set))


    # Loop over all circuits
    for circuit in circuits:
        print('    ' + circuit)

        # Call the placer and get statistics
        circuit_stats = [None] * len(stat_names)
        circuit_command = substitute(command, {'circuit': circuit})
        call(circuit_command, circuit_stats, stat_regexes)

        # If necessary: let java calculate correct statistics
        if call_statistics:
            circuit_stats_command = substitute(stats_command, {'circuit': circuit})
            call(circuit_stats_command, circuit_stats, stat_regexes)

        for i in range(num_stats):
            geomeans[i] *= float(circuit_stats[i])

        stats_writer.writerow([circuit] + circuit_stats)

    for i in range(num_stats):
        geomeans[i] = str(math.pow(geomeans[i], 1.0 / len(circuits)))

    stats_writer.writerow([''] + geomeans)
    summary_writer.writerow([iteration_name] + geomeans)

    empty_row = [''] * (num_stats + 1)
    stats_writer.writerow(empty_row + [' '.join(argument_set)])
    stats_writer.writerow(empty_row + [' '.join(command)])
    if call_statistics:
        stats_writer.writerow(empty_row + [' '.join(stats_command)])

    stats_file.close()


summary_file.close()
