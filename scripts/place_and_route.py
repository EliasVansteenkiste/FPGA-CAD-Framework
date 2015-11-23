import subprocess

import errno
import numpy
import copy

import re
import csv

import sys
import os
import glob
import shutil


def silentremove(filename):
    try:
        os.remove(filename)
    except OSError as e:
        if e.errno != errno.ENOENT: # errno.ENOENT = no such file or directory
            raise # re-raise exception if a different error occured


class Caller:

    def __init__(self, circuits):
        self.circuits = circuits.split(' ')


    def call_circuit(self, command, circuit):
        circuit_command = copy.deepcopy(command)
        for i in range(len(circuit_command)):
            circuit_command[i] = circuit_command[i].replace('{circuit}', circuit)

        p = subprocess.Popen(circuit_command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # Return output
        out, err = p.communicate()
        return out.decode('utf-8'), err.decode('utf-8')


    def call_all_circuits(self, command):

        stats_pattern = re.compile(self.stats_regex, re.DOTALL)
        self.results = {}
        self.command = command

        for circuit in self.circuits:
            out, err = self.call_circuit(command, circuit)

            if err:
                print(err)
                print('There was a problem with circuit "{0}"'.format(circuit))
                sys.exit(1)

            # Get and print statistics
            match = stats_pattern.search(out)

            if match is None:
                print(out)
                print('Failed to match pattern: {0}'.format(self.stats_regex))
                sys.exit(1)

            result = {}
            for metric in self.metrics:
                group_name = metric.lower().replace(' ', '_')
                result[metric] = match.group(group_name)

            results[circuit] = result


    def save_results(self, filename, append=False):
        if append:
            mode = 'a'
        else:
            mode = 'w'

        _file = open(output_file, 'w')
        csv_writer = csv.writer(_file)

        # Print the header
        num_metrics = len(self.metrics)
        row = [''] * (1 + num_metrics)
        row.append(' '.join(self.command))
        csv_file.writerow(row)
        csv_file.writerow(['benchmark'] + self.metrics)

        # Print the results for each
        for circuit in sorted(self.results):
            result = self.results[circuit]

            row = [circuit]
            for column in self.stats_columns:
                group_name = column.lower().replace(' ', '_')
                row.append(match.group(group_name))

            csv_writer.writerow(row)
            _file.flush()

        csv_writer.writerow([])
        _file.close()


    def get_geomeans(self):
        geomeans = []
        for metric in self.metrics:
            geomeans.append(self.get_geomean(metric))

        return geomeans

    def get_geomean(self, metric, circuits=None):
        if circuits is None:
            circuits = self.circuits

        metric_results = []
        for circuit in circuits:
            metric_results.append(self.results[circuit][metric])

        return numpy.gmean(metric_results)



class PlaceCaller(Caller):

    stats_metrics = ['Runtime', 'BB cost', 'Max delay']
    stats_regex = r'.*time\s+|\s+(?P<runtime>[0-9.e+-]+).*BB cost\s+|\s+(?P<bb_cost>[0-9.e+-]+).*max delay\s+|\s+(?P<max_delay>[0-9.e+-]+)'

    def __init__(self, architecture, circuits_folder, circuits):
        super().__init__(circuits)

        self.architecture = architecture
        self.circuit = os.path.join(circuits_folder, '{circuit}.blif')

    def place_all(self, options, results_file):

        command = [
            'java',
            '-cp', 'bin',
            'interfaces.CLI',
            self.architecture,
            self.circuit,
            '--output_place_file', 'tmp/{circuit}.place'
        ]

        command += options

        self.call_all_circuits(command)
        self.save_results(results_file)

        shutil.rmtree('tmp')
