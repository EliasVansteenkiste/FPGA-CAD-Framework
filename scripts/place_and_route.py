import subprocess
import os
import re
import csv
import glob
import sys
import errno


def silentremove(filename):
    try:
        os.remove(filename)
    except OSError as e: # this would be "except OSError, e:" before Python 2.6
        if e.errno != errno.ENOENT: # errno.ENOENT = no such file or directory
            raise # re-raise exception if a different error occured


class Caller:
    circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
    circuits = 'stereovision3'

    def __init__(self, base_folder):
        self.circuit_list = self.circuits.split(' ')

        self.base_folder = base_folder
        if self.base_folder[-1] != '/':
            self.base_folder += '/'


    def get_folder(self, architecture, placers=None):
        if placers is None:
            place_string = ''

        else:
            place_string = '_place-'

            if isinstance(placers, str):
                place_string += placers
            else:
                place_string += '+'.join(placers)


        return self.base_folder + architecture + place_string + '/'


    def print_message(self, message, input_placers):
        if isinstance(input_placers, str):
            placers = [input_placers]
        else:
            placers = input_placers

        print(message + '+'.join(placers))


    def call(self, command):
        p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

        # Return output
        out, err = p.communicate()
        return out.decode('utf-8'), err.decode('utf-8')


    def call_all(self, options, output_file):

        stats_pattern = re.compile(self.stats_regex, re.DOTALL)

        # Open the output csv file and write the header
        output_folder = os.path.dirname(output_file)
        if not os.path.exists(output_folder):
            os.mkdir(output_folder)

        _file = open(output_file, 'w')
        csv_file = csv.writer(_file)

        num_columns = len(self.stats_columns)
        options_string = ' '.join(options)
        csv_file.writerow([''] * (1 + num_columns) + [options_string])
        csv_file.writerow(['benchmark'] + self.stats_columns)

        # Loop through all circuits
        for circuit in self.circuit_list:
            # Call the java placer
            print('  ' + circuit)
            base_command = self.get_base_command()
            circuit_options = self.get_circuit_options(circuit)

            command = base_command + options + circuit_options
            out, err = self.call(command)

            # If there is an error: print it and exit
            if err:
                print('There was a problem with circuit "{0}":'.format(circuit))
                print(err)
                sys.exit(1)


            # Get and print statistics
            match = stats_pattern.search(out)

            if match is None:
                print(out)
                print("Failed to match pattern: " + self.stats_regex)
                sys.exit(1)

            row = [circuit]
            for column in self.stats_columns:
                group_name = column.lower().replace(' ', '_')
                row.append(match.group(group_name))

            csv_file.writerow(row)
            _file.flush()

            self.post_process_circuit(circuit)

        _file.close()



class StatisticsCaller(Caller):

    stats_columns = ['time', 'BB cost', 'max delay']
    stats_regex = r'.*time:\s+(?P<time>[0-9.e+-]+).*\s+BB cost:\s+(?P<bb_cost>[0-9.e+-]+).*\s+max delay:\s+(?P<max_delay>[0-9.e+-]+)'

    def place_all(self, architecture, input_folder, output_folder):
        print("Printing statistics")

        self.input_folder = self.base_folder + input_folder
        self.output_folder = self.base_folder + output_folder

        options = [
            '--architecture', 'architectures/' + architecture + '.json',
            '--input', self.output_folder,
            '--output', self.output_folder
        ]

        output_file = self.output_folder + 'results_place.csv'

        self.call_all(options, output_file)


    def get_base_command(self):
        return [
            'java',
            '-cp',
            'bin:dependencies/args4j-2.32.jar:dependencies/json-simple-1.1.1.jar',
            'cli.CLI'
        ]


    def get_circuit_options(self, circuit):
        return ['--net_file', self.input_folder + circuit + '.net']

    def post_process_circuit(self, circuit):
        pass


class VPRPlaceCaller(Caller):

    stats_columns = ['time', 'BB cost', 'max delay']
    stats_regex = r'(Placement estimated critical path delay: (?P<max_delay>[0-9.e+-]+) ns)?.*bb_cost: (?P<bb_cost>[0-9.e+-]+),.*Placement took (?P<time>[0-9.e+-]+) seconds'

    def place_all(self, architecture, input_folder, output_folder, options=[]):
        print("Placing with vpr to " + output_folder)

        self.input_folder = self.base_folder + input_folder
        self.output_folder = self.base_folder + output_folder
        self.options = options

        architecture_file = self.input_folder + architecture + '.xml'
        output_file = self.output_folder + 'results_place_vpr.csv'

        self.call_all([architecture_file], output_file)


        # Print statistics
        statistics_caller = StatisticsCaller(self.base_folder)
        architecture = '10fle'
        statistics_caller.place_all(architecture, input_folder, output_folder)


    def get_base_command(self):
        return ['../../vtr/vpr/vpr']


    def get_circuit_options(self, circuit):
        return [
            circuit,
            '--place',
            '--blif_file', self.input_folder + circuit + '.blif',
            '--net_file', self.input_folder + circuit + '.net',
            '--place_file', self.output_folder + circuit + '.place',
        ] + self.options


    def post_process_circuit(self, circuit):
        silentremove('vpr_stdout.log')




class PlaceCaller(Caller):

    stats_columns = ['time', 'BB cost', 'max delay']
    stats_regex = r'.*time:\s+(?P<time>[0-9.e+-]+).*\s+BB cost:\s+(?P<bb_cost>[0-9.e+-]+).*\s+max delay:\s+(?P<max_delay>[0-9.e+-]+)'


    def place_all(self, architecture, placers, input_options=None):
        self.print_message('Placing with ', placers)

        # Set easy stuff
        self.net_folder = self.get_folder(architecture)
        output_folder = self.get_folder(architecture, placers)
        architecture_file = 'architectures/' + architecture + '.json'
        options = []

        # Set the optional input placer file, and the current placer
        if isinstance(placers, str):
            placer = placers
        else:
            placer = placers[1]
            input_place_folder = self.get_folder(architecture, placers[0])
            options += ['--input', input_place_folder]

        options += [
            '--architecture', architecture_file,
            '--output', output_folder,
            '--placer', placer
        ]
        if input_options is not None:
            options += input_options

        # Run the caller
        output_file = output_folder + 'results_place.csv'
        self.call_all(options, output_file)


    def get_base_command(self):
        return [
            'java',
            '-cp',
            'bin:dependencies/args4j-2.32.jar:dependencies/json-simple-1.1.1.jar',
            'cli.CLI'
        ]


    def get_circuit_options(self, circuit):
        return ['--net_file', self.net_folder + circuit + '.net']

    def post_process_circuit(self, circuit):
        pass




class RouteCaller(Caller):

    stats_columns = ['time', 'BB cost', 'max delay']
    stats_regex = r'Total wirelength: (?P<bb_cost>[0-9.e+-]+),.*Final critical path: (?P<max_delay>[0-9.e+-]+) ns.*Routing took (?P<time>[0-9.e+-]+) seconds'


    def route_all(self, architecture, placers):
        self.print_message('Routing from ', placers)

        # Set folders
        self.net_folder = self.get_folder(architecture)
        self.place_folder = self.get_folder(architecture, placers)
        self.route_folder = self.place_folder

        # Get the architecture file
        arch_files = glob.glob(self.net_folder + '/*.xml')
        if len(arch_files) != 1:
            print('Less or more than 1 architecture file found in input folder')
            sys.exit(1)
        arch_file = arch_files[0]

        options = [arch_file]

        # Run the caller
        output_file = self.route_folder + 'results_route.csv'
        self.call_all(options, output_file)


    def get_base_command(self):
        return ['../../vtr/vpr/vpr']


    def get_circuit_options(self, circuit):
        return [
            circuit,
            '--route',
            '--blif_file', self.net_folder + circuit + '.blif',
            '--net_file', self.net_folder + circuit + '.net',
            '--place_file', self.place_folder + circuit + '.place',
            '--route_file', self.route_folder + circuit + '.route',
            '--route_chan_width', '300'
        ]

    def post_process_circuit(self, circuit):
        os.remove(circuit + '.critical_path.out')
        os.remove(circuit + '.slack.out')
        os.remove('vpr_stdout.log')
